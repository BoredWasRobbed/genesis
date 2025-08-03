package net.bored.genesis.item;

import net.bored.genesis.Genesis;
import net.bored.genesis.item.custom.Velocity9SyringeItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ItemRegistry {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, Genesis.MOD_ID);

    public static final RegistryObject<Item> VIAL = ITEMS.register("vial",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> VIAL_SPEEDFORCE = ITEMS.register("vial_speedforce",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> SYRINGE = ITEMS.register("syringe",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> VELOCITY_9_SYRINGE = ITEMS.register("velocity_9_syringe",
            () -> new Velocity9SyringeItem(new Item.Properties()));
}