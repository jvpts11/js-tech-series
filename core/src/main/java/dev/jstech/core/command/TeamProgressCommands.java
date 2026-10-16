/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.command;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.jstech.core.progression.Knowledge;
import dev.jstech.core.progression.Knowledges;
import dev.jstech.core.progression.Milestone;
import dev.jstech.core.progression.Milestones;
import dev.jstech.core.progression.TeamProgress;
import dev.jstech.core.team.CoreTeams;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.Collection;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * What an operator does with what teams know: {@code /jstech knowledge <players>}, what their teams know;
 * {@code grant <id> [<share>]} and {@code forget <id>}, giving and taking a piece of knowledge; and
 * {@code /jstech milestone <players> <id>}, a milestone reached.
 */
@TextHolder
public final class TeamProgressCommands {

    /* A player and what their team knows: "Alex's team knows: Fusion, Orbit". */
    public static final TextKey KNOWS = TextKey.of("jscore.command.knowledge.knows", "%s's team knows: %s");
    public static final TextKey KNOWS_NOTHING = TextKey.of("jscore.command.knowledge.knows_nothing",
            "%s's team knows nothing yet");
    public static final TextKey GRANTED = TextKey.of("jscore.command.knowledge.granted", "%s's team now knows %s");
    public static final TextKey FORGOTTEN = TextKey.of("jscore.command.knowledge.forgotten", "%s's team forgot %s");
    public static final TextKey MILESTONE_REACHED = TextKey.of("jscore.command.milestone.reached",
            "%s's team reached %s");
    public static final TextKey UNKNOWN_KNOWLEDGE = TextKey.of("jscore.command.knowledge.unknown",
            "There is no knowledge %s");
    public static final TextKey UNKNOWN_MILESTONE = TextKey.of("jscore.command.milestone.unknown",
            "There is no milestone %s");

    private static final String PLAYERS = "players";
    private static final String ID = "id";
    private static final String SHARE = "share";
    private static final String LIST = ", ";
    private static final DynamicCommandExceptionType NO_KNOWLEDGE =
            new DynamicCommandExceptionType(id -> GameText.component(UNKNOWN_KNOWLEDGE.with(String.valueOf(id))));
    private static final DynamicCommandExceptionType NO_MILESTONE =
            new DynamicCommandExceptionType(id -> GameText.component(UNKNOWN_MILESTONE.with(String.valueOf(id))));
    private static final SuggestionProvider<CommandSourceStack> KNOWLEDGE = (context, builder) ->
            SharedSuggestionProvider.suggestResource(Knowledges.all().stream().map(Knowledge::id), builder);
    private static final SuggestionProvider<CommandSourceStack> MILESTONES = (context, builder) ->
            SharedSuggestionProvider.suggestResource(Milestones.sets().stream()
                    .flatMap(set -> Milestones.of(set).stream()).map(Milestone::id), builder);

    private TeamProgressCommands() {
    }

    /** Declares the commands, from the Core's constructor. */
    public static void declare() {
        SeriesCommands.declare("knowledge", SeriesCommands.GAME_MASTERS, (node, context) -> node
                .then(Commands.argument(PLAYERS, EntityArgument.players())
                        .executes(TeamProgressCommands::show)
                        .then(Commands.literal("grant")
                                .then(Commands.argument(ID, ResourceLocationArgument.id()).suggests(KNOWLEDGE)
                                        .executes(command -> grant(command, 1.0))
                                        .then(Commands.argument(SHARE, DoubleArgumentType.doubleArg(0.0, 1.0))
                                                .executes(command -> grant(command,
                                                        DoubleArgumentType.getDouble(command, SHARE))))))
                        .then(Commands.literal("forget")
                                .then(Commands.argument(ID, ResourceLocationArgument.id()).suggests(KNOWLEDGE)
                                        .executes(TeamProgressCommands::forget)))));
        SeriesCommands.declare("milestone", SeriesCommands.GAME_MASTERS, (node, context) -> node
                .then(Commands.argument(PLAYERS, EntityArgument.players())
                        .then(Commands.argument(ID, ResourceLocationArgument.id()).suggests(MILESTONES)
                                .executes(TeamProgressCommands::reach))));
    }

    private static int show(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final MinecraftServer server = context.getSource().getServer();
        final Collection<ServerPlayer> players = EntityArgument.getPlayers(context, PLAYERS);
        for (final ServerPlayer player : players) {
            final List<String> known = TeamProgress.ofPlayer(server, player.getUUID()).known();
            if (known.isEmpty()) {
                context.getSource().sendSuccess(() -> GameText.component(KNOWS_NOTHING.with(
                        player.getScoreboardName())), false);
            } else {
                // Each name in the language of whoever reads the answer.
                final Text names = GameText.of(ComponentUtils.formatList(known.stream()
                        .map(TeamProgressCommands::nameOf).map(GameText::component).toList(),
                        Component.literal(LIST)));
                context.getSource().sendSuccess(() -> GameText.component(KNOWS.with(player.getScoreboardName(),
                        names)), false);
            }
        }
        return players.size();
    }

    private static int grant(final CommandContext<CommandSourceStack> context, final double share)
            throws CommandSyntaxException {
        final ResourceLocation id = ResourceLocationArgument.getId(context, ID);
        final Knowledge knowledge = Knowledges.byId(id).orElseThrow(() -> NO_KNOWLEDGE.create(id));
        final MinecraftServer server = context.getSource().getServer();
        final Collection<ServerPlayer> players = EntityArgument.getPlayers(context, PLAYERS);
        for (final ServerPlayer player : players) {
            TeamProgress.grant(server, CoreTeams.teamOf(server, player.getUUID()), knowledge, share);
            context.getSource().sendSuccess(() -> GameText.component(GRANTED.with(player.getScoreboardName(),
                    knowledge.name())), true);
        }
        return players.size();
    }

    private static int forget(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final ResourceLocation id = ResourceLocationArgument.getId(context, ID);
        final Knowledge knowledge = Knowledges.byId(id).orElseThrow(() -> NO_KNOWLEDGE.create(id));
        final MinecraftServer server = context.getSource().getServer();
        final Collection<ServerPlayer> players = EntityArgument.getPlayers(context, PLAYERS);
        for (final ServerPlayer player : players) {
            TeamProgress.forget(server, CoreTeams.teamOf(server, player.getUUID()), id);
            context.getSource().sendSuccess(() -> GameText.component(FORGOTTEN.with(player.getScoreboardName(),
                    knowledge.name())), true);
        }
        return players.size();
    }

    private static int reach(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final ResourceLocation id = ResourceLocationArgument.getId(context, ID);
        final Milestone milestone = Milestones.byId(id).orElseThrow(() -> NO_MILESTONE.create(id));
        final MinecraftServer server = context.getSource().getServer();
        final Collection<ServerPlayer> players = EntityArgument.getPlayers(context, PLAYERS);
        for (final ServerPlayer player : players) {
            TeamProgress.reach(server, CoreTeams.teamOf(server, player.getUUID()), milestone);
            context.getSource().sendSuccess(() -> GameText.component(MILESTONE_REACHED.with(
                    player.getScoreboardName(), milestone.name())), true);
        }
        return players.size();
    }

    /* The name of the knowledge {@code id}, or its id for one no longer declared. */
    private static Text nameOf(final String id) {
        final ResourceLocation key = ResourceLocation.tryParse(id);
        return key == null ? Text.literal(id) : Knowledges.byId(key).map(known -> known.name().text())
                .orElseGet(() -> Text.literal(id));
    }
}
