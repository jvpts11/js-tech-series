/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.machine.CatalystWear;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestKitMachine;
import dev.jstech.tests.TestKitMachine.KitMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * What a machine counts and what wears in it as it works: a byproduct of a share comes exactly its share, a quarter of
 * one every fourth recipe, never by chance, and the count is kept with the machine; a catalyst that wears loses a use
 * each recipe and, worn out, becomes its spent form, or is gone when it has none, and the recipe waits for another.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class RecipeWearGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos MACHINE = new BlockPos(2, 2, 2);
    private static final int CATALYST_SLOT = 1;

    private RecipeWearGameTests() {
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void countedByproduct_comesEveryFourthRecipeExactly(final GameTestHelper helper) {
        final KitMachineBlockEntity machine = machine(helper);
        machine.getInventory().setStackInSlot(0, new ItemStack(Items.RAW_COPPER, 7));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(made(machine, Items.COPPER_INGOT) == 7,
                        "seven ingots made: " + made(machine, Items.COPPER_INGOT)))
                .thenExecute(() -> helper.assertTrue(made(machine, Items.GOLD_NUGGET) == 1,
                        "a quarter a recipe is one in seven recipes, at the fourth: "
                                + made(machine, Items.GOLD_NUGGET)))
                .thenExecute(() -> machine.getInventory().setStackInSlot(0, new ItemStack(Items.RAW_COPPER, 1)))
                .thenWaitUntil(() -> helper.assertTrue(made(machine, Items.COPPER_INGOT) == 8,
                        "the eighth ingot made"))
                .thenExecute(() -> helper.assertTrue(made(machine, Items.GOLD_NUGGET) == 2,
                        "and the second nugget with it, the count kept between: " + made(machine, Items.GOLD_NUGGET)))
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void catalyst_wearsAUseARecipeIntoItsSpentForm(final GameTestHelper helper) {
        final KitMachineBlockEntity machine = machine(helper);
        machine.getInventory().setStackInSlot(0, new ItemStack(Items.RAW_IRON, 5));
        machine.getInventory().setStackInSlot(CATALYST_SLOT, new ItemStack(TestKitMachine.CATALYST.get()));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(made(machine, Items.IRON_INGOT) == TestKitMachine.CATALYST_USES,
                        "a recipe for each of the catalyst's uses"))
                .thenExecuteAfter(10, () -> {
                    helper.assertTrue(machine.getInventory().getStackInSlot(CATALYST_SLOT)
                            .is(TestKitMachine.SPENT_CATALYST.get()), "worn out, it is its spent form: "
                            + machine.getInventory().getStackInSlot(CATALYST_SLOT));
                    helper.assertTrue(made(machine, Items.IRON_INGOT) == TestKitMachine.CATALYST_USES
                            && machine.getInventory().getStackInSlot(0).getCount() == 2,
                            "and the recipe waits for another catalyst");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void catalyst_withNoSpentFormIsGoneWornOut(final GameTestHelper helper) {
        final KitMachineBlockEntity machine = machine(helper);
        final ItemStack shears = new ItemStack(Items.SHEARS);
        shears.setDamageValue(shears.getMaxDamage() - 2);
        machine.getInventory().setStackInSlot(0, new ItemStack(Items.RAW_GOLD, 4));
        machine.getInventory().setStackInSlot(CATALYST_SLOT, shears);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(made(machine, Items.GOLD_INGOT) == 2, "two uses left, two"))
                .thenExecuteAfter(10, () -> {
                    helper.assertTrue(machine.getInventory().getStackInSlot(CATALYST_SLOT).isEmpty(),
                            "worn out with no spent form, it is gone");
                    helper.assertTrue(made(machine, Items.GOLD_INGOT) == 2, "and nothing more is made");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void catalyst_thatCannotBeDamagedNeverWears(final GameTestHelper helper) {
        final ItemStack rod = new ItemStack(Items.BLAZE_ROD);
        helper.assertTrue(!CatalystWear.wears(rod) && CatalystWear.worn(rod, 100) == rod,
                "a die, or a blaze rod, is not used up and never wears");
        final ItemStack catalyst = new ItemStack(TestKitMachine.CATALYST.get());
        helper.assertTrue(CatalystWear.worn(catalyst, 1).getDamageValue() == 1
                && CatalystWear.worn(catalyst, 5).is(TestKitMachine.SPENT_CATALYST.get()),
                "one that wears loses a use a recipe, and past its last is its spent form");
        helper.succeed();
    }

    private static KitMachineBlockEntity machine(final GameTestHelper helper) {
        helper.setBlock(MACHINE, TestKitMachine.BLOCK.get());
        final KitMachineBlockEntity machine = (KitMachineBlockEntity) helper.getLevel().getBlockEntity(
                helper.absolutePos(MACHINE));
        machine.power().portStore().setStored(TestKitMachine.CAPACITY);
        return machine;
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
}
