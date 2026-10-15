/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.mechanical.LevelMechanics;
import dev.jstech.core.mechanical.MechanicalPartBlockEntity;
import dev.jstech.core.mechanical.RotationMath;
import dev.jstech.core.mechanical.ShaftNetwork;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestMechanical;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Core's mechanical power in a world: a crank turning a line of shafts nearly at its free speed, a load slowing it
 * where the torque meets, gears trading speed for torque and turning the other way, gears whose ratios disagree
 * jamming, a broken shaft splitting the line, a motor spending energy to turn its shaft, and a dynamo making energy of
 * a turn.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class MechanicalGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 40;

    private MechanicalGameTests() {
    }

    @GameTest(template = ARENA, timeoutTicks = 80)
    public static void crank_turnsALineOfShaftsNearlyAtItsFreeSpeed(final GameTestHelper helper) {
        place(helper, new BlockPos(1, 2, 2), TestMechanical.CRANK.get(), Direction.Axis.X);
        for (int x = 2; x <= 4; x++) {
            place(helper, new BlockPos(x, 2, 2), TestMechanical.SHAFT.get(), Direction.Axis.X);
        }
        helper.runAfterDelay(SETTLE, () -> {
            // Only the shafts' friction holds it back: 0.3 N·m of the crank's 20, a little below its free speed.
            final double expected = TestMechanical.CRANK_CURVE.freeRpm()
                    * (1.0 - 0.3 / TestMechanical.CRANK_CURVE.stall());
            for (int x = 2; x <= 4; x++) {
                final double rpm = part(helper, new BlockPos(x, 2, 2)).rpm();
                helper.assertTrue(Math.abs(rpm - expected) < 0.5, "every shaft turns as the crank does, nearly free: "
                        + rpm + " rpm, expected " + expected);
            }
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 80)
    public static void load_slowsTheNetworkWhereTheTorqueMeets(final GameTestHelper helper) {
        place(helper, new BlockPos(1, 2, 2), TestMechanical.CRANK.get(), Direction.Axis.X);
        place(helper, new BlockPos(2, 2, 2), TestMechanical.SHAFT.get(), Direction.Axis.X);
        place(helper, new BlockPos(3, 2, 2), TestMechanical.LOAD_BLOCK.get(), Direction.Axis.X);
        helper.runAfterDelay(SETTLE, () -> {
            final double rpm = part(helper, new BlockPos(3, 2, 2)).rpm();
            // The crank's torque falls from 20 N·m as it turns; it meets the load's 15 and the shaft's 0.1 near 15 rpm.
            final double expected = TestMechanical.CRANK_CURVE.freeRpm()
                    * (1.0 - (TestMechanical.LOAD + 0.1) / TestMechanical.CRANK_CURVE.stall());
            helper.assertTrue(Math.abs(rpm - expected) < 0.5, "it turns where the crank's torque meets the load's: "
                    + rpm + " rpm, expected " + expected);
            helper.assertTrue(TestMechanical.CRANK_CURVE.torque(rpm)
                    < RotationMath.torque(TestMechanical.CRANK_CURVE.power(), rpm), "within the crank's power");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 80)
    public static void gears_tradeSpeedForTorqueAndTurnTheOtherWay(final GameTestHelper helper) {
        place(helper, new BlockPos(1, 2, 2), TestMechanical.CRANK.get(), Direction.Axis.X);
        place(helper, new BlockPos(2, 2, 2), TestMechanical.SMALL_GEAR.get(), Direction.Axis.X);
        place(helper, new BlockPos(2, 2, 3), TestMechanical.LARGE_GEAR.get(), Direction.Axis.X);
        helper.runAfterDelay(SETTLE, () -> {
            final double small = part(helper, new BlockPos(2, 2, 2)).rpm();
            final double large = part(helper, new BlockPos(2, 2, 3)).rpm();
            helper.assertTrue(small > 1.0, "the small gear turns: " + small);
            helper.assertTrue(Math.abs(large / small + 0.5) < 0.01,
                    "the large gear turns half as fast, the other way: " + large + " against " + small);
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 80)
    public static void gearbox_turnsTheFacesAroundItOppositeFacesOppositeWays(final GameTestHelper helper) {
        place(helper, new BlockPos(1, 2, 2), TestMechanical.CRANK.get(), Direction.Axis.X);
        place(helper, new BlockPos(2, 2, 2), TestMechanical.GEARBOX.get(), Direction.Axis.Y);
        place(helper, new BlockPos(2, 2, 1), TestMechanical.SHAFT.get(), Direction.Axis.Z);
        place(helper, new BlockPos(2, 2, 3), TestMechanical.SHAFT.get(), Direction.Axis.Z);
        helper.runAfterDelay(SETTLE, () -> {
            final double crank = part(helper, new BlockPos(1, 2, 2)).rpm();
            final double north = part(helper, new BlockPos(2, 2, 1)).rpm();
            final double south = part(helper, new BlockPos(2, 2, 3)).rpm();
            helper.assertTrue(crank > 1.0, "the crank turns: " + crank);
            helper.assertTrue(Math.abs(Math.abs(north) - crank) < 0.01 && Math.abs(north + south) < 0.01,
                    "the box turns both faces as fast as the crank, opposite ways: " + north + ", " + south);
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 80)
    public static void gears_whoseRatiosDisagreeJam(final GameTestHelper helper) {
        place(helper, new BlockPos(1, 2, 2), TestMechanical.CRANK.get(), Direction.Axis.X);
        // Two shafts of gears side by side: one pair meshes 16 to 32, the other 16 to 16.
        place(helper, new BlockPos(2, 2, 2), TestMechanical.SMALL_GEAR.get(), Direction.Axis.X);
        place(helper, new BlockPos(3, 2, 2), TestMechanical.SMALL_GEAR.get(), Direction.Axis.X);
        place(helper, new BlockPos(2, 2, 3), TestMechanical.LARGE_GEAR.get(), Direction.Axis.X);
        place(helper, new BlockPos(3, 2, 3), TestMechanical.SMALL_GEAR.get(), Direction.Axis.X);
        helper.runAfterDelay(SETTLE, () -> {
            final ShaftNetwork network = LevelMechanics.of(helper.getLevel())
                    .networkAt(helper.absolutePos(new BlockPos(2, 2, 2)));
            helper.assertTrue(network != null && network.jammed(), "the network jams");
            helper.assertTrue(part(helper, new BlockPos(3, 2, 3)).rpm() == 0.0, "and nothing in it turns");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void brokenShaft_splitsTheLineAndTheFarEndStops(final GameTestHelper helper) {
        place(helper, new BlockPos(1, 2, 2), TestMechanical.CRANK.get(), Direction.Axis.X);
        for (int x = 2; x <= 4; x++) {
            place(helper, new BlockPos(x, 2, 2), TestMechanical.SHAFT.get(), Direction.Axis.X);
        }
        helper.runAfterDelay(SETTLE, () -> {
            helper.setBlock(new BlockPos(3, 2, 2), Blocks.AIR);
            helper.succeedWhen(() -> {
                helper.assertTrue(part(helper, new BlockPos(4, 2, 2)).rpm() == 0.0,
                        "the far shaft stops: " + part(helper, new BlockPos(4, 2, 2)).rpm());
                helper.assertTrue(part(helper, new BlockPos(2, 2, 2)).rpm() > 50.0, "while the near one turns on");
            });
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void motor_spendsEnergyToTurnItsShaft(final GameTestHelper helper) {
        // A motor facing north has its back to the south; its shaft and the load run on south of it.
        helper.setBlock(new BlockPos(2, 2, 1), TestMechanical.MOTOR.get());
        final TestMechanical.ShaftMachineBlockEntity motor = helper.getBlockEntity(new BlockPos(2, 2, 1));
        Objects.requireNonNull(motor.getEnergy()).setEnergyStored(TestMechanical.ENERGY);
        place(helper, new BlockPos(2, 2, 2), TestMechanical.SHAFT.get(), Direction.Axis.Z);
        place(helper, new BlockPos(2, 2, 3), TestMechanical.LOAD_BLOCK.get(), Direction.Axis.Z);
        helper.runAfterDelay(SETTLE, () -> {
            final double rpm = Math.abs(part(helper, new BlockPos(2, 2, 3)).rpm());
            // The motor's 50 N·m fall as it turns; they meet the load's 15 and 0.3 of friction near 1,040 rpm.
            final double expected = TestMechanical.MOTOR_CURVE.freeRpm()
                    * (1.0 - (TestMechanical.LOAD + 0.3) / TestMechanical.MOTOR_CURVE.stall());
            helper.assertTrue(Math.abs(rpm - expected) < 5.0, "the motor turns the load: " + rpm + " rpm, expected "
                    + expected);
            helper.assertTrue(motor.power().stored() < TestMechanical.ENERGY,
                    "and the motor spent energy to turn it: " + motor.power().stored());
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void dynamo_makesEnergyOfATurn(final GameTestHelper helper) {
        helper.setBlock(new BlockPos(2, 2, 1), TestMechanical.DYNAMO.get());
        final TestMechanical.ShaftMachineBlockEntity dynamo = helper.getBlockEntity(new BlockPos(2, 2, 1));
        place(helper, new BlockPos(2, 2, 2), TestMechanical.SHAFT.get(), Direction.Axis.Z);
        place(helper, new BlockPos(2, 2, 3), TestMechanical.CRANK.get(), Direction.Axis.Z);
        helper.runAfterDelay(SETTLE, () -> {
            helper.assertTrue(Math.abs(part(helper, new BlockPos(2, 2, 2)).rpm()) > 1.0,
                    "the crank turns the shaft");
            helper.assertTrue(dynamo.power().stored() > 0L, "and the dynamo makes energy of it: "
                    + dynamo.power().stored());
            helper.succeed();
        });
    }

    private static void place(final GameTestHelper helper, final BlockPos pos, final Block block,
                              final Direction.Axis axis) {
        helper.setBlock(pos, block.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis));
    }

    private static MechanicalPartBlockEntity part(final GameTestHelper helper, final BlockPos pos) {
        return helper.getBlockEntity(pos);
    }
}
