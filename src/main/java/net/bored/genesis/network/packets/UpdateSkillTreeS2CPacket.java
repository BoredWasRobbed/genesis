package net.bored.genesis.network.packets;

import net.bored.genesis.client.gui.SkillTreeScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class UpdateSkillTreeS2CPacket {

    private final int skillPoints;
    private final Set<ResourceLocation> unlockedSkills;
    private final int level;
    private final int experience;
    private final int xpToNextLevel;
    private final Map<Integer, ResourceLocation> abilityBindings;

    public UpdateSkillTreeS2CPacket(int skillPoints, Set<ResourceLocation> unlockedSkills, int level, int experience, int xpToNextLevel, Map<Integer, ResourceLocation> abilityBindings) {
        this.skillPoints = skillPoints;
        this.unlockedSkills = new HashSet<>(unlockedSkills);
        this.level = level;
        this.experience = experience;
        this.xpToNextLevel = xpToNextLevel;
        this.abilityBindings = new HashMap<>(abilityBindings);
    }

    public UpdateSkillTreeS2CPacket(FriendlyByteBuf buf) {
        this.skillPoints = buf.readInt();
        this.unlockedSkills = buf.readCollection(HashSet::new, FriendlyByteBuf::readResourceLocation);
        this.level = buf.readInt();
        this.experience = buf.readInt();
        this.xpToNextLevel = buf.readInt();
        this.abilityBindings = buf.readMap(FriendlyByteBuf::readInt, FriendlyByteBuf::readResourceLocation);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(skillPoints);
        buf.writeCollection(unlockedSkills, FriendlyByteBuf::writeResourceLocation);
        buf.writeInt(level);
        buf.writeInt(experience);
        buf.writeInt(xpToNextLevel);
        buf.writeMap(abilityBindings, FriendlyByteBuf::writeInt, FriendlyByteBuf::writeResourceLocation);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                Screen currentScreen = Minecraft.getInstance().screen;
                if (currentScreen instanceof SkillTreeScreen skillTreeScreen) {
                    skillTreeScreen.updateData(unlockedSkills, skillPoints, level, experience, xpToNextLevel, abilityBindings);
                }
            });
        });
        return true;
    }
}
