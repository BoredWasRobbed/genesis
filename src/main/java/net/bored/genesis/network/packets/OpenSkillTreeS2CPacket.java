package net.bored.genesis.network.packets;

import com.google.common.collect.ImmutableMap;
import net.bored.genesis.client.gui.SkillTreeScreen;
import net.bored.genesis.core.powers.ISkillPower;
import net.bored.genesis.core.skills.Skill;
import net.bored.genesis.core.skills.SkillTree;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class OpenSkillTreeS2CPacket {

    private final ResourceLocation powerId;
    private final int skillPoints;
    private final Set<ResourceLocation> unlockedSkills;
    private final int level;
    private final int experience;
    private final int xpToNextLevel;
    private final List<Skill.Deserializer> skillDeserializers;

    public OpenSkillTreeS2CPacket(ISkillPower skillPower, SkillTree skillTree) {
        this.powerId = skillPower.getRegistryName();
        this.skillPoints = skillPower.getSkillPoints();
        this.unlockedSkills = skillTree.getAllSkills().keySet().stream()
                .filter(skillPower::isSkillUnlocked)
                .collect(Collectors.toSet());
        this.level = skillPower.getLevel();
        this.experience = skillPower.getExperience();
        this.xpToNextLevel = skillPower.getXpNeededForNextLevel();
        this.skillDeserializers = skillTree.getAllSkills().values().stream()
                .map(Skill.Deserializer::new)
                .collect(Collectors.toList());
    }

    public OpenSkillTreeS2CPacket(FriendlyByteBuf buf) {
        this.powerId = buf.readResourceLocation();
        this.skillPoints = buf.readInt();
        this.unlockedSkills = buf.readCollection(HashSet::new, FriendlyByteBuf::readResourceLocation);
        this.level = buf.readInt();
        this.experience = buf.readInt();
        this.xpToNextLevel = buf.readInt();
        this.skillDeserializers = buf.readList(Skill.Deserializer::new);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeResourceLocation(powerId);
        buf.writeInt(skillPoints);
        buf.writeCollection(unlockedSkills, FriendlyByteBuf::writeResourceLocation);
        buf.writeInt(level);
        buf.writeInt(experience);
        buf.writeInt(xpToNextLevel);
        buf.writeCollection(skillDeserializers, (b, ds) -> ds.toBytes(b));
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                Map<ResourceLocation, Skill> skills = new HashMap<>();
                for (Skill.Deserializer ds : skillDeserializers) {
                    Skill skill = ds.build();
                    skills.put(skill.getId(), skill);
                }
                SkillTree clientTree = new SkillTree(skills);
                Minecraft.getInstance().setScreen(new SkillTreeScreen(powerId, clientTree, unlockedSkills, skillPoints, level, experience, xpToNextLevel));
            });
        });
        return true;
    }
}
