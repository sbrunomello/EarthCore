package mello.jobs;

/**
 * Representa uma recompensa granular de job. O pagamento agora é exclusivo em
 * coins: gems devem ser obtidas apenas via loja ou eventos.
 */
public record JobReward(double coins) {

    public static JobReward empty() {
        return new JobReward(0D);
    }

    public boolean isEmpty() {
        return coins <= 0;
    }
}
