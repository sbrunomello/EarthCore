package mello.skills.hud;

import mello.skills.PlayerSkillProgress;
import mello.skills.SkillType;
import org.bukkit.entity.Player;

/**
 * Abstração de HUD para exibir o progresso de XP das skills. Implementações
 * devem ser leves e reutilizar recursos (como BossBars) para evitar flicker ou
 * vazamento de objetos no servidor.
 */
public interface SkillHudService {

    /**
     * Atualiza/mostra a HUD ao ganhar XP.
     *
     * @param player                 jogador que receberá a HUD
     * @param skill                  skill alterada
     * @param progress               progresso atualizado da skill
     * @param gainedXp               XP ganho (já com multiplicadores aplicados)
     * @param requiredXpForNextLevel XP necessário para o próximo nível (0 se nível máximo)
     * @param previousLevel          nível antes da adição de XP (para mensagens complementares)
     */
    void showXpGain(Player player, SkillType skill, PlayerSkillProgress progress, double gainedXp,
                    double requiredXpForNextLevel, int previousLevel);
}
