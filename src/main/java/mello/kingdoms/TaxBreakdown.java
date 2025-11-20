package mello.kingdoms;

/**
 * Resultado de um cálculo de imposto, contendo valores brutos e líquidos.
 */
public record TaxBreakdown(double netAmount, double taxAmount, String collectorName) {
}
