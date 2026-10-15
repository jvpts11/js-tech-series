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
import dev.jstech.core.material.MaterialKind;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestCableTypes;
import dev.jstech.tests.TestHeat;
import dev.jstech.tests.testkit.TestCables;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Core's heat in a world: the materials' real data, heat flowing from a hot block to its colder neighbour, a hot
 * block cooling in the air, a radiator's fins cooling it faster, a heater warming what it touches, a heat pipe
 * carrying heat between two blocks apart, and a block in balance going to sleep.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class HeatGameTests {

    private static final String ARENA = "empty";
    private static final double HOT = 600.0;

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

    private static HeatBody place(final GameTestHelper helper, final BlockPos pos, final Block block) {
        helper.setBlock(pos, block);
        final TestHeat.HeatBlockEntity entity = helper.getBlockEntity(pos);
        return entity.heatBody();
    }
}
