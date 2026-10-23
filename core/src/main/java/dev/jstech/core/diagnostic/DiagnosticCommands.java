/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.diagnostic;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import dev.jstech.core.command.CommandRoot;
import dev.jstech.core.team.CoreTeams;
import dev.jstech.core.team.IOwned;
import dev.jstech.core.team.Ownership;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * The diagnostics' commands, for the server's administrators: {@code /jstech health}, the state of the series'
 * systems and their warnings; {@code /jstech bandwidth [player]}, what the server sent over the last minute, with a
 * graph, by mod, by kind and by the blocks whose updates took the most; {@code /jstech cost [seconds]}, a measure of
 * what the series' blocks cost the server, the worst listed with their owners; and {@code /jstech diagnostics}, the
 * diagnostic file for a bug report.
 */
@TextHolder
public final class DiagnosticCommands {

    /* How many warnings. */
    public static final TextKey HEALTH_HEADER = TextKey.of("jscore.command.health.header",
            "The series' systems, with %s warnings:");
    public static final TextKey HEALTH_EMPTY = TextKey.of("jscore.command.health.empty",
            "No system has anything to say");
    /* Who it was sent to, and what a second. */
    public static final TextKey BANDWIDTH_HEADER = TextKey.of("jscore.command.bandwidth.header",
            "What the server sent %s over the last minute: %s");
    public static final TextKey EVERYONE = TextKey.of("jscore.command.bandwidth.everyone", "everyone");
    /* The graph, and what its tallest bar stands for a second. */
    public static final TextKey BANDWIDTH_GRAPH = TextKey.of("jscore.command.bandwidth.graph",
            "%s  the tallest bar is %s");
    /* Each mod and what it sent a second. */
    public static final TextKey BANDWIDTH_MODS = TextKey.of("jscore.command.bandwidth.mods", "By mod: %s");
    public static final TextKey BANDWIDTH_KINDS = TextKey.of("jscore.command.bandwidth.kinds", "What took the most:");
    /* A kind of packet, and what it took a second. */
    public static final TextKey BANDWIDTH_KIND = TextKey.of("jscore.command.bandwidth.kind", "%s: %s");
    public static final TextKey BANDWIDTH_SENDERS = TextKey.of("jscore.command.bandwidth.senders",
            "The blocks whose updates took the most:");
    /* What stands there, where, in which dimension, and what its updates took a second. */
    public static final TextKey BANDWIDTH_SENDER = TextKey.of("jscore.command.bandwidth.sender", "%s at %s in %s: %s");
    /* The same, and whose it is. */
    public static final TextKey BANDWIDTH_SENDER_OWNED = TextKey.of("jscore.command.bandwidth.sender_owned",
            "%s at %s in %s: %s (%s's)");
    /* The player. */
    public static final TextKey BANDWIDTH_NOT_COUNTED = TextKey.of("jscore.command.bandwidth.not_counted",
            "What is sent to %s is not counted, as they play in their own game");
    public static final TextKey BANDWIDTH_OFF = TextKey.of("jscore.command.bandwidth.off",
            "Network use is not counted; turn on \"Count network use\" in the world's settings");
    /* How many seconds the measure takes. */
    public static final TextKey COST_STARTED = TextKey.of("jscore.command.cost.started",
            "Measuring what the series' blocks cost; the result comes in %s seconds");
    /* How many ticks it measured, what every place cost a tick together, and in how many places. */
    public static final TextKey COST_HEADER = TextKey.of("jscore.command.cost.header",
            "What the series' blocks cost over %s ticks: %s ms a tick in all, in %s places");
    public static final TextKey COST_NOTHING = TextKey.of("jscore.command.cost.nothing",
            "None of the series' blocks ticked while it measured");
    /* Its rank, what stands there, where, in which dimension, and what it cost a tick. */
    public static final TextKey COST_PLACE = TextKey.of("jscore.command.cost.place",
            "%s. %s at %s in %s: %s ms a tick");
    /* The same, and whose it is. */
    public static final TextKey COST_PLACE_OWNED = TextKey.of("jscore.command.cost.place_owned",
            "%s. %s at %s in %s: %s ms a tick (%s's)");
    public static final TextKey COST_KINDS = TextKey.of("jscore.command.cost.kinds", "By kind:");
    /* The kind, what its places cost a tick together, and how many places. */
    public static final TextKey COST_KIND = TextKey.of("jscore.command.cost.kind", "%s: %s ms a tick in %s places");
    /* The file. */
    public static final TextKey DIAGNOSTICS_WRITTEN = TextKey.of("jscore.command.diagnostics.written",
            "Wrote the diagnostics to %s; attach the file to a bug report");
    /* What went wrong. */
    public static final TextKey DIAGNOSTICS_FAILED = TextKey.of("jscore.command.diagnostics.failed",
            "The diagnostics could not be written: %s");

