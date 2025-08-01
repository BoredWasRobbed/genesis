package net.bored.genesis.core.powers;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.util.INBTSerializable;

public interface IPower extends INBTSerializable<CompoundTag> {
    void onTick(Player player);
    void onActivate(Player player);

    /**
     * Called when this power is removed from a player.
     * This should be used for all cleanup logic, such as removing persistent
     * potion effects and resetting any internal state to default values.
     * @param player The player the power is being removed from.
     */
    void onRemoved(Player player);

    ResourceLocation getRegistryName();
    void setRegistryName(ResourceLocation name);
}