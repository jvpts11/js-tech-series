/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.series;

import dev.jstech.core.jstechseries.probe.NetworkProbe;
import dev.jstech.core.jstechseries.probe.ProbeLayout;
import dev.jstech.core.jstechseries.probe.ProbeView;
import dev.jstech.core.jstechseries.probe.client.NetworkProbeScreen;
import dev.jstech.core.jstechseries.probe.client.ProbeMarks;
import dev.jstech.tests.TestEnergies;
import dev.jstech.tests.TestEnergyEnds;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.testkit.TestCables;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * The network probe on a player's game, to set beside the approved design: linked to a line of energy from a full
 * source to a sink that takes a thousand a tick, losing a thousandth each block, held up on its summary after a few
 * seconds of flow, then on its members', flow and losses pages; OK outlining the chosen blocks in the world; and its
 * reading beside the crosshair.
 */
public final class NetworkProbeClientTests {

    private static final BlockPos STAND = new BlockPos(0, 2, -2);
    private static final int FLOOR = 2;
    private static final int ROW = 1;
    private static final BlockPos SOURCE = new BlockPos(-3, FLOOR, ROW);
    private static final List<BlockPos> RUN = List.of(new BlockPos(-2, FLOOR, ROW), new BlockPos(-1, FLOOR, ROW),
            new BlockPos(0, FLOOR, ROW), new BlockPos(1, FLOOR, ROW));
    private static final BlockPos SINK = new BlockPos(2, FLOOR, ROW);
    private static final int WAIT = 200;
    private static final int SECONDS = 80;

    private NetworkProbeClientTests() {
    }

    @ClientTest(timeoutTicks = 1400)
    public static void probe_readsALineOnItsPagesAndShowsItsBlocks(final ClientTestContext ctx) {
        ctx.afterTest(() -> ctx.mc().setScreen(null));
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> build(ctx, level))
                .thenGive(0, new ItemStack(NetworkProbe.ITEM.get()))
                .then(2, () -> {
                    ctx.selectHotbar(0);
                    ctx.lookAt(Vec3.atCenterOf(ctx.abs(RUN.get(2))));
                })
                .then(2, () -> ctx.rightClick(RUN.get(2), Direction.NORTH))
                .thenWaitUntil(() -> ctx.player().getMainHandItem().get(NetworkProbe.LINK.get()) != null, WAIT,
                        "the probe to link to the cable")
                .then(2, () -> Objects.requireNonNull(ctx.mc().gameMode, "no game mode")
                        .useItem(ctx.player(), InteractionHand.MAIN_HAND))
                .thenWaitUntil(() -> ctx.mc().screen instanceof NetworkProbeScreen
                        && ProbeView.shown(false).present(), WAIT, "the probe's display to read the line")
                .thenScreenshot(SECONDS, "probe-summary")
                .then(0, () -> press(ctx, ProbeLayout.softX(1) + 4, ProbeLayout.SOFT_Y + 4))
                .thenScreenshot(5, "probe-members")
                .then(0, () -> press(ctx, ProbeLayout.softX(2) + 4, ProbeLayout.SOFT_Y + 4))
                .thenScreenshot(5, "probe-flow")
                .then(0, () -> press(ctx, ProbeLayout.softX(3) + 4, ProbeLayout.SOFT_Y + 4))
                .thenScreenshot(5, "probe-losses")
                .then(0, () -> press(ctx, ProbeLayout.softX(1) + 4, ProbeLayout.SOFT_Y + 4))
                .then(2, () -> press(ctx, ProbeLayout.PAD_X, ProbeLayout.PAD_Y))
                .thenWaitUntil(() -> ctx.mc().screen == null && !ProbeMarks.shown().isEmpty(), WAIT,
                        "OK to outline the chosen blocks and put the probe down")
                .then(0, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(RUN.get(2)))))
                .thenScreenshot(10, "probe-marks-and-hud")
                .thenServer(0, level -> clear(ctx, level));
    }

    /* A full source, a run of a line that loses a thousandth each block, and a sink that takes a thousand a tick. */
    private static void build(final ClientTestContext ctx, final ServerLevel level) {
        clear(ctx, level);
        TestEnergyEnds.clear(level, ctx.abs(SOURCE));
        TestEnergyEnds.clear(level, ctx.abs(SINK));
        level.setBlockAndUpdate(ctx.abs(SOURCE), Blocks.CUT_COPPER.defaultBlockState());
        level.setBlockAndUpdate(ctx.abs(SINK), Blocks.EXPOSED_CUT_COPPER.defaultBlockState());
        for (final BlockPos at : RUN) {
            TestCables.lay(level, ctx.abs(at), TestEnergies.LOW_LINE.get());
        }
        TestEnergyEnds.storeAt(level, ctx.abs(SOURCE)).setStored(TestEnergyEnds.CAPACITY);
    }

    /* Presses the probe held up at ({@code x}, {@code y}) of its picture, which is drawn in the middle of the screen. */
    private static void press(final ClientTestContext ctx, final int x, final int y) {
        final int left = (ctx.mc().getWindow().getGuiScaledWidth() - ProbeLayout.WIDTH) / 2;
        final int top = (ctx.mc().getWindow().getGuiScaledHeight() - ProbeLayout.HEIGHT) / 2;
        ctx.click(left + x + 0.5, top + y + 0.5);
    }

    private static void clear(final ClientTestContext ctx, final ServerLevel level) {
        for (final BlockPos at : List.of(SOURCE, RUN.get(0), RUN.get(1), RUN.get(2), RUN.get(3), SINK)) {
            level.setBlockAndUpdate(ctx.abs(at), Blocks.AIR.defaultBlockState());
        }
    }
}
