/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.jstech.core.dimension.RuntimeDimensions;
import dev.jstech.core.progression.IAxisStep;
import dev.jstech.core.progression.PlayerProgress;
import dev.jstech.core.progression.ProgressionAxes;
import dev.jstech.core.progression.ProgressionAxis;
import dev.jstech.core.region.ChunkLoaders;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * The Core's own commands: {@code /jstech progress <players> <axis> [<step>]}, where players stand along a progression
 * axis and, with a step, putting them there; {@code /jstech chunks <players> [release]}, how many chunks their
 * machines keep loaded and letting go of them; and {@code /jstech dimension create <id> <template>} and
 * {@code remove <id>}, a dimension made while the game runs and taken away again.
 */
@TextHolder
public final class CoreCommands {

    /* A player, the step they stand at, and the axis: "Alex is at Legacy (Hardware era)". */
    public static final TextKey PROGRESS_AT = TextKey.of("jscore.command.progress.at", "%s is at %s (%s)");
    public static final TextKey PROGRESS_SET = TextKey.of("jscore.command.progress.set", "%s is now at %s (%s)");
    public static final TextKey UNKNOWN_AXIS = TextKey.of("jscore.command.progress.unknown_axis",
            "There is no progression axis %s");
    public static final TextKey UNKNOWN_STEP = TextKey.of("jscore.command.progress.unknown_step",
            "That axis has no step %s");
    public static final TextKey CHUNKS_HELD = TextKey.of("jscore.command.chunks.held",
            "%s keeps %s chunks loaded");
    public static final TextKey CHUNKS_RELEASED = TextKey.of("jscore.command.chunks.released",
            "Let go of the %s chunks %s kept loaded");
    public static final TextKey DIMENSION_MADE = TextKey.of("jscore.command.dimension.made",
            "Made the dimension %s, a copy of %s");
    public static final TextKey DIMENSION_REMOVED = TextKey.of("jscore.command.dimension.removed",
            "Took away the dimension %s");
    public static final TextKey DIMENSION_NOT_MADE = TextKey.of("jscore.command.dimension.not_made",
            "%s is not a dimension made while the game runs");
    public static final TextKey NO_TEMPLATE = TextKey.of("jscore.command.dimension.no_template",
            "No dimension is declared as %s");

    private static final String PLAYERS = "players";
    private static final String AXIS = "axis";
    private static final String STEP = "step";
    private static final String DIMENSION = "dimension";
    private static final String TEMPLATE = "template";
    private static final DynamicCommandExceptionType NO_SUCH_TEMPLATE =
            new DynamicCommandExceptionType(id -> GameText.component(NO_TEMPLATE.with(String.valueOf(id))));
    private static final DynamicCommandExceptionType NOT_MADE =
            new DynamicCommandExceptionType(id -> GameText.component(DIMENSION_NOT_MADE.with(String.valueOf(id))));
    private static final SuggestionProvider<CommandSourceStack> TEMPLATES = (context, builder) ->
            SharedSuggestionProvider.suggestResource(context.getSource().getServer().registryAccess()
                    .registryOrThrow(Registries.LEVEL_STEM).keySet(), builder);
    private static final SuggestionProvider<CommandSourceStack> MADE = (context, builder) ->
            SharedSuggestionProvider.suggestResource(RuntimeDimensions.made(context.getSource().getServer())
                    .keySet(), builder);
    private static final DynamicCommandExceptionType NO_AXIS =
            new DynamicCommandExceptionType(id -> GameText.component(UNKNOWN_AXIS.with(String.valueOf(id))));
    private static final DynamicCommandExceptionType NO_STEP =
            new DynamicCommandExceptionType(name -> GameText.component(UNKNOWN_STEP.with(String.valueOf(name))));
    private static final SuggestionProvider<CommandSourceStack> AXES = (context, builder) ->
            SharedSuggestionProvider.suggestResource(ProgressionAxes.all().stream().map(ProgressionAxis::id), builder);
    private static final SuggestionProvider<CommandSourceStack> STEPS = (context, builder) -> {
        final ProgressionAxis<?> axis = ProgressionAxes.byId(ResourceLocationArgument.getId(context, AXIS));
        return axis == null ? builder.buildFuture()
                : SharedSuggestionProvider.suggest(axis.steps().stream().map(IAxisStep::serializedName), builder);
    };

    private CoreCommands() {
    }

