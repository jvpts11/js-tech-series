/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.heat.HeatBody;
import dev.jstech.core.heat.HeatClimate;
import dev.jstech.core.heat.LevelHeat;
import dev.jstech.core.heat.Thermal;
import dev.jstech.core.jstechseries.Element;
import dev.jstech.core.jstechseries.Elements;
import dev.jstech.core.jstechseries.SeriesMaterials;
import dev.jstech.core.jstechseries.SharedCatalogue;
import dev.jstech.core.material.MaterialKind;
import dev.jstech.core.material.MaterialLiquids;
import dev.jstech.core.matter.MatterStack;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestCableTypes;
import dev.jstech.tests.TestFluids;
import dev.jstech.tests.TestHeat;
import dev.jstech.tests.testkit.TestCables;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Core's heat in a world: the materials' real data, heat flowing from a hot block to its colder neighbour, a hot
 * block cooling in the air, a radiator's fins cooling it faster, a heater warming what it touches and stopping at its
 * limit, a heat pipe carrying heat between two blocks apart, a block in balance going to sleep, a boiler boiling water
 * by its real enthalpy and lower at low pressure, and an exchanger's oil changing form through its wall.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class HeatGameTests {

    private static final String ARENA = "empty";
    private static final double HOT = 600.0;
    /** A boiler's wall well above where water boils, and one below it that a boiler of low pressure still boils at. */
    private static final double HOT_WALL = 1_000.0;
    private static final double WARM_WALL = 360.0;
    /** The water a boiler is given, in millibuckets. */
    private static final int WATER = 4_000;
    /*
     * What a millibucket, a litre, of the game's water at 300 K takes to boil in the open air: 0.998 kg, warmed to
     * 373.15 K at 4,184 J/(kg·K), then 2.257 MJ a kilogram; and the litres of steam it makes, a mole to 22.414.
     */
    private static final double WATER_BOILS = 0.998 * (4_184.0 * (373.15 - 300.0) + 2_257_000.0);
    private static final double STEAM_PER_LITRE = 0.998 / 0.018_015 * 22.414;

    private HeatGameTests() {
    }

    @GameTest(template = ARENA)
    public static void materials_carryTheirRealHeatData(final GameTestHelper helper) {
        final Thermal copper = Thermal.of(Elements.of(Element.COPPER), false);
        helper.assertTrue(copper.density() == 8_960.0 && copper.specificHeat() == 385.0
                && copper.conductivity() == 401.0, "copper's own data: " + copper);
        helper.assertTrue(Thermal.of(SeriesMaterials.BRONZE, false).melting() == 1_223.0, "bronze melts at 1223 K");
        final Thermal unknown = Thermal.of(Elements.of(Element.OGANESSON), false);
        helper.assertTrue(unknown.equals(Thermal.ofKind(Elements.of(Element.OGANESSON).kind())),
                "an element with no data is taken as its kind is");
        helper.assertTrue(Thermal.ofKind(MaterialKind.GAS).density() < 2.0, "a gas is light");
        helper.assertTrue(MaterialLiquids.materialOf(Fluids.WATER).orElse(null) == SharedCatalogue.WATER,
                "the game's water is the series' water");
        helper.assertTrue(Thermal.of(SharedCatalogue.WATER, false).heatOfBoiling() == 2_257_000.0,
                "which takes 2.257 MJ a kilogram to boil");
        final Thermal nitrogen = Thermal.of(Elements.of(Element.NITROGEN), false);
        helper.assertTrue(Math.abs(nitrogen.litreKilograms() - 0.8061) < 1e-9
                && Math.abs(nitrogen.molarMass() - 0.028_014) < 1e-9, "liquid nitrogen's own data: " + nitrogen);
        helper.assertTrue(Double.isNaN(Thermal.of(SharedCatalogue.GASOLINE, false).heatOfBoiling()),
                "and a liquid that says nothing of boiling has no heat of boiling");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 80)
    public static void heat_flowsFromAHotBlockToItsColderNeighbour(final GameTestHelper helper) {
        final HeatBody hot = place(helper, new BlockPos(2, 2, 2), TestHeat.COPPER.get());
        final HeatBody cold = place(helper, new BlockPos(3, 2, 2), TestHeat.COPPER.get());
        helper.runAfterDelay(2, () -> {
            final double ambient = cold.temperature();
            hot.set(HOT);
            helper.runAfterDelay(40, () -> {
                helper.assertTrue(cold.temperature() > ambient + 20.0, "the colder one warmed: " + cold.temperature());
                helper.assertTrue(hot.temperature() < HOT - 20.0, "the hot one cooled: " + hot.temperature());
                helper.assertTrue(hot.temperature() >= cold.temperature(), "and none passed the other");
                helper.succeed();
            });
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 80)
    public static void heat_isLostToTheAirAndAFinnedRadiatorLosesItFaster(final GameTestHelper helper) {
        final HeatBody plain = place(helper, new BlockPos(1, 2, 2), TestHeat.COPPER.get());
        final HeatBody finned = place(helper, new BlockPos(5, 2, 2), TestHeat.RADIATOR.get());
        helper.runAfterDelay(2, () -> {
            plain.set(HOT);
            finned.set(HOT);
            helper.runAfterDelay(40, () -> {
                helper.assertTrue(plain.temperature() < HOT - 1.0, "a hot block cools in the air: "
                        + plain.temperature());
                helper.assertTrue(finned.temperature() < plain.temperature() - 10.0,
                        "and its fins make a radiator cool faster: " + finned.temperature());
                helper.succeed();
            });
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void heater_warmsWhatItTouches(final GameTestHelper helper) {
        place(helper, new BlockPos(2, 2, 2), TestHeat.HEATER.get());
        final HeatBody beside = place(helper, new BlockPos(3, 2, 2), TestHeat.COPPER.get());
        helper.runAfterDelay(2, () -> {
            final double ambient = beside.temperature();
            helper.runAfterDelay(60, () -> {
                helper.assertTrue(beside.temperature() > ambient + 2.0, "the copper beside the heater warmed: "
                        + beside.temperature());
                helper.succeed();
            });
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 80)
    public static void heatPipe_carriesHeatBetweenBlocksApart(final GameTestHelper helper) {
        final HeatBody hot = place(helper, new BlockPos(1, 2, 2), TestHeat.COPPER.get());
        final HeatBody far = place(helper, new BlockPos(5, 2, 2), TestHeat.COPPER.get());
        for (int x = 2; x <= 4; x++) {
            TestCables.lay(helper, new BlockPos(x, 2, 2), TestCableTypes.HEAT_PIPE);
        }
        helper.runAfterDelay(5, () -> {
            final double ambient = far.temperature();
            hot.set(HOT);
            helper.runAfterDelay(40, () -> {
                helper.assertTrue(far.temperature() > ambient + 10.0, "the far block warmed along the pipe: "
                        + far.temperature());
                helper.succeed();
            });
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 60)
    public static void balanced_aBlockAtThePlacesTemperatureSleeps(final GameTestHelper helper) {
        final HeatBody body = place(helper, new BlockPos(2, 2, 2), TestHeat.IRON.get());
        helper.runAfterDelay(20, () -> {
            final BlockPos at = helper.absolutePos(new BlockPos(2, 2, 2));
            helper.assertTrue(Math.abs(body.temperature() - HeatClimate.ambient(helper.getLevel(), at))
                    < LevelHeat.BALANCE, "it takes the place's temperature: " + body.temperature());
            helper.assertTrue(!LevelHeat.of(helper.getLevel()).awake(at), "and sleeps");
            body.set(body.temperature() + 50.0);
            helper.assertTrue(LevelHeat.of(helper.getLevel()).awake(at), "a change wakes it");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 60)
    public static void heater_stopsAtItsLimit(final GameTestHelper helper) {
        final HeatBody heater = place(helper, new BlockPos(2, 2, 2), TestHeat.HEATER.get());
        helper.runAfterDelay(2, () -> {
            heater.set(TestHeat.HEATER_LIMIT - 5.0);
            helper.runAfterDelay(40, () -> {
                helper.assertTrue(heater.temperature() <= TestHeat.HEATER_LIMIT + 1e-6,
                        "a heater never passes its limit: " + heater.temperature());
                helper.assertTrue(heater.temperature() > TestHeat.HEATER_LIMIT - 5.0,
                        "and holds near it against the air: " + heater.temperature());
                helper.succeed();
            });
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 80)
    public static void boiler_boilsWaterIntoSteamByTheRealEnthalpy(final GameTestHelper helper) {
        final TestHeat.VesselBlockEntity boiler = vessel(helper, new BlockPos(2, 2, 2), TestHeat.BOILER.get());
        helper.runAfterDelay(2, () -> {
            boiler.in().fill(new FluidStack(Fluids.WATER, WATER), IFluidHandler.FluidAction.EXECUTE);
            final double capacity = boiler.heatBody().capacity();
            boiler.heatBody().set(HOT_WALL);
            helper.runAfterDelay(40, () -> {
                final int boiled = WATER - boiler.in().getFluidAmount();
                helper.assertTrue(boiled > 10, "water boiled: " + boiled + " mB");
                final MatterStack steam = boiler.gas().held().orElse(null);
                helper.assertTrue(steam != null && steam.material() == SharedCatalogue.WATER,
                        "into steam, water's gas: " + steam);
                final double perLitre = (double) steam.amount() / boiled;
                helper.assertTrue(Math.abs(perLitre - STEAM_PER_LITRE) < 1.0,
                        "a litre of water makes about 1,240 of steam: " + perLitre);
                final double given = capacity * (HOT_WALL - boiler.heatBody().temperature());
                final double taken = boiled * WATER_BOILS + boiler.boiler().held();
                helper.assertTrue(Math.abs(given - taken) < given * 0.01, "each litre took its real enthalpy: the "
                        + "wall gave " + given + " J and the water took " + taken + " J");
                helper.succeed();
            });
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 80)
    public static void boiler_ofLowPressureBoilsWhereTheOpenAirsDoesNot(final GameTestHelper helper) {
        final TestHeat.VesselBlockEntity open = vessel(helper, new BlockPos(1, 2, 2), TestHeat.BOILER.get());
        final TestHeat.VesselBlockEntity low = vessel(helper, new BlockPos(4, 2, 2), TestHeat.LOW_BOILER.get());
        helper.runAfterDelay(2, () -> {
            for (final TestHeat.VesselBlockEntity boiler : new TestHeat.VesselBlockEntity[] {open, low}) {
                boiler.in().fill(new FluidStack(Fluids.WATER, WATER), IFluidHandler.FluidAction.EXECUTE);
                boiler.heatBody().set(WARM_WALL);
            }
            helper.runAfterDelay(40, () -> {
                helper.assertTrue(open.in().getFluidAmount() == WATER,
                        "below 100 °C nothing boils in the open air: " + open.in().getFluidAmount());
                helper.assertTrue(low.in().getFluidAmount() < WATER,
                        "but at 10 kPa water boils at about 44 °C: " + low.in().getFluidAmount());
                helper.succeed();
            });
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 80)
    public static void exchanger_coolsHotOilAndWarmsItsWall(final GameTestHelper helper) {
        final TestHeat.VesselBlockEntity exchanger = vessel(helper, new BlockPos(2, 2, 2),
                TestHeat.EXCHANGER.get());
        helper.runAfterDelay(2, () -> {
            final double ambient = exchanger.heatBody().temperature();
            exchanger.in().fill(TestFluids.HOT_OIL.stack(WATER), IFluidHandler.FluidAction.EXECUTE);
            helper.runAfterDelay(40, () -> {
                final FluidStack cooled = exchanger.out().getFluid();
                helper.assertTrue(cooled.is(TestFluids.COOL_OIL.get()) && cooled.getAmount() > 5,
                        "the hot oil came out cool: " + cooled.getAmount() + " mB of " + cooled.getFluid());
                final double wall = exchanger.heatBody().temperature();
                helper.assertTrue(wall > ambient + 1.0, "its heat went into the wall: " + wall);
                helper.assertTrue(wall < TestFluids.COOL_OIL_TEMPERATURE,
                        "which never passes the cool oil's temperature: " + wall);
                helper.succeed();
            });
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 80)
    public static void exchanger_heatsCoolOilOnAHotWall(final GameTestHelper helper) {
        final TestHeat.VesselBlockEntity exchanger = vessel(helper, new BlockPos(2, 2, 2),
                TestHeat.EXCHANGER.get());
        helper.runAfterDelay(2, () -> {
            exchanger.in().fill(TestFluids.COOL_OIL.stack(WATER), IFluidHandler.FluidAction.EXECUTE);
            exchanger.heatBody().set(HOT_WALL - 100.0);
            helper.runAfterDelay(40, () -> {
                final FluidStack heated = exchanger.out().getFluid();
                helper.assertTrue(heated.is(TestFluids.HOT_OIL.get()) && heated.getAmount() > 10,
                        "the cool oil came out hot: " + heated.getAmount() + " mB of " + heated.getFluid());
                helper.assertTrue(exchanger.heatBody().temperature() < HOT_WALL - 105.0,
                        "taking the wall's heat: " + exchanger.heatBody().temperature());
                helper.succeed();
            });
        });
    }

    private static HeatBody place(final GameTestHelper helper, final BlockPos pos, final Block block) {
        helper.setBlock(pos, block);
        final TestHeat.HeatBlockEntity entity = helper.getBlockEntity(pos);
        return entity.heatBody();
    }

    private static TestHeat.VesselBlockEntity vessel(final GameTestHelper helper, final BlockPos pos,
                                                     final Block block) {
        helper.setBlock(pos, block);
        return helper.getBlockEntity(pos);
    }
}
