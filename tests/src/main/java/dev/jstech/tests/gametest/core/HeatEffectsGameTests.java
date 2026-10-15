/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.connect.RelativeFace;
import dev.jstech.core.heat.HeatHazards;
import dev.jstech.core.heat.HeatPorts;
import dev.jstech.core.heat.HeatReadings;
import dev.jstech.core.inventory.FaceMode;
import dev.jstech.core.machine.MachineHealth;
import dev.jstech.core.machine.MachineHeat;
import dev.jstech.core.machine.MachineResource;
import dev.jstech.core.machine.MachineStatus;
import dev.jstech.core.signal.Reading;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestHeat;
import dev.jstech.tests.TestHotMachine;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * What heat does: a machine that makes heat stops to cool past its limit and loses health while it stays past it,
 * then starts again once cooler; its heat port opens and closes with its Sides tab; it reads its temperature; a
 * thermocouple reads the block under it; a block past 150 °C burns whoever stands on it; and one past 300 °C sets
 * alight what burns against it.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class HeatEffectsGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos AT = new BlockPos(2, 2, 2);

    private HeatEffectsGameTests() {
    }

    @GameTest(template = ARENA, timeoutTicks = 80)
    public static void machine_pastItsLimitStopsToCoolAndIsDamaged(final GameTestHelper helper) {
        final TestHotMachine.HotMachineBlockEntity machine = machine(helper);
        helper.runAfterDelay(2, () -> {
            machine.heatBody().set(TestHotMachine.LIMIT + 100.0);
            helper.runAfterDelay(20, () -> {
                helper.assertTrue(machine.status() == MachineStatus.OVERHEATED, "it stopped: " + machine.status());
                helper.assertTrue(machine.health().health() < MachineHealth.FULL,
                        "and lost health while past its limit: " + machine.health().health());
                helper.succeed();
            });
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void machine_startsAgainOnceCooler(final GameTestHelper helper) {
        final TestHotMachine.HotMachineBlockEntity machine = machine(helper);
        helper.runAfterDelay(2, () -> {
            machine.heatBody().set(TestHotMachine.LIMIT + 1.0);
            helper.runAfterDelay(5, () -> {
                helper.assertTrue(machine.status() == MachineStatus.OVERHEATED, "it stopped: " + machine.status());
                machine.heatBody().set(TestHotMachine.LIMIT - MachineHeat.STARTS_BELOW - 20.0);
                helper.succeedWhen(() -> helper.assertTrue(machine.status() == MachineStatus.WORKING,
                        "and works again once cooler: " + machine.status()));
            });
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 60)
    public static void machine_offersHeatOnlyWhereItsSidesTabOpensIt(final GameTestHelper helper) {
        final TestHotMachine.HotMachineBlockEntity machine = machine(helper);
        final BlockPos pos = helper.absolutePos(AT);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(HeatPorts.at(helper.getLevel(), pos, Direction.UP) != null,
                    "every face lets heat through until it is set");
            helper.assertTrue(machine.sides().resources().contains(MachineResource.HEAT),
                    "and the Sides tab shows heat");
            for (final RelativeFace face : RelativeFace.values()) {
                machine.sides().set(MachineResource.HEAT, face, FaceMode.NONE);
            }
            for (final Direction side : Direction.values()) {
                helper.assertTrue(HeatPorts.at(helper.getLevel(), pos, side) == null,
                        "a face closed to heat lets none through: " + side);
            }
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 60)
    public static void machine_readsItsTemperature(final GameTestHelper helper) {
        final TestHotMachine.HotMachineBlockEntity machine = machine(helper);
        helper.runAfterDelay(2, () -> {
            machine.heatBody().set(TestHotMachine.LIMIT - 50.0);
            final Reading reading = machine.readings().stream()
                    .filter(found -> found.name() == HeatReadings.TEMPERATURE).findFirst().orElse(null);
            helper.assertTrue(reading != null && Math.abs(reading.value() - (TestHotMachine.LIMIT - 50.0)) < 1.0,
                    "a sensor reads how hot it is: " + reading);
            helper.assertTrue(reading.most() == TestHotMachine.LIMIT, "over a span up to its limit");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 60)
    public static void thermocouple_readsTheBlockUnderIt(final GameTestHelper helper) {
        helper.setBlock(AT, TestHeat.COPPER.get());
        final TestHeat.HeatBlockEntity copper = helper.getBlockEntity(AT);
        helper.setBlock(AT.above(), TestHotMachine.THERMOCOUPLE.get());
        final TestHotMachine.ThermocoupleBlockEntity sensor = helper.getBlockEntity(AT.above());
        helper.runAfterDelay(2, () -> {
            copper.heatBody().set(1_000.0);
            final List<Reading> readings = sensor.readings();
            helper.assertTrue(readings.size() == 1 && Math.abs(readings.getFirst().value() - 1_000.0) < 1.0,
                    "it reads the copper's heat: " + readings);
            helper.assertTrue(readings.getFirst().strength() == Reading.strength(1_000.0,
                    HeatReadings.THERMOCOUPLE_LEAST, HeatReadings.THERMOCOUPLE_MOST),
                    "and speaks it over a thermocouple's span");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 80)
    public static void hotSurface_burnsWhoeverStandsOnIt(final GameTestHelper helper) {
        helper.setBlock(AT, TestHeat.COPPER.get());
        final TestHeat.HeatBlockEntity copper = helper.getBlockEntity(AT);
        final Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, AT.above());
        helper.runAfterDelay(2, () -> {
            copper.heatBody().set(800.0);
            helper.runAfterDelay(20, () -> {
                helper.assertTrue(pig.getHealth() < pig.getMaxHealth(), "the pig standing on it is burned: "
                        + pig.getHealth());
                helper.succeed();
            });
        });
    }

    @GameTest(template = ARENA)
    public static void hotBlock_setsAlightWhatBurnsAgainstIt(final GameTestHelper helper) {
        final BlockPos planks = AT.east();
        helper.setBlock(planks, Blocks.OAK_PLANKS);
        final GameRules.BooleanValue fire = helper.getLevel().getGameRules().getRule(GameRules.RULE_DOFIRETICK);
        final boolean before = fire.get();
        // The test server keeps fire still; it spreads for this one call only, within the tick.
        fire.set(true, null);
        HeatHazards.ignite(helper.getLevel(), helper.absolutePos(planks), Blocks.OAK_PLANKS.defaultBlockState(),
                Direction.WEST, 10.0);
        fire.set(before, null);
        helper.assertTrue(helper.getBlockState(planks.above()).getBlock() instanceof BaseFireBlock,
                "the planks caught fire on top");
        helper.setBlock(planks.above(), Blocks.AIR);
        helper.succeed();
    }

    private static TestHotMachine.HotMachineBlockEntity machine(final GameTestHelper helper) {
        helper.setBlock(AT, TestHotMachine.BLOCK.get());
        return helper.getBlockEntity(AT);
    }
}