    /** How long a measure lasts when the command does not say, in seconds. */
    public static final int DEFAULT_SECONDS = 10;
    /** How many places, kinds and senders a command lists, the worst first. */
    public static final int SHOWN = 10;

    private static final long TICKS_PER_SECOND = 20L;
    private static final String PLAYER = "player";
    private static final String SECONDS = "seconds";
    private static final String SEPARATOR = ", ";
    private static final DynamicCommandExceptionType FAILED = new DynamicCommandExceptionType(
            why -> GameText.component(DIAGNOSTICS_FAILED.with(String.valueOf(why))));

    private DiagnosticCommands() {
    }

    /** Declares the commands, from the Core's constructor. */
    public static void declare() {
        CommandRoot.CORE.declare("health", CommandRoot.ADMINS, (node, context) -> node
                .executes(DiagnosticCommands::health));
        CommandRoot.CORE.declare("bandwidth", CommandRoot.ADMINS, (node, context) -> node
                .executes(source -> bandwidth(source, null))
                .then(Commands.argument(PLAYER, EntityArgument.player())
                        .executes(source -> bandwidth(source, EntityArgument.getPlayer(source, PLAYER)))));
        CommandRoot.CORE.declare("cost", CommandRoot.ADMINS, (node, context) -> node
                .executes(source -> cost(source, DEFAULT_SECONDS))
                .then(Commands.argument(SECONDS, IntegerArgumentType.integer(1, CostMap.LONGEST_SECONDS))
                        .executes(source -> cost(source, IntegerArgumentType.getInteger(source, SECONDS)))));
        CommandRoot.CORE.declare("diagnostics", CommandRoot.ADMINS, (node, context) -> node
                .executes(DiagnosticCommands::diagnostics));
    }

    /**
     * What stands at {@code pos}: its block's name when its chunk is loaded and a block stands there, else the id it
     * was counted under, as for a moving structure, which is counted where it is with no block of its own there.
     */
    public static Text whatIsAt(final MinecraftServer server, final ResourceKey<Level> dimension, final BlockPos pos,
                                final ResourceLocation kind) {
        final ServerLevel level = server.getLevel(dimension);
        if (level == null || !level.isLoaded(pos) || level.getBlockState(pos).isAir()) {
            return Text.literal(kind.toString());
        }
        return GameText.of(level.getBlockState(pos).getBlock().getName());
    }

    /** Whose the block entity at {@code pos} is, by name, when its chunk is loaded and it has an owner. */
    public static Optional<String> ownerAt(final MinecraftServer server, final ResourceKey<Level> dimension,
                                           final BlockPos pos) {
        final ServerLevel level = server.getLevel(dimension);
        if (level == null || !level.isLoaded(pos) || !(level.getBlockEntity(pos) instanceof IOwned owned)) {
            return Optional.empty();
        }
        final Ownership ownership = owned.ownership();
        if (ownership == null) {
            return Optional.empty();
        }
        return Optional.of(CoreTeams.nameOf(server, ownership.owner()).orElse(ownership.owner().toString()));
    }

    private static int health(final CommandContext<CommandSourceStack> context) {
        final CommandSourceStack source = context.getSource();
        final List<Health.Part> parts = Health.read(source.getServer());
        if (parts.isEmpty()) {
            source.sendSuccess(() -> GameText.component(HEALTH_EMPTY), false);
            return 0;
        }
        final int warnings = Health.warnings(parts);
        source.sendSuccess(() -> GameText.component(HEALTH_HEADER.with(warnings)), false);
        for (final Health.Part part : parts) {
            final String system = part.system().id();
            source.sendSuccess(() -> GameText.component(Text.literal(system)).withStyle(ChatFormatting.UNDERLINE),
                    false);
            for (final Health.Line line : part.lines()) {
                source.sendSuccess(() -> GameText.component(line.text())
                        .withStyle(line.warning() ? ChatFormatting.GOLD : ChatFormatting.GRAY), false);
            }
        }
        return warnings;
    }

