package net.bored.genesis.network.packets;

import net.bored.genesis.Genesis;
import net.bored.genesis.core.capabilities.PowerCapability;
import net.bored.genesis.core.powers.ISkillPower;
import net.bored.genesis.network.PacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Optional;
import java.util.function.Supplier;

public class RequestOpenSkillTreeC2SPacket {

    public RequestOpenSkillTreeC2SPacket() {}

    public RequestOpenSkillTreeC2SPacket(FriendlyByteBuf buf) {}

    public void toBytes(FriendlyByteBuf buf) {}

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            player.getCapability(PowerCapability.POWER_MANAGER).ifPresent(manager -> {
                Optional<ISkillPower> skillPowerOpt = manager.getAllPowers().values().stream()
                        .filter(p -> p instanceof ISkillPower)
                        .map(p -> (ISkillPower) p)
                        .findFirst();

                if (skillPowerOpt.isPresent()) {
                    ISkillPower skillPower = skillPowerOpt.get();
                    // Correctly look up the tree in the manager
                    Genesis.SKILL_TREE_MANAGER.getSkillTree(skillPower.getSkillTreeId()).ifPresentOrElse(
                            tree -> PacketHandler.sendToPlayer(new OpenSkillTreeS2CPacket(skillPower, tree), player),
                            () -> player.sendSystemMessage(Component.literal("Error: Could not find skill tree data for " + skillPower.getSkillTreeId()))
                    );
                } else {
                    player.sendSystemMessage(Component.literal("You have no powers with a skill tree."));
                }
            });
        });
        return true;
    }
}
