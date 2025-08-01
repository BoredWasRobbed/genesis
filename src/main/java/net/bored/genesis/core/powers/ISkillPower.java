package net.bored.genesis.core.powers;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.Map;

public interface ISkillPower extends IPower {

    void onPlayerUpdate(Player player);
    ResourceLocation getSkillTreeId();

    // --- Skill Methods ---
    boolean isSkillUnlocked(ResourceLocation skillId);
    void unlockSkill(ResourceLocation skillId);
    void activateSkill(Player player, int slot);

    // --- Leveling & XP Methods ---
    int getLevel();
    int getSkillPoints();
    void addSkillPoints(int amount);
    int getExperience();
    int getXpNeededForNextLevel();
    void addExperience(Player player, int amount);

    // --- Ability Binding Methods ---
    void setAbilityBinding(int slot, ResourceLocation skillId);
    ResourceLocation getAbilityBinding(int slot);
    Map<Integer, ResourceLocation> getAbilityBindings();
}
