package net.bored.genesis.network.packets;

import net.bored.genesis.Genesis;
import net.bored.genesis.core.capabilities.PowerCapability;
import net.bored.genesis.core.powers.IPower;
import net.bored.genesis.core.powers.PowerRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * S2C packet to send the complete, updated data of a single power from the server to the client.
 * This is used to sync changes like trail color and to perform the initial sync on login.
 */
public class SyncPowerDataS2CPacket {

    private final ResourceLocation powerId;
    private final CompoundTag powerData;

    public SyncPowerDataS2CPacket(ResourceLocation powerId, CompoundTag powerData) {
        this.powerId = powerId;
        this.powerData = powerData;
    }

    public SyncPowerDataS2CPacket(FriendlyByteBuf buf) {
        this.powerId = buf.readResourceLocation();
        this.powerData = buf.readNbt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeResourceLocation(powerId);
        buf.writeNbt(powerData);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                if (Minecraft.getInstance().player != null) {
                    Minecraft.getInstance().player.getCapability(PowerCapability.POWER_MANAGER).ifPresent(manager -> {
                        IPower power = manager.getPower(powerId).orElse(null);

                        if (power == null) {
                            power = PowerRegistry.getPower(powerId);
                            if (power != null) {
                                manager.addPower(power);
                            } else {
                                Genesis.LOGGER.warn("Failed to create client instance for " + powerId);
                                return;
                            }
                        }

                        power.deserializeNBT(powerData);
                    });
                }
            });
        });
        return true;
    }
}
