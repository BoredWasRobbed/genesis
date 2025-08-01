package net.bored.genesis.powers;

import net.bored.genesis.Genesis;
import net.bored.genesis.core.powers.ISkillPower;
import net.bored.genesis.core.skills.Skill;
import net.bored.genesis.core.skills.SkillTree;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

import java.util.HashSet;
import java.util.Set;

public class SpeedsterPower implements ISkillPower {

    private ResourceLocation registryName;
    private int level = 1;
    private int experience = 0;
    private int xpToNextLevel = 100;
    private int skillPoints = 0;
    private final Set<ResourceLocation> unlockedSkills = new HashSet<>();

    // --- Skill IDs are still useful to have as constants ---
    public static final ResourceLocation SKILL_SPEED_1 = new ResourceLocation(Genesis.MOD_ID, "speedster/speed_1");
    public static final ResourceLocation SKILL_SPEED_2 = new ResourceLocation(Genesis.MOD_ID, "speedster/speed_2");

    @Override
    public void onTick(Player player) {
        if (isSkillUnlocked(SKILL_SPEED_2)) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 2, 1, false, false, false));
        } else if (isSkillUnlocked(SKILL_SPEED_1)) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 2, 0, false, false, false));
        }
    }

    @Override
    public void onPlayerUpdate(Player player) {
        if (player.isSprinting()) {
            addExperience(player, 1);
        }
    }

    @Override
    public void onActivate(Player player) {}

    @Override
    public void onRemoved(Player player) {
        player.removeEffect(MobEffects.MOVEMENT_SPEED);
        this.level = 1;
        this.experience = 0;
        this.xpToNextLevel = 100;
        this.skillPoints = 0;
        this.unlockedSkills.clear();
    }

    @Override
    public ResourceLocation getRegistryName() { return registryName; }

    @Override
    public void setRegistryName(ResourceLocation name) { this.registryName = name; }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("level", this.level);
        nbt.putInt("experience", this.experience);
        nbt.putInt("xpToNextLevel", this.xpToNextLevel);
        nbt.putInt("skillPoints", this.skillPoints);
        ListTag unlockedList = new ListTag();
        for (ResourceLocation id : this.unlockedSkills) {
            unlockedList.add(StringTag.valueOf(id.toString()));
        }
        nbt.put("unlockedSkills", unlockedList);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        this.level = nbt.getInt("level");
        this.experience = nbt.getInt("experience");
        this.xpToNextLevel = nbt.getInt("xpToNextLevel");
        this.skillPoints = nbt.getInt("skillPoints");
        this.unlockedSkills.clear();
        ListTag unlockedList = nbt.getList("unlockedSkills", Tag.TAG_STRING);
        for (Tag tag : unlockedList) {
            this.unlockedSkills.add(new ResourceLocation(tag.getAsString()));
        }
    }

    @Override
    public ResourceLocation getSkillTreeId() {
        // This power now points to the 'speedster' skill tree defined in our JSON.
        return new ResourceLocation(Genesis.MOD_ID, "speedster");
    }

    @Override
    public int getLevel() { return this.level; }

    @Override
    public int getSkillPoints() { return this.skillPoints; }

    @Override
    public void addSkillPoints(int amount) { this.skillPoints += amount; }

    @Override
    public int getExperience() { return this.experience; }

    @Override
    public int getXpNeededForNextLevel() { return this.xpToNextLevel; }

    @Override
    public void addExperience(Player player, int amount) {
        this.experience += amount;
        if (this.experience >= this.xpToNextLevel) {
            this.level++;
            this.experience -= this.xpToNextLevel;
            this.xpToNextLevel = (int) (this.xpToNextLevel * 1.5);
            addSkillPoints(1);
            player.sendSystemMessage(Component.literal("Your power has reached Level " + this.level + "! You gained 1 Skill Point."));
        }
    }

    @Override
    public boolean isSkillUnlocked(ResourceLocation skillId) { return this.unlockedSkills.contains(skillId); }

    @Override
    public void unlockSkill(ResourceLocation skillId) {
        // The check for cost and prerequisites is now done in the command/packet layer
        // before this method is ever called.
        this.unlockedSkills.add(skillId);
        Genesis.SKILL_TREE_MANAGER.getSkillTree(getSkillTreeId()).ifPresent(tree -> {
            tree.getSkill(skillId).ifPresent(skill -> {
                this.skillPoints -= skill.getCost();
            });
        });
    }
}
