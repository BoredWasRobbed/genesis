package net.bored.genesis.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Invoker("updateUsingItem")
    public abstract void genesis$invokeUpdateUsingItem(ItemStack stack);
}