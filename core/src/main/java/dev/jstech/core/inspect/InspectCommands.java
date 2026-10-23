/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.inspect;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.jstech.core.command.CommandRoot;
import dev.jstech.core.diagnostic.Bandwidth;
import dev.jstech.core.diagnostic.DiagnosticBalance;
import dev.jstech.core.diagnostic.DiagnosticCommands;
import dev.jstech.core.diagnostic.TickLedger;
import dev.jstech.core.text.GameText;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

/**
 * The inspection's commands, for the server's administrators: {@code /jstech inspect [pos]}, what every system says
 * of the block looked at or of a place, in chat; {@code /jstech inspect watch [pos]}, the same on the debug screen
 * (F3), read again every second, with what the server sent the player in its last tick; {@code /jstech inspect stop};
 * and {@code /jstech inspect payloads [player]}, what the server sent in its last tick and in the busiest of the last
 * ones, each kind of packet with its size.
 */
public final class InspectCommands {

    /** How far a block looked at may be, in blocks. */
    public static final double REACH = 64.0;
    /** How many kinds of packet of a tick are listed. */
    public static final int KINDS_SHOWN = 10;

    private static final String POS = "pos";
    private static final String PLAYER = "player";
    private static final String INDENT = "  ";
    private static final String BETWEEN = ": ";

    private InspectCommands() {
    }

    /** Declares the commands, from the Core's constructor. */
    public static void declare() {
        CommandRoot.CORE.declare("inspect", CommandRoot.ADMINS, (node, context) -> node
                .executes(source -> inspect(source, null))
                .then(Commands.argument(POS, BlockPosArgument.blockPos())
                        .executes(source -> inspect(source, BlockPosArgument.getLoadedBlockPos(source, POS))))
                .then(Commands.literal("watch")
                        .executes(source -> watch(source, null))
                        .then(Commands.argument(POS, BlockPosArgument.blockPos())
                                .executes(source -> watch(source, BlockPosArgument.getLoadedBlockPos(source, POS)))))
                .then(Commands.literal("stop")
                        .executes(InspectCommands::stop))
                .then(Commands.literal("payloads")
                        .executes(source -> payloads(source, null))
                        .then(Commands.argument(PLAYER, EntityArgument.player())
                                .executes(source -> payloads(source, EntityArgument.getPlayer(source, PLAYER))))));
    }

    /** The block {@code player} looks at, within {@link #REACH}, when they look at one. */
    public static Optional<BlockPos> lookedAt(final ServerPlayer player) {
        final HitResult hit = player.pick(REACH, 1.0F, false);
        return hit instanceof BlockHitResult block && hit.getType() == HitResult.Type.BLOCK
                ? Optional.of(block.getBlockPos()) : Optional.empty();
    }

    private static int inspect(final CommandContext<CommandSourceStack> context, final @Nullable BlockPos named) {
        final CommandSourceStack source = context.getSource();
        final Optional<BlockPos> pos = target(source, named);
        if (pos.isEmpty()) {
            source.sendFailure(GameText.component(InspectTexts.NO_TARGET));
            return 0;
        }
        final ServerLevel level = source.getLevel();
        final List<Inspectors.Part> parts = Inspectors.read(level, pos.get());
        say(source, GameText.component(InspectTexts.HEADER.with(pos.get().toShortString(),
                level.dimension().location().toString())));
        if (parts.isEmpty()) {
            say(source, GameText.component(InspectTexts.NOTHING).withStyle(ChatFormatting.GRAY));
            return 0;
        }
        for (final InspectionPayload.Line line : InspectWatch.linesOf(parts)) {
            say(source, lineOf(line));
        }
        return parts.size();
    }

    private static int watch(final CommandContext<CommandSourceStack> context, final @Nullable BlockPos named)
            throws CommandSyntaxException {
        final CommandSourceStack source = context.getSource();
        final ServerPlayer player = source.getPlayerOrException();
        final Optional<BlockPos> pos = target(source, named);
        if (pos.isEmpty()) {
            source.sendFailure(GameText.component(InspectTexts.NO_TARGET));
            return 0;
        }
        InspectWatch.watch(player, pos.get());
        say(source, GameText.component(InspectTexts.WATCHING.with(pos.get().toShortString(),
                player.level().dimension().location().toString())));
        return 1;
    }

    private static int stop(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        InspectWatch.stop(context.getSource().getPlayerOrException());
        say(context.getSource(), GameText.component(InspectTexts.STOPPED));
        return 1;
    }

    private static int payloads(final CommandContext<CommandSourceStack> context,
                                final @Nullable ServerPlayer player) {
        final CommandSourceStack source = context.getSource();
        if (!DiagnosticBalance.countNetwork()) {
            source.sendFailure(GameText.component(DiagnosticCommands.BANDWIDTH_OFF));
            return 0;
        }
        final TickLedger ledger;
        if (player == null) {
            ledger = Bandwidth.everyoneByTick();
        } else {
            final Optional<TickLedger> own = Bandwidth.byTick(player.getUUID());
            if (own.isEmpty()) {
                source.sendFailure(GameText.component(DiagnosticCommands.BANDWIDTH_NOT_COUNTED
                        .with(player.getGameProfile().getName())));
                return 0;
            }
            ledger = own.get();
        }
        final List<TickLedger.Tick> ticks = ledger.ticks();
        if (ticks.isEmpty()) {
            say(source, GameText.component(InspectTexts.PAYLOADS_NONE).withStyle(ChatFormatting.GRAY));
            return 0;
        }
        final TickLedger.Tick last = ticks.getFirst();
        tell(source, last);
        final TickLedger.Tick busiest = ledger.busiest().orElse(last);
        if (busiest.tick() != last.tick()) {
            say(source, GameText.component(InspectTexts.PAYLOADS_BUSIEST));
            tell(source, busiest);
        }
        return (int) Math.min(Integer.MAX_VALUE, last.bytes());
    }

    private static void tell(final CommandSourceStack source, final TickLedger.Tick tick) {
        say(source, GameText.component(InspectTexts.PAYLOADS_TICK.with(tick.tick(), tick.packets(), tick.bytes()))
                .withStyle(ChatFormatting.UNDERLINE));
        for (final TickLedger.Sent sent : tick.sent().subList(0, Math.min(KINDS_SHOWN, tick.sent().size()))) {
            say(source, Component.literal(INDENT).append(GameText.component(InspectTexts.PAYLOADS_KIND
                    .with(sent.kind(), sent.packets(), sent.bytes()))).withStyle(ChatFormatting.GRAY));
        }
    }

    /* The place named, or else the block the player running the command looks at. */
    private static Optional<BlockPos> target(final CommandSourceStack source, final @Nullable BlockPos named) {
        if (named != null) {
            return Optional.of(named);
        }
        return source.getPlayer() == null ? Optional.empty() : lookedAt(source.getPlayer());
    }

    /* A line in chat: a system's name underlined, its lines under it in grey, each by its depth. */
    private static MutableComponent lineOf(final InspectionPayload.Line line) {
        if (line.depth() == 0) {
            return line.name().copy().withStyle(ChatFormatting.UNDERLINE);
        }
        final MutableComponent out = Component.literal(INDENT.repeat(line.depth())).append(line.name().copy());
        return (line.value().isEmpty() ? out : out.append(BETWEEN + line.value())).withStyle(ChatFormatting.GRAY);
    }

    private static void say(final CommandSourceStack source, final Component text) {
        source.sendSuccess(() -> text, false);
    }
}
