package net.bored.genesis.core.powers;

import net.bored.genesis.Genesis;
import net.bored.genesis.powers.ArtificialSpeedsterPower;
import net.bored.genesis.powers.NegativeSpeedsterPower;
import net.bored.genesis.powers.SpeedsterPower;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryBuilder;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class PowerRegistry {

    public static final DeferredRegister<IPower> POWER_REGISTRY =
            DeferredRegister.create(new ResourceLocation(Genesis.MOD_ID, "powers"), Genesis.MOD_ID);

    public static final Supplier<IForgeRegistry<IPower>> REGISTRY = POWER_REGISTRY.makeRegistry(RegistryBuilder::new);

    /**
     * Creates a new instance of a power from its registered ID.
     * This is critical for giving each player their own data instance of a power.
     * @param id The ResourceLocation of the power.
     * @return A new IPower instance, or null if not found.
     */
    public static IPower getPower(ResourceLocation id) {
        // Get the singleton instance from the registry to find the class and registry name
        IPower registeredPower = REGISTRY.get().getValue(id);
        if (registeredPower != null) {
            try {
                // Create a new instance of the power's class
                IPower newInstance = registeredPower.getClass().getDeclaredConstructor().newInstance();
                // CRITICAL: Set the registry name on the new instance. This was the bug.
                newInstance.setRegistryName(id);
                return newInstance;
            } catch (Exception e) {
                Genesis.LOGGER.error("Failed to create new instance of power: " + id, e);
                return null;
            }
        }
        return null;
    }

    // --- REGISTER POWERS HERE ---
    public static final RegistryObject<IPower> SPEEDSTER = POWER_REGISTRY.register("speedster", SpeedsterPower::new);
    public static final RegistryObject<IPower> NEGATIVE_SPEEDSTER = POWER_REGISTRY.register("negative_speedster", NegativeSpeedsterPower::new);
    public static final RegistryObject<IPower> ARTIFICIAL_SPEEDSTER = POWER_REGISTRY.register("artificial_speedster", ArtificialSpeedsterPower::new);

}