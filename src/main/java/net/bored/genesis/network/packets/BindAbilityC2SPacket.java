package net.bored.genesis.network.packets;

import net.bored.genesis.core.capabilities.PowerCapability;
import net.bored.genesis.core.powers.ISkillPower;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * C2S packet sent when a player binds a skill to an ability slot in the GUI.
 */
public class BindAbilityC2SPacket {

    private final int slot;
    private final ResourceLocation skillId;

    public BindAbilityC2SPacket(int slot, ResourceLocation skillId) {
        this.slot = slot;
        this.skillId = skillId;
    }

    public BindAbilityC2SPacket(FriendlyByteBuf buf) {
        this.slot = buf.readInt();
        this.skillId = buf.readResourceLocation();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(slot);
        buf.writeResourceLocation(skillId);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            player.getCapability(PowerCapability.POWER_MANAGER).ifPresent(manager -> {
                // Find the first skill power to bind the ability to.
                Optional<ISkillPower> skillPowerOpt = manager.getAllPowers().values().stream()
                        .filter(p -> p instanceof ISkillPower)
                        .map(p -> (ISkillPower) p)
                        .findFirst();

                skillPowerOpt.ifPresent(skillPower -> {
                    skillPower.setAbilityBinding(slot, skillId);
                    // No need to send a sync packet back, as the client UI updates instantly.
                    // The data will be correct the next time the GUI is opened.
                });
            });
        });
        return true;
    }
}
