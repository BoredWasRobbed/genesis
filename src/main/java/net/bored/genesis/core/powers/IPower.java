package net.bored.genesis.core.powers;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.util.INBTSerializable;

public interface IPower extends INBTSerializable<CompoundTag> {
    void onTick(Player player);
    void onActivate(Player player);
    void onRemoved(Player player);

    /**
     * Called when the main "Power Key" is pressed.
     * Used for general power activation/deactivation.
     * @param player The player who pressed the key.
     */
    void onPowerKey(Player player);

    ResourceLocation getRegistryName();
    void setRegistryName(ResourceLocation name);

    /**
     * Gets the integer representation of the trail color (0xAARRGGBB).
     * Alpha is typically handled separately by the renderer.
     * A value of 0 can be used to signify that this power should not produce a trail.
     * @return The integer color of the trail, or 0 for no trail.
     */
    int getTrailColor();

    /**
     * Sets the trail color for this power.
     * @param color The new color as an integer (e.g., 0xFFFFFF00 for yellow).
     */
    void setTrailColor(int color);
}
