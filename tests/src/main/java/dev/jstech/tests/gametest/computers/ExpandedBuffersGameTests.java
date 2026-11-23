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

import dev.jstech.computers.chain.machine.ChainMachineBlockEntity;
import dev.jstech.core.jstechseries.SeriesCompounds;
import dev.jstech.core.jstechseries.upgrade.SharedUpgrades;
import dev.jstech.core.material.Forms;
import dev.jstech.core.material.MaterialUses;
import dev.jstech.tests.JsTests;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Expanded Buffers in a chain machine, the Electrolytic Cell: with them its salt slot holds two stacks and its
 * water tank twice its water, which a save keeps; taken out with that much inside, the rest stays until it is used
 * and nothing more goes in.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ExpandedBuffersGameTests {

    private static final String ARENA = "empty";
    private static final int SALT_SLOT = 0;
    private static final int STACK = 64;
    private static final int TANK = 4_000;

    private ExpandedBuffersGameTests() {
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void buffers_doubleTheSlotsAndTanksAndKeepTheRestOnceOut(final GameTestHelper helper) {
        final BlockPos terminal = formed(helper, machine("electrolytic_cell"));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(machineAt(helper, terminal).formed(), "the cell forms"))
                .thenExecute(() -> {
                    final ChainMachineBlockEntity cell = machineAt(helper, terminal);
                    final int bay = cell.layout().firstUpgrade();
                    helper.assertTrue(cell.getInventory().insertItem(bay, new ItemStack(
                            SharedUpgrades.EXPANDED_BUFFERS.get()), false).isEmpty(), "the buffers go in");
                    final ItemStack salt = new ItemStack(MaterialUses.item(SeriesCompounds.SALT, Forms.DUST).get(),
                            STACK * 3);
                    final ItemStack left = cell.getInventory().insertItem(SALT_SLOT, salt, false);
                    helper.assertTrue(cell.getInventory().getStackInSlot(SALT_SLOT).getCount() == STACK * 2
                            && left.getCount() == STACK, "its salt slot holds two stacks: "
                            + cell.getInventory().getStackInSlot(SALT_SLOT));
                    final FluidTank water = cell.inputTank(0);
                    final int filled = water.fill(new FluidStack(Fluids.WATER, TANK * 3),
                            IFluidHandler.FluidAction.EXECUTE);
                    helper.assertTrue(filled == TANK * 2, "its water tank holds twice its water: " + filled);
                    // A save keeps the two stacks, past what the game saves of one.
                    final CompoundTag saved = cell.saveWithoutMetadata(helper.getLevel().registryAccess());
                    cell.getInventory().setStackInSlot(SALT_SLOT, ItemStack.EMPTY);
                    cell.loadWithComponents(saved, helper.getLevel().registryAccess());
                    helper.assertTrue(cell.getInventory().getStackInSlot(SALT_SLOT).getCount() == STACK * 2,
                            "read back, it still holds two stacks: " + cell.getInventory().getStackInSlot(SALT_SLOT));
                    cell.getInventory().extractItem(bay, 1, false);
                    helper.assertTrue(cell.getInventory().getStackInSlot(SALT_SLOT).getCount() == STACK * 2
                            && cell.inputTank(0).getFluidAmount() == TANK * 2,
                            "the buffers out, what it held stays");
                    helper.assertTrue(cell.getInventory().insertItem(SALT_SLOT, salt.copyWithCount(1), false)
                            .getCount() == 1, "and no more salt goes in");
                    helper.assertTrue(cell.inputTank(0).fill(new FluidStack(Fluids.WATER, 1),
                            IFluidHandler.FluidAction.EXECUTE) == 0, "nor water");
                })
                .thenSucceed();
    }
}
