package net.bored.genesis.util;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * A utility class for sending debug messages to the in-game chat on the client.
 */
public class ClientDebugHelper {

    private static final String PREFIX = "[Genesis Debug] ";

    public static void send(String message) {
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            player.sendSystemMessage(Component.literal(PREFIX).withStyle(ChatFormatting.GOLD).append(Component.literal(message).withStyle(ChatFormatting.WHITE)));
        }
    }

    public static void sendError(String message) {
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            player.sendSystemMessage(Component.literal(PREFIX).withStyle(ChatFormatting.GOLD).append(Component.literal(message).withStyle(ChatFormatting.RED)));
        }
    }
}
