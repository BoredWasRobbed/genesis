package net.bored.genesis.network.packets;

import net.bored.genesis.Genesis;
import net.bored.genesis.core.capabilities.PowerCapability;
import net.bored.genesis.core.powers.ISkillPower;
import net.bored.genesis.core.skills.Skill;
import net.bored.genesis.network.PacketHandler;
import net.minecraft.ChatFormatting;
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
                        return;
                    }

                    Genesis.SKILL_TREE_MANAGER.getSkillTree(skillPower.getSkillTreeId()).ifPresent(tree -> {
                        Skill skill = tree.getSkill(skillId).orElse(null);
                        if (skill == null) {
                            return;
                        }

                        if (skillPower.isSkillUnlocked(skillId)) {
                            return;
                        }

                        if (skillPower.getSkillPoints() < skill.getCost()) {
                            player.displayClientMessage(Component.literal("Not enough Skill Points").withStyle(ChatFormatting.RED), true);
                            player.level().playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_DIDGERIDOO.get(), SoundSource.PLAYERS, 0.7F, 1.5F);
                            return;
                        }

                        AtomicBoolean isExcluded = new AtomicBoolean(false);
                        tree.getAllSkills().values().forEach(s -> {
                            if (skillPower.isSkillUnlocked(s.getId())) {
                                if (s.getExclusions().contains(skillId)) {
                                    player.displayClientMessage(Component.literal("Blocked by " + s.getName().getString()).withStyle(ChatFormatting.RED), true);
                                    player.level().playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_DIDGERIDOO.get(), SoundSource.PLAYERS, 0.7F, 1.5F);
                                    isExcluded.set(true);
                                }
                                if (skill.getExclusions().contains(s.getId())) {
                                    player.displayClientMessage(Component.literal("Conflicts with " + s.getName().getString()).withStyle(ChatFormatting.RED), true);
                                    player.level().playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_DIDGERIDOO.get(), SoundSource.PLAYERS, 0.7F, 1.5F);
                                    isExcluded.set(true);
                                }
                            }
                        });

                        if (isExcluded.get()) {
                            return;
                        }

                        boolean hasPrereqs;
                        if (skill.unlocksWithAnyPrerequisite()) {
                            hasPrereqs = skill.getPrerequisites().stream().anyMatch(skillPower::isSkillUnlocked);
                        } else {
                            hasPrereqs = skill.getPrerequisites().stream().allMatch(skillPower::isSkillUnlocked);
                        }


                        if (hasPrereqs) {
                            skillPower.unlockSkill(skillId);
                            player.displayClientMessage(Component.literal("Unlocked: ").withStyle(ChatFormatting.GREEN).append(skill.getName()), true);
                            player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.75F, 1.0F);

                            Set<ResourceLocation> unlocked = tree.getAllSkills().keySet().stream()
                                    .filter(skillPower::isSkillUnlocked)
                                    .collect(Collectors.toSet());
                            PacketHandler.sendToPlayer(new UpdateSkillTreeS2CPacket(skillPower.getSkillPoints(), unlocked, skillPower.getActiveSkills(), skillPower.getLevel(), skillPower.getExperience(), skillPower.getXpNeededForNextLevel(), skillPower.getAbilityBindings()), player);
                        } else {
                            player.displayClientMessage(Component.literal("Missing prerequisite").withStyle(ChatFormatting.RED), true);
                            player.level().playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_DIDGERIDOO.get(), SoundSource.PLAYERS, 0.7F, 1.5F);
                        }
                    });
                });
            });
        });
        return true;
    }
}