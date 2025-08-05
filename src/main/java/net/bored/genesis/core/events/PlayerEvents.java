package net.bored.genesis.core.events;

import net.bored.genesis.Genesis;
import net.bored.genesis.core.capabilities.PowerCapability;
import net.bored.genesis.core.powers.ISkillPower;
import net.bored.genesis.network.PacketHandler;
import net.bored.genesis.network.packets.SyncPowerDataS2CPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Genesis.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerEvents {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.player.level().isClientSide) {
            Player player = event.player;
            player.getCapability(PowerCapability.POWER_MANAGER).ifPresent(powerManager -> {
                powerManager.getAllPowers().values().forEach(power -> {
                    // Call the standard tick for all powers
                    power.onTick(player);

                    // If it's a skill power, also call the update for XP gain
                    if (power instanceof ISkillPower skillPower) {
                        skillPower.onPlayerUpdate(player);
                    }
                });
            });
        }
    }

    /**
     * Synchronizes a player's power data to their client upon logging in.
     * This is the crucial step that ensures the client knows which powers the player has.
     */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(PowerCapability.POWER_MANAGER).ifPresent(powerManager -> {
                Genesis.LOGGER.info("Player {} logged in. Syncing {} powers to client.", player.getName().getString(), powerManager.getAllPowers().size());
                powerManager.getAllPowers().values().forEach(power -> {
                    PacketHandler.sendToPlayer(new SyncPowerDataS2CPacket(power.getRegistryName(), power.serializeNBT()), player);
                });
            });
        }
    }
}
