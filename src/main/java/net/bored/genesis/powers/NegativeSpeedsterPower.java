package net.bored.genesis.powers;

import net.bored.genesis.Genesis;
import net.bored.genesis.core.powers.ISkillPower;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class NegativeSpeedsterPower implements ISkillPower {

    private ResourceLocation registryName;
    private int level = 1;
    private int experience = 0;
    private int xpToNextLevel = 100;
    private int skillPoints = 0;
    private final Set<ResourceLocation> unlockedSkills = new HashSet<>();
    private final Map<Integer, ResourceLocation> abilityBindings = new HashMap<>();

    // --- Skill IDs ---
    public static final ResourceLocation SKILL_NEGATIVE_SPEED_1 = new ResourceLocation(Genesis.MOD_ID, "negative_speedster/neg_speed_1");
    public static final ResourceLocation SKILL_NEGATIVE_SPEED_2 = new ResourceLocation(Genesis.MOD_ID, "negative_speedster/neg_speed_2");
    public static final ResourceLocation SKILL_NEGATIVE_SPEED_3 = new ResourceLocation(Genesis.MOD_ID, "negative_speedster/neg_speed_3");
    public static final ResourceLocation SKILL_NEGATIVE_SPEED_4 = new ResourceLocation(Genesis.MOD_ID, "negative_speedster/neg_speed_4");
    public static final ResourceLocation SKILL_NEGATIVE_SPEED_5 = new ResourceLocation(Genesis.MOD_ID, "negative_speedster/neg_speed_5");
    public static final ResourceLocation SKILL_NEGATIVE_SPEED_6 = new ResourceLocation(Genesis.MOD_ID, "negative_speedster/neg_speed_6");


    // --- Power State ---
    private static final UUID SPEED_MODIFIER_UUID = UUID.fromString("a2b34e74-4208-444e-9845-2a2b85191a26");
    private static final UUID KNOCKBACK_RESISTANCE_UUID = UUID.fromString("b8a5a4a5-3a75-4a5e-9e1a-1a8a9a2a3a4a");

    private final AttributeModifier knockbackResistanceModifier = new AttributeModifier(KNOCKBACK_RESISTANCE_UUID, "Negative Speedster Knockback Resistance", 1.0, AttributeModifier.Operation.ADDITION);

    private boolean isSpeedActive = true;
    private int sprintTicks = 0;

    @Override
    public void onTick(Player player) {
        handleAttributes(player);
        handleWaterRunning(player);
        handleAcceleration(player);
    }

    private void handleAcceleration(Player player) {
        AttributeInstance speedAttribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttribute == null) return;

        speedAttribute.removeModifier(SPEED_MODIFIER_UUID);

        if (isSpeedActive && player.isSprinting()) {
            sprintTicks++;

            double maxBonus = 0;
            double accelerationFactor = 0;

            if (isSkillUnlocked(SKILL_NEGATIVE_SPEED_6)) { maxBonus = 4.5; accelerationFactor = 0.022; }
            else if (isSkillUnlocked(SKILL_NEGATIVE_SPEED_5)) { maxBonus = 4.0; accelerationFactor = 0.020; }
            else if (isSkillUnlocked(SKILL_NEGATIVE_SPEED_4)) { maxBonus = 3.0; accelerationFactor = 0.018; }
            else if (isSkillUnlocked(SKILL_NEGATIVE_SPEED_3)) { maxBonus = 2.0; accelerationFactor = 0.015; }
            else if (isSkillUnlocked(SKILL_NEGATIVE_SPEED_2)) { maxBonus = 1.2; accelerationFactor = 0.010; }
            else if (isSkillUnlocked(SKILL_NEGATIVE_SPEED_1)) { maxBonus = 0.7; accelerationFactor = 0.007; }
            else { sprintTicks = 0; return; }

            double currentBonus = Math.min(maxBonus, sprintTicks * accelerationFactor);

            AttributeModifier speedModifier = new AttributeModifier(SPEED_MODIFIER_UUID, "Negative Speedster Acceleration", currentBonus, AttributeModifier.Operation.MULTIPLY_TOTAL);
            speedAttribute.addPermanentModifier(speedModifier);

        } else {
            sprintTicks = 0;
        }
    }

    private void handleAttributes(Player player) {
        AttributeInstance knockbackAttribute = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (knockbackAttribute == null) return;

        if (isSpeedActive) {
            if (!knockbackAttribute.hasModifier(knockbackResistanceModifier)) {
                knockbackAttribute.addPermanentModifier(knockbackResistanceModifier);
            }
        } else {
            if (knockbackAttribute.hasModifier(knockbackResistanceModifier)) {
                knockbackAttribute.removeModifier(knockbackResistanceModifier);
            }
        }
    }

    private void handleWaterRunning(Player player) {
        if (isSpeedActive && player.isInWater() && player.isSprinting()) {
            Vec3 motion = player.getDeltaMovement();
            if (motion.y < 0) {
                player.setDeltaMovement(motion.x, 0, motion.z);
            }
        }
    }

    @Override
    public void onPlayerUpdate(Player player) {
        if (player.isSprinting() && isSpeedActive) {
            addExperience(player, 1);
        }
    }

    @Override
    public void onPowerKey(Player player) {
        this.isSpeedActive = !this.isSpeedActive;
        player.sendSystemMessage(Component.literal("Negative Speed Force " + (this.isSpeedActive ? "surges" : "recedes")));
        if (!this.isSpeedActive) {
            sprintTicks = 0;
            AttributeInstance speedAttribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speedAttribute != null) {
                speedAttribute.removeModifier(SPEED_MODIFIER_UUID);
            }
        }
    }

    @Override
    public void activateSkill(Player player, int slot) {}

    @Override
    public void onRemoved(Player player) {
        AttributeInstance speedAttribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttribute != null) {
            speedAttribute.removeModifier(SPEED_MODIFIER_UUID);
        }
        AttributeInstance knockbackAttribute = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (knockbackAttribute != null) {
            knockbackAttribute.removeModifier(knockbackResistanceModifier);
        }
    }

    @Override
    public void onActivate(Player player) {}

    // --- Boilerplate and Data Management ---
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
        nbt.putBoolean("isSpeedActive", this.isSpeedActive);
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
        if (nbt.contains("isSpeedActive")) {
            this.isSpeedActive = nbt.getBoolean("isSpeedActive");
        }
    }
    @Override
    public ResourceLocation getSkillTreeId() { return new ResourceLocation(Genesis.MOD_ID, "negative_speedster"); }
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
            this.xpToNextLevel = (int) (this.xpToNextLevel * 1.6); // Slightly faster leveling
            addSkillPoints(1);
            player.sendSystemMessage(Component.literal("Your corruption has reached Level " + this.level + "! You gained 1 Skill Point."));
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
    public void setAbilityBinding(int slot, ResourceLocation skillId) {
        this.abilityBindings.put(slot, skillId);
    }
    @Override
    public ResourceLocation getAbilityBinding(int slot) { return this.abilityBindings.get(slot); }

    @Override
    public Map<Integer, ResourceLocation> getAbilityBindings() { return this.abilityBindings; }
}