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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class SpeedsterPower implements ISkillPower {

    private ResourceLocation registryName;
    private int level = 1;
    private int experience = 0;
    private int xpToNextLevel = 100;
    private int skillPoints = 0;
    private final Set<ResourceLocation> unlockedSkills = new HashSet<>();
    private final Set<ResourceLocation> activeSkills = new HashSet<>();
    private final Map<Integer, ResourceLocation> abilityBindings = new HashMap<>();
    private int trailColor = 0xFFFFFF00; // Default Yellow

    // --- Skill IDs ---
    public static final ResourceLocation POWER_ID = new ResourceLocation(Genesis.MOD_ID, "speedster");
    public static final ResourceLocation SKILL_SPEED_1 = new ResourceLocation(Genesis.MOD_ID, "speedster/speed_boost_1");
    public static final ResourceLocation SKILL_SPEED_2 = new ResourceLocation(Genesis.MOD_ID, "speedster/speed_boost_2");
    public static final ResourceLocation SKILL_SPEED_3 = new ResourceLocation(Genesis.MOD_ID, "speedster/speed_boost_3");
    public static final ResourceLocation SKILL_SPEED_4 = new ResourceLocation(Genesis.MOD_ID, "speedster/speed_boost_4");
    public static final ResourceLocation SKILL_SPEED_5 = new ResourceLocation(Genesis.MOD_ID, "speedster/speed_boost_5");
    public static final ResourceLocation SKILL_SPEED_6 = new ResourceLocation(Genesis.MOD_ID, "speedster/speed_boost_6");
    public static final ResourceLocation SKILL_SPEED_7 = new ResourceLocation(Genesis.MOD_ID, "speedster/speed_boost_7");
    public static final ResourceLocation SKILL_SPEED_8 = new ResourceLocation(Genesis.MOD_ID, "speedster/speed_boost_8");
    public static final ResourceLocation SKILL_SPEED_9 = new ResourceLocation(Genesis.MOD_ID, "speedster/speed_boost_9");
    public static final ResourceLocation SKILL_SPEED_10 = new ResourceLocation(Genesis.MOD_ID, "speedster/speed_boost_10");
    public static final ResourceLocation SKILL_SPEED_HEALING_1 = new ResourceLocation(Genesis.MOD_ID, "speedster/speed_healing_1");
    public static final ResourceLocation SKILL_SPEED_HEALING_2 = new ResourceLocation(Genesis.MOD_ID, "speedster/speed_healing_2");
    public static final ResourceLocation SKILL_SPEED_HEALING_3 = new ResourceLocation(Genesis.MOD_ID, "speedster/speed_healing_3");
    public static final ResourceLocation SKILL_ACCELERATED_METABOLISM_1 = new ResourceLocation(Genesis.MOD_ID, "speedster/accelerated_metabolism_1");
    public static final ResourceLocation SKILL_ACCELERATED_METABOLISM_2 = new ResourceLocation(Genesis.MOD_ID, "speedster/accelerated_metabolism_2");
    public static final ResourceLocation SKILL_ACCELERATED_METABOLISM_3 = new ResourceLocation(Genesis.MOD_ID, "speedster/accelerated_metabolism_3");
    public static final ResourceLocation SKILL_RAPID_CONSTRUCTION = new ResourceLocation(Genesis.MOD_ID, "speedster/rapid_construction");
    public static final ResourceLocation SKILL_CALORIE_EFFICIENT = new ResourceLocation(Genesis.MOD_ID, "speedster/calorie_efficient");


    private static final List<ResourceLocation> SPEED_SKILL_TIERS = ImmutableList.of(
            SKILL_SPEED_1, SKILL_SPEED_2, SKILL_SPEED_3, SKILL_SPEED_4,
            SKILL_SPEED_5, SKILL_SPEED_6, SKILL_SPEED_7, SKILL_SPEED_8,
            SKILL_SPEED_9, SKILL_SPEED_10
    );

    // --- Power State ---
    private static final UUID BASE_SPEED_UUID = UUID.fromString("9a49a457-81dc-4aac-b4ec-94583f8c48cd");
    private static final UUID SPRINT_BOOST_UUID = UUID.fromString("0b6a2b3c-9b7a-4b1a-8b1a-9b6b7a2d1a3e");
    private static final UUID V9_BOOST_UUID = UUID.fromString("5c3f2b4a-4f1c-4b1a-8f1a-9b6b7a2d1a3e");
    private static final UUID STEP_HEIGHT_UUID = UUID.fromString("70a1d9de-f27f-4a7a-bafc-561f0b36b8ae");

    private final AttributeModifier stepHeightModifier = new AttributeModifier(STEP_HEIGHT_UUID, "Speedster Step Height", 1.0, AttributeModifier.Operation.ADDITION);

    private boolean isSpeedActive = true;
    private int sprintTicks = 0;
    private int currentSpeedTier = 10;
    private int healingTicker = 0;

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
        handleSpeedHealing(player);
        handleAcceleratedMetabolism(player);
    }

    private void handleAcceleratedMetabolism(Player player) {
        if (!isSpeedActive || player.getActiveEffects().isEmpty()) {
            return;
        }

        int ticksToAdvance = 0;
        if (isSkillUnlocked(SKILL_ACCELERATED_METABOLISM_3)) {
            ticksToAdvance = 19; // Effectively 20x faster (1 normal tick + 19 extra)
        } else if (isSkillUnlocked(SKILL_ACCELERATED_METABOLISM_2)) {
            ticksToAdvance = 9;  // Effectively 10x faster
        } else if (isSkillUnlocked(SKILL_ACCELERATED_METABOLISM_1)) {
            ticksToAdvance = 4;  // Effectively 5x faster
        }

        if (ticksToAdvance > 0) {
            for (MobEffectInstance effect : new ArrayList<>(player.getActiveEffects())) {
                if (effect.getDuration() > 1) {
                    for (int i = 0; i < ticksToAdvance; i++) {
                        // This safely ticks down the effect duration
                        effect.tick(player, () -> {});
                    }
                }
            }
        }
    }

    private void handleSpeedHealing(Player player) {
        if (!isSpeedActive || player.getHealth() >= player.getMaxHealth()) {
            return;
        }

        healingTicker++;

        if (isSkillUnlocked(SKILL_SPEED_HEALING_3) && healingTicker >= 20) {
            player.heal(1.0f);
            healingTicker = 0;
        } else if (isSkillUnlocked(SKILL_SPEED_HEALING_2) && healingTicker >= 60) {
            player.heal(1.0f);
            healingTicker = 0;
        } else if (isSkillUnlocked(SKILL_SPEED_HEALING_1) && healingTicker >= 100) {
            player.heal(1.0f);
            healingTicker = 0;
        }
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
                case 1: baseBonus = 1.0; break;
                case 2: baseBonus = 1.6; break;
                case 3: baseBonus = 2.4; break;
                case 4: baseBonus = 3.2; break;
                case 5: baseBonus = 4.0; break;
                case 6: baseBonus = 5.0; break;
                case 7: baseBonus = 6.0; break;
                case 8: baseBonus = 7.0; break;
                case 9: baseBonus = 8.0; break;
                case 10: baseBonus = 9.0; break;
            }

            if (baseBonus > 0) {
                AttributeModifier baseSpeedModifier = new AttributeModifier(BASE_SPEED_UUID, "Speedster Base Speed", baseBonus, AttributeModifier.Operation.MULTIPLY_TOTAL);
                speedAttribute.addPermanentModifier(baseSpeedModifier);
            }

            if (player.isSprinting()) {
                double sprintBonusMax = 0.30; // +30% sprint boost
                double sprintAcceleration = 0.01;
                sprintTicks++;
                double currentSprintBonus = Math.min(sprintBonusMax, sprintTicks * sprintAcceleration);

                AttributeModifier sprintModifier = new AttributeModifier(SPRINT_BOOST_UUID, "Speedster Sprint Boost", currentSprintBonus, AttributeModifier.Operation.MULTIPLY_TOTAL);
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
            message = Component.literal("Speed Force: ").withStyle(ChatFormatting.WHITE).append(Component.literal("ON").withStyle(ChatFormatting.YELLOW));
            pitch = 1.2F;
        } else {
            message = Component.literal("Speed Force: ").withStyle(ChatFormatting.WHITE).append(Component.literal("OFF").withStyle(ChatFormatting.GRAY));
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
        player.displayClientMessage(Component.literal("Velocity-9 surges through you!").withStyle(ChatFormatting.BLUE), true);
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
    public void activateSkill(Player player, int slot) {}
    @Override
    public void onActivate(Player player) {}
    @Override
    public ResourceLocation getRegistryName() { return registryName; }
    @Override
    public void setRegistryName(ResourceLocation name) { this.registryName = name; }
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
            Component message = Component.literal("Speed Force Level Up! ").withStyle(ChatFormatting.AQUA)
                    .append(Component.literal("[" + this.level + "]").withStyle(ChatFormatting.YELLOW));
            player.displayClientMessage(message, true);
            player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 1.2F);
        }
    }
    @Override
    public boolean isSkillUnlocked(ResourceLocation skillId) { return this.unlockedSkills.contains(skillId); }
    @Override
    public void setAbilityBinding(int slot, ResourceLocation skillId) {
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
