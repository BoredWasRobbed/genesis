package net.bored.genesis.core.powers;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * An extension of IPower for powers that have a progression system
 * based on a skill tree, experience, and levels.
 */
public interface ISkillPower extends IPower {

    void onPlayerUpdate(Player player);

    /**
     * @return The ResourceLocation of the skill tree this power uses.
     */
    ResourceLocation getSkillTreeId();

    boolean isSkillUnlocked(ResourceLocation skillId);
    void unlockSkill(ResourceLocation skillId);

    int getLevel();
    int getSkillPoints();
    void addSkillPoints(int amount);
    int getExperience();
    int getXpNeededForNextLevel();
    void addExperience(Player player, int amount);
}
