/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.series;

import dev.jstech.core.cable.Cables;
import dev.jstech.core.jstechseries.energy.ClampMeter;
import dev.jstech.core.jstechseries.energy.ClampMeterLayout;
import dev.jstech.core.jstechseries.energy.MeterReading;
import dev.jstech.core.jstechseries.energy.MeterState;
import dev.jstech.core.jstechseries.energy.MeterView;
import dev.jstech.core.jstechseries.energy.client.ClampMeterScreen;
import dev.jstech.tests.TestEnergies;
import dev.jstech.tests.TestEnergyEnds;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/**
 * The clamp meter on a player's game, to set beside the approved design: clamped on a low line, its reading beside
 * the crosshair; held up, the line's reading on its display, then in FE once its dial is turned; clamped on the sink,
 * the block's faces.
 */
public final class ClampMeterClientTests {

    private static final BlockPos STAND = new BlockPos(0, 2, -1);
    private static final int ROW = 2;
    private static final int FLOOR = 2;
    private static final BlockPos SOURCE = new BlockPos(-2, FLOOR, ROW);
    private static final BlockPos SINK = new BlockPos(2, FLOOR, ROW);
    private static final List<BlockPos> RUN = List.of(new BlockPos(-1, FLOOR, ROW), new BlockPos(0, FLOOR, ROW),
            new BlockPos(1, FLOOR, ROW));
    private static final int WAIT = 120;

    private ClampMeterClientTests() {
    }

    @ClientTest(timeoutTicks = 600)
    public static void meter_readsALineAndABlockOnTheHudAndOnItsDisplay(final ClientTestContext ctx) {
        ctx.afterTest(() -> {
            ctx.mc().setScreen(null);
            ctx.hold(ItemStack.EMPTY);
        });
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> build(ctx, level))
                .then(2, () -> ctx.hold(new ItemStack(ClampMeter.ITEM.get())))
                .thenRightClick(5, RUN.get(1))
                .thenWaitUntil(() -> {
                    final MeterReading reading = MeterView.shown(false);
                    return reading.present() && reading.line() && reading.perTick() > 0
                            && reading.toward().isPresent();
                }, WAIT, "the meter to read the line, and which way it runs")
                .thenScreenshot(10, "meter-hud")
                .then(0, () -> ClampMeter.open(InteractionHand.MAIN_HAND))
                .thenAssert(2, () -> ctx.mc().screen instanceof ClampMeterScreen, "the meter is held up")
                .thenScreenshot(5, "meter-line")
                .then(0, () -> ctx.click(left(ctx) + ClampMeterLayout.DIAL_X + 0.5,
                        top(ctx) + ClampMeterLayout.DIAL_Y + 0.5))
                .thenAssert(2, () -> ctx.mc().screen instanceof ClampMeterScreen meter
                        && meter.state().dial() == MeterState.Dial.FE, "a click on the dial turns it to FE")
                .thenScreenshot(5, "meter-line-fe")
                .then(0, () -> ctx.click(left(ctx) + ClampMeterLayout.MAX_MIN_X + 4.5,
                        top(ctx) + ClampMeterLayout.MAX_MIN_Y + 4.5))
                .thenAssert(2, () -> ctx.mc().screen instanceof ClampMeterScreen meter && meter.state().maxMin(),
                        "MAX MIN shows the most and the least")
                .then(0, () -> ctx.mc().setScreen(null))
                .thenRightClick(2, SINK)
                .thenWaitUntil(() -> {
                    final MeterReading reading = MeterView.shown(false);
                    return reading.present() && !reading.line() && reading.stored() > 0;
                }, WAIT, "the meter to read the sink")
                .then(0, () -> ClampMeter.open(InteractionHand.MAIN_HAND))
                .thenScreenshot(5, "meter-faces")
                .thenServer(0, level -> clear(ctx, level));
    }

    /* A low source, a run of low line and a low sink, the source full. */
    private static void build(final ClientTestContext ctx, final ServerLevel level) {
        TestEnergyEnds.clear(level, ctx.abs(SOURCE));
        TestEnergyEnds.clear(level, ctx.abs(SINK));
        level.setBlockAndUpdate(ctx.abs(SOURCE), Blocks.CUT_COPPER.defaultBlockState());
        level.setBlockAndUpdate(ctx.abs(SINK), Blocks.EXPOSED_CUT_COPPER.defaultBlockState());
        for (final BlockPos at : RUN) {
            Cables.lay(level, ctx.abs(at), TestEnergies.LOW_LINE.get());
        }
        TestEnergyEnds.storeAt(level, ctx.abs(SOURCE)).setStored(TestEnergyEnds.CAPACITY);
    }

    private static void clear(final ClientTestContext ctx, final ServerLevel level) {
        for (final BlockPos at : List.of(SOURCE, SINK, RUN.get(0), RUN.get(1), RUN.get(2))) {
            level.setBlockAndUpdate(ctx.abs(at), Blocks.AIR.defaultBlockState());
        }
    }

    private static int left(final ClientTestContext ctx) {
        return (ctx.mc().getWindow().getGuiScaledWidth() - ClampMeterLayout.WIDTH) / 2;
    }

    private static int top(final ClientTestContext ctx) {
        return (ctx.mc().getWindow().getGuiScaledHeight() - ClampMeterLayout.HEIGHT) / 2;
    }
}
