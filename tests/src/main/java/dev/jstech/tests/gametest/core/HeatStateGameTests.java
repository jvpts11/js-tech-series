/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.cable.CableType;
import dev.jstech.core.heat.HeatBalance;
import dev.jstech.core.heat.HeatBody;
import dev.jstech.core.heat.HeatMath;
import dev.jstech.core.heat.Thermal;
import dev.jstech.core.jstechseries.Element;
import dev.jstech.core.jstechseries.Elements;
import dev.jstech.core.jstechseries.SharedCatalogue;
import dev.jstech.core.material.MatterState;
import dev.jstech.core.matter.MatterStack;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestCableTypes;
import dev.jstech.tests.TestClimate;
import dev.jstech.tests.TestFluidEnds;
import dev.jstech.tests.TestHeat;
import dev.jstech.tests.TestMaterials;
import dev.jstech.tests.testkit.TestCables;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Heat changing the state of things: water solid, liquid or gas by temperature and pressure, a hot block kept hot by
 * the wool around it, a pipe of water frozen in deep cold unless insulated or the server turned freezing off, and
 * liquid nitrogen boiling off a tank as the place's heat leaks in, less the better the tank is insulated.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class HeatStateGameTests {

    private static final String ARENA = "empty";
    private static final double HOT = 600.0;
    private static final int WAIT = 20;
    /** The liquid nitrogen a tank is given, in millibuckets. */
    private static final int NITROGEN = 4_000;
    /* The litres of nitrogen gas a litre of the liquid makes: 0.8061 kg at 28.014 g a mole, a mole to 22.414. */
    private static final double NITROGEN_GAS_PER_LITRE = 0.8061 / 0.028_014 * 22.414;

    private HeatStateGameTests() {
    }

    @GameTest(template = ARENA)
    public static void water_isSolidLiquidOrGasByTemperatureAndPressure(final GameTestHelper helper) {
        final Thermal water = Thermal.of(SharedCatalogue.WATER, false);
        final double air = HeatMath.SEA_LEVEL_KPA;
        helper.assertTrue(water.phaseAt(260.0, air) == MatterState.SOLID, "ice below 0 °C");
        helper.assertTrue(water.phaseAt(300.0, air) == MatterState.LIQUID, "water at room temperature");
        helper.assertTrue(water.phaseAt(380.0, air) == MatterState.GAS, "steam above 100 °C");
        helper.assertTrue(water.phaseAt(350.0, 33.7) == MatterState.GAS, "but at 77 °C on top of Everest");
        helper.assertTrue(water.phaseAt(270.0, 0.6) == MatterState.GAS,
                "and on Mars ice goes straight to steam at −3 °C");
        helper.assertTrue(water.phaseAt(400.0, 1_000.0) == MatterState.LIQUID, "while a vessel keeps it liquid");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 80)
    public static void insulator_keepsAHotBlockHotterThanStoneDoes(final GameTestHelper helper) {
        final HeatBody wrapped = wrapped(helper, new BlockPos(2, 3, 2), Blocks.WHITE_WOOL);
        final HeatBody bare = wrapped(helper, new BlockPos(6, 3, 2), Blocks.STONE);
        helper.runAfterDelay(2, () -> {
            wrapped.set(HOT);
            bare.set(HOT);
            helper.runAfterDelay(40, () -> {
                helper.assertTrue(bare.temperature() < HOT - 2.0, "stone draws heat off a hot block: "
                        + bare.temperature());
                helper.assertTrue(wrapped.temperature() > bare.temperature() + 2.0,
                        "wool keeps it in: " + wrapped.temperature() + " K against " + bare.temperature() + " K");
                helper.succeed();
            });
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void pipe_ofWaterFreezesInDeepColdUnlessInsulated(final GameTestHelper helper) {
        final FluidTank plain = run(helper, 1, TestCableTypes.PLAIN_PIPE);
        final FluidTank insulated = run(helper, 4, TestCableTypes.INSULATED_PIPE);
        helper.runAfterDelay(WAIT, () -> {
            helper.assertTrue(plain.isEmpty(), "the plain pipe froze: " + plain.getFluidAmount() + " mB came");
            helper.assertTrue(insulated.getFluidAmount() > 0, "the insulated one still carries water");
            HeatBalance.setPipesFreeze(false);
            helper.runAfterDelay(WAIT, () -> {
                HeatBalance.setPipesFreeze(true);
                warm(helper, 1);
                warm(helper, 4);
                helper.assertTrue(plain.getFluidAmount() > 0,
                        "and with freezing turned off the plain pipe carries it too");
                helper.succeed();
            });
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void boilOff_ofLiquidNitrogenFillsItsGasTankUnlessTheTankIsAVacuumFlask(
            final GameTestHelper helper) {
        final TestHeat.VesselBlockEntity bare = vessel(helper, new BlockPos(1, 2, 2), TestHeat.COLD_TANK.get());
        final TestHeat.VesselBlockEntity flask = vessel(helper, new BlockPos(4, 2, 2), TestHeat.DEWAR.get());
        for (final TestHeat.VesselBlockEntity tank : new TestHeat.VesselBlockEntity[] {bare, flask}) {
            tank.in().fill(new FluidStack(TestMaterials.LIQUID_NITROGEN.get(), NITROGEN),
                    IFluidHandler.FluidAction.EXECUTE);
        }
        helper.runAfterDelay(60, () -> {
            final int lost = NITROGEN - bare.in().getFluidAmount();
            helper.assertTrue(lost > 5, "a tank open to the air's heat boils its nitrogen off: " + lost + " mB");
            final MatterStack gas = bare.gas().held().orElse(null);
            helper.assertTrue(gas != null && gas.material() == Elements.of(Element.NITROGEN),
                    "into its gas: " + gas);
            helper.assertTrue(Math.abs((double) gas.amount() / lost - NITROGEN_GAS_PER_LITRE) < 1.0,
                    "about 645 of gas to a litre of liquid: " + (double) gas.amount() / lost);
            helper.assertTrue(flask.in().getFluidAmount() == NITROGEN, "a vacuum flask loses none in seconds");
            helper.succeed();
        });
    }

    /* A litre of copper at {@code pos}, every face against a block of {@code around}. */
    private static HeatBody wrapped(final GameTestHelper helper, final BlockPos pos, final Block around) {
        for (final Direction face : Direction.values()) {
            helper.setBlock(pos.relative(face), around);
        }
        helper.setBlock(pos, TestHeat.COPPER.get());
        final TestHeat.HeatBlockEntity entity = helper.getBlockEntity(pos);
        return entity.heatBody();
    }

    /*
     * A run of {@code pipe} along z = {@code row}, chilled, from a tank of water at x = 1 into an empty one at x = 5;
     * the tank it fills.
     */
    private static FluidTank run(final GameTestHelper helper, final int row, final Supplier<CableType> pipe) {
        final BlockPos output = new BlockPos(1, 2, row);
        final BlockPos input = new BlockPos(5, 2, row);
        TestFluidEnds.clear(helper.getLevel(), helper.absolutePos(output));
        TestFluidEnds.clear(helper.getLevel(), helper.absolutePos(input));
        TestFluidEnds.tankAt(helper.getLevel(), helper.absolutePos(output))
                .fill(new FluidStack(Fluids.WATER, 10_000), IFluidHandler.FluidAction.EXECUTE);
        for (int x = 2; x <= 4; x++) {
            TestClimate.chill(helper.absolutePos(new BlockPos(x, 2, row)));
        }
        helper.setBlock(output, Blocks.CHISELED_TUFF);
        helper.setBlock(input, Blocks.CHISELED_TUFF_BRICKS);
        for (int x = 2; x <= 4; x++) {
            TestCables.lay(helper, new BlockPos(x, 2, row), pipe);
        }
        return TestFluidEnds.tankAt(helper.getLevel(), helper.absolutePos(input));
    }

    private static void warm(final GameTestHelper helper, final int row) {
        for (int x = 2; x <= 4; x++) {
            TestClimate.warm(helper.absolutePos(new BlockPos(x, 2, row)));
        }
    }

    private static TestHeat.VesselBlockEntity vessel(final GameTestHelper helper, final BlockPos pos,
                                                     final Block block) {
        helper.setBlock(pos, block);
        return helper.getBlockEntity(pos);
    }
}
