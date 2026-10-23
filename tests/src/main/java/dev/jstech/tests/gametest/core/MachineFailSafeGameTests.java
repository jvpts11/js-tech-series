/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.energy.WrongClass;
import dev.jstech.core.machine.MachineHealth;
import dev.jstech.core.machine.MachineStatus;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestEnergies;
import dev.jstech.tests.TestHotMachine;
import dev.jstech.tests.TestHotMachine.SafeMachineBlockEntity;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A machine that fails safe, as the sources a player builds to forget do: a line however high only keeps it from
 * starting, and it starts again once the line is gone; an arc flash neither burns it nor sets fire around it; heat past
 * its limit stops it to cool without taking its health; and an explosion from outside still damages it.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class MachineFailSafeGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos AT = new BlockPos(2, 2, 2);

    private MachineFailSafeGameTests() {
    }

    @GameTest(template = ARENA, timeoutTicks = 80)
    public static void failSafe_aLineTooHighOnlyKeepsItFromStarting(final GameTestHelper helper) {
        final SafeMachineBlockEntity machine = place(helper);
        final AtomicBoolean live = new AtomicBoolean(true);
        helper.onEachTick(() -> {
            if (live.get()) {
                machine.power().portStore().meetWrongClass(WrongClass.BURNS, TestEnergies.HIGH,
                        helper.getLevel().getGameTime());
            }
        });
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(machine.status() == MachineStatus.OVERVOLTAGE, "a line that would burn another machine"
                    + " keeps it from starting: " + machine.status());
            helper.assertTrue(!machine.power().burnt() && machine.health().health() == MachineHealth.FULL,
                    "and harms nothing");
            live.set(false);
            helper.succeedWhen(() -> helper.assertTrue(machine.status() == MachineStatus.WORKING,
                    "once the line is gone it works again: " + machine.status()));
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void failSafe_anArcFlashNeitherBurnsItNorSetsFireAround(final GameTestHelper helper) {
        final SafeMachineBlockEntity machine = place(helper);
        machine.power().portStore().meetWrongClass(WrongClass.ARC_FLASH, TestEnergies.HIGH,
                helper.getLevel().getGameTime());
        helper.runAfterDelay(3, () -> {
            helper.assertBlockPresent(TestHotMachine.SAFE_BLOCK.get(), AT);
            helper.assertTrue(!machine.power().burnt() && machine.health().health() == MachineHealth.FULL,
                    "it is unharmed");
            for (final BlockPos near : BlockPos.betweenClosed(AT.offset(-1, 0, -1), AT.offset(1, 1, 1))) {
                helper.assertBlockNotPresent(Blocks.FIRE, near);
            }
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 80)
    public static void failSafe_heatPastItsLimitStopsItUnharmed(final GameTestHelper helper) {
        final SafeMachineBlockEntity machine = place(helper);
        helper.runAfterDelay(2, () -> {
            machine.heatBody().set(TestHotMachine.LIMIT + 100.0);
            helper.runAfterDelay(20, () -> {
                helper.assertTrue(machine.status() == MachineStatus.OVERHEATED, "it stopped: " + machine.status());
                helper.assertTrue(machine.health().health() == MachineHealth.FULL,
                        "and kept its health while past its limit: " + machine.health().health());
                helper.succeed();
            });
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void failSafe_anExplosionFromOutsideStillDamagesIt(final GameTestHelper helper) {
        final SafeMachineBlockEntity machine = place(helper);
        final Vec3 at = Vec3.atCenterOf(helper.absolutePos(AT.east()));
        helper.getLevel().explode(null, at.x, at.y, at.z, 2.0F, Level.ExplosionInteraction.BLOCK);
        helper.runAfterDelay(3, () -> {
            helper.assertBlockPresent(TestHotMachine.SAFE_BLOCK.get(), AT);
            helper.assertTrue(machine.health().health() < MachineHealth.FULL,
                    "what strikes it from outside is not a fault of its own: " + machine.health().health());
            helper.succeed();
        });
    }

    private static SafeMachineBlockEntity place(final GameTestHelper helper) {
        helper.setBlock(AT, TestHotMachine.SAFE_BLOCK.get());
        return helper.getBlockEntity(AT);
    }
}
