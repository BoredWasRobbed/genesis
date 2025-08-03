package net.bored.genesis.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.bored.genesis.Genesis;
import net.bored.genesis.core.capabilities.PowerCapability;
import net.bored.genesis.core.powers.IPower;
import net.bored.genesis.core.powers.ISkillPower;
import net.bored.genesis.core.powers.PowerManager;
import net.bored.genesis.core.powers.PowerRegistry;
import net.bored.genesis.core.skills.Skill;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public class PowerCommand {

    private static final SuggestionProvider<CommandSourceStack> POWER_SUGGESTIONS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(
                    PowerRegistry.REGISTRY.get().getKeys().stream().map(ResourceLocation::toString),
                    builder
            );

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("power")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("add").then(Commands.argument("power_id", ResourceLocationArgument.id()).suggests(POWER_SUGGESTIONS).executes(PowerCommand::addPower)))
                .then(Commands.literal("remove").then(Commands.argument("power_id", ResourceLocationArgument.id()).suggests((ctx, builder) -> { try { ServerPlayer player = ctx.getSource().getPlayerOrException(); player.getCapability(PowerCapability.POWER_MANAGER).ifPresent(manager -> { manager.getAllPowers().keySet().forEach(id -> builder.suggest(id.toString())); }); } catch (CommandSyntaxException e) {} return builder.buildFuture(); }).executes(PowerCommand::removePower)))
                .then(Commands.literal("list").executes(PowerCommand::listPowers))
                .then(Commands.literal("info").then(Commands.argument("power_id", ResourceLocationArgument.id()).suggests(POWER_SUGGESTIONS).executes(PowerCommand::info)))
                .then(Commands.literal("skill")
                        .then(Commands.literal("list").then(Commands.argument("power_id", ResourceLocationArgument.id()).suggests(POWER_SUGGESTIONS).executes(PowerCommand::listSkills)))
                        .then(Commands.literal("unlock").then(Commands.argument("power_id", ResourceLocationArgument.id()).suggests(POWER_SUGGESTIONS).then(Commands.argument("skill_id", ResourceLocationArgument.id()).suggests((ctx, builder) -> { try { ResourceLocation powerId = ResourceLocationArgument.getId(ctx, "power_id"); IPower power = PowerRegistry.REGISTRY.get().getValue(powerId); if (power instanceof ISkillPower skillPower) { Genesis.SKILL_TREE_MANAGER.getSkillTree(skillPower.getSkillTreeId()).ifPresent(tree -> tree.getAllSkills().keySet().forEach(id -> builder.suggest(id.toString()))); } } catch (Exception e) {} return builder.buildFuture(); }).executes(PowerCommand::unlockSkill))))
                        .then(Commands.literal("sp").then(Commands.literal("add").then(Commands.argument("power_id", ResourceLocationArgument.id()).suggests(POWER_SUGGESTIONS).then(Commands.argument("amount", IntegerArgumentType.integer()).executes(PowerCommand::addSkillPoints))))))
                .then(Commands.literal("xp").then(Commands.literal("add").then(Commands.argument("power_id", ResourceLocationArgument.id()).suggests(POWER_SUGGESTIONS).then(Commands.argument("amount", IntegerArgumentType.integer()).executes(PowerCommand::addExperience)))));
    }

    private static int info(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ResourceLocation powerId = ResourceLocationArgument.getId(context, "power_id");
        IPower power = PowerRegistry.getPower(powerId);

        if (power == null) {
            context.getSource().sendFailure(Component.literal("Power not found: " + powerId));
            return 0;
        }

        if (power instanceof ISkillPower skillPower && skillPower.getSkillTreeId() != null) {
            Genesis.SKILL_TREE_MANAGER.getSkillTree(skillPower.getSkillTreeId()).ifPresentOrElse(tree -> {
                context.getSource().sendSuccess(() -> Component.literal("--- Power Info for ").append(Component.literal(powerId.toString()).withStyle(ChatFormatting.AQUA)).append(" ---"), false);
                tree.getAllSkills().values().stream()
                        .sorted(Comparator.comparingInt(Skill::getSkillX)) // Sort by X coordinate only
                        .forEach(skill -> {
                            context.getSource().sendSuccess(() -> Component.literal(""), false); // Spacer
                            context.getSource().sendSuccess(() -> Component.literal(skill.getName().getString()).withStyle(ChatFormatting.GOLD), false);
                            context.getSource().sendSuccess(() -> Component.literal("  Cost: ").withStyle(ChatFormatting.GRAY).append(Component.literal(String.valueOf(skill.getCost())).withStyle(ChatFormatting.WHITE)), false);
                            context.getSource().sendSuccess(() -> Component.literal("  Description: ").withStyle(ChatFormatting.GRAY).append(Component.literal(skill.getInfoDescription().getString()).withStyle(ChatFormatting.WHITE)), false);
                        });
            }, () -> context.getSource().sendFailure(Component.literal("Could not find skill tree data for " + powerId)));
        } else {
            context.getSource().sendSuccess(() -> Component.literal(powerId + " is a basic power with no skill tree or detailed info."), false);
        }
        return 1;
    }

    private static int addExperience(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ResourceLocation powerId = ResourceLocationArgument.getId(context, "power_id");
        int amount = IntegerArgumentType.getInteger(context, "amount");
        PowerManager manager = player.getCapability(PowerCapability.POWER_MANAGER).orElse(null);
        if (manager == null) return 0;
        Optional<IPower> powerOpt = manager.getPower(powerId);
        if (powerOpt.isEmpty()) {
            context.getSource().sendFailure(Component.literal("Player does not have power: " + powerId));
            return 0;
        }
        if (powerOpt.get() instanceof ISkillPower skillPower) {
            skillPower.addExperience(player, amount);
            context.getSource().sendSuccess(() -> Component.literal("Gave " + amount + " XP for " + powerId), true);
        } else {
            context.getSource().sendFailure(Component.literal("Power " + powerId + " does not support experience."));
        }
        return 1;
    }

    private static int addPower(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ResourceLocation powerId = ResourceLocationArgument.getId(context, "power_id");
        IPower power = PowerRegistry.getPower(powerId);
        if (power == null) {
            context.getSource().sendFailure(Component.literal("Power not found: " + powerId));
            return 0;
        }
        player.getCapability(PowerCapability.POWER_MANAGER).ifPresent(manager -> {
            if (manager.hasPower(powerId)) {
                context.getSource().sendFailure(Component.literal("Player already has power: " + powerId));
            } else {
                manager.addPower(power);
                context.getSource().sendSuccess(() -> Component.literal("Gave power " + powerId + " to " + player.getName().getString()), true);
            }
        });
        return 1;
    }

    private static int removePower(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ResourceLocation powerId = ResourceLocationArgument.getId(context, "power_id");
        player.getCapability(PowerCapability.POWER_MANAGER).ifPresent(manager -> {
            if (!manager.hasPower(powerId)) {
                context.getSource().sendFailure(Component.literal("Player does not have power: " + powerId));
            } else {
                manager.removePower(powerId, player);
                context.getSource().sendSuccess(() -> Component.literal("Removed power " + powerId + " from " + player.getName().getString()), true);
            }
        });
        return 1;
    }

    private static int listPowers(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        player.getCapability(PowerCapability.POWER_MANAGER).ifPresent(manager -> {
            if (manager.getAllPowers().isEmpty()) {
                context.getSource().sendSuccess(() -> Component.literal("Player has no powers."), false);
            } else {
                context.getSource().sendSuccess(() -> Component.literal("Player Powers:"), false);
                manager.getAllPowers().keySet().forEach(id -> {
                    context.getSource().sendSuccess(() -> Component.literal("- " + id.toString()), false);
                });
            }
        });
        return 1;
    }

    private static int addSkillPoints(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ResourceLocation powerId = ResourceLocationArgument.getId(context, "power_id");
        int amount = IntegerArgumentType.getInteger(context, "amount");
        PowerManager manager = player.getCapability(PowerCapability.POWER_MANAGER).orElse(null);
        if (manager == null) return 0;
        Optional<IPower> powerOpt = manager.getPower(powerId);
        if (powerOpt.isEmpty()) {
            context.getSource().sendFailure(Component.literal("Player does not have power: " + powerId));
            return 0;
        }
        if (powerOpt.get() instanceof ISkillPower skillPower) {
            skillPower.addSkillPoints(amount);
            context.getSource().sendSuccess(() -> Component.literal("Gave " + amount + " skill points for " + powerId + ". New total: " + skillPower.getSkillPoints()), true);
        } else {
            context.getSource().sendFailure(Component.literal("Power " + powerId + " does not have a skill tree."));
        }
        return 1;
    }

    private static int listSkills(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ResourceLocation powerId = ResourceLocationArgument.getId(context, "power_id");
        PowerManager manager = player.getCapability(PowerCapability.POWER_MANAGER).orElse(null);
        if (manager == null) return 0;
        Optional<IPower> powerOpt = manager.getPower(powerId);
        if (powerOpt.isEmpty()) {
            context.getSource().sendFailure(Component.literal("Player does not have power: " + powerId));
            return 0;
        }
        if (powerOpt.get() instanceof ISkillPower skillPower) {
            Genesis.SKILL_TREE_MANAGER.getSkillTree(skillPower.getSkillTreeId()).ifPresentOrElse(tree -> {
                context.getSource().sendSuccess(() -> Component.literal("Skills for " + powerId + " (SP: " + skillPower.getSkillPoints() + "):"), false);
                tree.getAllSkills().values().forEach(skill -> {
                    String status = skillPower.isSkillUnlocked(skill.getId()) ? "§a[Unlocked]" : "§c[Locked]";
                    context.getSource().sendSuccess(() -> Component.literal("- " + skill.getId() + " " + status), false);
                });
            }, () -> context.getSource().sendFailure(Component.literal("Could not find skill tree with ID: " + skillPower.getSkillTreeId())));
        } else {
            context.getSource().sendFailure(Component.literal("Power " + powerId + " does not have a skill tree."));
        }
        return 1;
    }

    private static int unlockSkill(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ResourceLocation powerId = ResourceLocationArgument.getId(context, "power_id");
        ResourceLocation skillId = ResourceLocationArgument.getId(context, "skill_id");
        PowerManager manager = player.getCapability(PowerCapability.POWER_MANAGER).orElse(null);
        if (manager == null) return 0;
        Optional<IPower> powerOpt = manager.getPower(powerId);
        if (powerOpt.isEmpty()) {
            context.getSource().sendFailure(Component.literal("Player does not have power: " + powerId));
            return 0;
        }
        if (!(powerOpt.get() instanceof ISkillPower skillPower)) {
            context.getSource().sendFailure(Component.literal("Power " + powerId + " does not have a skill tree."));
            return 0;
        }
        Skill skill = Genesis.SKILL_TREE_MANAGER.getSkillTree(skillPower.getSkillTreeId())
                .flatMap(tree -> tree.getSkill(skillId))
                .orElse(null);
        if (skill == null) {
            context.getSource().sendFailure(Component.literal("Skill " + skillId + " not found in tree for " + powerId));
            return 0;
        }
        if (skillPower.isSkillUnlocked(skillId)) {
            context.getSource().sendFailure(Component.literal("Skill " + skillId + " is already unlocked."));
            return 0;
        }
        if (skillPower.getSkillPoints() < skill.getCost()) {
            context.getSource().sendFailure(Component.literal("Not enough skill points. Requires " + skill.getCost() + ", has " + skillPower.getSkillPoints()));
            return 0;
        }
        AtomicBoolean hasPrereqs = new AtomicBoolean(true);
        skill.getPrerequisites().forEach(prereqId -> {
            if (!skillPower.isSkillUnlocked(prereqId)) {
                context.getSource().sendFailure(Component.literal("Missing prerequisite: " + prereqId));
                hasPrereqs.set(false);
            }
        });
        if (hasPrereqs.get()) {
            skillPower.unlockSkill(skillId);
            context.getSource().sendSuccess(() -> Component.literal("Unlocked skill " + skillId), true);
        }
        return 1;
    }
}
