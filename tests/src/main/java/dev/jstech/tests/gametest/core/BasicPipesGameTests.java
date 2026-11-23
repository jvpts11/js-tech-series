/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.cable.BundleShape;
import dev.jstech.core.cable.CableBlockEntity;
import dev.jstech.core.cable.CableItem;
import dev.jstech.core.cable.Wire;
import dev.jstech.core.cable.WireFlow;
import dev.jstech.core.jstechseries.energy.SharedEnergy;
import dev.jstech.core.jstechseries.lines.SharedPipes;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestFluidEnds;
import dev.jstech.tests.testkit.TestCables;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

    /**
     * Pipes of different kinds share a block, each five pixels thick in its own place in the cross, the liquid pipe in
     * the middle, the gas pipe at the top and the cryogenic pipe on the right; one alone runs through the middle; and
     * two of one kind never share a block, nor a pipe and a cable.
     */
    @GameTest(template = ARENA)
    public static void pipes_shareABlockEachInItsPlaceInTheCross(final GameTestHelper helper) {
        final List<BlockPos> row = List.of(new BlockPos(1, 2, 5), new BlockPos(2, 2, 5), new BlockPos(3, 2, 5));
        for (final BlockPos at : row) {
            TestCables.lay(helper, at, SharedPipes.COPPER_PIPE);
            TestCables.lay(helper, at, SharedPipes.IRON_GAS_PIPE);
            TestCables.lay(helper, at, SharedPipes.VACUUM_JACKETED_PIPE);
        }
        final BlockPos alone = new BlockPos(1, 2, 8);
        TestCables.lay(helper, alone, SharedPipes.IRON_GAS_PIPE);
        TestCables.lay(helper, alone.east(), SharedPipes.IRON_GAS_PIPE);
        helper.assertTrue(!CableItem.lay(helper.getLevel(), helper.absolutePos(alone), SharedPipes.IRON_GAS_PIPE.get()),
                "two gas pipes never share a block");
        helper.assertTrue(!CableItem.lay(helper.getLevel(), helper.absolutePos(alone),
                SharedEnergy.INSULATED_COPPER_CABLE.get()), "nor a pipe and a cable");
        helper.succeedWhen(() -> {
            final BundleShape shared = TestCables.cable(helper, row.get(1)).shape();
            helper.assertTrue(!shared.junction(), "three pipes side by side need no junction box");
            // Along x, a lane's across is z and its up is y, five pixels from one lane to the next.
            final Set<BundleShape.Box> expected = Set.of(new BundleShape.Box(0, 5.5, 5.5, 16, 10.5, 10.5),
                    new BundleShape.Box(0, 10.5, 5.5, 16, 15.5, 10.5),
                    new BundleShape.Box(0, 5.5, 10.5, 16, 10.5, 15.5));
            final Set<BundleShape.Box> laid = new HashSet<>(jackets(shared));
            helper.assertTrue(laid.equals(expected), "the liquid pipe in the middle, the gas pipe at the top and the "
                    + "cryogenic pipe on the right, straight through: " + laid);
            final List<BundleShape.Box> single = jackets(TestCables.cable(helper, alone).shape());
            helper.assertTrue(single.equals(List.of(new BundleShape.Box(5.5, 5.5, 5.5, 10.5, 10.5, 10.5),
                            new BundleShape.Box(10.5, 5.5, 5.5, 16, 10.5, 10.5))),
                    "a gas pipe alone runs through the middle, its core and its arm east: " + single);
        });
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

    private static List<BundleShape.Box> jackets(final BundleShape shape) {
        return shape.pieces().stream().filter(piece -> piece.kind() == BundleShape.Kind.JACKET)
                .map(BundleShape.Piece::box).toList();
    }

    private static WireFlow flow(final GameTestHelper helper, final BlockPos at) {
        final CableBlockEntity cable = TestCables.cable(helper, at);
        final Wire wire = cable.wire(SharedPipes.COPPER_PIPE.get());
        return wire == null ? WireFlow.EMPTY : cable.flow(wire.slot());
    }
}
