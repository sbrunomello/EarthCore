package mello.shops;

import mello.clans.Clan;
import mello.clans.ClanService;
import mello.common.OperationResult;
import mello.core.claims.ClaimContext;
import mello.core.claims.ClaimService;
import mello.core.gui.GuiManager;
import mello.economy.EconomyService;
import mello.economy.MoneyTransactionType;
import mello.kingdoms.Kingdom;
import mello.kingdoms.KingdomService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Instant;
import java.util.*;
import java.util.logging.Logger;

/**
 * Orquestra o fluxo de lojas pessoais: criação, associação com NPCs e controle
 * das GUIs de edição/compra.
 */
public class ShopService {

    private final JavaPlugin plugin;
    private final ShopStorage storage;
    private final EconomyService economyService;
    private final ClaimService claimService;
    private final ClanService clanService;
    private final KingdomService kingdomService;
    private final GuiManager guiManager;
    private final Logger logger;
    private final NamespacedKey shopKey;

    private final Map<UUID, Shop> shopsByNpc = new HashMap<>();
    private final Map<UUID, List<ShopItem>> itemsByShop = new HashMap<>();
    private final Map<UUID, PendingItemSetup> pendingPriceInput = new HashMap<>();

    /**
     * Constrói o serviço de lojas com todas as dependências injetadas. O cache
     * é pré-carregado para minimizar IO durante o uso in-game.
     */
    public ShopService(JavaPlugin plugin,
                       ShopStorage storage,
                       EconomyService economyService,
                       ClaimService claimService,
                       ClanService clanService,
                       KingdomService kingdomService,
                       GuiManager guiManager,
                       Logger logger) {
        this.plugin = plugin;
        this.storage = storage;
        this.economyService = economyService;
        this.claimService = claimService;
        this.clanService = clanService;
        this.kingdomService = kingdomService;
        this.guiManager = guiManager;
        this.logger = logger;
        this.shopKey = new NamespacedKey(plugin, "shop-id");
        warmUpCache();
    }

    /**
     * Recarrega o cache de lojas e itens a partir do armazenamento, garantindo
     * que NPCs órfãos sejam recriados após reinícios do servidor.
     */
    public void warmUpCache() {
        shopsByNpc.clear();
        itemsByShop.clear();
        for (Shop shop : storage.getShops()) {
            shopsByNpc.put(shop.getNpcUuid(), shop);
            itemsByShop.put(shop.getId(), new ArrayList<>(storage.getItems(shop.getId())));
            ensureNpcExists(shop);
        }
    }

    /**
     * Cria uma nova loja vinculada a um vilarejo NPC, validando liderança e
     * localização dentro de claims do jogador.
     */
    public OperationResult createShop(Player player) {
        UUID playerId = player.getUniqueId();
        if (!isLeader(playerId)) {
            return OperationResult.fail("Apenas líderes de clã ou reino podem criar uma loja pessoal.");
        }

        Optional<ClaimContext> claimContext = claimService.findClaimAt(player.getLocation());
        if (claimContext.isEmpty()) {
            return OperationResult.fail("Você precisa estar em um claim do seu clã ou reino para criar a loja.");
        }

        if (!canCreateHere(playerId, claimContext.get())) {
            return OperationResult.fail("Somente é possível criar lojas dentro de claims do seu clã ou reino onde você seja líder.");
        }

        Location spawnLocation = player.getLocation().getBlock().getLocation().add(0.5, 0, 0.5);
        Villager villager = spawnShopKeeper(spawnLocation, player.getName());
        UUID shopId = UUID.randomUUID();
        villager.getPersistentDataContainer().set(shopKey, PersistentDataType.STRING, shopId.toString());
        Shop shop = new Shop(shopId, playerId, spawnLocation, villager.getUniqueId(), true, Instant.now(), Instant.now());
        shopsByNpc.put(villager.getUniqueId(), shop);
        itemsByShop.put(shopId, new ArrayList<>());
        storage.saveShop(shop, itemsByShop.get(shopId));
        new ShopEditGui(player, this, guiManager, shop).open();
        return OperationResult.ok("Loja criada com sucesso!");
    }

