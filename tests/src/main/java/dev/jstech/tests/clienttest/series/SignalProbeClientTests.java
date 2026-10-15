/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.series;

import dev.jstech.core.jstechseries.probe.ProbeLayout;
import dev.jstech.core.jstechseries.probe.SignalProbe;
import dev.jstech.core.jstechseries.probe.SignalView;
import dev.jstech.core.jstechseries.probe.client.ProbeMarks;
import dev.jstech.core.jstechseries.probe.client.SignalProbeScreen;
import dev.jstech.core.signal.Band;
import dev.jstech.core.signal.RadioBlockEntity;
import dev.jstech.core.signal.Tuning;
import dev.jstech.core.team.Ownership;
import dev.jstech.tests.TestRadios;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * The signal probe on a player's game, to set beside the approved design: linked to a redstone wire fed full, held up
 * where a transmitter of the player's team sends 15, two of theirs clash, a pair sends, and one of theirs is too far
 * to be heard; first here, with the wire and the frequencies, then on the radio's page with one chosen; OK outlining
 * where the chosen one comes from; and its reading beside the crosshair.
 */
public final class SignalProbeClientTests {

    private static final BlockPos STAND = new BlockPos(0, 2, -2);
    private static final int FLOOR = 2;
    private static final BlockPos FEED = new BlockPos(-1, FLOOR, 1);
    private static final BlockPos WIRE = new BlockPos(0, FLOOR, 1);
    private static final BlockPos TEAM = new BlockPos(3, FLOOR, 4);
    private static final BlockPos TEAM_FEED = new BlockPos(4, FLOOR, 4);
    private static final BlockPos CLASH_A = new BlockPos(-3, FLOOR, 4);
    private static final BlockPos CLASH_B = new BlockPos(-3, FLOOR, 9);
    private static final BlockPos PAIR = new BlockPos(1, FLOOR, 7);
    private static final BlockPos FAR = new BlockPos(0, FLOOR, 44);
    private static final int WAIT = 200;

    private SignalProbeClientTests() {
    }

    @ClientTest(timeoutTicks = 1200)
    public static void probe_readsTheWireAndTheFrequenciesHeardHere(final ClientTestContext ctx) {
        ctx.afterTest(() -> ctx.mc().setScreen(null));
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> build(ctx, level, ctx.player().getUUID()))
                .thenGive(0, new ItemStack(SignalProbe.ITEM.get()))
                .then(2, () -> {
                    ctx.selectHotbar(0);
                    ctx.lookAt(Vec3.atCenterOf(ctx.abs(WIRE)));
                })
                .then(2, () -> ctx.rightClick(WIRE, Direction.UP))
                .thenWaitUntil(() -> ctx.player().getMainHandItem().get(SignalProbe.LINK.get()) != null, WAIT,
                        "the probe to link to the wire")
                .then(2, () -> Objects.requireNonNull(ctx.mc().gameMode, "no game mode")
                        .useItem(ctx.player(), InteractionHand.MAIN_HAND))
                .thenWaitUntil(() -> ctx.mc().screen instanceof SignalProbeScreen
                        && SignalView.shown(false).linked() && SignalView.shown(false).rows().size() >= 4, WAIT,
                        "the probe's display to read the wire and hear the frequencies")
                .thenScreenshot(10, "signal-here")
                .then(0, () -> press(ctx, ProbeLayout.softX(2) + 4, ProbeLayout.SOFT_Y + 4))
                .thenScreenshot(5, "signal-radio")
                .then(0, () -> press(ctx, ProbeLayout.PAD_X, ProbeLayout.PAD_Y + 16))
                .thenScreenshot(5, "signal-radio-chosen")
                .then(0, () -> press(ctx, ProbeLayout.PAD_X, ProbeLayout.PAD_Y - 16))
                .then(2, () -> press(ctx, ProbeLayout.PAD_X, ProbeLayout.PAD_Y))
                .thenWaitUntil(() -> ctx.mc().screen == null && !ProbeMarks.shown().isEmpty(), WAIT,
                        "OK to outline where the chosen frequency comes from")
                .then(0, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(TEAM))))
                .thenScreenshot(10, "signal-marks-and-hud")
                .thenServer(0, level -> clear(ctx, level));
    }

    /*
     * A wire fed full; a transmitter of the player's team fed full; two of theirs clashing; a pair; and one of theirs
     * past its reach.
     */
    private static void build(final ClientTestContext ctx, final ServerLevel level, final UUID player) {
        clear(ctx, level);
        level.setBlockAndUpdate(ctx.abs(FEED), Blocks.REDSTONE_BLOCK.defaultBlockState());
        // Shaped as it would be placed, joined to its feed: a lone dot would take the click to change its shape.
        level.setBlockAndUpdate(ctx.abs(WIRE), Block.updateFromNeighbourShapes(
                Blocks.REDSTONE_WIRE.defaultBlockState(), level, ctx.abs(WIRE)));
        radio(ctx, level, TEAM, TestRadios.TRANSMITTER.get(), player, Tuning.on(Band.TEAM, 142));
        level.setBlockAndUpdate(ctx.abs(TEAM_FEED), Blocks.REDSTONE_BLOCK.defaultBlockState());
        radio(ctx, level, CLASH_A, TestRadios.TRANSMITTER.get(), player, Tuning.on(Band.TEAM, 305));
        radio(ctx, level, CLASH_B, TestRadios.TRANSMITTER.get(), player, Tuning.on(Band.TEAM, 305));
        radio(ctx, level, PAIR, TestRadios.TRANSMITTER.get(), player, Tuning.newPair());
        radio(ctx, level, FAR, TestRadios.TRANSMITTER.get(), player, Tuning.on(Band.PRIVATE, 211));
    }

    private static void radio(final ClientTestContext ctx, final ServerLevel level, final BlockPos pos,
                              final Block block, final UUID owner, final Tuning tuning) {
        level.setBlockAndUpdate(ctx.abs(pos), block.defaultBlockState());
        if (level.getBlockEntity(ctx.abs(pos)) instanceof RadioBlockEntity radio) {
            radio.own(Ownership.of(owner));
            radio.tune(tuning);
        }
    }

    /* Presses the probe held up at ({@code x}, {@code y}) of its picture, drawn in the middle of the screen. */
    private static void press(final ClientTestContext ctx, final int x, final int y) {
        final int left = (ctx.mc().getWindow().getGuiScaledWidth() - ProbeLayout.WIDTH) / 2;
        final int top = (ctx.mc().getWindow().getGuiScaledHeight() - ProbeLayout.HEIGHT) / 2;
        ctx.click(left + x + 0.5, top + y + 0.5);
    }

    private static void clear(final ClientTestContext ctx, final ServerLevel level) {
        for (final BlockPos at : List.of(FEED, WIRE, TEAM, TEAM_FEED, CLASH_A, CLASH_B, PAIR, FAR)) {
            level.setBlockAndUpdate(ctx.abs(at), Blocks.AIR.defaultBlockState());
        }
    }
}
