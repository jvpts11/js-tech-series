/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.log;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.jstech.core.command.CommandRoot;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.Arrays;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;

/**
 * {@code /jstech log}, for the server's administrators: every system that writes to the log and how much it writes;
 * {@code /jstech log <system>}, one system's level; and {@code /jstech log <system> <level>}, setting it until the game
 * stops.
 */
@TextHolder
public final class LogCommand {

    public static final TextKey HEADER = TextKey.of("jscore.command.log.header",
            "Systems that write to the log, and how much each writes:");
    /* A system and its level: "jscore:media: info". */
    public static final TextKey LINE = TextKey.of("jscore.command.log.line", "%s: %s");
    public static final TextKey AT = TextKey.of("jscore.command.log.at", "%s writes at %s and above");
    public static final TextKey SET = TextKey.of("jscore.command.log.set",
            "%s now writes at %s and above, until the game stops");
    public static final TextKey UNKNOWN_SYSTEM = TextKey.of("jscore.command.log.unknown_system",
            "No system writes to the log as %s");
    public static final TextKey UNKNOWN_LEVEL = TextKey.of("jscore.command.log.unknown_level",
            "%s is not a level; the levels are error, warn, info, debug and trace");

    private static final String SYSTEM = "system";
    private static final String LEVEL = "level";
    private static final DynamicCommandExceptionType NO_SYSTEM =
            new DynamicCommandExceptionType(id -> GameText.component(UNKNOWN_SYSTEM.with(String.valueOf(id))));
    private static final DynamicCommandExceptionType NO_LEVEL =
            new DynamicCommandExceptionType(name -> GameText.component(UNKNOWN_LEVEL.with(String.valueOf(name))));
    private static final SuggestionProvider<CommandSourceStack> SYSTEMS = (context, builder) ->
            SharedSuggestionProvider.suggest(LogSystems.all().stream().map(LogSystem::id), builder);
    private static final SuggestionProvider<CommandSourceStack> LEVELS = (context, builder) ->
            SharedSuggestionProvider.suggest(Arrays.stream(LogLevel.values()).map(LogLevel::serializedName), builder);

    private LogCommand() {
    }

    /** Declares the command, from the Core's constructor. */
    public static void declare() {
        CommandRoot.CORE.declare("log", CommandRoot.ADMINS, (node, context) -> node
                .executes(LogCommand::list)
                .then(Commands.argument(SYSTEM, ResourceLocationArgument.id()).suggests(SYSTEMS)
                        .executes(LogCommand::show)
                        .then(Commands.argument(LEVEL, StringArgumentType.word()).suggests(LEVELS)
                                .executes(LogCommand::set))));
    }

    private static int list(final CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> GameText.component(HEADER), false);
        for (final LogSystem system : LogSystems.all()) {
            final String level = LogSystems.level(system).serializedName();
            context.getSource().sendSuccess(() -> GameText.component(LINE.with(system.id(), level)), false);
        }
        return LogSystems.all().size();
    }

    private static int show(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final LogSystem system = system(context);
        final String level = LogSystems.level(system).serializedName();
        context.getSource().sendSuccess(() -> GameText.component(AT.with(system.id(), level)), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int set(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final LogSystem system = system(context);
        final String written = StringArgumentType.getString(context, LEVEL);
        final LogLevel level = LogLevel.byName(written).orElseThrow(() -> NO_LEVEL.create(written));
        LogSystems.setLevel(system, level);
        context.getSource().sendSuccess(() -> GameText.component(SET.with(system.id(), level.serializedName())),
                true);
        return Command.SINGLE_SUCCESS;
    }

    private static LogSystem system(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final String id = ResourceLocationArgument.getId(context, SYSTEM).toString();
        return LogSystems.byId(id).orElseThrow(() -> NO_SYSTEM.create(id));
    }
}
