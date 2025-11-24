package mello.skills;

/**
 * Utilitário isolado para cálculos de progressão das skills. Centralizar as
 * fórmulas aqui evita números mágicos espalhados e facilita ajustes futuros de
 * balanceamento sem duplicar lógica.
 */
public final class SkillExperienceCalculator {

    private SkillExperienceCalculator() {
        // Utilitário estático, não deve ser instanciado.
    }

    /**
     * Calcula o XP necessário para avançar do nível informado para o próximo
     * nível, aplicando a curva configurada na definição.
     *
     * @param definition definição da skill
     * @param level      nível atual (base para cálculo)
     * @return quantidade de XP necessária para subir para o próximo nível
     */
    public static double requiredXpForLevel(SkillDefinition definition, int level) {
        if (definition == null) {
            return 0;
        }
        int normalizedLevel = Math.max(1, level);
        return definition.getBaseXpCurve() * Math.pow(definition.getCurveMultiplier(), normalizedLevel - 1);
    }
}
