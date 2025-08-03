package net.bored.genesis.powers;

import net.bored.genesis.Genesis;
import net.bored.genesis.core.powers.ISkillPower;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

public class ArtificialSpeedsterPower implements ISkillPower {

    public static final ResourceLocation POWER_ID = new ResourceLocation(Genesis.MOD_ID, "artificial_speedster");
    private ResourceLocation registryName;

    // --- Power State ---
    private static final UUID SPEED_MODIFIER_UUID = UUID.fromString("c8a5b8e3-5b8a-4f8e-a2d1-9a7c6a4d7a8b");
    private double currentSpeedBonus = 0.0;
    private int degenerationTimer = 0; // Time in ticks before degeneration starts
    private int damageTicker = 0; // Ticker to control damage frequency

    private static final int DEGENERATION_GRACE_PERIOD = 6000; // 5 minutes (5 * 60 * 20)
    private static final int DAMAGE_INTERVAL = 40; // Every 2 seconds

    @Override
    public void onTick(Player player) {
        if (player.level().isClientSide) return;

        // Apply speed modifier
        AttributeInstance speedAttribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttribute != null) {
            speedAttribute.removeModifier(SPEED_MODIFIER_UUID); // Remove old before applying new
            if (currentSpeedBonus > 0) {
                AttributeModifier speedModifier = new AttributeModifier(SPEED_MODIFIER_UUID, "Artificial Speed Boost", currentSpeedBonus, AttributeModifier.Operation.MULTIPLY_TOTAL);
                speedAttribute.addPermanentModifier(speedModifier);
            }
        }

        // Handle degeneration
        if (degenerationTimer > 0) {
            degenerationTimer--;
        } else if (currentSpeedBonus > 0) { // Only degenerate if they've used V9 at least once
            damageTicker++;
            if (damageTicker >= DAMAGE_INTERVAL) {
                player.hurt(player.damageSources().magic(), 1.0f); // Unblockable magic damage
                damageTicker = 0;
            }
        }
    }

    public void applyVelocity9(Player player) {
        this.currentSpeedBonus += 0.25; // Each dose adds 25% to the speed multiplier
        this.degenerationTimer = DEGENERATION_GRACE_PERIOD; // Reset the grace period
        this.damageTicker = 0; // Reset damage ticker

        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.5f);
        player.sendSystemMessage(Component.literal("Your speed potential grows, but at what cost?"));
    }

    // --- Boilerplate and Data Management ---
    @Override
    public ResourceLocation getRegistryName() { return this.registryName; }
    @Override
    public void setRegistryName(ResourceLocation name) { this.registryName = name; }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putDouble("currentSpeedBonus", this.currentSpeedBonus);
        nbt.putInt("degenerationTimer", this.degenerationTimer);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        this.currentSpeedBonus = nbt.getDouble("currentSpeedBonus");
        this.degenerationTimer = nbt.getInt("degenerationTimer");
    }

    // --- Unused ISkillPower Methods ---
    @Override
    public void onPlayerUpdate(Player player) {}
    @Override
    public ResourceLocation getSkillTreeId() { return null; } // No skill tree
    @Override
    public boolean isSkillUnlocked(ResourceLocation skillId) { return false; }
    @Override
    public void unlockSkill(ResourceLocation skillId) {}
    @Override
    public void activateSkill(Player player, int slot) {}
    @Override
    public int getLevel() { return 0; }
    @Override
    public int getSkillPoints() { return 0; }
    @Override
    public void addSkillPoints(int amount) {}
    @Override
    public int getExperience() { return 0; }
    @Override
    public int getXpNeededForNextLevel() { return 0; }
    @Override
    public void addExperience(Player player, int amount) {}
    @Override
    public void setAbilityBinding(int slot, ResourceLocation skillId) {}
    @Override
    public ResourceLocation getAbilityBinding(int slot) { return null; }
    @Override
    public Map<Integer, ResourceLocation> getAbilityBindings() { return Collections.emptyMap(); }
    @Override
    public void onActivate(Player player) {}
    @Override
    public void onRemoved(Player player) {
        AttributeInstance speedAttribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttribute != null) {
            speedAttribute.removeModifier(SPEED_MODIFIER_UUID);
        }
    }
    @Override
    public void onPowerKey(Player player) {
        player.sendSystemMessage(Component.literal("This power is passive. Use Velocity-9 to increase its effect."));
    }
}