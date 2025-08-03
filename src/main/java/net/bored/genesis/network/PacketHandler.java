package net.bored.genesis.network;

import net.bored.genesis.Genesis;
import net.bored.genesis.network.packets.ActivateAbilityC2SPacket;
import net.bored.genesis.network.packets.ActivatePowerC2SPacket;
import net.bored.genesis.network.packets.BindAbilityC2SPacket;
import net.bored.genesis.network.packets.CycleTopSpeedC2SPacket;
import net.bored.genesis.network.packets.OpenSkillTreeS2CPacket;
import net.bored.genesis.network.packets.RequestOpenSkillTreeC2SPacket;
import net.bored.genesis.network.packets.UnlockSkillC2SPacket;
import net.bored.genesis.network.packets.UpdateSkillTreeS2CPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class PacketHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Genesis.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;
    private static int id() {
        return packetId++;
    }

    public static void register() {
        // C2S
        INSTANCE.messageBuilder(UnlockSkillC2SPacket.class, id(), NetworkDirection.PLAY_TO_SERVER).encoder(UnlockSkillC2SPacket::toBytes).decoder(UnlockSkillC2SPacket::new).consumerMainThread(UnlockSkillC2SPacket::handle).add();
        INSTANCE.messageBuilder(RequestOpenSkillTreeC2SPacket.class, id(), NetworkDirection.PLAY_TO_SERVER).encoder(RequestOpenSkillTreeC2SPacket::toBytes).decoder(RequestOpenSkillTreeC2SPacket::new).consumerMainThread(RequestOpenSkillTreeC2SPacket::handle).add();
        INSTANCE.messageBuilder(ActivateAbilityC2SPacket.class, id(), NetworkDirection.PLAY_TO_SERVER).encoder(ActivateAbilityC2SPacket::toBytes).decoder(ActivateAbilityC2SPacket::new).consumerMainThread(ActivateAbilityC2SPacket::handle).add();
        INSTANCE.messageBuilder(BindAbilityC2SPacket.class, id(), NetworkDirection.PLAY_TO_SERVER).encoder(BindAbilityC2SPacket::toBytes).decoder(BindAbilityC2SPacket::new).consumerMainThread(BindAbilityC2SPacket::handle).add();
        INSTANCE.messageBuilder(ActivatePowerC2SPacket.class, id(), NetworkDirection.PLAY_TO_SERVER).encoder(ActivatePowerC2SPacket::toBytes).decoder(ActivatePowerC2SPacket::new).consumerMainThread(ActivatePowerC2SPacket::handle).add();
        INSTANCE.messageBuilder(CycleTopSpeedC2SPacket.class, id(), NetworkDirection.PLAY_TO_SERVER).encoder(CycleTopSpeedC2SPacket::toBytes).decoder(CycleTopSpeedC2SPacket::new).consumerMainThread(CycleTopSpeedC2SPacket::handle).add();

        // S2C
        INSTANCE.messageBuilder(OpenSkillTreeS2CPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT).encoder(OpenSkillTreeS2CPacket::toBytes).decoder(OpenSkillTreeS2CPacket::new).consumerMainThread(OpenSkillTreeS2CPacket::handle).add();
        INSTANCE.messageBuilder(UpdateSkillTreeS2CPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT).encoder(UpdateSkillTreeS2CPacket::toBytes).decoder(UpdateSkillTreeS2CPacket::new).consumerMainThread(UpdateSkillTreeS2CPacket::handle).add();
    }

    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }
}
