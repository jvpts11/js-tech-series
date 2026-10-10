/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.core.cable.CableBlockEntity;
import dev.jstech.core.cable.CableType;
import dev.jstech.core.fluid.FluidGrids;
import dev.jstech.core.fluid.PipeLimits;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestCableTypes;
import dev.jstech.tests.TestFluidEnds;
import dev.jstech.tests.TestFluids;
import dev.jstech.tests.testkit.TestCables;
import java.util.List;
import java.util.OptionalLong;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Pipes of the Core's fluid grid: a run moves what its slowest pipe carries in a tick from an output into an input,
 * leaves where it is a fluid it is not made for (a gas, a corrosive, one hotter than it stands), carries them when it
 * is made for them, and fills a tank; a run of pipes with no temperature in common carries nothing.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class PipeGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos OUTPUT = new BlockPos(1, 2, 2);
    private static final BlockPos INPUT = new BlockPos(5, 2, 2);
    private static final List<BlockPos> RUN = List.of(new BlockPos(2, 2, 2), new BlockPos(3, 2, 2),
            new BlockPos(4, 2, 2));
    private static final int WAIT = 10;

    private PipeGameTests() {
    }

    @GameTest(template = ARENA)
    public static void pipe_movesWhatItsRateAllowsFromAnOutputIntoAnInput(final GameTestHelper helper) {
        final FluidTank output = ends(helper, new FluidStack(Fluids.WATER, 10_000), TestCableTypes.PLAIN_PIPE);
        final FluidTank input = TestFluidEnds.tankAt(helper.getLevel(), helper.absolutePos(INPUT));

        helper.succeedWhen(() -> {
            final OptionalLong moved = FluidGrids.movedLastTickAt(helper.getLevel(),
                    TestCables.cable(helper, RUN.get(0)).place(TestCableTypes.PLAIN_PIPE.get()));
            helper.assertTrue(moved.isPresent() && moved.getAsLong() == TestCableTypes.PLAIN_RATE,
                    "the run moves a hundred millibuckets a tick: " + moved);
            helper.assertTrue(input.getFluid().is(Fluids.WATER) && input.getFluidAmount() > 0, "water arrives");
            helper.assertTrue(output.getFluidAmount() + input.getFluidAmount() == 10_000, "and none is lost");
        });
    }

    /**
     * A pipe that stands 100 to 200 K and one that stands 300 to 400 K have no temperature in common, so a run of the
     * two carries nothing: water at 300 K, which the second stands on its own, does not pass them both.
     */
    @GameTest(template = ARENA)
    public static void and_ofPipesWithNoTemperatureInCommon_carriesNothing(final GameTestHelper helper) {
        final int water = Fluids.WATER.getFluidType().getTemperature();
        final PipeLimits cold = new PipeLimits(water - 200, water - 100, Set.of());
        final PipeLimits hot = new PipeLimits(water, water + 100, Set.of());
        helper.assertTrue(hot.carries(Fluids.WATER), "the hot pipe alone carries water at " + water + " K");
        helper.assertFalse(cold.and(hot).carries(Fluids.WATER), "a run of both does not");
        helper.assertFalse(hot.and(cold).carries(Fluids.WATER), "whichever way round it is joined");
        helper.assertTrue(new PipeLimits(water - 200, water + 50, Set.of()).and(hot).carries(Fluids.WATER),
                "while pipes whose temperatures meet carry what both stand");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void plainPipe_leavesAGasWhereItIs(final GameTestHelper helper) {
        stays(helper, TestFluids.GAS.stack(5_000));
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void plainPipe_leavesACorrosiveWhereItIs(final GameTestHelper helper) {
        stays(helper, TestFluids.ACID.stack(5_000));
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void plainPipe_leavesAFluidHotterThanItStandsWhereItIs(final GameTestHelper helper) {
        helper.assertTrue(Fluids.LAVA.getFluidType().getTemperature() > TestCableTypes.PLAIN_HOTTEST,
                "lava is hotter than the plain pipe stands");
        stays(helper, new FluidStack(Fluids.LAVA, 5_000));
    }

    @GameTest(template = ARENA)
    public static void pressurePipe_carriesGasesCorrosivesAndLava(final GameTestHelper helper) {
        final FluidTank output = ends(helper, TestFluids.GAS.stack(5_000), TestCableTypes.PRESSURE_PIPE);
        final FluidTank input = TestFluidEnds.tankAt(helper.getLevel(), helper.absolutePos(INPUT));
        final PipeLimits limits = TestCableTypes.PRESSURE_PIPE.get().pipe();
        helper.assertTrue(limits.carries(TestFluids.ACID.source()) && limits.carries(Fluids.LAVA),
                "it is made for corrosives and stands lava");

        helper.succeedWhen(() -> {
            helper.assertTrue(input.getFluid().is(TestFluids.GAS.source()), "the gas arrives");
            helper.assertTrue(output.getFluidAmount() + input.getFluidAmount() == 5_000, "and none is lost");
        });
    }

    @GameTest(template = ARENA)
    public static void pipe_fillsATank(final GameTestHelper helper) {
        final FluidTank output = ends(helper, new FluidStack(Fluids.WATER, 2_000), TestCableTypes.PLAIN_PIPE);
        helper.setBlock(INPUT, ComputingModule.TANK.get());
        final IFluidHandler tank = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK,
                helper.absolutePos(INPUT), null);
        helper.assertTrue(tank != null, "the tank holds fluids");

        helper.succeedWhen(() -> {
            helper.assertTrue(tank.getFluidInTank(0).is(Fluids.WATER) && tank.getFluidInTank(0).getAmount() > 0,
                    "the tank fills with water");
            helper.assertTrue(output.getFluidAmount() + tank.getFluidInTank(0).getAmount() == 2_000,
                    "none is lost");
        });
    }

    /* An output holding {@code fluid}, a run of {@code pipe} and an input; the output's tank. */
    private static FluidTank ends(final GameTestHelper helper, final FluidStack fluid,
                                  final Supplier<CableType> pipe) {
        TestFluidEnds.clear(helper.getLevel(), helper.absolutePos(OUTPUT));
        TestFluidEnds.clear(helper.getLevel(), helper.absolutePos(INPUT));
        final FluidTank output = TestFluidEnds.tankAt(helper.getLevel(), helper.absolutePos(OUTPUT));
        output.fill(fluid, IFluidHandler.FluidAction.EXECUTE);
        helper.setBlock(OUTPUT, Blocks.CHISELED_TUFF);
        helper.setBlock(INPUT, Blocks.CHISELED_TUFF_BRICKS);
        for (final BlockPos at : RUN) {
            TestCables.lay(helper, at, pipe);
        }
        return output;
    }

    /* A plain run between an output of {@code fluid} and an input moves none of it. */
    private static void stays(final GameTestHelper helper, final FluidStack fluid) {
        final FluidTank output = ends(helper, fluid, TestCableTypes.PLAIN_PIPE);
        final FluidTank input = TestFluidEnds.tankAt(helper.getLevel(), helper.absolutePos(INPUT));
        helper.runAfterDelay(WAIT, () -> {
            final CableBlockEntity last = TestCables.cable(helper, RUN.get(RUN.size() - 1));
            helper.assertTrue((last.plugs(last.wire(TestCableTypes.PLAIN_PIPE.get()).slot())
                    & 1 << Direction.EAST.get3DDataValue()) != 0, "the pipe meets the input");
            helper.assertTrue(input.isEmpty() && output.getFluidAmount() == fluid.getAmount(),
                    "the fluid stayed where it was");
            helper.succeed();
        });
    }
}
