/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.world.event;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.jstech.core.command.SeriesCommands;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

/**
 * What an operator does with the world's events, in the dimension they stand in: {@code /jstech event list}, what is
 * planned and under way; {@code start <type> [<ticks>]}, one begun now; {@code plan <type> <ticks>}, one planned to
 * begin that far ahead, warned of as it nears; {@code delay <type> <ticks>}, a planned one put off; {@code stop
 * <type>}, one ended now. One that reaches only so far happens round the operator's place.
 */
@TextHolder
public final class WorldEventCommands {

    public static final TextKey NONE = TextKey.of("jscore.command.event.none", "No world event is planned here");
    public static final TextKey COMING = TextKey.of("jscore.command.event.coming", "%s: begins in %s");
    public static final TextKey UNDER_WAY = TextKey.of("jscore.command.event.under_way", "%s: under way, %s left");
    public static final TextKey STARTED = TextKey.of("jscore.command.event.started", "Started %s");
    public static final TextKey PLANNED = TextKey.of("jscore.command.event.planned", "Planned %s in %s");
    public static final TextKey STOPPED = TextKey.of("jscore.command.event.stopped", "Stopped %s");
    public static final TextKey UNKNOWN = TextKey.of("jscore.command.event.unknown",
            "No world event is declared as %s");
    public static final TextKey REFUSED = TextKey.of("jscore.command.event.refused",
            "%s is planned or under way already, or does not happen here");
    public static final TextKey NOT_PLANNED = TextKey.of("jscore.command.event.not_planned",
            "%s is neither planned nor under way here");
    public static final TextKey DELAYED = TextKey.of("jscore.command.event.delayed", "Put %s off by %s");
    public static final TextKey NOT_YET = TextKey.of("jscore.command.event.not_yet",
            "%s is not planned here, or has begun already");

    private static final String TYPE = "type";
    private static final String TICKS = "ticks";
    private static final int LONGEST = 24_000 * 365;
    private static final DynamicCommandExceptionType NO_TYPE =
            new DynamicCommandExceptionType(id -> GameText.component(UNKNOWN.with(String.valueOf(id))));
    private static final DynamicCommandExceptionType CANNOT =
            new DynamicCommandExceptionType(id -> GameText.component(REFUSED.with(String.valueOf(id))));
    private static final DynamicCommandExceptionType NOT_THERE =
            new DynamicCommandExceptionType(id -> GameText.component(NOT_PLANNED.with(String.valueOf(id))));
    private static final DynamicCommandExceptionType NOT_COMING =
            new DynamicCommandExceptionType(id -> GameText.component(NOT_YET.with(String.valueOf(id))));
    private static final SuggestionProvider<CommandSourceStack> TYPES = (context, builder) ->
            SharedSuggestionProvider.suggestResource(WorldEvents.all(context.getSource().getLevel()).keySet(),
                    builder);

    private WorldEventCommands() {
    }

    /** Declares the commands, from the Core's constructor. */
    public static void declare() {
        SeriesCommands.declare("event", SeriesCommands.GAME_MASTERS, (node, context) -> node
                .then(Commands.literal("list").executes(WorldEventCommands::list))
                .then(Commands.literal("start")
                        .then(Commands.argument(TYPE, ResourceLocationArgument.id()).suggests(TYPES)
                                .executes(command -> start(command, 0))
                                .then(Commands.argument(TICKS, IntegerArgumentType.integer(1, LONGEST))
                                        .executes(command -> start(command, IntegerArgumentType.getInteger(command,
                                                TICKS))))))
                .then(Commands.literal("plan")
                        .then(Commands.argument(TYPE, ResourceLocationArgument.id()).suggests(TYPES)
                                .then(Commands.argument(TICKS, IntegerArgumentType.integer(0, LONGEST))
                                        .executes(WorldEventCommands::plan))))
                .then(Commands.literal("delay")
                        .then(Commands.argument(TYPE, ResourceLocationArgument.id()).suggests(TYPES)
                                .then(Commands.argument(TICKS, IntegerArgumentType.integer(1, LONGEST))
                                        .executes(WorldEventCommands::delay))))
                .then(Commands.literal("stop")
                        .then(Commands.argument(TYPE, ResourceLocationArgument.id()).suggests(TYPES)
                                .executes(WorldEventCommands::stop))));
    }

    private static int list(final CommandContext<CommandSourceStack> context) {
        final ServerLevel level = context.getSource().getLevel();
        final List<WorldEvent> planned = WorldEvents.planned(level);
        if (planned.isEmpty()) {
            context.getSource().sendSuccess(() -> GameText.component(NONE.text()), false);
            return 0;
        }
        final long now = level.getGameTime();
        for (final WorldEvent event : planned) {
            final Object name = WorldEvents.get(level, event.type()).map(type -> (Object) type.title().text())
                    .orElse(event.type().toString());
            final boolean going = event.activeAt(now);
            final String left = WorldEvents.clock((going ? event.endsAt() : event.startsAt()) - now);
            context.getSource().sendSuccess(() -> GameText.component((going ? UNDER_WAY : COMING).with(name, left)),
                    false);
        }
        return planned.size();
    }

    private static int start(final CommandContext<CommandSourceStack> context, final int ticks)
            throws CommandSyntaxException {
        final ResourceLocation id = type(context);
        if (!WorldEvents.start(context.getSource().getLevel(), id, ticks, place(context))) {
            throw CANNOT.create(id);
        }
        context.getSource().sendSuccess(() -> GameText.component(STARTED.with(id.toString())), true);
        return 1;
    }

    private static int plan(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final ResourceLocation id = type(context);
        final int ticks = IntegerArgumentType.getInteger(context, TICKS);
        if (!WorldEvents.plan(context.getSource().getLevel(), id, ticks, place(context))) {
            throw CANNOT.create(id);
        }
        context.getSource().sendSuccess(() -> GameText.component(PLANNED.with(id.toString(),
                WorldEvents.clock(ticks))), true);
        return 1;
    }

    private static int delay(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final ResourceLocation id = type(context);
        final int ticks = IntegerArgumentType.getInteger(context, TICKS);
        if (!WorldEvents.delay(context.getSource().getLevel(), id, ticks)) {
            throw NOT_COMING.create(id);
        }
        context.getSource().sendSuccess(() -> GameText.component(DELAYED.with(id.toString(),
                WorldEvents.clock(ticks))), true);
        return 1;
    }

    private static int stop(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final ResourceLocation id = type(context);
        if (!WorldEvents.stop(context.getSource().getLevel(), id)) {
            throw NOT_THERE.create(id);
        }
        context.getSource().sendSuccess(() -> GameText.component(STOPPED.with(id.toString())), true);
        return 1;
    }

    private static ResourceLocation type(final CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        final ResourceLocation id = ResourceLocationArgument.getId(context, TYPE);
        if (WorldEvents.get(context.getSource().getLevel(), id).isEmpty()) {
            throw NO_TYPE.create(id);
        }
        return id;
    }

    private static Optional<BlockPos> place(final CommandContext<CommandSourceStack> context) {
        return Optional.of(BlockPos.containing(context.getSource().getPosition()));
    }
}
