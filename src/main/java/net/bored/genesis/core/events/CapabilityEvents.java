package net.bored.genesis.core.events;

import net.bored.genesis.Genesis;
import net.bored.genesis.core.capabilities.PowerCapability;
import net.bored.genesis.core.powers.PowerManager;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CapabilityEvents {

    private static final ResourceLocation POWER_CAPABILITY_ID = new ResourceLocation(Genesis.MOD_ID, "power_manager");

    @SubscribeEvent
    public void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(POWER_CAPABILITY_ID, new ICapabilitySerializable<CompoundTag>() {
                private final PowerManager instance = new PowerManager();
                private final LazyOptional<PowerManager> optional = LazyOptional.of(() -> instance);

                @NotNull @Override
                public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
                    return PowerCapability.POWER_MANAGER.orEmpty(cap, optional);
                }

                @Override public CompoundTag serializeNBT() { return instance.serializeNBT(); }
                @Override public void deserializeNBT(CompoundTag nbt) { instance.deserializeNBT(nbt); }
            });
        }
    }

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) {
            return;
        }
        Player originalPlayer = event.getOriginal();
        Player newPlayer = event.getEntity();
        originalPlayer.getCapability(PowerCapability.POWER_MANAGER).ifPresent(oldManager -> {
            newPlayer.getCapability(PowerCapability.POWER_MANAGER).ifPresent(newManager -> {
                newManager.deserializeNBT(oldManager.serializeNBT());
            });
        });
        Genesis.LOGGER.debug("Cloned power data for player " + newPlayer.getName().getString());
    }
}