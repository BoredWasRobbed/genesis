package net.bored.genesis.core.powers;

import net.minecraft.world.entity.player.Player;

/**
 * An interface for powers that can be affected by Velocity-9.
 * This allows for a unified system to handle the drug's effects across different speedster types.
 */
public interface IVelocitySusceptible {

    /**
     * Applies the effects of a Velocity-9 dose to the player.
     * @param player The player using the item.
     */
    void applyVelocity9(Player player);

    /**
     * @return The current toxicity level from Velocity-9 usage.
     */
    int getToxicity();

}
