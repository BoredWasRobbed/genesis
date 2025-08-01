package net.bored.genesis.events;

import net.bored.genesis.Genesis;
import net.bored.genesis.network.PacketHandler;
import net.bored.genesis.network.packets.ActivateAbilityC2SPacket;
import net.bored.genesis.network.packets.RequestOpenSkillTreeC2SPacket;
import net.bored.genesis.util.Keybindings;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Genesis.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key e) {
        if (Keybindings.OPEN_SKILL_TREE_KEY.consumeClick()) {
            PacketHandler.sendToServer(new RequestOpenSkillTreeC2SPacket());
        }
        if (Keybindings.ABILITY_1_KEY.consumeClick()) {
            PacketHandler.sendToServer(new ActivateAbilityC2SPacket(0)); // Slot 0
        }
        if (Keybindings.ABILITY_2_KEY.consumeClick()) {
            PacketHandler.sendToServer(new ActivateAbilityC2SPacket(1)); // Slot 1
        }
        if (Keybindings.ABILITY_3_KEY.consumeClick()) {
            PacketHandler.sendToServer(new ActivateAbilityC2SPacket(2)); // Slot 2
        }
        if (Keybindings.ABILITY_4_KEY.consumeClick()) {
            PacketHandler.sendToServer(new ActivateAbilityC2SPacket(3)); // Slot 3
        }
        if (Keybindings.ABILITY_5_KEY.consumeClick()) {
            PacketHandler.sendToServer(new ActivateAbilityC2SPacket(4)); // Slot 4
        }
    }
}
