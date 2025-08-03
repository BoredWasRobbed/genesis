package net.bored.genesis.item;

import net.bored.genesis.Genesis;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class CreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Genesis.MOD_ID);

    public static final RegistryObject<CreativeModeTab> GENESIS_TAB = CREATIVE_MODE_TABS.register("genesis_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ItemRegistry.VIAL_SPEEDFORCE.get()))
                    .title(Component.translatable("itemGroup.genesis_tab"))
                    .displayItems((displayParameters, output) -> {
                        output.accept(ItemRegistry.VIAL.get());
                        output.accept(ItemRegistry.VIAL_SPEEDFORCE.get());
                        output.accept(ItemRegistry.SYRINGE.get());
                        output.accept(ItemRegistry.VELOCITY_9_SYRINGE.get());
                    })
                    .build());
}