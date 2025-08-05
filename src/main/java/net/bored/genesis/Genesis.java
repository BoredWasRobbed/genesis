package net.bored.genesis;

import com.mojang.logging.LogUtils;
import net.bored.genesis.client.effects.LightningTrailHandler;
import net.bored.genesis.command.GenesisCommands;
import net.bored.genesis.core.events.CapabilityEvents;
import net.bored.genesis.core.events.PlayerEvents;
import net.bored.genesis.core.powers.PowerRegistry;
import net.bored.genesis.core.skills.SkillTreeManager;
import net.bored.genesis.item.CreativeModeTabs;
import net.bored.genesis.item.ItemRegistry;
import net.bored.genesis.network.PacketHandler;
import net.bored.genesis.util.Keybindings;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(Genesis.MOD_ID)
public class Genesis {
    public static final String MOD_ID = "genesis";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final SkillTreeManager SKILL_TREE_MANAGER = new SkillTreeManager();

    public Genesis() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register Content
        ItemRegistry.ITEMS.register(modEventBus);
        CreativeModeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        PowerRegistry.POWER_REGISTRY.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);

        // Register event handlers
        MinecraftForge.EVENT_BUS.register(new CapabilityEvents());
        MinecraftForge.EVENT_BUS.register(new PlayerEvents());
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(PacketHandler::register);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        GenesisCommands.register(event.getServer().getCommands().getDispatcher());
    }

    @SubscribeEvent
    public void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(SKILL_TREE_MANAGER);
        Genesis.LOGGER.info("Registered SkillTreeManager to reload listeners.");
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        // --- FIX: Add a field for the handler ---
        public static LightningTrailHandler lightningTrailHandler;

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // --- FIX: Instantiate the handler here ---
            // This event only runs on the client, making it safe from server-side crashes.
            lightningTrailHandler = new LightningTrailHandler();
        }

        @SubscribeEvent
        public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
            event.register(Keybindings.OPEN_SKILL_TREE_KEY);
            event.register(Keybindings.POWER_KEY);
            event.register(Keybindings.VARIANT_KEY);
            event.register(Keybindings.ABILITY_1_KEY);
            event.register(Keybindings.ABILITY_2_KEY);
            event.register(Keybindings.ABILITY_3_KEY);
            event.register(Keybindings.ABILITY_4_KEY);
            event.register(Keybindings.ABILITY_5_KEY);
        }
    }
}
