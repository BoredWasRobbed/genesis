package net.bored.genesis.powers;

import com.google.common.collect.ImmutableList;
import net.bored.genesis.Genesis;
import net.bored.genesis.core.powers.ISkillPower;
import net.bored.genesis.network.PacketHandler;
import net.bored.genesis.network.packets.SyncPowerDataS2CPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
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
    private final Set<ResourceLocation> activeSkills = new HashSet<>();
    private final Map<Integer, ResourceLocation> abilityBindings = new HashMap<>();
    private int trailColor = 0xFFFF0000; // Default Red

    // --- Skill IDs ---
    public static final ResourceLocation SKILL_NEGATIVE_SPEED_1 = new ResourceLocation(Genesis.MOD_ID, "negative_speedster/neg_speed_1");
    public static final ResourceLocation SKILL_NEGATIVE_SPEED_2 = new ResourceLocation(Genesis.MOD_ID, "negative_speedster/neg_speed_2");
    public static final ResourceLocation SKILL_NEGATIVE_SPEED_3 = new ResourceLocation(Genesis.MOD_ID, "negative_speedster/neg_speed_3");
    public static final ResourceLocation SKILL_NEGATIVE_SPEED_4 = new ResourceLocation(Genesis.MOD_ID, "negative_speedster/neg_speed_4");
    public static final ResourceLocation SKILL_NEGATIVE_SPEED_5 = new ResourceLocation(Genesis.MOD_ID, "negative_speedster/neg_speed_5");
    public static final ResourceLocation SKILL_NEGATIVE_SPEED_6 = new ResourceLocation(Genesis.MOD_ID, "negative_speedster/neg_speed_6");
    public static final ResourceLocation SKILL_NEGATIVE_SPEED_7 = new ResourceLocation(Genesis.MOD_ID, "negative_speedster/neg_speed_7");
    public static final ResourceLocation SKILL_NEGATIVE_SPEED_8 = new ResourceLocation(Genesis.MOD_ID, "negative_speedster/neg_speed_8");
    public static final ResourceLocation SKILL_KINETIC_KICKSTART = new ResourceLocation(Genesis.MOD_ID, "negative_speedster/kinetic_kickstart");

    private static final List<ResourceLocation> SPEED_SKILL_TIERS = ImmutableList.of(
            SKILL_NEGATIVE_SPEED_1, SKILL_NEGATIVE_SPEED_2, SKILL_NEGATIVE_SPEED_3,
            SKILL_NEGATIVE_SPEED_4, SKILL_NEGATIVE_SPEED_5, SKILL_NEGATIVE_SPEED_6,
            SKILL_NEGATIVE_SPEED_7, SKILL_NEGATIVE_SPEED_8
    );

    // --- Power State ---
    private static final UUID BASE_SPEED_UUID = UUID.fromString("a2b34e74-4208-444e-9845-2a2b85191a26");
    private static final UUID SPRINT_BOOST_UUID = UUID.fromString("d5e8f3f6-8b1a-4b6a-8b1a-9b6b7a2d1a3e");
    private static final UUID V9_BOOST_UUID = UUID.fromString("8b5a3e1c-6f3e-4a1a-9c4c-2b8a7d4e3f2b");
    private static final UUID STEP_HEIGHT_UUID = UUID.fromString("70a1d9de-f27f-4a7a-bafc-561f0b36b8ae");

    private final AttributeModifier stepHeightModifier = new AttributeModifier(STEP_HEIGHT_UUID, "Negative Speedster Step Height", 1.0, AttributeModifier.Operation.ADDITION);

    private boolean isSpeedActive = true;
    private int sprintTicks = 0;
    private int currentSpeedTier = 8;

    // --- Velocity-9 State ---
    private int v9Toxicity = 0;
    private int v9DegenerationTimer = 0;
    private int v9BoostTicks = 0;
    private int damageTicker = 0;
    private static final int V9_TOXICITY_THRESHOLD = 5;
    private static final int DEGENERATION_GRACE_PERIOD = 6000;
    private static final int DAMAGE_INTERVAL = 40;

    @Override
    public int getTrailColor() {
        return this.isSpeedActive ? this.trailColor : 0;
    }

    @Override
    public void setTrailColor(int color) {
        this.trailColor = color;
    }

    @Override
    public void onTick(Player player) {
        handleAttributes(player);
        handleWaterRunning(player);
        handleSpeedModifiers(player);
        handleV9Effects(player);
    }

    private void handleSpeedModifiers(Player player) {
        AttributeInstance speedAttribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttribute == null) return;

        speedAttribute.removeModifier(BASE_SPEED_UUID);
        speedAttribute.removeModifier(SPRINT_BOOST_UUID);

        if (isSpeedActive) {
            int highestUnlockedTier = 0;
            for (int i = 0; i < SPEED_SKILL_TIERS.size(); i++) {
                if(isSkillUnlocked(SPEED_SKILL_TIERS.get(i))) {
                    highestUnlockedTier = i + 1;
                }
            }
            int effectiveTier = Math.min(currentSpeedTier, highestUnlockedTier);
            double baseBonus = 0;

            switch(effectiveTier) {
                case 1: baseBonus = 1.4; break;
                case 2: baseBonus = 2.4; break;
                case 3: baseBonus = 4.0; break;
                case 4: baseBonus = 6.0; break;
                case 5: baseBonus = 8.0; break;
                case 6: baseBonus = 9.0; break;
                case 7: baseBonus = 10.0; break;
                case 8: baseBonus = 12.0; break;
            }

            if (baseBonus > 0) {
                AttributeModifier baseSpeedModifier = new AttributeModifier(BASE_SPEED_UUID, "Negative Speedster Base Speed", baseBonus, AttributeModifier.Operation.MULTIPLY_TOTAL);
                speedAttribute.addPermanentModifier(baseSpeedModifier);
            }

            if (player.isSprinting()) {
                double sprintBonusMax = 0.30; // +30% sprint boost
                double sprintAcceleration = 0.01;
                double currentSprintBonus;

                if (isSkillActive(SKILL_KINETIC_KICKSTART)) {
                    currentSprintBonus = sprintBonusMax;
                } else {
                    sprintTicks++;
                    currentSprintBonus = Math.min(sprintBonusMax, sprintTicks * sprintAcceleration);
                }

                AttributeModifier sprintModifier = new AttributeModifier(SPRINT_BOOST_UUID, "Negative Speedster Sprint Boost", currentSprintBonus, AttributeModifier.Operation.MULTIPLY_TOTAL);
                speedAttribute.addPermanentModifier(sprintModifier);
            } else {
                sprintTicks = 0;
            }
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
                player.displayClientMessage(Component.literal("The Velocity-9 boost has worn off."), true);
                player.level().playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5F, 1.0F);
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
        Component message;
        float pitch;

        if (this.isSpeedActive) {
            message = Component.literal("Negative Speed Force: ").withStyle(ChatFormatting.WHITE).append(Component.literal("SURGING").withStyle(ChatFormatting.RED));
            pitch = 1.2F;
        } else {
            message = Component.literal("Negative Speed Force: ").withStyle(ChatFormatting.WHITE).append(Component.literal("RECEDING").withStyle(ChatFormatting.GRAY));
            pitch = 0.8F;
        }

        player.displayClientMessage(message, true);
        player.level().playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_PLING.get(), SoundSource.PLAYERS, 0.7F, pitch);

        // --- FIX: SYNC STATE CHANGE TO CLIENT ---
        if (!player.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
            PacketHandler.sendToPlayer(new SyncPowerDataS2CPacket(this.getRegistryName(), this.serializeNBT()), serverPlayer);
        }

        if (!this.isSpeedActive) {
            sprintTicks = 0;
            AttributeInstance speedAttribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speedAttribute != null) {
                speedAttribute.removeModifier(BASE_SPEED_UUID);
                speedAttribute.removeModifier(SPRINT_BOOST_UUID);
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
            player.displayClientMessage(Component.literal("Unlock speed skills to set a top speed.").withStyle(ChatFormatting.RED), true);
            player.level().playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_DIDGERIDOO.get(), SoundSource.PLAYERS, 0.7F, 1.5F);
            return;
        }

        int oldTier = currentSpeedTier;
        int newTier;

        if (cycleUp) {
            newTier = Math.min(highestUnlockedTier, currentSpeedTier + 1);
        } else {
            newTier = Math.max(1, currentSpeedTier - 1);
        }

        if(oldTier != newTier) {
            currentSpeedTier = newTier;
            player.displayClientMessage(Component.literal("Top Speed: Tier " + currentSpeedTier), true);
            player.level().playSound(null, player.blockPosition(), SoundEvents.UI_BUTTON_CLICK.get(), SoundSource.PLAYERS, 0.5F, 1.2F);
        } else {
            player.level().playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_DIDGERIDOO.get(), SoundSource.PLAYERS, 0.7F, 1.5F);
        }
    }

    @Override
    public void onRemoved(Player player) {
        AttributeInstance speedAttribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttribute != null) {
            speedAttribute.removeModifier(BASE_SPEED_UUID);
            speedAttribute.removeModifier(SPRINT_BOOST_UUID);
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
        ListTag activeList = new ListTag();
        this.activeSkills.forEach(id -> activeList.add(StringTag.valueOf(id.toString())));
        nbt.put("activeSkills", activeList);
        CompoundTag bindingsTag = new CompoundTag();
        this.abilityBindings.forEach((slot, id) -> bindingsTag.putString(String.valueOf(slot), id.toString()));
        nbt.put("abilityBindings", bindingsTag);
        nbt.putBoolean("isSpeedActive", this.isSpeedActive);
        nbt.putInt("currentSpeedTier", this.currentSpeedTier);
        nbt.putInt("v9Toxicity", this.v9Toxicity);
        nbt.putInt("v9DegenerationTimer", this.v9DegenerationTimer);
        nbt.putInt("v9BoostTicks", this.v9BoostTicks);
        nbt.putInt("trailColor", this.trailColor);
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
        this.activeSkills.clear();
        ListTag activeList = nbt.getList("activeSkills", Tag.TAG_STRING);
        activeList.forEach(tag -> this.activeSkills.add(new ResourceLocation(tag.getAsString())));
        if (this.activeSkills.isEmpty() && !this.unlockedSkills.isEmpty()) {
            this.activeSkills.addAll(this.unlockedSkills);
        }
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
        if (nbt.contains("trailColor", Tag.TAG_INT)) {
            this.trailColor = nbt.getInt("trailColor");
        }
    }

    @Override
    public void applyVelocity9(Player player) {
        AttributeInstance speedAttribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttribute == null) return;

        speedAttribute.removeModifier(V9_BOOST_UUID);
        AttributeModifier v9boost = new AttributeModifier(V9_BOOST_UUID, "V9 Temporary Boost", 4.0, AttributeModifier.Operation.MULTIPLY_TOTAL);
        speedAttribute.addPermanentModifier(v9boost);

        this.v9BoostTicks = 600;
        this.v9Toxicity++;
        if (this.v9Toxicity >= V9_TOXICITY_THRESHOLD) {
            this.v9DegenerationTimer = DEGENERATION_GRACE_PERIOD;
        }

        player.level().playSound(null, player.blockPosition(), SoundEvents.ZOMBIE_VILLAGER_CURE, SoundSource.PLAYERS, 0.8f, 1.5f);
        player.displayClientMessage(Component.literal("Velocity-9 corrupts your speed!").withStyle(ChatFormatting.DARK_RED), true);
    }

    @Override
    public int getToxicity() {
        return this.v9Toxicity;
    }

    @Override
    public void unlockSkill(ResourceLocation skillId) {
        if (this.unlockedSkills.add(skillId)) {
            this.activeSkills.add(skillId);
            Genesis.SKILL_TREE_MANAGER.getSkillTree(getSkillTreeId()).ifPresent(tree -> {
                tree.getSkill(skillId).ifPresent(skill -> {
                    this.skillPoints -= skill.getCost();
                });
            });
        }
    }

    @Override
    public boolean isSkillActive(ResourceLocation skillId) {
        return this.activeSkills.contains(skillId);
    }

    @Override
    public void toggleSkill(ResourceLocation skillId) {
        if (isSkillUnlocked(skillId)) {
            if (isSkillActive(skillId)) {
                this.activeSkills.remove(skillId);
            } else {
                this.activeSkills.add(skillId);
            }
        }
    }

    @Override
    public Set<ResourceLocation> getActiveSkills() {
        return this.activeSkills;
    }

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
            this.xpToNextLevel = (int) (this.xpToNextLevel * 1.6);
            addSkillPoints(1);
            Component message = Component.literal("Corruption Level Up! ").withStyle(ChatFormatting.DARK_RED)
                    .append(Component.literal("[" + this.level + "]").withStyle(ChatFormatting.RED));
            player.displayClientMessage(message, true);
            player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 0.8F);
        }
    }
    @Override
    public boolean isSkillUnlocked(ResourceLocation skillId) { return this.unlockedSkills.contains(skillId); }
    @Override
    public void setAbilityBinding(int slot, ResourceLocation skillId) {
        this.abilityBindings.put(slot, skillId);
    }
    @Override
    public ResourceLocation getAbilityBinding(int slot) { return this.abilityBindings.get(slot); }
    @Override
    public Map<Integer, ResourceLocation> getAbilityBindings() { return this.abilityBindings; }
}
