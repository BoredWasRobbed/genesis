package net.bored.genesis.events;

import net.bored.genesis.Genesis;
import net.bored.genesis.network.PacketHandler;
import net.bored.genesis.network.packets.RequestOpenSkillTreeC2SPacket;
import net.bored.genesis.util.Keybindings;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// This annotation now correctly specifies the FORGE event bus, which is where
// input events are fired.
@Mod.EventBusSubscriber(modid = Genesis.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key e) {
        if (Keybindings.OPEN_SKILL_TREE_KEY.consumeClick()) {
            // When key is pressed, send a packet to the server asking for the GUI data.
            PacketHandler.sendToServer(new RequestOpenSkillTreeC2SPacket());
        }
    }
}
