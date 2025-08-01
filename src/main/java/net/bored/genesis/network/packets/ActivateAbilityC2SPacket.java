package net.bored.genesis.network.packets;

import net.bored.genesis.core.capabilities.PowerCapability;
import net.bored.genesis.core.powers.ISkillPower;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * C2S packet sent when a player presses an ability keybind.
 */
public class ActivateAbilityC2SPacket {

    private final int slot;

    public ActivateAbilityC2SPacket(int slot) {
        this.slot = slot;
    }

    public ActivateAbilityC2SPacket(FriendlyByteBuf buf) {
        this.slot = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(slot);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            player.getCapability(PowerCapability.POWER_MANAGER).ifPresent(manager -> {
                // Find the first skill power the player has to activate the ability on.
                // A more advanced system might need to know which power the ability belongs to.
                Optional<ISkillPower> skillPowerOpt = manager.getAllPowers().values().stream()
                        .filter(p -> p instanceof ISkillPower)
                        .map(p -> (ISkillPower) p)
                        .findFirst();

                skillPowerOpt.ifPresent(skillPower -> skillPower.activateSkill(player, slot));
            });
        });
        return true;
    }
}
