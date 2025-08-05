package net.bored.genesis.events;

import net.bored.genesis.Genesis;
import net.bored.genesis.network.PacketHandler;
import net.bored.genesis.network.packets.ActivateAbilityC2SPacket;
import net.bored.genesis.network.packets.ActivatePowerC2SPacket;
import net.bored.genesis.network.packets.CycleTopSpeedC2SPacket;
import net.bored.genesis.network.packets.RequestOpenSkillTreeC2SPacket;
import net.bored.genesis.util.Keybindings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Genesis.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key e) {
        if (Keybindings.OPEN_SKILL_TREE_KEY.consumeClick()) {
            PacketHandler.sendToServer(new RequestOpenSkillTreeC2SPacket());
        }
        if (Keybindings.POWER_KEY.consumeClick()) {
            if (Keybindings.VARIANT_KEY.isDown()) {
                PacketHandler.sendToServer(new CycleTopSpeedC2SPacket(true)); // Cycle Up
            } else if (Screen.hasControlDown()) {
                PacketHandler.sendToServer(new CycleTopSpeedC2SPacket(false)); // Cycle Down
            } else {
                PacketHandler.sendToServer(new ActivatePowerC2SPacket());
            }
        }
        if (Keybindings.ABILITY_1_KEY.consumeClick()) {
            PacketHandler.sendToServer(new ActivateAbilityC2SPacket(0));
        }
        if (Keybindings.ABILITY_2_KEY.consumeClick()) {
            PacketHandler.sendToServer(new ActivateAbilityC2SPacket(1));
        }
        if (Keybindings.ABILITY_3_KEY.consumeClick()) {
            PacketHandler.sendToServer(new ActivateAbilityC2SPacket(2));
        }
        if (Keybindings.ABILITY_4_KEY.consumeClick()) {
            PacketHandler.sendToServer(new ActivateAbilityC2SPacket(3));
        }
        if (Keybindings.ABILITY_5_KEY.consumeClick()) {
            PacketHandler.sendToServer(new ActivateAbilityC2SPacket(4));
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && Minecraft.getInstance().player != null) {
            // --- FIX: Use the safely instantiated handler from the main mod class ---
            if (Genesis.ClientModEvents.lightningTrailHandler != null) {
                Genesis.ClientModEvents.lightningTrailHandler.onClientTick();
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLevelLast(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            // --- FIX: Use the safely instantiated handler ---
            if (Genesis.ClientModEvents.lightningTrailHandler != null) {
                Genesis.ClientModEvents.lightningTrailHandler.onRender(event.getPoseStack(), event.getPartialTick());
            }
        }
    }
}
