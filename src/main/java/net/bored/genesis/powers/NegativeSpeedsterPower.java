package net.bored.genesis.powers;

import com.google.common.collect.ImmutableList;
import net.bored.genesis.Genesis;
import net.bored.genesis.core.powers.ISkillPower;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
    public static final ResourceLocation SKILL_KINETIC_KICKSTART = new ResourceLocation(Genesis.MOD_ID, "negative_speedster/kinetic_kickstart");

    private static final List<ResourceLocation> SPEED_SKILL_TIERS = ImmutableList.of(
            SKILL_NEGATIVE_SPEED_1, SKILL_NEGATIVE_SPEED_2, SKILL_NEGATIVE_SPEED_3,
            SKILL_NEGATIVE_SPEED_4, SKILL_NEGATIVE_SPEED_5, SKILL_NEGATIVE_SPEED_6
    );

    // --- Power State ---
    private static final UUID SPEED_MODIFIER_UUID = UUID.fromString("a2b34e74-4208-444e-9845-2a2b85191a26");
    private static final UUID V9_BOOST_UUID = UUID.fromString("8b5a3e1c-6f3e-4a1a-9c4c-2b8a7d4e3f2b");
    private static final UUID STEP_HEIGHT_UUID = UUID.fromString("70a1d9de-f27f-4a7a-bafc-561f0b36b8ae");

    private final AttributeModifier stepHeightModifier = new AttributeModifier(STEP_HEIGHT_UUID, "Negative Speedster Step Height", 1.0, AttributeModifier.Operation.ADDITION);

    private boolean isSpeedActive = true;
    private int sprintTicks = 0;
    private int currentSpeedTier = 6;

    // --- Velocity-9 State ---
    private int v9Toxicity = 0;
    private int v9DegenerationTimer = 0;
    private int v9BoostTicks = 0;
    private int damageTicker = 0;
    private static final int V9_TOXICITY_THRESHOLD = 5;
    private static final int DEGENERATION_GRACE_PERIOD = 6000;
    private static final int DAMAGE_INTERVAL = 40;


    @Override
    public void onTick(Player player) {
        handleAttributes(player);
        handleWaterRunning(player);
        handleAcceleration(player);
        handleV9Effects(player);
    }

    private void handleAcceleration(Player player) {
        AttributeInstance speedAttribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttribute == null) return;

        speedAttribute.removeModifier(SPEED_MODIFIER_UUID);

        if (isSpeedActive && player.isSprinting()) {
            double maxBonus = 0;
            double accelerationFactor = 0;
            int highestUnlockedTier = 0;

            for (int i = 0; i < SPEED_SKILL_TIERS.size(); i++) {
                if(isSkillUnlocked(SPEED_SKILL_TIERS.get(i))) {
                    highestUnlockedTier = i + 1;
                }
            }
            int effectiveTier = Math.min(currentSpeedTier, highestUnlockedTier);

            switch(effectiveTier) {
                case 1: maxBonus = 1.4; accelerationFactor = 0.014; break; // 0.7 * 2
                case 2: maxBonus = 2.4; accelerationFactor = 0.020; break; // 1.2 * 2
                case 3: maxBonus = 4.0; accelerationFactor = 0.030; break; // 2.0 * 2
                case 4: maxBonus = 6.0; accelerationFactor = 0.036; break; // 3.0 * 2
                case 5: maxBonus = 8.0; accelerationFactor = 0.040; break; // 4.0 * 2
                case 6: maxBonus = 9.0; accelerationFactor = 0.044; break; // 4.5 * 2
                default: sprintTicks = 0; return;
            }

            double currentBonus;
            if (isSkillUnlocked(SKILL_KINETIC_KICKSTART)) {
                currentBonus = maxBonus; // Instant acceleration
            } else {
                sprintTicks++;
                currentBonus = Math.min(maxBonus, sprintTicks * accelerationFactor);
            }

            AttributeModifier speedModifier = new AttributeModifier(SPEED_MODIFIER_UUID, "Negative Speedster Acceleration", currentBonus, AttributeModifier.Operation.MULTIPLY_TOTAL);
            speedAttribute.addPermanentModifier(speedModifier);

        } else {
            sprintTicks = 0;
        }
    }

    private void handleAttributes(Player player) {
        AttributeInstance stepHeightAttribute = player.getAttribute(ForgeMod.STEP_HEIGHT_ADDITION.get());

        if (isSpeedActive) {
            if (stepHeightAttribute != null && !stepHeightAttribute.hasModifier(stepHeightModifier)) {
                stepHeightAttribute.addPermanentModifier(stepHeightModifier);
            }
        } else {
            if (stepHeightAttribute != null && stepHeightAttribute.hasModifier(stepHeightModifier)) {
                stepHeightAttribute.removeModifier(stepHeightModifier);
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

    private void handleV9Effects(Player player) {
        AttributeInstance speedAttribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttribute == null) return;

        if (v9BoostTicks > 0) {
            v9BoostTicks--;
            if (v9BoostTicks == 0) {
                speedAttribute.removeModifier(V9_BOOST_UUID);
                player.sendSystemMessage(Component.literal("The Velocity-9 boost has worn off."));
            }
        }

        if (v9Toxicity >= V9_TOXICITY_THRESHOLD) {
            if (v9DegenerationTimer > 0) {
                v9DegenerationTimer--;
            } else {
                damageTicker++;
                if (damageTicker >= DAMAGE_INTERVAL) {
                    player.hurt(player.damageSources().magic(), 1.0f);
                    damageTicker = 0;
                }
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

    public void cycleTopSpeed(Player player, boolean cycleUp) {
        int highestUnlockedTier = 0;
        for (int i = 0; i < SPEED_SKILL_TIERS.size(); i++) {
            if(isSkillUnlocked(SPEED_SKILL_TIERS.get(i))) {
                highestUnlockedTier = i + 1;
            }
        }

        if (highestUnlockedTier == 0) {
            player.sendSystemMessage(Component.literal("Unlock speed skills to set a top speed."));
            return;
        }

        int oldTier = currentSpeedTier;

        if (cycleUp) {
            currentSpeedTier = Math.min(highestUnlockedTier, currentSpeedTier + 1);
        } else { // cycle down
            currentSpeedTier = Math.max(1, currentSpeedTier - 1);
        }

        if(oldTier != currentSpeedTier) {
            player.sendSystemMessage(Component.literal("Top speed set to Tier " + currentSpeedTier));
        }
    }

    @Override
    public void onRemoved(Player player) {
        AttributeInstance speedAttribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttribute != null) {
            speedAttribute.removeModifier(SPEED_MODIFIER_UUID);
            speedAttribute.removeModifier(V9_BOOST_UUID);
        }
        AttributeInstance stepHeightAttribute = player.getAttribute(ForgeMod.STEP_HEIGHT_ADDITION.get());
        if (stepHeightAttribute != null) {
            stepHeightAttribute.removeModifier(stepHeightModifier);
        }
    }

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
        nbt.putInt("currentSpeedTier", this.currentSpeedTier);
        nbt.putInt("v9Toxicity", this.v9Toxicity);
        nbt.putInt("v9DegenerationTimer", this.v9DegenerationTimer);
        nbt.putInt("v9BoostTicks", this.v9BoostTicks);
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
        this.isSpeedActive = nbt.getBoolean("isSpeedActive");
        this.currentSpeedTier = nbt.getInt("currentSpeedTier");
        this.v9Toxicity = nbt.getInt("v9Toxicity");
        this.v9DegenerationTimer = nbt.getInt("v9DegenerationTimer");
        this.v9BoostTicks = nbt.getInt("v9BoostTicks");
    }

    @Override
    public void applyVelocity9(Player player) {
        AttributeInstance speedAttribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttribute == null) return;

        speedAttribute.removeModifier(V9_BOOST_UUID);
        AttributeModifier v9boost = new AttributeModifier(V9_BOOST_UUID, "V9 Temporary Boost", 4.0, AttributeModifier.Operation.MULTIPLY_TOTAL);
        speedAttribute.addPermanentModifier(v9boost);

        this.v9BoostTicks = 600; // 30 seconds
        this.v9Toxicity++;
        if (this.v9Toxicity >= V9_TOXICITY_THRESHOLD) {
            this.v9DegenerationTimer = DEGENERATION_GRACE_PERIOD;
        }

        player.level().playSound(null, player.blockPosition(), SoundEvents.ZOMBIE_VILLAGER_CURE, SoundSource.PLAYERS, 0.8f, 1.5f);
        player.sendSystemMessage(Component.literal("Velocity-9 surges through you, granting immense temporary speed!"));
    }

    @Override
    public int getToxicity() {
        return this.v9Toxicity;
    }

    // --- Unused / Boilerplate ---
    @Override
    public void onActivate(Player player) {}
    @Override
    public void activateSkill(Player player, int slot) {}
    @Override
    public ResourceLocation getRegistryName() { return registryName; }
    @Override
    public void setRegistryName(ResourceLocation name) { this.registryName = name; }
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
