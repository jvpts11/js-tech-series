/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.jstechseries.Element;
import dev.jstech.core.jstechseries.Elements;
import dev.jstech.core.material.MatterState;
import dev.jstech.core.matter.MatterStack;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestKitMachine;
import dev.jstech.tests.TestKitMachine.KitMachineBlockEntity;
import dev.jstech.tests.TestUpgrades;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The effects of upgrades on a machine at work: two recipes at once, twice the items, half the matter a recipe
 * spends, no byproducts, a buffer twice as large and back, and no more of an upgrade in its bays than fit in one
 * machine.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class MachineUpgradeGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos MACHINE = new BlockPos(4, 2, 4);
    private static final long HYDROGEN = 500L;
    /* Past one piece of work of twenty ticks, short of a second. */
    private static final int ONE_WORK = 30;

    private MachineUpgradeGameTests() {
    }

    @GameTest(template = ARENA, timeoutTicks = 60)
    public static void parallel_worksTwoRecipesAtOnce(final GameTestHelper helper) {
        final KitMachineBlockEntity machine = machine(helper);
        machine.getInventory().setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 4));
        upgrade(machine, 0, new ItemStack(TestUpgrades.PARALLEL.get()));
        helper.runAfterDelay(ONE_WORK, () -> {
            helper.assertTrue(made(machine, Items.STONE) == 2, "one piece of work made two stone: "
                    + made(machine, Items.STONE));
            helper.assertTrue(machine.getInventory().getStackInSlot(0).getCount() == 2, "from two cobblestone");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 60)
    public static void yield_makesTwiceTheItems(final GameTestHelper helper) {
        final KitMachineBlockEntity machine = machine(helper);
        machine.getInventory().setStackInSlot(0, new ItemStack(Items.COBBLESTONE));
        upgrade(machine, 0, new ItemStack(TestUpgrades.YIELD.get()));
        helper.runAfterDelay(ONE_WORK, () -> {
            helper.assertTrue(made(machine, Items.STONE) == 2, "one cobblestone made two stone: "
                    + made(machine, Items.STONE));
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 80)
    public static void use_halvesTheMatterARecipeSpends(final GameTestHelper helper) {
        final KitMachineBlockEntity machine = refining(helper);
        upgrade(machine, 0, new ItemStack(TestUpgrades.THRIFT.get()));
        helper.succeedWhen(() -> {
            helper.assertTrue(made(machine, Items.IRON_NUGGET) == 9, "the nuggets are made");
            helper.assertTrue(gas(machine) == HYDROGEN - 50L, "with half the hydrogen: " + gas(machine));
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 80)
    public static void byproducts_noneComeWithAnUpgradeThatTakesThemAway(final GameTestHelper helper) {
        final KitMachineBlockEntity machine = refining(helper);
        upgrade(machine, 0, new ItemStack(TestUpgrades.NO_BYPRODUCTS.get()));
        helper.succeedWhen(() -> {
            helper.assertTrue(made(machine, Items.IRON_NUGGET) == 9, "the nuggets are made");
            helper.assertTrue(made(machine, Items.GOLD_NUGGET) == 0, "and the byproduct that always came does not");
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void buffer_doublesTheStoreAndGivesItBack(final GameTestHelper helper) {
        final KitMachineBlockEntity machine = machine(helper);
        upgrade(machine, 0, new ItemStack(TestUpgrades.BUFFER.get()));
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(machine.power().capacity() == 2 * TestKitMachine.CAPACITY,
                    "the buffer holds twice: " + machine.power().capacity());
            machine.power().portStore().setStored(2 * TestKitMachine.CAPACITY);
            machine.getInventory().setStackInSlot(TestKitMachine.LAYOUT.firstUpgrade(), ItemStack.EMPTY);
            helper.runAfterDelay(3, () -> {
                helper.assertTrue(machine.power().capacity() == TestKitMachine.CAPACITY,
                        "taken out, it holds what it was built to");
                helper.assertTrue(machine.power().stored() <= TestKitMachine.CAPACITY, "and no more than that");
                helper.succeed();
            });
        });
    }

    @GameTest(template = ARENA)
    public static void most_letsNoMoreOfAnUpgradeIntoTheBaysThanFit(final GameTestHelper helper) {
        final KitMachineBlockEntity machine = machine(helper);
        final int first = TestKitMachine.LAYOUT.firstUpgrade();
        final ItemStack left = machine.getInventory().insertItem(first, new ItemStack(TestUpgrades.PARALLEL.get(), 3),
                false);
        helper.assertTrue(left.getCount() == 2, "one parallel upgrade fits in a machine, two are left: " + left);
        final ItemStack again = machine.getInventory().insertItem(first + 1,
                new ItemStack(TestUpgrades.PARALLEL.get()), false);
        helper.assertTrue(again.getCount() == 1, "and another bay takes no second one");
        final ItemStack many = machine.getInventory().insertItem(first + 1,
                new ItemStack(TestUpgrades.THRIFT.get(), 8), false);
        helper.assertTrue(many.isEmpty(), "an upgrade of no limit fills its bay");
        helper.assertTrue(machine.upgrades().parallel() == 1 && machine.upgrades().use() < 0.01,
                "and both count: " + machine.upgrades());
        helper.succeed();
    }

    private static KitMachineBlockEntity machine(final GameTestHelper helper) {
        helper.setBlock(MACHINE, TestKitMachine.BLOCK.get());
        final KitMachineBlockEntity machine = helper.getBlockEntity(MACHINE);
        machine.power().portStore().setStored(TestKitMachine.CAPACITY);
        return machine;
    }

    /* The machine ready to refine an iron ingot with its catalyst and hydrogen. */
    private static KitMachineBlockEntity refining(final GameTestHelper helper) {
        final KitMachineBlockEntity machine = machine(helper);
        machine.getInventory().setStackInSlot(0, new ItemStack(Items.IRON_INGOT));
        machine.getInventory().setStackInSlot(1, new ItemStack(Items.BLAZE_ROD));
        machine.matter().inputs(MatterState.GAS).getFirst().insert(new MatterStack(Elements.of(Element.HYDROGEN),
                HYDROGEN), false);
        return machine;
    }

    private static void upgrade(final KitMachineBlockEntity machine, final int bay, final ItemStack upgrade) {
        machine.getInventory().setStackInSlot(TestKitMachine.LAYOUT.firstUpgrade() + bay, upgrade);
    }

    /* How many of {@code item} its outputs hold. */
    private static int made(final KitMachineBlockEntity machine, final Item item) {
        int count = 0;
        for (int slot = TestKitMachine.OUT; slot < TestKitMachine.OUT + 2; slot++) {
            final ItemStack held = machine.getInventory().getStackInSlot(slot);
            if (held.is(item)) {
                count += held.getCount();
            }
        }
        return count;
    }

    private static long gas(final KitMachineBlockEntity machine) {
        return machine.matter().inputs(MatterState.GAS).getFirst().held().map(MatterStack::amount).orElse(0L);
    }
}
