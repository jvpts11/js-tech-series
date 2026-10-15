/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.series;

import dev.jstech.core.cable.CableBlockEntity;
import dev.jstech.core.cable.Lane;
import dev.jstech.core.cable.WireFlow;
import dev.jstech.core.jstechseries.lines.SharedPipes;
import dev.jstech.tests.TestFluidEnds;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.testkit.TestCables;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * The basic pipes on a player's game, to set beside the approved art: copper pipes carrying water from an output to an
 * input, round a bend, their fitting solid, the water seen moving through their sight glasses at its level; and the
 * gas and cryogenic pipes beside them.
 */
public final class PipeClientTests {

    private static final BlockPos STAND = new BlockPos(0, 2, -2);
    private static final int FLOOR = 2;
    private static final BlockPos OUTPUT = new BlockPos(-3, FLOOR, 1);
    private static final BlockPos INPUT = new BlockPos(2, FLOOR + 1, 1);
    private static final List<BlockPos> RUN = List.of(new BlockPos(-2, FLOOR, 1), new BlockPos(-1, FLOOR, 1),
            new BlockPos(0, FLOOR, 1), new BlockPos(1, FLOOR, 1), new BlockPos(1, FLOOR + 1, 1));
    private static final BlockPos GAS = new BlockPos(-1, FLOOR, 3);
    private static final BlockPos CRYO = new BlockPos(1, FLOOR, 3);
    private static final int WAIT = 200;

    private PipeClientTests() {
    }

    @ClientTest(timeoutTicks = 600)
    public static void pipes_showTheirWaterMovingThroughTheGlass(final ClientTestContext ctx) {
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> build(ctx, level))
                .thenWaitUntil(() -> flowing(ctx), WAIT, "the client to see water in the pipes")
                .thenScreenshot(10, "pipes-water")
                .thenServer(0, level -> clear(ctx, level));
    }

    /* Whether the client sees water in the middle pipe. */
    private static boolean flowing(final ClientTestContext ctx) {
        return ctx.mc().level != null && ctx.mc().level.getBlockEntity(ctx.abs(RUN.get(2)))
                instanceof CableBlockEntity cable && !cable.flow(Lane.MIDDLE).isEmpty()
                && cable.flow(Lane.MIDDLE).level() > 0 && !cable.flow(Lane.MIDDLE).equals(WireFlow.EMPTY);
    }

    private static void build(final ClientTestContext ctx, final ServerLevel level) {
        clear(ctx, level);
        TestFluidEnds.clear(level, ctx.abs(OUTPUT));
        TestFluidEnds.clear(level, ctx.abs(INPUT));
        TestFluidEnds.tankAt(level, ctx.abs(OUTPUT)).fill(new FluidStack(Fluids.WATER, 60_000),
                IFluidHandler.FluidAction.EXECUTE);
        level.setBlockAndUpdate(ctx.abs(OUTPUT), Blocks.CHISELED_TUFF.defaultBlockState());
        level.setBlockAndUpdate(ctx.abs(INPUT), Blocks.CHISELED_TUFF_BRICKS.defaultBlockState());
        for (final BlockPos at : RUN) {
            TestCables.lay(level, ctx.abs(at), SharedPipes.COPPER_PIPE.get());
        }
        TestCables.lay(level, ctx.abs(GAS), SharedPipes.IRON_GAS_PIPE.get());
        TestCables.lay(level, ctx.abs(GAS.east()), SharedPipes.IRON_GAS_PIPE.get());
        TestCables.lay(level, ctx.abs(CRYO), SharedPipes.VACUUM_JACKETED_PIPE.get());
        TestCables.lay(level, ctx.abs(CRYO.east()), SharedPipes.VACUUM_JACKETED_PIPE.get());
    }

    private static void clear(final ClientTestContext ctx, final ServerLevel level) {
        for (final BlockPos at : List.of(OUTPUT, INPUT, RUN.get(0), RUN.get(1), RUN.get(2), RUN.get(3), RUN.get(4),
                GAS, GAS.east(), CRYO, CRYO.east())) {
            level.setBlockAndUpdate(ctx.abs(at), Blocks.AIR.defaultBlockState());
        }
    }
}
