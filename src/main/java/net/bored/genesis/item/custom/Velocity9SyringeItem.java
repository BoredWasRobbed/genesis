package net.bored.genesis.item.custom;

import net.bored.genesis.core.capabilities.PowerCapability;
import net.bored.genesis.core.powers.IVelocitySusceptible;
import net.bored.genesis.item.ItemRegistry;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

public class Velocity9SyringeItem extends Item {

    public Velocity9SyringeItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        if (!(livingEntity instanceof ServerPlayer player)) {
            return stack;
        }

        // Check for any power that is susceptible to Velocity-9
        player.getCapability(PowerCapability.POWER_MANAGER).ifPresent(manager -> {
            manager.getAllPowers().values().stream()
                    .filter(p -> p instanceof IVelocitySusceptible)
                    .map(p -> (IVelocitySusceptible) p)
                    .findFirst() // Affect the first susceptible power found
                    .ifPresent(susceptiblePower -> susceptiblePower.applyVelocity9(player));
        });

        CriteriaTriggers.CONSUME_ITEM.trigger(player, stack);
        player.awardStat(Stats.ITEM_USED.get(this));

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        // Return empty syringe
        ItemStack emptySyringe = new ItemStack(ItemRegistry.SYRINGE.get());
        if (!player.getInventory().add(emptySyringe)) {
            player.drop(emptySyringe, false);
        }

        return stack.isEmpty() ? ItemStack.EMPTY : stack;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 32; // Time it takes to "drink" the item
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK; // Shows the drinking animation
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }
}
