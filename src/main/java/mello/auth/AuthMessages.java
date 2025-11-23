package mello.auth;

import org.bukkit.ChatColor;

/**
 * Mensagens centralizadas para o módulo de autenticação para manter
 * consistência e facilitar futuras traduções/configurações.
 */
public final class AuthMessages {

    private AuthMessages() {
    }

    public static final String REGISTER_USAGE = ChatColor.YELLOW + "Use /register <senha> <senha>";
    public static final String LOGIN_USAGE = ChatColor.YELLOW + "Use /login <senha>";
    public static final String ALREADY_REGISTERED = ChatColor.RED + "Você já está registrado. Use /login <senha>.";
    public static final String NOT_REGISTERED = ChatColor.RED + "Você ainda não está registrado. Use /register <senha> <senha>.";
    public static final String PASSWORD_MISMATCH = ChatColor.RED + "As senhas não conferem.";
    public static final String PASSWORD_TOO_SHORT = ChatColor.RED + "A senha deve ter pelo menos " + AuthService.MIN_PASSWORD_LENGTH
            + " caracteres.";
    public static final String PASSWORD_TOO_LONG = ChatColor.RED + "A senha deve ter no máximo " + AuthService.MAX_PASSWORD_LENGTH
            + " caracteres.";
    public static final String REGISTER_SUCCESS = ChatColor.GREEN + "Registrado e logado com sucesso!";
    public static final String ALREADY_LOGGED_IN = ChatColor.RED + "Você já está logado.";
    public static final String LOGIN_SUCCESS = ChatColor.GREEN + "Login efetuado com sucesso!";
    public static final String WRONG_PASSWORD = ChatColor.RED + "Senha incorreta.";
    public static final String LOGIN_REQUIRED_CHAT = ChatColor.RED + "Você precisa fazer login para usar o chat.";
    public static final String LOGIN_REQUIRED_COMMAND = ChatColor.RED
            + "Você precisa fazer login para usar comandos. Use /login <senha>.";
    public static final String LOGIN_REQUIRED_INTERACTION = ChatColor.RED
            + "Você precisa fazer login antes de interagir.";
    public static final String JOIN_MESSAGE_REGISTER = ChatColor.YELLOW + "Use /register <senha> <senha> para criar sua conta.";
    public static final String JOIN_MESSAGE_LOGIN = ChatColor.YELLOW + "Use /login <senha> para entrar.";
}
