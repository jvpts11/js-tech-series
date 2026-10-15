/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.cable.CableBlockEntity;
import dev.jstech.core.cable.Wire;
import dev.jstech.core.cable.WireFlow;
import dev.jstech.core.jstechseries.lines.SharedPipes;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestFluidEnds;
import dev.jstech.tests.testkit.TestCables;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The basic pipes the series shares: each pipe a liquid crosses shows what it is, which way it goes and how full the
 * pipe is, for its sight glass and a wrench to read, and shows nothing once the liquid has stopped; and a copper pipe
 * that carries lava, far hotter than it stands, bursts and spills it.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class BasicPipesGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos OUTPUT = new BlockPos(1, 2, 2);
    private static final List<BlockPos> RUN = List.of(new BlockPos(2, 2, 2), new BlockPos(3, 2, 2),
            new BlockPos(4, 2, 2));
    private static final BlockPos INPUT = new BlockPos(5, 2, 2);

    private BasicPipesGameTests() {
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void flow_showsWhatCrossesEachPipeAndClearsWhenItStops(final GameTestHelper helper) {
        ends(helper, Fluids.WATER, 1_000);
        helper.startSequence().thenWaitUntil(() -> {
            final WireFlow flow = flow(helper, RUN.get(1));
            helper.assertTrue("minecraft:water".equals(flow.what()), "the middle pipe shows water: " + flow);
            helper.assertTrue(flow.toward() == Direction.EAST, "going east, toward the input");
            helper.assertTrue(flow.perTick() > 0 && flow.level() > 0, "at a level its glass shows");
        }).thenWaitUntil(() -> helper.assertTrue(flow(helper, RUN.get(1)).isEmpty(),
                "the output empty, the pipe shows nothing")).thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void copperPipe_burstsUnderLavaAndSpillsIt(final GameTestHelper helper) {
        ends(helper, Fluids.LAVA, 5_000);
        helper.succeedWhen(() -> helper.assertTrue(helper.getBlockState(RUN.get(0)).is(Blocks.LAVA),
                "the copper pipe burst, spilling lava"));
    }

    /* An output of {@code amount} of {@code fluid}, a run of copper pipe east, and an input past it. */
    private static void ends(final GameTestHelper helper, final Fluid fluid, final int amount) {
        TestFluidEnds.clear(helper.getLevel(), helper.absolutePos(OUTPUT));
        TestFluidEnds.clear(helper.getLevel(), helper.absolutePos(INPUT));
        TestFluidEnds.tankAt(helper.getLevel(), helper.absolutePos(OUTPUT)).fill(new FluidStack(fluid, amount),
                IFluidHandler.FluidAction.EXECUTE);
        helper.setBlock(OUTPUT, Blocks.CHISELED_TUFF);
        helper.setBlock(INPUT, Blocks.CHISELED_TUFF_BRICKS);
        for (final BlockPos at : RUN) {
            TestCables.lay(helper, at, SharedPipes.COPPER_PIPE);
        }
    }

    private static WireFlow flow(final GameTestHelper helper, final BlockPos at) {
        final CableBlockEntity cable = TestCables.cable(helper, at);
        final Wire wire = cable.wire(SharedPipes.COPPER_PIPE.get());
        return wire == null ? WireFlow.EMPTY : cable.flow(wire.slot());
    }
}
