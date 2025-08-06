package net.bored.genesis.network.packets;

import net.bored.genesis.Genesis;
import net.bored.genesis.core.capabilities.PowerCapability;
import net.bored.genesis.core.powers.ISkillPower;
import net.bored.genesis.network.PacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * C2S packet sent when a player right-clicks a toggleable skill in the GUI.
 */
public class ToggleSkillC2SPacket {

    private final ResourceLocation skillId;

    public ToggleSkillC2SPacket(ResourceLocation skillId) {
        this.skillId = skillId;
    }

    public ToggleSkillC2SPacket(FriendlyByteBuf buf) {
        this.skillId = buf.readResourceLocation();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeResourceLocation(this.skillId);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            player.getCapability(PowerCapability.POWER_MANAGER).ifPresent(manager -> {
                // Find the first skill power and toggle the skill
                manager.getAllPowers().values().stream()
                        .filter(p -> p instanceof ISkillPower)
                        .map(p -> (ISkillPower) p)
                        .findFirst()
                        .ifPresent(skillPower -> {
                            skillPower.toggleSkill(this.skillId);

                            // --- FIX: Send a full data sync to the client ---
                            PacketHandler.sendToPlayer(new SyncPowerDataS2CPacket(skillPower.getRegistryName(), skillPower.serializeNBT()), player);

                            // Also update the GUI if it's open
                            Genesis.SKILL_TREE_MANAGER.getSkillTree(skillPower.getSkillTreeId()).ifPresent(tree -> {
                                Set<ResourceLocation> unlocked = tree.getAllSkills().keySet().stream()
                                        .filter(skillPower::isSkillUnlocked)
                                        .collect(Collectors.toSet());

                                PacketHandler.sendToPlayer(new UpdateSkillTreeS2CPacket(
                                        skillPower.getSkillPoints(),
                                        unlocked, // Send all unlocked skills
                                        skillPower.getActiveSkills(), // Send active skills
                                        skillPower.getLevel(),
                                        skillPower.getExperience(),
                                        skillPower.getXpNeededForNextLevel(),
                                        skillPower.getAbilityBindings()
                                ), player);
                            });
                        });
            });
        });
        return true;
    }
}
