package net.bored.genesis.network.packets;

import net.bored.genesis.core.capabilities.PowerCapability;
import net.bored.genesis.core.powers.IPower;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * C2S packet sent when a player presses the main "Power Key".
 */
public class ActivatePowerC2SPacket {

    public ActivatePowerC2SPacket() {
        // No data needed
    }

    public ActivatePowerC2SPacket(FriendlyByteBuf buf) {
        // No data needed
    }

    public void toBytes(FriendlyByteBuf buf) {
        // No data needed
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            player.getCapability(PowerCapability.POWER_MANAGER).ifPresent(manager -> {
                // Find the first power the player has to activate.
                // A more advanced system might let the player choose which power is "active".
                Optional<IPower> powerOpt = manager.getAllPowers().values().stream().findFirst();

                powerOpt.ifPresent(power -> power.onPowerKey(player));
            });
        });
        return true;
    }
}