package net.bored.genesis.core.capabilities;

import net.bored.genesis.core.powers.PowerManager;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

public class PowerCapability {
    public static final Capability<PowerManager> POWER_MANAGER =
            CapabilityManager.get(new CapabilityToken<>() {});
}