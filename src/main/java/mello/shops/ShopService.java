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
import org.bukkit.*;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Chest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Instant;
import java.util.*;
import java.util.logging.Logger;

/**
 * Orquestra o fluxo de lojas (pessoal, clã e reino), incluindo NPC, estoque
 * físico e GUIs de compra/edição.
 */
public class ShopService {

    private static final ChatColor PERSONAL_COLOR = ChatColor.AQUA;
    private static final ChatColor CLAN_COLOR = ChatColor.GREEN;
    private static final ChatColor KINGDOM_COLOR = ChatColor.GOLD;

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
    private final Map<String, Shop> shopsByChest = new HashMap<>();

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

    public void warmUpCache() {
        shopsByNpc.clear();
        itemsByShop.clear();
        shopsByChest.clear();
        for (Shop shop : storage.getShops()) {
            shopsByNpc.put(shop.getNpcUuid(), shop);
            itemsByShop.put(shop.getId(), new ArrayList<>(storage.getItems(shop.getId())));
            registerChest(shop);
            ensureNpcExists(shop);
        }
    }

    public OperationResult createPersonalShop(Player player, String name) {
        return createShop(player, ShopType.PERSONAL, name);
    }

    public OperationResult createClanShop(Player player, String name) {
        return createShop(player, ShopType.CLAN, name);
    }

    public OperationResult createKingdomShop(Player player, String name) {
        return createShop(player, ShopType.KINGDOM, name);
    }