    /** Declares the commands, from the Core's constructor. */
    public static void declare() {
        SeriesCommands.declare("progress", SeriesCommands.GAME_MASTERS, (node, context) -> node
                .then(Commands.argument(PLAYERS, EntityArgument.players())
                        .then(Commands.argument(AXIS, ResourceLocationArgument.id()).suggests(AXES)
                                .executes(CoreCommands::showProgress)
                                .then(Commands.argument(STEP, StringArgumentType.word()).suggests(STEPS)
                                        .executes(CoreCommands::setProgress)))));
        SeriesCommands.declare("chunks", SeriesCommands.GAME_MASTERS, (node, context) -> node
                .then(Commands.argument(PLAYERS, EntityArgument.players())
                        .executes(CoreCommands::showChunks)
                        .then(Commands.literal("release").executes(CoreCommands::releaseChunks))));
        SeriesCommands.declare("dimension", SeriesCommands.ADMINS, (node, context) -> node
                .then(Commands.literal("create")
                        .then(Commands.argument(DIMENSION, ResourceLocationArgument.id())
                                .then(Commands.argument(TEMPLATE, ResourceLocationArgument.id()).suggests(TEMPLATES)
                                        .executes(CoreCommands::createDimension))))
                .then(Commands.literal("remove")
                        .then(Commands.argument(DIMENSION, ResourceLocationArgument.id()).suggests(MADE)
                                .executes(CoreCommands::removeDimension))));
    }

    private static int showProgress(final CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        final ProgressionAxis<?> axis = axis(context);
        final Collection<ServerPlayer> players = EntityArgument.getPlayers(context, PLAYERS);
        for (final ServerPlayer player : players) {
            final IAxisStep at = PlayerProgress.reached(context.getSource().getServer(), player.getUUID(), axis);
            context.getSource().sendSuccess(() -> GameText.component(PROGRESS_AT.with(player.getScoreboardName(),
                    at.text(), axis.text())), false);
        }
        return players.size();
    }

    private static int setProgress(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return setTo(context, axis(context));
    }

    /* Typed by the axis, so the step put is one of its own. */
    private static <S extends IAxisStep> int setTo(final CommandContext<CommandSourceStack> context,
                                                   final ProgressionAxis<S> axis) throws CommandSyntaxException {
        final String name = StringArgumentType.getString(context, STEP);
        final S step = axis.byName(name);
        if (step == null) {
            throw NO_STEP.create(name);
        }
        final Collection<ServerPlayer> players = EntityArgument.getPlayers(context, PLAYERS);
        for (final ServerPlayer player : players) {
            PlayerProgress.set(context.getSource().getServer(), player.getUUID(), axis, step);
            context.getSource().sendSuccess(() -> GameText.component(PROGRESS_SET.with(player.getScoreboardName(),
                    step.text(), axis.text())), true);
        }
        return players.size();
    }

    private static int showChunks(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        int total = 0;
        for (final ServerPlayer player : EntityArgument.getPlayers(context, PLAYERS)) {
            final int held = ChunkLoaders.loadedBy(context.getSource().getServer(), player.getUUID());
            context.getSource().sendSuccess(() -> GameText.component(CHUNKS_HELD.with(player.getScoreboardName(),
                    held)), false);
            total += held;
        }
        return total;
    }

    private static int releaseChunks(final CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        int total = 0;
        for (final ServerPlayer player : EntityArgument.getPlayers(context, PLAYERS)) {
            final int released = ChunkLoaders.releaseOwnedBy(context.getSource().getServer(), player.getUUID());
            context.getSource().sendSuccess(() -> GameText.component(CHUNKS_RELEASED.with(released,
                    player.getScoreboardName())), true);
            total += released;
        }
        return total;
    }

    private static int createDimension(final CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        final ResourceLocation id = ResourceLocationArgument.getId(context, DIMENSION);
        final ResourceLocation template = ResourceLocationArgument.getId(context, TEMPLATE);
        final MinecraftServer server = context.getSource().getServer();
        if (RuntimeDimensions.stemOf(server, template).isEmpty()) {
            throw NO_SUCH_TEMPLATE.create(template);
        }
        RuntimeDimensions.getOrCreate(server, ResourceKey.create(Registries.DIMENSION, id), template);
        context.getSource().sendSuccess(() -> GameText.component(DIMENSION_MADE.with(id.toString(),
                template.toString())), true);
        return 1;
    }

    private static int removeDimension(final CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        final ResourceLocation id = ResourceLocationArgument.getId(context, DIMENSION);
        if (!RuntimeDimensions.remove(context.getSource().getServer(), ResourceKey.create(Registries.DIMENSION, id))) {
            throw NOT_MADE.create(id);
        }
        context.getSource().sendSuccess(() -> GameText.component(DIMENSION_REMOVED.with(id.toString())), true);
        return 1;
    }

    private static ProgressionAxis<?> axis(final CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        final ResourceLocation id = ResourceLocationArgument.getId(context, AXIS);
        final ProgressionAxis<?> axis = ProgressionAxes.byId(id);
        if (axis == null) {
            throw NO_AXIS.create(id);
        }
        return axis;
    }
}
