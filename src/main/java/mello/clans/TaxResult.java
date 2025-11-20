package mello.clans;

/**
 * Resultado de cálculo de imposto de clã.
 */
public record TaxResult(double netAmount, double taxAmount, String collectorName) {
}