    /**
     * Resolve a loja associada a um NPC específico, lendo o identificador do
     * PersistentDataContainer e aplicando fallback para cache em memória.
     */
    public Optional<Shop> getShopByNpc(Entity entity) {
        if (entity == null || entity.getType() != EntityType.VILLAGER) {
            return Optional.empty();
        }
        String rawId = entity.getPersistentDataContainer().get(shopKey, PersistentDataType.STRING);
        if (rawId != null) {
            try {
                UUID id = UUID.fromString(rawId);
                return storage.getShop(id);
            } catch (IllegalArgumentException ignored) {
                // fallback to map
            }
        }
        return Optional.ofNullable(shopsByNpc.get(entity.getUniqueId()));
    }

    public List<ShopItem> getItems(Shop shop) {
        return itemsByShop.getOrDefault(shop.getId(), Collections.emptyList());
    }

    public void openEdit(Player player, Shop shop) {
        new ShopEditGui(player, this, guiManager, shop).open();
    }

    public void openBuy(Player player, Shop shop) {
        if (!shop.isEnabled()) {
            player.sendMessage("§cEsta loja está desativada no momento.");
            return;
        }
        new ShopBuyGui(player, guiManager, shop, this).open();
    }

    public void startItemSetup(Player player, Shop shop, int slot) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType().isAir()) {
            player.sendMessage("§cSegure o item que deseja vender na mão antes de configurar o slot.");
            return;
        }
        player.closeInventory();
        pendingPriceInput.put(player.getUniqueId(), new PendingItemSetup(shop.getId(), slot, hand.clone(), 0));
        player.sendMessage("§eDigite no chat o preço para vender " + formatItemName(hand) + " (apenas números positivos).");
    }

    public void handlePriceInput(Player player, String message) {
        PendingItemSetup setup = pendingPriceInput.get(player.getUniqueId());
        if (setup == null) {
            return;
        }
        double price;
        try {
            price = Double.parseDouble(message.replace(',', '.'));
        } catch (NumberFormatException ex) {
            player.sendMessage("§cValor inválido. Informe apenas números.");
            return;
        }
        if (price <= 0 || Double.isInfinite(price) || Double.isNaN(price)) {
            player.sendMessage("§cO preço precisa ser maior que zero.");
            return;
        }
        pendingPriceInput.put(player.getUniqueId(), setup.withPrice(price));
        player.sendMessage("§aPreço registrado! Agora escolha a quantidade por clique (1 ou 64).");
        new ShopQuantityGui(player, guiManager, this, setup.slot()).open();
    }

    public void finalizeItemSetup(Player player, int quantityPerClick) {
        PendingItemSetup setup = pendingPriceInput.remove(player.getUniqueId());
        if (setup == null) {
            player.sendMessage("§cNenhuma configuração de item pendente.");
            return;
        }
        if (quantityPerClick != ShopItem.QUANTITY_SINGLE && quantityPerClick != ShopItem.QUANTITY_STACK) {
            player.sendMessage("§cQuantidade inválida. Use 1 ou 64.");
            return;
        }
        Shop shop = storage.getShop(setup.shopId()).orElse(null);
        if (shop == null) {
            player.sendMessage("§cLoja não encontrada.");
            return;
        }
        ItemStack itemStack = setup.item().clone();
        itemStack.setAmount(Math.min(itemStack.getType().getMaxStackSize(), quantityPerClick));
        List<ShopItem> items = new ArrayList<>(itemsByShop.getOrDefault(shop.getId(), new ArrayList<>()));
        items.removeIf(existing -> existing.getSlot() == setup.slot());
        ShopItem newItem = new ShopItem(UUID.randomUUID(), shop.getId(), setup.slot(), itemStack, setup.price(), quantityPerClick, true, 0);
        items.add(newItem);
        itemsByShop.put(shop.getId(), items);
        storage.saveShop(shop, items);
        player.sendMessage("§aItem configurado com sucesso na loja!");
        openEdit(player, shop);
    }

    public void removeItem(Player player, Shop shop, int slot) {
        List<ShopItem> items = new ArrayList<>(itemsByShop.getOrDefault(shop.getId(), new ArrayList<>()));
        boolean removed = items.removeIf(item -> item.getSlot() == slot);
        if (!removed) {
            player.sendMessage("§cNão há item configurado nesse slot.");
            return;
        }
        itemsByShop.put(shop.getId(), items);
        storage.saveShop(shop, items);
        player.sendMessage("§aItem removido do slot " + slot + ".");
        openEdit(player, shop);
    }

    public void handlePurchase(Player buyer, Shop shop, ShopItem shopItem) {
        ItemStack toGive = shopItem.getItem().clone();
        toGive.setAmount(shopItem.getQuantityPerClick());

        if (!buyer.getInventory().addItem(toGive).isEmpty()) {
            buyer.sendMessage("§cVocê precisa de espaço no inventário para comprar.");
            return;
        }

        UUID buyerId = buyer.getUniqueId();
        if (!economyService.hasEnough(buyerId, shopItem.getPrice())) {
            buyer.sendMessage("§cSaldo insuficiente para esta compra.");
            return;
        }

        if (!economyService.withdraw(buyerId, shopItem.getPrice(), MoneyTransactionType.PLAYER_TRADE, "Compra em loja pessoal")) {
            buyer.sendMessage("§cNão foi possível processar a compra agora. Tente novamente.");
            return;
        }
        economyService.deposit(shop.getOwnerId(), shopItem.getPrice(), MoneyTransactionType.PLAYER_TRADE, "Venda em loja pessoal");
        buyer.sendMessage("§aCompra realizada por §f" + formatValue(shopItem.getPrice()) + "§a!");
        Optional.ofNullable(Bukkit.getPlayer(shop.getOwnerId()))
                .ifPresent(owner -> owner.sendMessage("§aVocê vendeu " + formatItemName(shopItem.getItem()) + " por §f" + formatValue(shopItem.getPrice())));
    }

    public boolean isPendingPriceInput(Player player) {
        return pendingPriceInput.containsKey(player.getUniqueId());
    }

    public void saveAll() {
        storage.saveAll();
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    private boolean isLeader(UUID playerId) {
        return isClanLeader(playerId) || isKingdomLeader(playerId);
    }

    private boolean isClanLeader(UUID playerId) {
        Clan clan = clanService.getByMember(playerId);
        return clan != null && playerId.equals(clan.getLeader());
    }

    private boolean isKingdomLeader(UUID playerId) {
        Kingdom kingdom = kingdomService.getByMember(playerId);
        return kingdom != null && playerId.equals(kingdom.getKing());
    }

    private boolean canCreateHere(UUID playerId, ClaimContext context) {
        if (context.isClanClaim()) {
            return context.getClan() != null && playerId.equals(context.getClan().getLeader());
        }
        if (context.isKingdomClaim()) {
            return (context.getKingdom() != null && playerId.equals(context.getKingdom().getKing()))
                    || (context.getClan() != null && playerId.equals(context.getClan().getLeader()));
        }
        return false;
    }

    private Villager spawnShopKeeper(Location location, String ownerName) {
        location.getChunk().load();
        Villager villager = (Villager) location.getWorld().spawnEntity(location, EntityType.VILLAGER);
        villager.setAI(false);
        villager.setCollidable(false);
        villager.setGravity(false);
        villager.setInvulnerable(true);
        villager.setPersistent(true);
        villager.setRemoveWhenFarAway(false);
        villager.setAdult();
        villager.setCustomName(ChatColor.GOLD + "Loja de " + ownerName);
        villager.setCustomNameVisible(true);
        villager.setVillagerExperience(0);
        villager.setProfession(Villager.Profession.NONE);
        return villager;
    }

    private void ensureNpcExists(Shop shop) {
        Entity current = Bukkit.getEntity(shop.getNpcUuid());
        if (current instanceof Villager villager && villager.isValid()) {
            villager.getPersistentDataContainer().set(shopKey, PersistentDataType.STRING, shop.getId().toString());
            shopsByNpc.put(villager.getUniqueId(), shop);
            return;
        }
        Villager spawned = spawnShopKeeper(shop.getLocation(), resolveOwnerName(shop.getOwnerId()));
        spawned.getPersistentDataContainer().set(shopKey, PersistentDataType.STRING, shop.getId().toString());
        shop.setNpcUuid(spawned.getUniqueId());
        storage.saveShop(shop, itemsByShop.getOrDefault(shop.getId(), Collections.emptyList()));
        shopsByNpc.put(spawned.getUniqueId(), shop);
        logger.info("[Shop] NPC recriado para loja " + shop.getId());
    }

    private String resolveOwnerName(UUID ownerId) {
        String name = Optional.ofNullable(Bukkit.getOfflinePlayer(ownerId).getName()).orElse("jogador");
        return name;
    }

    private String formatItemName(ItemStack item) {
        String display = item.hasItemMeta() && item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : item.getType().name();
        return ChatColor.stripColor(display);
    }

    private String formatValue(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private record PendingItemSetup(UUID shopId, int slot, ItemStack item, double price) {
        PendingItemSetup withPrice(double price) {
            return new PendingItemSetup(shopId, slot, item, price);
        }
    }
}
