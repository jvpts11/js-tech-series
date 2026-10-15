/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestKitMachine;
import dev.jstech.tests.TestKitMachine.FactoryBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * A machine is never jammed: a factory of three lanes sorts what is put into it among its lanes and works them side by
 * side; an input slot locked to an item takes nothing else; and when its inputs make two recipes, the player picks
 * which.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class MachineLanesGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos FACTORY = new BlockPos(4, 2, 4);
    private static final ResourceLocation FLINT = ResourceLocation.fromNamespaceAndPath(JsTests.MODID,
            "test_choosing/flint");
    private static final ResourceLocation SAND = ResourceLocation.fromNamespaceAndPath(JsTests.MODID,
            "test_choosing/sand");

    private MachineLanesGameTests() {
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void factory_sortsItsInputsAmongItsLanesAndWorksThemTogether(final GameTestHelper helper) {
        final FactoryBlockEntity factory = factory(helper);
        factory.getInventory().setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 9));
        helper.runAfterDelay(TestKitMachine.TICKS + 15, () -> {
            for (int lane = 0; lane < TestKitMachine.LANES; lane++) {
                final ItemStack made = factory.getInventory().getStackInSlot(factory.layout().laneOutput(lane, 0));
                helper.assertTrue(made.is(Items.STONE), "lane " + lane + " worked its share: " + made);
            }
            helper.succeed();
        });
    }

    @GameTest(template = ARENA)
    public static void lock_keepsTheWrongItemOutOfASlot(final GameTestHelper helper) {
        final FactoryBlockEntity factory = factory(helper);
        factory.locks().lock(0, new ItemStack(Items.COBBLESTONE));
        final IItemHandler items = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK,
                helper.absolutePos(FACTORY), Direction.UP);
        helper.assertTrue(!items.insertItem(0, new ItemStack(Items.DIRT), false).isEmpty(),
                "a slot locked to cobblestone takes no dirt");
        helper.assertTrue(items.insertItem(0, new ItemStack(Items.COBBLESTONE), false).isEmpty(),
                "and takes cobblestone");
        helper.assertTrue(factory.locks().locked(0).is(Items.COBBLESTONE), "its lock is kept, to show as a ghost");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 120)
    public static void choice_picksAmongRecipesOfTheSameInput(final GameTestHelper helper) {
        final FactoryBlockEntity factory = factory(helper);
        final int out = factory.layout().laneOutput(0, 0);
        factory.getInventory().setStackInSlot(0, new ItemStack(Items.GRAVEL));
        helper.runAfterDelay(25, () -> {
            final ItemStack first = factory.getInventory().getStackInSlot(out);
            helper.assertTrue(first.is(Items.FLINT) || first.is(Items.SAND), "with no pick, the first: " + first);
            final boolean wasFlint = first.is(Items.FLINT);
            factory.getInventory().setStackInSlot(out, ItemStack.EMPTY);
            factory.chooseRecipe(wasFlint ? SAND : FLINT);
            factory.getInventory().setStackInSlot(0, new ItemStack(Items.GRAVEL));
            helper.runAfterDelay(25, () -> {
                final ItemStack second = factory.getInventory().getStackInSlot(out);
                helper.assertTrue(second.is(wasFlint ? Items.SAND : Items.FLINT),
                        "picked, the other: " + second);
                helper.succeed();
            });
        });
    }

    private static FactoryBlockEntity factory(final GameTestHelper helper) {
        helper.setBlock(FACTORY, TestKitMachine.FACTORY.get());
        final FactoryBlockEntity factory = (FactoryBlockEntity) helper.getLevel().getBlockEntity(
                helper.absolutePos(FACTORY));
        factory.getEnergy().setEnergyStored((int) TestKitMachine.CAPACITY);
        return factory;
    }
}