    private static int bandwidth(final CommandContext<CommandSourceStack> context,
                                 final @Nullable ServerPlayer player) {
        final CommandSourceStack source = context.getSource();
        if (!DiagnosticBalance.countNetwork()) {
            source.sendFailure(GameText.component(BANDWIDTH_OFF));
            return 0;
        }
        final BandwidthLedger ledger;
        final Text who;
        if (player == null) {
            ledger = Bandwidth.everyone();
            who = EVERYONE.text();
        } else {
            final Optional<BandwidthLedger> own = Bandwidth.of(player.getUUID());
            if (own.isEmpty()) {
                source.sendFailure(GameText.component(BANDWIDTH_NOT_COUNTED.with(player.getGameProfile().getName())));
                return 0;
            }
            ledger = own.get();
            who = Text.literal(player.getGameProfile().getName());
        }
        final long rate = ledger.bytesPerSecond();
        final long[] seconds = ledger.lastMinute();
        say(source, BANDWIDTH_HEADER.with(who, Bandwidth.rate(rate)));
        say(source, BANDWIDTH_GRAPH.with(Bandwidth.graph(seconds), Bandwidth.rate(tallestBar(seconds))));
        final List<String> mods = new ArrayList<>();
        for (final BandwidthLedger.Rate mod : ledger.mods()) {
            mods.add(mod.name() + " " + Bandwidth.rate(mod.bytesPerSecond()));
        }
        if (!mods.isEmpty()) {
            say(source, BANDWIDTH_MODS.with(String.join(SEPARATOR, mods)));
        }
        final List<BandwidthLedger.Rate> kinds = ledger.kinds();
        if (!kinds.isEmpty()) {
            say(source, BANDWIDTH_KINDS.text());
            for (final BandwidthLedger.Rate kind : kinds.subList(0, Math.min(SHOWN, kinds.size()))) {
                say(source, BANDWIDTH_KIND.with(kind.name(), Bandwidth.rate(kind.bytesPerSecond())));
            }
        }
        final List<BandwidthLedger.SenderRate> senders = ledger.senders(SHOWN);
        if (!senders.isEmpty()) {
            say(source, BANDWIDTH_SENDERS.text());
            final MinecraftServer server = source.getServer();
            for (final BandwidthLedger.SenderRate sender : senders) {
                final BandwidthLedger.Sender at = sender.sender();
                final Text what = whatIsAt(server, at.dimension(), at.pos(), at.type());
                final String where = at.pos().toShortString();
                final String dimension = at.dimension().location().toString();
                final String sent = Bandwidth.rate(sender.bytesPerSecond());
                say(source, ownerAt(server, at.dimension(), at.pos())
                        .map(owner -> BANDWIDTH_SENDER_OWNED.with(what, where, dimension, sent, owner))
                        .orElseGet(() -> BANDWIDTH_SENDER.with(what, where, dimension, sent)));
            }
        }
        return (int) Math.min(Integer.MAX_VALUE, rate);
    }

    private static int cost(final CommandContext<CommandSourceStack> context, final int seconds) {
        final CommandSourceStack source = context.getSource();
        final long ticks = CostMap.measure(source.getServer(), seconds, measure -> report(source, measure));
        say(source, COST_STARTED.with((ticks + TICKS_PER_SECOND - 1L) / TICKS_PER_SECOND));
        return (int) ticks;
    }

    private static void report(final CommandSourceStack source, final CostMap.Measure measure) {
        if (measure.places().isEmpty()) {
            say(source, COST_NOTHING.text());
            return;
        }
        say(source, COST_HEADER.with(measure.ticks(), Diagnostics.millis(measure.totalNanosPerTick()),
                measure.places().size()));
        final MinecraftServer server = source.getServer();
        int rank = 0;
        for (final CostMap.Entry entry : measure.places().subList(0, Math.min(SHOWN, measure.places().size()))) {
            rank++;
            final Text what = whatIsAt(server, entry.dimension(), entry.pos(), entry.kind());
            final String where = entry.pos().toShortString();
            final String dimension = entry.dimension().location().toString();
            final String cost = Diagnostics.millis(entry.nanosPerTick());
            final int place = rank;
            say(source, ownerAt(server, entry.dimension(), entry.pos())
                    .map(owner -> COST_PLACE_OWNED.with(place, what, where, dimension, cost, owner))
                    .orElseGet(() -> COST_PLACE.with(place, what, where, dimension, cost)));
        }
        say(source, COST_KINDS.text());
        final List<CostMap.Kind> kinds = measure.kinds();
        for (final CostMap.Kind kind : kinds.subList(0, Math.min(SHOWN, kinds.size()))) {
            say(source, COST_KIND.with(kind.kind().toString(), Diagnostics.millis(kind.nanosPerTick()),
                    kind.places()));
        }
    }

    private static int diagnostics(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final Path file;
        try {
            file = DiagnosticReports.write(context.getSource().getServer());
        } catch (final IOException e) {
            throw FAILED.create(e.getMessage());
        }
        say(context.getSource(), DIAGNOSTICS_WRITTEN.with(file.toString()));
        return 1;
    }

    /* What the tallest bar of a minute's graph stands for, a second. */
    private static long tallestBar(final long[] seconds) {
        long most = 0L;
        for (int i = 0; i < seconds.length; i += Bandwidth.SECONDS_PER_BAR) {
            long sum = 0L;
            for (int j = i; j < Math.min(seconds.length, i + Bandwidth.SECONDS_PER_BAR); j++) {
                sum += seconds[j];
            }
            most = Math.max(most, sum);
        }
        return most / Bandwidth.SECONDS_PER_BAR;
    }

    private static void say(final CommandSourceStack source, final Text text) {
        source.sendSuccess(() -> GameText.component(text), false);
    }
}
