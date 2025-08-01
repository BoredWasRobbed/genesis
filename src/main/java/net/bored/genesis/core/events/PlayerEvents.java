package net.bored.genesis.core.events;

import net.bored.genesis.core.capabilities.PowerCapability;
import net.bored.genesis.core.powers.ISkillPower;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class PlayerEvents {

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
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
}
