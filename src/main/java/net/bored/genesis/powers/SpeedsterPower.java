package net.bored.genesis.powers;

import net.bored.genesis.Genesis;
import net.bored.genesis.core.powers.ISkillPower;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SpeedsterPower implements ISkillPower {

    private ResourceLocation registryName;
    private int level = 1;
    private int experience = 0;
    private int xpToNextLevel = 100;
    private int skillPoints = 0;
    private final Set<ResourceLocation> unlockedSkills = new HashSet<>();
    private final Map<Integer, ResourceLocation> abilityBindings = new HashMap<>();

    public static final ResourceLocation SKILL_SPEED_1 = new ResourceLocation(Genesis.MOD_ID, "speedster/speed_1");
    public static final ResourceLocation SKILL_SPEED_2 = new ResourceLocation(Genesis.MOD_ID, "speedster/speed_2");
    public static final ResourceLocation SKILL_PHASING = new ResourceLocation(Genesis.MOD_ID, "speedster/phasing");

    private boolean isPhasing = false;

    @Override
    public void onTick(Player player) {
        // Passive effects
        if (isSkillUnlocked(SKILL_SPEED_2)) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 2, 1, false, false, false));
        } else if (isSkillUnlocked(SKILL_SPEED_1)) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 2, 0, false, false, false));
        }

        // Active effects
        if (isPhasing) {
            player.setNoGravity(true);
            player.noPhysics = true;
        } else if (player.noPhysics) {
            // Ensure physics is restored if phasing ends unexpectedly
            player.noPhysics = false;
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
        player.noPhysics = false;
        this.level = 1;
        this.experience = 0;
        this.xpToNextLevel = 100;
        this.skillPoints = 0;
        this.unlockedSkills.clear();
        this.abilityBindings.clear();
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
        this.unlockedSkills.forEach(id -> unlockedList.add(StringTag.valueOf(id.toString())));
        nbt.put("unlockedSkills", unlockedList);

        CompoundTag bindingsTag = new CompoundTag();
        this.abilityBindings.forEach((slot, id) -> bindingsTag.putString(String.valueOf(slot), id.toString()));
        nbt.put("abilityBindings", bindingsTag);

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
        unlockedList.forEach(tag -> this.unlockedSkills.add(new ResourceLocation(tag.getAsString())));

        this.abilityBindings.clear();
        CompoundTag bindingsTag = nbt.getCompound("abilityBindings");
        bindingsTag.getAllKeys().forEach(key -> {
            int slot = Integer.parseInt(key);
            ResourceLocation id = new ResourceLocation(bindingsTag.getString(key));
            this.abilityBindings.put(slot, id);
        });
    }

    @Override
    public ResourceLocation getSkillTreeId() { return new ResourceLocation(Genesis.MOD_ID, "speedster"); }
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
        while (this.experience >= this.xpToNextLevel) {
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
        this.unlockedSkills.add(skillId);
        Genesis.SKILL_TREE_MANAGER.getSkillTree(getSkillTreeId()).ifPresent(tree -> {
            tree.getSkill(skillId).ifPresent(skill -> {
                this.skillPoints -= skill.getCost();
            });
        });
    }

    @Override
    public void activateSkill(Player player, int slot) {
        ResourceLocation skillId = getAbilityBinding(slot);
        if (skillId == null || !isSkillUnlocked(skillId)) return;

        if (skillId.equals(SKILL_PHASING)) {
            this.isPhasing = !this.isPhasing;
            player.sendSystemMessage(Component.literal("Phasing " + (this.isPhasing ? "Enabled" : "Disabled")));
            if (!isPhasing) {
                // When disabling, ensure noPhysics is also turned off immediately.
                player.noPhysics = false;
            }
        }
    }

    @Override
    public void setAbilityBinding(int slot, ResourceLocation skillId) {
        // Ensure a skill isn't bound to multiple slots by removing old entries
        this.abilityBindings.entrySet().removeIf(entry -> entry.getValue().equals(skillId));
        this.abilityBindings.put(slot, skillId);
    }

    @Override
    public ResourceLocation getAbilityBinding(int slot) {
        return this.abilityBindings.get(slot);
    }

    @Override
    public Map<Integer, ResourceLocation> getAbilityBindings() {
        return this.abilityBindings;
    }
}
