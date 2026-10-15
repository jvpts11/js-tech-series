/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.series;

import dev.jstech.core.overhead.OverheadLines;
import dev.jstech.core.overhead.WireEnd;
import dev.jstech.tests.TestEnergies;
import dev.jstech.tests.TestEnergyEnds;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Overhead wires on a player's game: a line hung from a source over two poles to a sink, each span sagging along its
 * curve between its insulators, the longest the most.
 */
public final class OverheadLinesClientTests {

    private static final BlockPos STAND = new BlockPos(0, 2, -4);
    private static final int FLOOR = 2;
    private static final BlockPos SOURCE = new BlockPos(-7, FLOOR, 6);
    private static final BlockPos POLE = new BlockPos(-2, FLOOR, 6);
    private static final BlockPos TALL = new BlockPos(2, FLOOR, 6);
    private static final BlockPos SINK = new BlockPos(9, FLOOR, 6);
    private static final int WAIT = 200;

    private OverheadLinesClientTests() {
    }

    @ClientTest(timeoutTicks = 600)
    public static void wires_sagBetweenTheirInsulators(final ClientTestContext ctx) {
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> build(ctx, level))
                .thenWaitUntil(() -> OverheadLines.WIRES.client().wires().size() >= 3, WAIT,
                        "the client to hear of the three wires")
                .then(0, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(POLE)).add(1.5, 1.0, 0)))
                .thenScreenshot(10, "overhead-wires")
                .thenServer(0, level -> clear(ctx, level));
    }

    /* A source, a pole, a pole on a pillar, and a sink, a wire hung between each and the next. */
    private static void build(final ClientTestContext ctx, final ServerLevel level) {
        clear(ctx, level);
        TestEnergyEnds.clear(level, ctx.abs(SOURCE));
        TestEnergyEnds.clear(level, ctx.abs(SINK));
        level.setBlockAndUpdate(ctx.abs(SOURCE), Blocks.CUT_COPPER.defaultBlockState());
        level.setBlockAndUpdate(ctx.abs(SINK), Blocks.EXPOSED_CUT_COPPER.defaultBlockState());
        level.setBlockAndUpdate(ctx.abs(POLE), Blocks.END_ROD.defaultBlockState());
        level.setBlockAndUpdate(ctx.abs(TALL), Blocks.STONE_BRICKS.defaultBlockState());
        level.setBlockAndUpdate(ctx.abs(TALL.above()), Blocks.END_ROD.defaultBlockState());
        TestEnergyEnds.storeAt(level, ctx.abs(SOURCE)).setStored(TestEnergyEnds.CAPACITY);
        hang(ctx, level, SOURCE, POLE);
        hang(ctx, level, POLE, TALL.above());
        hang(ctx, level, TALL.above(), SINK);
    }

    private static void hang(final ClientTestContext ctx, final ServerLevel level, final BlockPos from,
                             final BlockPos to) {
        OverheadLines.hang(null, level, new WireEnd(ctx.abs(from), 0), new WireEnd(ctx.abs(to), 0),
                TestEnergies.HUNG_LINE.get(), new ItemStack(Items.STRING, 64));
    }

    private static void clear(final ClientTestContext ctx, final ServerLevel level) {
        for (final BlockPos at : List.of(SOURCE, POLE, TALL, TALL.above(), SINK)) {
            level.setBlockAndUpdate(ctx.abs(at), Blocks.AIR.defaultBlockState());
        }
    }
}
