package mello.jobs;

/**
 * Representa uma recompensa granular de job, permitindo combinar coins e gems
 * sem obrigar duplicação de tabelas ou novas listas de pagamento.
 */
public record JobReward(double coins, double gems) {

    public static JobReward empty() {
        return new JobReward(0D, 0D);
    }

    public boolean isEmpty() {
        return coins <= 0 && gems <= 0;
    }
}
