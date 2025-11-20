package mello.common;

/**
 * Representa o resultado de uma operação de domínio, encapsulando sucesso e mensagem amigável.
 */
public record OperationResult(boolean success, String message) {
    public static OperationResult ok(String message) {
        return new OperationResult(true, message);
    }

    public static OperationResult fail(String message) {
        return new OperationResult(false, message);
    }
}