    private OperationResult createShop(Player player, ShopType type, String shopName) {
        String trimmedName = shopName == null ? "" : shopName.trim();
        if (trimmedName.isEmpty()) {
            return OperationResult.fail("Informe um nome para a loja.");
        }

        UUID playerId = player.getUniqueId();
        Clan clan = clanService.getByMember(playerId);
        Kingdom kingdom = kingdomService.getByMember(playerId);
        Optional<ClaimContext> claimContextOpt = claimService.findClaimAt(player.getLocation());

        OperationResult validation = validateCreation(player, type, clan, kingdom, claimContextOpt.orElse(null));
        if (!validation.success()) {
            return validation;
        }

        Location spawnLocation = player.getLocation().getBlock().getLocation().add(0.5, 0, 0.5);
        float facingYaw = player.getLocation().getYaw() + 180; // aldeão olha para o criador
        spawnLocation.setYaw(facingYaw);
        spawnLocation.setPitch(0);
        BlockFace facing = yawToFace(spawnLocation.getYaw());

        Villager villager = spawnShopKeeper(spawnLocation, type, buildDisplayName(type, trimmedName, player.getName(), clan, kingdom));

        ChestPlacement placement = placeDoubleChest(spawnLocation, facing);
        if (!placement.success()) {
            villager.remove();
            return OperationResult.fail(placement.message());
        }

        UUID shopId = UUID.randomUUID();
        villager.getPersistentDataContainer().set(shopKey, PersistentDataType.STRING, shopId.toString());

        Shop shop = new Shop(shopId,
                playerId,
                type,
                clan != null ? clan.getLeader() : null,
                kingdom != null ? kingdom.getKing() : null,
                trimmedName,
                spawnLocation,
                villager.getUniqueId(),
                placement.primary(),
                placement.secondary(),
                true,
                Instant.now(),
                Instant.now());

        shopsByNpc.put(villager.getUniqueId(), shop);
        itemsByShop.put(shopId, new ArrayList<>());
        registerChest(shop);
        storage.saveShop(shop, itemsByShop.get(shopId));
        new ShopEditGui(player, this, guiManager, shop).open();
        return OperationResult.ok("Loja criada com sucesso!");
    }

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
            }
        }
        return Optional.ofNullable(shopsByNpc.get(entity.getUniqueId()));
    }

    public Optional<Shop> getShopByChest(Block block) {
        if (block == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(shopsByChest.get(blockKey(block.getLocation())));
    }

    public List<ShopItem> getItems(Shop shop) {
        return itemsByShop.getOrDefault(shop.getId(), Collections.emptyList());
    }

    public void openEdit(Player player, Shop shop) {
        new ShopEditGui(player, this, guiManager, shop).open();
    }

    public OperationResult deleteByName(Player player, String rawName) {
        String name = rawName == null ? "" : rawName.trim();
        if (name.isEmpty()) {
            return OperationResult.fail("Informe o nome da loja a ser removida.");
        }

        UUID ownerId = player.getUniqueId();
        List<Shop> ownedShops = new ArrayList<>(storage.getShops()).stream()
                .filter(shop -> shop.getOwnerId().equals(ownerId))
                .filter(shop -> shop.getName().equalsIgnoreCase(name))
                .toList();

        if (ownedShops.isEmpty()) {
            return OperationResult.fail("Nenhuma loja sua com esse nome foi encontrada.");
        }

        ownedShops.forEach(this::deleteShop);
        return OperationResult.ok(ownedShops.size() + " loja(s) removida(s). Estoque descartado.");
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
        ShopItem newItem = new ShopItem(UUID.randomUUID(), shop.getId(), setup.slot(), itemStack, setup.price(), quantityPerClick);
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
        Optional<Inventory> inventoryOpt = resolveStockInventory(shop);
        if (inventoryOpt.isEmpty()) {
            buyer.sendMessage("§cO estoque desta loja está inacessível.");
            return;
        }

        Inventory stock = inventoryOpt.get();
        int requiredAmount = shopItem.getQuantityPerClick();
        ItemStack template = shopItem.getItem().clone();
        template.setAmount(1);

        if (!hasStock(stock, template, requiredAmount)) {
            buyer.sendMessage("§cEstoque insuficiente para esta compra.");
            return;
        }

        UUID buyerId = buyer.getUniqueId();
        if (!economyService.hasEnough(buyerId, shopItem.getPrice())) {
            buyer.sendMessage("§cSaldo insuficiente para esta compra.");
            return;
        }

        ItemStack toGive = shopItem.getItem().clone();
        toGive.setAmount(requiredAmount);
        if (!canFit(buyer, toGive)) {
            buyer.sendMessage("§cVocê precisa de espaço no inventário para comprar.");
            return;
        }

        if (!economyService.withdraw(buyerId, shopItem.getPrice(), MoneyTransactionType.PLAYER_TRADE, "Compra em loja")) {
            buyer.sendMessage("§cNão foi possível processar a compra agora. Tente novamente.");
            return;
        }

        economyService.deposit(shop.getOwnerId(), shopItem.getPrice(), MoneyTransactionType.PLAYER_TRADE, "Venda em loja");
        removeFromStock(stock, template, requiredAmount);
        buyer.getInventory().addItem(toGive);

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

    public String getDisplayName(Shop shop) {
        return buildDisplayName(shop);
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    private OperationResult validateCreation(Player player, ShopType type, Clan clan, Kingdom kingdom, ClaimContext context) {
        UUID playerId = player.getUniqueId();

        if (type == ShopType.CLAN) {
            if (clan == null || !playerId.equals(clan.getLeader())) {
                return OperationResult.fail("Apenas o líder do clã pode criar esta loja.");
            }
        }

        if (type == ShopType.KINGDOM) {
            if (kingdom == null || !playerId.equals(kingdom.getKing())) {
                return OperationResult.fail("Apenas o líder do reino pode criar esta loja.");
            }
        }

        if (context != null && !claimService.isMember(playerId, context)) {
            return OperationResult.fail("Você precisa estar em uma área onde tenha permissão para construir.");
        }

        if (type == ShopType.CLAN) {
            if (context == null || !context.isClanClaim() || context.getClan() == null || !context.getClan().getName().equalsIgnoreCase(clan.getName())) {
                return OperationResult.fail("Lojas de clã só podem ser criadas dentro do claim do seu clã.");
            }
        }

        if (type == ShopType.KINGDOM) {
            if (context == null || !context.isKingdomClaim() || context.getKingdom() == null || !context.getKingdom().getKing().equals(kingdom.getKing())) {
                return OperationResult.fail("Lojas de reino precisam ser criadas dentro do claim do seu reino.");
            }
        }

        if (type == ShopType.PERSONAL && context == null) {
            return OperationResult.fail("Você precisa estar em um claim autorizado para criar uma loja pessoal.");
        }

        return OperationResult.ok("");
    }

    private Villager spawnShopKeeper(Location location, ShopType type, String displayName) {
        location.getChunk().load();
        Villager villager = (Villager) location.getWorld().spawnEntity(location, EntityType.VILLAGER);
        villager.setAI(false);
        villager.setCollidable(false);
        villager.setGravity(false);
        villager.setInvulnerable(true);
        villager.setPersistent(true);
        villager.setRemoveWhenFarAway(false);
        villager.setAdult();
        villager.setRotation(location.getYaw(), location.getPitch());
        villager.setCustomName(displayName);
        villager.setCustomNameVisible(true);
        villager.setVillagerExperience(0);

        if (type == ShopType.CLAN) {
            villager.setProfession(Villager.Profession.FARMER);
            villager.setVillagerType(Villager.Type.SAVANNA);
        } else if (type == ShopType.KINGDOM) {
            villager.setProfession(Villager.Profession.LIBRARIAN);
            villager.setVillagerType(Villager.Type.SNOW);
        } else {
            villager.setProfession(Villager.Profession.NONE);
            villager.setVillagerType(Villager.Type.PLAINS);
        }
        return villager;
    }

    private void ensureNpcExists(Shop shop) {
        Entity current = Bukkit.getEntity(shop.getNpcUuid());
        if (current instanceof Villager villager && villager.isValid()) {
            villager.getPersistentDataContainer().set(shopKey, PersistentDataType.STRING, shop.getId().toString());
            shopsByNpc.put(villager.getUniqueId(), shop);
            return;
        }
        Villager spawned = spawnShopKeeper(shop.getLocation(), shop.getType(), buildDisplayName(shop));
        spawned.getPersistentDataContainer().set(shopKey, PersistentDataType.STRING, shop.getId().toString());
        shop.setNpcUuid(spawned.getUniqueId());
        storage.saveShop(shop, itemsByShop.getOrDefault(shop.getId(), Collections.emptyList()));
        shopsByNpc.put(spawned.getUniqueId(), shop);
        logger.info("[Shop] NPC recriado para loja " + shop.getId());
    }

    private void deleteShop(Shop shop) {
        removeNpc(shop);
        clearStock(shop);
        unregisterChest(shop);
        shopsByNpc.remove(shop.getNpcUuid());
        itemsByShop.remove(shop.getId());
        storage.removeShop(shop.getId());
    }

    private void removeNpc(Shop shop) {
        if (shop.getNpcUuid() == null) {
            return;
        }
        Entity entity = Bukkit.getEntity(shop.getNpcUuid());
        if (entity != null) {
            entity.remove();
        }
    }

    private void clearStock(Shop shop) {
        removeChestAt(shop.getPrimaryChestLocation());
        removeChestAt(shop.getSecondaryChestLocation());
    }

    private void removeChestAt(Location location) {
        if (location == null) {
            return;
        }
        Block block = location.getBlock();
        if (block.getState() instanceof org.bukkit.block.Chest chestState) {
            chestState.getBlockInventory().clear();
        }
        block.setType(Material.AIR, false);
        shopsByChest.remove(blockKey(location));
    }

    private ChestPlacement placeDoubleChest(Location villagerLocation, BlockFace facing) {
        Block primary = villagerLocation.getBlock().getRelative(facing.getOppositeFace());
        Block secondary = primary.getRelative(rotateLeft(facing));

        if (!canPlaceChest(primary) || !canPlaceChest(secondary)) {
            return new ChestPlacement(false, "Espaço insuficiente atrás do aldeão para criar o baú do estoque", null, null);
        }

        primary.setType(Material.CHEST, false);
        secondary.setType(Material.CHEST, false);

        Chest primaryData = (Chest) primary.getBlockData();
        Chest secondaryData = (Chest) secondary.getBlockData();
        primaryData.setFacing(facing);
        secondaryData.setFacing(facing);
        primaryData.setType(Chest.Type.RIGHT);
        secondaryData.setType(Chest.Type.LEFT);
        primary.setBlockData(primaryData);
        secondary.setBlockData(secondaryData);

        return new ChestPlacement(true, "", primary.getLocation(), secondary.getLocation());
    }

    private boolean canPlaceChest(Block block) {
        Material type = block.getType();
        return type.isAir() || type == Material.CAVE_AIR || type == Material.VOID_AIR;
    }

    private void registerChest(Shop shop) {
        if (shop.getPrimaryChestLocation() != null) {
            shopsByChest.put(blockKey(shop.getPrimaryChestLocation()), shop);
        }
        if (shop.getSecondaryChestLocation() != null) {
            shopsByChest.put(blockKey(shop.getSecondaryChestLocation()), shop);
        }
    }

    private void unregisterChest(Shop shop) {
        if (shop.getPrimaryChestLocation() != null) {
            shopsByChest.remove(blockKey(shop.getPrimaryChestLocation()));
        }
        if (shop.getSecondaryChestLocation() != null) {
            shopsByChest.remove(blockKey(shop.getSecondaryChestLocation()));
        }
    }

    private Optional<Inventory> resolveStockInventory(Shop shop) {
        Location chestLocation = shop.getPrimaryChestLocation();
        if ((chestLocation == null || !(chestLocation.getBlock().getState() instanceof org.bukkit.block.Chest))
                && shop.getSecondaryChestLocation() != null) {
            chestLocation = shop.getSecondaryChestLocation();
        }
        if (chestLocation == null) {
            return Optional.empty();
        }
        Block block = chestLocation.getBlock();
        if (!(block.getState() instanceof org.bukkit.block.Chest chest)) {
            return Optional.empty();
        }
        return Optional.ofNullable(chest.getInventory());
    }

    private boolean hasStock(Inventory inventory, ItemStack template, int requiredAmount) {
        int remaining = requiredAmount;
        for (ItemStack content : inventory.getContents()) {
            if (content == null || content.getType().isAir()) {
                continue;
            }
            if (content.isSimilar(template)) {
                remaining -= content.getAmount();
                if (remaining <= 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private void removeFromStock(Inventory inventory, ItemStack template, int requiredAmount) {
        int remaining = requiredAmount;
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack content = inventory.getItem(slot);
            if (content == null || content.getType().isAir() || !content.isSimilar(template)) {
                continue;
            }
            int toRemove = Math.min(content.getAmount(), remaining);
            int newAmount = content.getAmount() - toRemove;
            if (newAmount <= 0) {
                inventory.setItem(slot, null);
            } else {
                content.setAmount(newAmount);
                inventory.setItem(slot, content);
            }
            remaining -= toRemove;
            if (remaining <= 0) {
                return;
            }
        }
    }

    private boolean canFit(Player player, ItemStack stack) {
        int remaining = stack.getAmount();
        ItemStack[] contents = player.getInventory().getStorageContents();
        for (ItemStack content : contents) {
            if (content == null || content.getType().isAir()) {
                remaining -= stack.getMaxStackSize();
            } else if (content.isSimilar(stack) && content.getAmount() < content.getMaxStackSize()) {
                remaining -= (content.getMaxStackSize() - content.getAmount());
            }
            if (remaining <= 0) {
                return true;
            }
        }
        return false;
    }

    private String blockKey(Location location) {
        return location.getWorld().getName() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ();
    }

    private BlockFace yawToFace(float yaw) {
        int i = Math.round(yaw / 90f) & 3;
        return switch (i) {
            case 0 -> BlockFace.SOUTH;
            case 1 -> BlockFace.WEST;
            case 2 -> BlockFace.NORTH;
            default -> BlockFace.EAST;
        };
    }

    private BlockFace rotateLeft(BlockFace face) {
        return switch (face) {
            case NORTH -> BlockFace.WEST;
            case SOUTH -> BlockFace.EAST;
            case EAST -> BlockFace.NORTH;
            case WEST -> BlockFace.SOUTH;
            default -> BlockFace.WEST;
        };
    }

    private String buildDisplayName(Shop shop) {
        OfflinePlayer offline = Bukkit.getOfflinePlayer(shop.getOwnerId());
        String ownerName = offline.getName() != null ? offline.getName() : "Jogador";
        return buildDisplayName(shop.getType(), shop.getName(), ownerName,
                clanService.getByMember(shop.getOwnerId()), kingdomService.getByMember(shop.getOwnerId()));
    }

    private String buildDisplayName(ShopType type, String name, String ownerName, Clan clan, Kingdom kingdom) {
        return switch (type) {
            case CLAN -> CLAN_COLOR + resolveClanName(clan) + ChatColor.WHITE + ":" + CLAN_COLOR + name;
            case KINGDOM -> KINGDOM_COLOR + resolveKingdomName(kingdom) + ChatColor.WHITE + ":" + KINGDOM_COLOR + name;
            default -> PERSONAL_COLOR + "[" + ownerName + "] " + name;
        };
    }

    private String resolveClanName(Clan clan) {
        return clan != null ? clan.getName() : "Clã";
    }

    private String resolveKingdomName(Kingdom kingdom) {
        return kingdom != null ? kingdom.getName() : "Reino";
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

    private record ChestPlacement(boolean success, String message, Location primary, Location secondary) {
    }
}
