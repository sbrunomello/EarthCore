package mello.core.gui;

import mello.jobs.JobService;
import mello.jobs.gui.JobSelectGui;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Menu principal com atalhos para os comandos mais usados do servidor.
 * Cada item dispara o comando correspondente ou abre a GUI específica
 * sem que o jogador precise decorar textos ou parâmetros.
 */
public class MainMenuGui extends AbstractGui {

    private final GuiMessages guiMessages;
    private final JobService jobService;

    public MainMenuGui(Player player, GuiManager guiManager, GuiMessages guiMessages, JobService jobService) {
        super(player, guiManager, 45, "&8Menu Principal");
        this.guiMessages = guiMessages;
        this.jobService = jobService;
    }

    @Override
    protected void build() {

        setActionItem(10, Material.EMERALD, "&aEconomia",
                List.of("&7Veja seu saldo", "&7ou pague jogadores."),
                () -> dispatchAndClose("balance"));

        setActionItem(12, GuiTheme.JOBS.getTitleIcon(), "&eJobs",
                List.of("&7Escolha ou troque seu job", "&7direto pela interface."),
                this::openJobSelector);

        setActionItem(14, GuiTheme.KINGDOM.getTitleIcon(), "&6Reinos",
                List.of("&7Gerencie convites, claims", "&7e impostos do seu reino."),
                () -> dispatchAndClose("kingdom"));

        setActionItem(16, GuiTheme.CLAN.getTitleIcon(), "&bClãs",
                List.of("&7Convites, banco e informações", "&7do seu clã em um clique."),
                () -> dispatchAndClose("clan"));

        setActionItem(19, GuiTheme.CLAIM.getTitleIcon(), "&6Claims",
                List.of("&7Abra o fluxo guiado", "&7para proteger seu terreno."),
                () -> dispatchAndClose("claim"));

        setActionItem(21, GuiTheme.SHOP.getTitleIcon(), "&dLojas",
                List.of("&7Crie ou edite sua loja", "&7pessoal rapidamente."),
                () -> dispatchAndClose("shop"));


        setActionItem(25, Material.PAPER, "&fChat",
                List.of("&7Troque de canal", "&7global, local, clã ou cidade."),
                () -> dispatchAndClose("chat help"));

        setActionItem(28, Material.COMPASS, "&aSpawn",
                List.of("&7Teleporte imediato", "&7para o spawn global."),
                () -> dispatchAndClose("spawn"));

        setActionItem(30, Material.RED_BED, "&cHome",
                List.of("&7Vá para sua home", "&7ou defina uma nova."),
                () -> dispatchAndClose("home"));

        setActionItem(32, Material.ENDER_PEARL, "&5Pedidos de TP",
                List.of("&7Use /tpa <jogador> para ir", "&7ou /tphere <jogador> para puxar.",
                        "&7Responda com /tpa y ou /tpa n."),
                this::sendTeleportHelp);

        setActionItem(34, Material.WRITABLE_BOOK, "&eMensagens",
                List.of("&7Envie /msg <jogador> ...", "&7ou use /reply para responder."),
                this::sendMessageHelp);

        setActionItem(23, Material.CHEST, "&6Starter Kit",
                List.of("&7Resgate itens iniciais", "&7quando necessário."),
                () -> dispatchAndClose("starterkit"));

        setCloseButton(44);
    }

    private void setActionItem(int slot, Material material, String title, List<String> lore, Runnable action) {
        setItem(slot, styledItem(material, title, lore));
        registerClickAction(slot, action);
    }

    private void dispatchAndClose(String command) {
        player.closeInventory();
        boolean dispatched = player.performCommand(command);
        if (!dispatched) {
            player.sendMessage(ChatColor.RED + "Não foi possível executar o comando: /" + command);
        }
    }

    private void openJobSelector() {
        player.closeInventory();
        new JobSelectGui(player, guiManager, jobService, guiMessages).open();
    }

    private void sendTeleportHelp() {
        player.closeInventory();
        player.sendMessage(ChatColor.YELLOW + "Teleportes rápidos:");
        player.sendMessage(ChatColor.GRAY + "/tpa <jogador> " + ChatColor.WHITE + "- pede para ir até alguém");
        player.sendMessage(ChatColor.GRAY + "/tphere <jogador> " + ChatColor.WHITE + "- pede para trazer alguém até você");
        player.sendMessage(ChatColor.GRAY + "/tpa y | /tpa n " + ChatColor.WHITE
                + "- aceita ou recusa o último pedido recebido");
    }

    private void sendMessageHelp() {
        player.closeInventory();
        player.sendMessage(ChatColor.YELLOW + "Mensagens privadas:");
        player.sendMessage(ChatColor.GRAY + "/msg <jogador> <mensagem> " + ChatColor.WHITE + "- envia uma mensagem direta");
        player.sendMessage(ChatColor.GRAY + "/reply <mensagem> " + ChatColor.WHITE + "- responde para o último contato");
    }
}
