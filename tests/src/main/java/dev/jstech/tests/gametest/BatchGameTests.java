/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.core.transfer.Batch;
import dev.jstech.core.transfer.HandlerSteps;
import dev.jstech.tests.JsTests;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Batches of moves over the game's stores: iron from one chest to another whole, a batch refused because one step
 * would come short, a batch undone because a store took less than it promised, and fluid paid for with energy.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class BatchGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos FROM = new BlockPos(1, 2, 1);
    private static final BlockPos TO = new BlockPos(3, 2, 1);

    private BatchGameTests() {
    }

    @GameTest(template = ARENA)
    public static void batch_movesIronFromOneChestToAnotherWhole(final GameTestHelper helper) {
        final IItemHandler from = chest(helper, FROM);
        final IItemHandler to = chest(helper, TO);
        from.insertItem(5, new ItemStack(Items.IRON_INGOT, 3), false);

        final Batch.Outcome outcome = new Batch()
                .add(HandlerSteps.extract(from, new ItemStack(Items.IRON_INGOT), 3))
                .add(HandlerSteps.insert(to, new ItemStack(Items.IRON_INGOT, 3)))
                .commit();

        helper.assertTrue(outcome == Batch.Outcome.DONE, "the batch went through: " + outcome);
        helper.assertTrue(from.getStackInSlot(5).isEmpty(), "the iron left the first chest");
        helper.assertTrue(to.getStackInSlot(0).is(Items.IRON_INGOT) && to.getStackInSlot(0).getCount() == 3,
                "and is in the second");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void batch_movesNothingWhenAStepWouldComeShort(final GameTestHelper helper) {
        final IItemHandler from = chest(helper, FROM);
        final IItemHandler to = chest(helper, TO);
        from.insertItem(0, new ItemStack(Items.IRON_INGOT, 3), false);

        final Batch.Outcome outcome = new Batch()
                .add(HandlerSteps.insert(to, new ItemStack(Items.GOLD_INGOT, 2)))
                .add(HandlerSteps.extract(from, new ItemStack(Items.IRON_INGOT), 5))
                .commit();

        helper.assertTrue(outcome == Batch.Outcome.REFUSED, "five iron are not there: " + outcome);
        helper.assertTrue(to.getStackInSlot(0).isEmpty(), "so the gold never went in");
        helper.assertTrue(from.getStackInSlot(0).getCount() == 3, "and the iron never left");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void batch_undoesAPartialAcceptance(final GameTestHelper helper) {
        final IItemHandler from = chest(helper, FROM);
        from.insertItem(7, new ItemStack(Items.IRON_INGOT, 4), false);
        final HalfTaker liar = new HalfTaker();

        final Batch.Outcome outcome = new Batch()
                .add(HandlerSteps.extract(from, new ItemStack(Items.IRON_INGOT), 4))
                .add(HandlerSteps.insert(liar, new ItemStack(Items.IRON_INGOT, 4)))
                .commit();

        helper.assertTrue(outcome == Batch.Outcome.UNDONE, "the store took half of what it promised: " + outcome);
        helper.assertTrue(liar.getStackInSlot(0).isEmpty(), "the half it took was taken back");
        helper.assertTrue(from.getStackInSlot(7).is(Items.IRON_INGOT) && from.getStackInSlot(7).getCount() == 4,
                "and the iron is back in the slot it came from");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void batch_paysEnergyForFluidOrMovesNeither(final GameTestHelper helper) {
        final FluidTank tank = new FluidTank(4_000);
        final EnergyStorage battery = new EnergyStorage(1_000, 1_000, 1_000, 600);

        final Batch.Outcome paid = new Batch()
                .add(HandlerSteps.extract(battery, 500))
                .add(HandlerSteps.fill(tank, new FluidStack(Fluids.WATER, 1_000)))
                .commit();
        helper.assertTrue(paid.done() && battery.getEnergyStored() == 100 && tank.getFluidAmount() == 1_000,
                "five hundred FE paid for a bucket of water");

        final Batch.Outcome unpaid = new Batch()
                .add(HandlerSteps.fill(tank, new FluidStack(Fluids.WATER, 1_000)))
                .add(HandlerSteps.extract(battery, 500))
                .commit();
        helper.assertTrue(unpaid == Batch.Outcome.REFUSED && battery.getEnergyStored() == 100
                && tank.getFluidAmount() == 1_000, "a hundred FE pays for nothing, and no water goes in");
        helper.succeed();
    }

    private static IItemHandler chest(final GameTestHelper helper, final BlockPos at) {
        helper.setBlock(at, Blocks.CHEST);
        final IItemHandler handler = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK,
                helper.absolutePos(at), null);
        if (handler == null) {
            helper.fail("no chest at " + at);
        }
        return handler;
    }

    /* A store that says it takes everything and then takes only half. */
    private static final class HalfTaker extends ItemStackHandler {

        private HalfTaker() {
            super(1);
        }

        @Override
        public ItemStack insertItem(final int slot, final ItemStack stack, final boolean simulate) {
            if (simulate || stack.getCount() < 2) {
                return super.insertItem(slot, stack, simulate);
            }
            final int half = stack.getCount() / 2;
            super.insertItem(slot, stack.copyWithCount(half), false);
            return stack.copyWithCount(stack.getCount() - half);
        }
    }
}
