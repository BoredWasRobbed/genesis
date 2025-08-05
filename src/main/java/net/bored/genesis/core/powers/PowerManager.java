package net.bored.genesis.core.powers;

import net.bored.genesis.Genesis;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class PowerManager {

    private final Map<ResourceLocation, IPower> powers = new ConcurrentHashMap<>();

    public boolean hasPower(ResourceLocation powerId) { return powers.containsKey(powerId); }
    public Optional<IPower> getPower(ResourceLocation powerId) { return Optional.ofNullable(powers.get(powerId)); }
    public Map<ResourceLocation, IPower> getAllPowers() { return powers; }

    public void addPower(IPower power) {
        if (power != null && power.getRegistryName() != null) {
            powers.put(power.getRegistryName(), power);
        } else {
            Genesis.LOGGER.warn("Attempted to add a null power or a power with no registry name.");
        }
    }

    /**
     * Removes a power from the player and calls its cleanup method.
     * @param powerId The ID of the power to remove.
     * @param player The player from whom the power is being removed.
     */
    public void removePower(ResourceLocation powerId, Player player) {
        IPower power = powers.get(powerId);
        if (power != null) {
            power.onRemoved(player);
        }
        powers.remove(powerId);
    }

    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        ListTag powerList = new ListTag();
        for (IPower power : powers.values()) {
            CompoundTag powerTag = new CompoundTag();
            powerTag.putString("id", power.getRegistryName().toString());
            powerTag.put("data", power.serializeNBT());
            powerList.add(powerTag);
        }
        nbt.put("powers", powerList);
        return nbt;
    }

    public void deserializeNBT(CompoundTag nbt) {
        powers.clear();
        ListTag powerList = nbt.getList("powers", Tag.TAG_COMPOUND);

        for (int i = 0; i < powerList.size(); i++) {
            CompoundTag powerTag = powerList.getCompound(i);
            ResourceLocation id = new ResourceLocation(powerTag.getString("id"));
            IPower power = PowerRegistry.getPower(id);
            if (power != null) {
                power.deserializeNBT(powerTag.getCompound("data"));
                addPower(power);
                Genesis.LOGGER.debug("Successfully deserialized power: " + id);
            } else {
                Genesis.LOGGER.warn("Could not find power in registry for id: " + id + ". It will not be loaded.");
            }
        }
    }
}
