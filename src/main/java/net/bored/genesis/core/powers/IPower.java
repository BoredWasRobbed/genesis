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
}
