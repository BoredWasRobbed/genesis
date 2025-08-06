package net.bored.genesis.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Minecraft.class)
public interface MinecraftMixin {
    @Accessor("rightClickDelay")
    int genesis$getRightClickDelay();

    @Accessor("rightClickDelay")
    void genesis$setRightClickDelay(int delay);
}