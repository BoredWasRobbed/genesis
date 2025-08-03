package net.bored.genesis.powers;

import net.bored.genesis.Genesis;
import net.bored.genesis.core.powers.ISkillPower;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.ForgeMod;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

public class ArtificialSpeedsterPower implements ISkillPower {

    public static final ResourceLocation POWER_ID = new ResourceLocation(Genesis.MOD_ID, "artificial_speedster");
    private ResourceLocation registryName;

    // --- Power State ---
    private static final UUID SPEED_MODIFIER_UUID = UUID.fromString("c8a5b8e3-5b8a-4f8e-a2d1-9a7c6a4d7a8b");
    private static final UUID STEP_HEIGHT_UUID = UUID.fromString("70a1d9de-f27f-4a7a-bafc-561f0b36b8ae");
    private final AttributeModifier stepHeightModifier = new AttributeModifier(STEP_HEIGHT_UUID, "Artificial Step Height", 1.0, AttributeModifier.Operation.ADDITION);

    private double v9SpeedBonus = 0.0;
    private int v9Doses = 0;
    private int v9DegenerationTimer = 0;
    private int damageTicker = 0;

    private static final int DEGENERATION_GRACE_PERIOD = 6000;
    private static final int DAMAGE_INTERVAL = 40;

    @Override
    public void onTick(Player player) {
        if (player.level().isClientSide) return;

        AttributeInstance speedAttribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttribute != null) {
            speedAttribute.removeModifier(SPEED_MODIFIER_UUID);
            if (v9SpeedBonus > 0) {
                AttributeModifier speedModifier = new AttributeModifier(SPEED_MODIFIER_UUID, "Artificial Speed Boost", v9SpeedBonus, AttributeModifier.Operation.MULTIPLY_TOTAL);
                speedAttribute.addPermanentModifier(speedModifier);
            }
        }

        AttributeInstance stepHeightAttribute = player.getAttribute(ForgeMod.STEP_HEIGHT_ADDITION.get());
        if (stepHeightAttribute != null && !stepHeightAttribute.hasModifier(stepHeightModifier)) {
            stepHeightAttribute.addPermanentModifier(stepHeightModifier);
        }

        if (v9DegenerationTimer > 0) {
            v9DegenerationTimer--;
        } else if (v9SpeedBonus > 0) {
            damageTicker++;
            if (damageTicker >= DAMAGE_INTERVAL) {
                player.hurt(player.damageSources().magic(), 1.0f);
                damageTicker = 0;
            }
        }
    }

    @Override
    public void applyVelocity9(Player player) {
        this.v9SpeedBonus += 0.5; // Artificial speedsters get a bigger permanent boost
        this.v9Doses++;
        this.v9DegenerationTimer = DEGENERATION_GRACE_PERIOD;
        this.damageTicker = 0;

        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.5f);
        player.sendSystemMessage(Component.literal("Your speed potential grows, but at what cost?"));
    }

    @Override
    public int getToxicity() {
        return this.v9Doses;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putDouble("v9SpeedBonus", this.v9SpeedBonus);
        nbt.putInt("v9Doses", this.v9Doses);
        nbt.putInt("v9DegenerationTimer", this.v9DegenerationTimer);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        this.v9SpeedBonus = nbt.getDouble("v9SpeedBonus");
        this.v9Doses = nbt.getInt("v9Doses");
        this.v9DegenerationTimer = nbt.getInt("v9DegenerationTimer");
    }

    @Override
    public void onRemoved(Player player) {
        AttributeInstance speedAttribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttribute != null) {
            speedAttribute.removeModifier(SPEED_MODIFIER_UUID);
        }
        AttributeInstance stepHeightAttribute = player.getAttribute(ForgeMod.STEP_HEIGHT_ADDITION.get());
        if (stepHeightAttribute != null) {
            stepHeightAttribute.removeModifier(stepHeightModifier);
        }
    }

    @Override
    public void onPowerKey(Player player) {
        player.sendSystemMessage(Component.literal("This power is passive. Use Velocity-9 to increase its effect."));
    }

    // --- Unused ISkillPower Methods ---
    @Override
    public void onPlayerUpdate(Player player) {}
    @Override
    public ResourceLocation getSkillTreeId() { return null; }
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
    public ResourceLocation getRegistryName() { return this.registryName; }
    @Override
    public void setRegistryName(ResourceLocation name) { this.registryName = name; }
}
