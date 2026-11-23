/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import static dev.jstech.tests.testkit.computers.ChainMachineSite.formed;
import static dev.jstech.tests.testkit.computers.ChainMachineSite.machine;
import static dev.jstech.tests.testkit.computers.ChainMachineSite.machineAt;

import dev.jstech.computers.chain.machine.ChainMachine;
import dev.jstech.computers.chain.machine.ChainMachineBlockEntity;
import dev.jstech.computers.chain.machine.ChainMachines;
import dev.jstech.core.jstechseries.upgrade.SharedUpgrades;
import dev.jstech.tests.JsTests;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The chain's machines of recipes take the series' shared upgrades in slots of their era: 2 of the Vintage, which a
 * machine that serves every era is built of, 3 of the Legacy and the Transition, 4 from the Standard; a machine of a
 * work of its own takes none. A formed machine lets no more of an upgrade in than fit, and adds what they gain.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ChainUpgradeSlotsGameTests {

    private static final String ARENA = "empty";
    private static final double CLOSE = 1e-9;

    private ChainUpgradeSlotsGameTests() {
    }

    @GameTest(template = ARENA)
    public static void machines_haveTheUpgradeSlotsOfTheirEra(final GameTestHelper helper) {
        final List<String> wrong = new ArrayList<>();
        for (final ChainMachine machine : ChainMachines.all()) {
            final int expected = ChainMachines.workedBy(machine).isEmpty() ? 0
                    : ChainMachines.upgradeSlots(machine.era());
            if (machine.layout().upgrades() != expected) {
                wrong.add(machine.id() + " has " + machine.layout().upgrades() + ", not " + expected);
            }
        }
        helper.assertTrue(wrong.isEmpty(), "each machine has its era's upgrade slots: " + wrong);
        helper.assertTrue(machine("crusher").layout().upgrades() == 2, "the Crusher, of every era, has the Vintage's 2");
        helper.assertTrue(machine("lithography_legacy").layout().upgrades() == 3, "the Legacy station has 3");
        helper.assertTrue(machine("lithography_advanced").layout().upgrades() == 4, "the Advanced station has 4");
        helper.assertTrue(machine("chiller").layout().upgrades() == 0, "the Chiller, of a work of its own, has none");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void crusher_takesFourMotorsAndAddsWhatTheyGain(final GameTestHelper helper) {
        final BlockPos terminal = formed(helper, machine("crusher"));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(machineAt(helper, terminal).formed(), "the crusher forms"))
                .thenExecute(() -> {
                    final ChainMachineBlockEntity crusher = machineAt(helper, terminal);
                    final int first = crusher.layout().firstUpgrade();
                    final ItemStack left = crusher.getInventory().insertItem(first,
                            new ItemStack(SharedUpgrades.BRUSHED_MOTOR.get(), 5), false);
                    helper.assertTrue(left.getCount() == 1, "four motors go in, the fifth is left: " + left);
                    final ItemStack drives = crusher.getInventory().insertItem(first + 1,
                            new ItemStack(SharedUpgrades.VARIABLE_FREQUENCY_DRIVE.get()), false);
                    helper.assertTrue(drives.isEmpty(), "and a drive into its second slot");
                    helper.assertTrue(Math.abs(crusher.upgradeEffect().speed() - 3.0) < CLOSE,
                            "four motors add up to three times as fast: " + crusher.upgradeEffect().speed());
                })
                .thenSucceed();
    }
}
