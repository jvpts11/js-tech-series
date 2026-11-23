/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.industrial;

import dev.jstech.core.jstechseries.upgrade.SharedUpgrades;
import dev.jstech.core.machine.UpgradeEffect;
import dev.jstech.industrial.IndustrialModule;
import dev.jstech.industrial.blockentity.CompressorBlockEntity;
import dev.jstech.tests.JsTests;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The series' shared upgrades in a machine that takes them, J's Industrial's Compressor, put into its upgrade slots as
 * a hopper or a player puts them: what they gain adds up and what they save multiplies, and no more of each than J's
 * Industrial's table lets into one machine counts or goes in, the three kinds of motor four in all.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class SharedUpgradesGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos AT = new BlockPos(1, 2, 1);
    private static final double CLOSE = 1e-9;

    private SharedUpgradesGameTests() {
    }

    @GameTest(template = ARENA)
    public static void upgrades_addTheirGainsAndMultiplyTheirSavings(final GameTestHelper helper) {
        final CompressorBlockEntity machine = compressor(helper);
        final int first = machine.layout().firstUpgrade();
        put(helper, machine, first, new ItemStack(SharedUpgrades.BRUSHED_MOTOR.get(), 4));
        put(helper, machine, first + 1, new ItemStack(SharedUpgrades.VARIABLE_FREQUENCY_DRIVE.get(), 2));
        final UpgradeEffect effect = machine.upgradeEffect();
        helper.assertTrue(Math.abs(effect.speed() - 3.0) < CLOSE, "four motors make it three times as fast: "
                + effect.speed());
        helper.assertTrue(Math.abs(effect.energy() - 4.0 * 0.8 * 0.8) < CLOSE, "four times the energy, of which two "
                + "drives save a fifth and a fifth again: " + effect.energy());
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void motors_ofAnyKindsFitFourInAll(final GameTestHelper helper) {
        final CompressorBlockEntity machine = compressor(helper);
        final int first = machine.layout().firstUpgrade();
        put(helper, machine, first, new ItemStack(SharedUpgrades.BRUSHED_MOTOR.get(), 3));
        final ItemStack left = machine.getInventory().insertItem(first + 1,
                new ItemStack(SharedUpgrades.INDUCTION_MOTOR.get(), 3), false);
        helper.assertTrue(left.getCount() == 2, "three brushed motors leave room for one induction motor: " + left);
        helper.assertTrue(Math.abs(machine.upgradeEffect().speed() - 3.0) < CLOSE, "and the four count as motors: "
                + machine.upgradeEffect().speed());
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void upgrades_letNoMoreIntoAMachineThanTheTableSays(final GameTestHelper helper) {
        final CompressorBlockEntity machine = compressor(helper);
        final int first = machine.layout().firstUpgrade();
        final ItemStack tooling = machine.getInventory().insertItem(first,
                new ItemStack(SharedUpgrades.PRECISION_TOOLING.get(), 3), false);
        helper.assertTrue(tooling.getCount() == 1, "two Precision Tooling fit, the third is left: " + tooling);
        put(helper, machine, first + 1, new ItemStack(SharedUpgrades.BYPRODUCT_SEPARATOR.get(), 16));
        final UpgradeEffect effect = machine.upgradeEffect();
        helper.assertTrue(Math.abs(effect.yield() - 1.2) < CLOSE, "two tenths more of what it makes: "
                + effect.yield());
        helper.assertTrue(Math.abs(effect.byproducts() - 9.0) < CLOSE, "and the separators, of no limit but the "
                + "slots, add half again each: " + effect.byproducts());
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void upgrades_saveGasesAndLiquidsEachTheirOwn(final GameTestHelper helper) {
        final CompressorBlockEntity machine = compressor(helper);
        final int first = machine.layout().firstUpgrade();
        put(helper, machine, first, new ItemStack(SharedUpgrades.GAS_RECOVERY_UNIT.get(), 2));
        put(helper, machine, first + 1, new ItemStack(SharedUpgrades.MUFFLER.get()));
        final UpgradeEffect effect = machine.upgradeEffect();
        helper.assertTrue(Math.abs(effect.gases() - 0.85 * 0.85) < CLOSE && effect.liquids() == 1.0,
                "the gas recovery saves gases alone: " + effect);
        helper.assertTrue(effect.sound() == 0.0, "and the muffler silences it");
        helper.succeed();
    }

    private static CompressorBlockEntity compressor(final GameTestHelper helper) {
        helper.setBlock(AT, IndustrialModule.COMPRESSOR.get());
        return helper.getBlockEntity(AT);
    }

    /* Puts {@code stack} into {@code slot} as a hopper would, all of it. */
    private static void put(final GameTestHelper helper, final CompressorBlockEntity machine, final int slot,
                            final ItemStack stack) {
        final ItemStack left = machine.getInventory().insertItem(slot, stack, false);
        helper.assertTrue(left.isEmpty(), "the machine takes " + stack + ", " + left + " left");
    }
}
