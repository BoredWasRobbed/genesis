package net.bored.genesis.core.powers;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.Set;

public interface ISkillPower extends IPower, IVelocitySusceptible {

    void onPlayerUpdate(Player player);
    ResourceLocation getSkillTreeId();

    // --- Skill Methods ---
    boolean isSkillUnlocked(ResourceLocation skillId);
    void unlockSkill(ResourceLocation skillId);
    void activateSkill(Player player, int slot);
    boolean isSkillActive(ResourceLocation skillId);
    void toggleSkill(ResourceLocation skillId);
    Set<ResourceLocation> getActiveSkills();


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

    // The onActivate method is inherited from the parent IPower interface
    // and does not need to be redeclared here.
}
