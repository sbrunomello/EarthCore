package mello.skills;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Evento disparado sempre que um jogador recebe XP em uma skill. Útil para
 * hooks externos (ex.: achievements) que não devem depender diretamente da
 * implementação do serviço.
 */
public class PlayerSkillXpGainEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final SkillType skill;
    private final double amount;
    private final int previousLevel;
    private final int newLevel;

    public PlayerSkillXpGainEvent(Player player, SkillType skill, double amount, int previousLevel, int newLevel) {
        this.player = player;
        this.skill = skill;
        this.amount = amount;
        this.previousLevel = previousLevel;
        this.newLevel = newLevel;
    }

    public Player getPlayer() {
        return player;
    }

    public SkillType getSkill() {
        return skill;
    }

    public double getAmount() {
        return amount;
    }

    public int getPreviousLevel() {
        return previousLevel;
    }

    public int getNewLevel() {
        return newLevel;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
