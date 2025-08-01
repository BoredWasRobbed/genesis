package net.bored.genesis.network.packets;

import net.bored.genesis.Genesis;
import net.bored.genesis.core.capabilities.PowerCapability;
import net.bored.genesis.core.powers.ISkillPower;
import net.bored.genesis.core.skills.Skill;
import net.bored.genesis.network.PacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.NetworkEvent;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class UnlockSkillC2SPacket {

    private final ResourceLocation powerId;
    private final ResourceLocation skillId;

    public UnlockSkillC2SPacket(ResourceLocation powerId, ResourceLocation skillId) {
        this.powerId = powerId;
        this.skillId = skillId;
    }

    public UnlockSkillC2SPacket(FriendlyByteBuf buf) {
        this.powerId = buf.readResourceLocation();
        this.skillId = buf.readResourceLocation();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeResourceLocation(powerId);
        buf.writeResourceLocation(skillId);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            player.getCapability(PowerCapability.POWER_MANAGER).ifPresent(manager -> {
                manager.getPower(powerId).ifPresent(power -> {
                    if (!(power instanceof ISkillPower skillPower)) {
                        player.sendSystemMessage(Component.literal("Power " + powerId + " does not have a skill tree."));
                        return;
                    }

                    Genesis.SKILL_TREE_MANAGER.getSkillTree(skillPower.getSkillTreeId()).ifPresent(tree -> {
                        Skill skill = tree.getSkill(skillId).orElse(null);
                        if (skill == null) {
                            player.sendSystemMessage(Component.literal("Skill " + skillId + " not found."));
                            return;
                        }

                        if (skillPower.isSkillUnlocked(skillId)) {
                            player.sendSystemMessage(Component.literal("Skill " + skillId + " is already unlocked."));
                            return;
                        }

                        if (skillPower.getSkillPoints() < skill.getCost()) {
                            player.sendSystemMessage(Component.literal("Not enough skill points."));
                            return;
                        }

                        AtomicBoolean hasPrereqs = new AtomicBoolean(true);
                        skill.getPrerequisites().forEach(prereqId -> {
                            if (!skillPower.isSkillUnlocked(prereqId)) {
                                player.sendSystemMessage(Component.literal("Missing prerequisite: " + prereqId));
                                hasPrereqs.set(false);
                            }
                        });

                        if (hasPrereqs.get()) {
                            skillPower.unlockSkill(skillId);
                            player.sendSystemMessage(Component.literal("Unlocked skill: " + skill.getName().getString()));

                            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.75F, 1.0F);

                            Set<ResourceLocation> unlocked = tree.getAllSkills().keySet().stream()
                                    .filter(skillPower::isSkillUnlocked)
                                    .collect(Collectors.toSet());
                            PacketHandler.sendToPlayer(new UpdateSkillTreeS2CPacket(skillPower.getSkillPoints(), unlocked, skillPower.getLevel(), skillPower.getExperience(), skillPower.getXpNeededForNextLevel(), skillPower.getAbilityBindings()), player);
                        }
                    });
                });
            });
        });
        return true;
    }
}
