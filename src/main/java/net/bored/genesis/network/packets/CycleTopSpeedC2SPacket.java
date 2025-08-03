package net.bored.genesis.network.packets;

import net.bored.genesis.core.capabilities.PowerCapability;
import net.bored.genesis.powers.NegativeSpeedsterPower;
import net.bored.genesis.powers.SpeedsterPower;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * C2S packet sent when a player presses a modifier + Power Key to cycle their top speed.
 */
public class CycleTopSpeedC2SPacket {

    private final boolean cycleUp;

    public CycleTopSpeedC2SPacket(boolean cycleUp) {
        this.cycleUp = cycleUp;
    }

    public CycleTopSpeedC2SPacket(FriendlyByteBuf buf) {
        this.cycleUp = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(this.cycleUp);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            player.getCapability(PowerCapability.POWER_MANAGER).ifPresent(manager -> {
                // Find the first applicable speedster power and cycle its speed.
                manager.getAllPowers().values().stream()
                        .filter(p -> p instanceof SpeedsterPower || p instanceof NegativeSpeedsterPower)
                        .findFirst()
                        .ifPresent(power -> {
                            if (power instanceof SpeedsterPower sp) {
                                sp.cycleTopSpeed(player, this.cycleUp);
                            } else if (power instanceof NegativeSpeedsterPower nsp) {
                                nsp.cycleTopSpeed(player, this.cycleUp);
                            }
                        });
            });
        });
        return true;
    }
}
