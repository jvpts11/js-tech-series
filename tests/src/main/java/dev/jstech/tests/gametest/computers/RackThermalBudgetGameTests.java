/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.block.ServerRackBlock;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.item.ServerHardwareHandler;
import dev.jstech.computers.item.ServerItem;
import dev.jstech.computers.rack.RackThermals;
import dev.jstech.computers.registry.ComputingComponents;
import dev.jstech.core.heat.IThermalBudget;
import dev.jstech.core.heat.ThermalBudgets;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.computers.ServerStacks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * A Server Rack's heat as J's Core's thermal budget capability gives it to any mod: read from the rack's own block and
 * from any other block of it, the same numbers the rack's screen and its machines go by.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class RackThermalBudgetGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 4;
    private static final BlockPos RACK = new BlockPos(2, 2, 2);
    private static final int RACK_BLOCKS = 12;
    private static final int HOT_SERVERS = 4;
    private static final int COOLING_ROW = 5;
    /* Kelvin at 0 °C, and how warm any surroundings the arena stands in are, at the most. */
    private static final double KELVIN = 273.15;
    private static final double WARM = 60.0;

    private RackThermalBudgetGameTests() {
    }

    @GameTest(template = ARENA)
    public static void rackBudget_isReadThroughTheCoreFromAnyOfItsBlocks(final GameTestHelper helper) {
        // The rack placed as a player places it, which raises its eleven other blocks.
        final ServerRackBlock block = (ServerRackBlock) ComputingModule.SERVER_RACK.get();
        helper.setBlock(RACK, block);
        block.setPlacedBy(helper.getLevel(), helper.absolutePos(RACK), helper.getBlockState(RACK), null,
                ItemStack.EMPTY);
        if (!(helper.getBlockEntity(RACK) instanceof ServerRackBlockEntity rack)) {
            throw new IllegalStateException("no server rack at " + RACK);
        }
        final List<BlockPos> blocks = block.footprint(helper.absolutePos(RACK),
                helper.getBlockState(RACK).getValue(HorizontalDirectionalBlock.FACING));
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final IThermalBudget empty = budget(helper, RACK);
                    helper.assertTrue(empty.loadWatts() == 0 && empty.budgetWatts() == RackThermals.PASSIVE_BUDGET_W
                            && empty.performance() == 1.0 && !empty.over(), "an empty rack makes no heat and sheds "
                            + RackThermals.PASSIVE_BUDGET_W + " W at full speed: " + describe(empty));
                    helper.assertTrue(empty.temperature() > KELVIN && empty.temperature() < KELVIN + WARM,
                            "an empty rack stands at its surroundings, in kelvin: " + empty.temperature());

                    // Machines laden with accelerators make the cabinet hotter than it can shed.
                    for (int row = 0; row < HOT_SERVERS; row++) {
                        rack.getServers().setStackInSlot(row, hotServer());
                    }
                })
                // The cabinet heats up over half a minute: settled, as long enough would leave it.
                .thenExecuteAfter(1, rack::settleHeat)
                .thenExecuteAfter(1, () -> {
                    final IThermalBudget hot = budget(helper, RACK);
                    helper.assertTrue(hot.over() && hot.loadWatts() == rack.thermalLoadWatts()
                            && hot.budgetWatts() == rack.thermalBudgetWatts()
                            && hot.performance() == rack.thermalThrottlePercent() / 100.0 && hot.performance() < 1.0,
                            "a dense rack is over its budget and slows its machines as its screen says: "
                                    + describe(hot) + " for " + rack.thermalThrottlePercent() + "%");
                    helper.assertTrue(blocks.size() == RACK_BLOCKS, "the rack stands on " + RACK_BLOCKS + " blocks: "
                            + blocks.size());
                    for (final BlockPos each : blocks) {
                        final IThermalBudget there = budgetAt(helper, each);
                        helper.assertTrue(there.loadWatts() == hot.loadWatts()
                                && there.budgetWatts() == hot.budgetWatts()
                                && there.performance() == hot.performance(),
                                "every block of the rack answers for the rack, " + each + " too: " + describe(there));
                    }

                    // A Cooling Unit raises the budget past the load, and the machines run free again.
                    rack.getServers().setStackInSlot(COOLING_ROW, new ItemStack(ComputingModule.COOLING_UNIT.get()));
                })
                .thenExecuteAfter(1, rack::settleHeat)
                .thenExecuteAfter(1, () -> {
                    final IThermalBudget cooled = budgetAt(helper, blocks.getLast());
                    helper.assertTrue(cooled.budgetWatts() > RackThermals.PASSIVE_BUDGET_W && !cooled.over()
                            && cooled.performance() == 1.0, "cooling buys the rack its speed back: "
                            + describe(cooled));
                })
                .thenSucceed();
    }

    /* The thermal budget the block at {@code pos} of the arena offers, failing the test when it offers none. */
    private static IThermalBudget budget(final GameTestHelper helper, final BlockPos pos) {
        return budgetAt(helper, helper.absolutePos(pos));
    }

    /* The thermal budget the block at {@code pos} of the world offers, failing the test when it offers none. */
    private static IThermalBudget budgetAt(final GameTestHelper helper, final BlockPos pos) {
        final IThermalBudget budget = ThermalBudgets.at(helper.getLevel(), pos, null);
        helper.assertTrue(budget != null, "the rack's block at " + pos + " offers its thermal budget");
        return budget;
    }

    private static String describe(final IThermalBudget budget) {
        return budget.loadWatts() + " W of " + budget.budgetWatts() + " W, at " + budget.performance();
    }

    /* A rack server with one graphics accelerator: hot enough that four of them fill a cabinet past its budget. */
    private static ItemStack hotServer() {
        final ItemStack server = ServerStacks.defaultServer();
        final NonNullList<ItemStack> hardware = NonNullList.withSize(ServerHardwareHandler.SLOTS, ItemStack.EMPTY);
        final ItemContainerContents existing = ServerItem.hardware(server);
        for (int i = 0; i < hardware.size() && i < existing.getSlots(); i++) {
            hardware.set(i, existing.getStackInSlot(i).copy());
        }
        hardware.set(ServerHardwareHandler.GPU_START, new ItemStack(ComputingModule.GPU_HD_7970.get()));
        server.set(ComputingComponents.SERVER_HARDWARE.get(), ItemContainerContents.fromItems(hardware));
        return server;
    }
}
