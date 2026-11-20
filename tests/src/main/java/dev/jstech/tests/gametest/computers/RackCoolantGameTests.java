/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.block.ServerRackStructure;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.plant.CoolingWater;
import dev.jstech.computers.plant.RackCoolant;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.computers.ServerStacks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A liquid-cooled rack's coolant (computers/docs/design/temperature.md): once a second it turns as many millibuckets
 * of Cold Water into Hot Water as its heat over its air needs, up to eight, and sheds a kilowatt more for each; it
 * takes Cold Water in, or plain water as it, at the top block at its back right as one faces its front, and gives Hot
 * Water out at the top block at its front left; a Server Rack, cooled by air only, has no such ports; and the water it
 * carries adds to the rack's budget.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class RackCoolantGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos RACK = new BlockPos(2, 1, 2);
    private static final int SECOND = 20;
    private static final int KILOWATT = 1_000;

    private RackCoolantGameTests() {
    }

    @GameTest(template = ARENA)
    public static void coolant_movesTheSecondsWaterAndShedsItsWatts(final GameTestHelper helper) {
        final RackCoolant coolant = new RackCoolant();
        coolant.inlet().fill(cold(5), IFluidHandler.FluidAction.EXECUTE);
        second(coolant, 8 * KILOWATT);
        helper.assertTrue(coolant.cooledWatts() == 5 * KILOWATT, "short of cold water, it sheds what went through: "
                + coolant.cooledWatts());
        final FluidStack out = coolant.outlet().drain(100, IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(out.is(CoolingWater.HOT.get()) && out.getAmount() == 5, "the 5 mB came out hot: " + out);
        coolant.inlet().fill(cold(100), IFluidHandler.FluidAction.EXECUTE);
        second(coolant, 2_500);
        helper.assertTrue(coolant.cooledWatts() == 3 * KILOWATT, "2,500 W over its air takes 3 mB: "
                + coolant.cooledWatts());
        second(coolant, 20 * KILOWATT);
        helper.assertTrue(coolant.cooledWatts() == RackCoolant.mostWatts(), "8 mB a second at most: "
                + coolant.cooledWatts());
        second(coolant, -KILOWATT);
        helper.assertTrue(coolant.cooledWatts() == 0, "within its air budget it moves no water");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void coolant_takesColdOrPlainWaterInAndGivesOnlyHotWaterOut(final GameTestHelper helper) {
        final RackCoolant coolant = new RackCoolant();
        helper.assertTrue(coolant.inlet().fill(new FluidStack(CoolingWater.HOT.get(), 10),
                IFluidHandler.FluidAction.EXECUTE) == 0, "the inlet takes no hot water");
        helper.assertTrue(coolant.inlet().fill(new FluidStack(Fluids.LAVA, 10), IFluidHandler.FluidAction.EXECUTE)
                == 0, "nor any other liquid");
        helper.assertTrue(coolant.inlet().fill(cold(10), IFluidHandler.FluidAction.EXECUTE) == 10,
                "it takes cold water");
        helper.assertTrue(coolant.inlet().fill(new FluidStack(Fluids.WATER, 10), IFluidHandler.FluidAction.EXECUTE)
                == 10 && coolant.inlet().getFluidInTank(0).is(CoolingWater.COLD.get())
                && coolant.inlet().getFluidInTank(0).getAmount() == 20, "and plain water, as cold water: its loop "
                + "is filled from any water supply");
        helper.assertTrue(coolant.inlet().drain(10, IFluidHandler.FluidAction.EXECUTE).isEmpty(),
                "nothing is drawn back out of the inlet");
        helper.assertTrue(coolant.outlet().fill(cold(10), IFluidHandler.FluidAction.EXECUTE) == 0,
                "nothing is put into the outlet");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void supercomputerRack_takesColdWaterAtItsTopBackRightAndGivesHotAtItsFrontLeft(
            final GameTestHelper helper) {
        final ServerRackBlockEntity rack = place(helper, ComputingModule.SUPERCOMPUTER_RACK.get());
        final IFluidHandler inlet = port(helper, rack, 0, 1, Direction.UP);
        final IFluidHandler outlet = port(helper, rack, 1, 0, Direction.UP);
        helper.assertTrue(inlet != null && inlet.fill(cold(50), IFluidHandler.FluidAction.EXECUTE) == 50,
                "its top block at the back right takes cold water");
        helper.assertTrue(outlet != null && outlet.fill(cold(50), IFluidHandler.FluidAction.SIMULATE) == 0,
                "its top block at the front left is the way out");
        helper.assertTrue(port(helper, rack, 0, 0, Direction.UP) == null && port(helper, rack, 1, 1, Direction.UP)
                == null, "its other top blocks have no port");
        helper.assertTrue(port(helper, rack, 0, 1, Direction.NORTH) == null, "a port is taken from above only");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void serverRack_hasNoWaterPorts(final GameTestHelper helper) {
        final ServerRackBlockEntity rack = place(helper, ComputingModule.SERVER_RACK.get());
        helper.assertTrue(!rack.liquidCooled() && port(helper, rack, 1, 0, Direction.UP) == null
                && port(helper, rack, 0, 1, Direction.UP) == null, "a rack cooled by air takes no water");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void supercomputerRack_budgetGrowsWithTheWaterItCarries(final GameTestHelper helper) {
        final ServerRackBlockEntity rack = place(helper, ComputingModule.SUPERCOMPUTER_RACK.get());
        for (final int row : new int[] {0, 2, 4}) {
            rack.getServers().setStackInSlot(row, ServerStacks.defaultSupercomputerNode());
        }
        final int air = rack.thermalBudgetWatts();
        final int load = rack.thermalLoadWatts();
        helper.assertTrue(load > air, "three nodes draw more than the cabinet sheds by air: " + load + " W over "
                + air + " W");
        final IFluidHandler inlet = port(helper, rack, 0, 1, Direction.UP);
        inlet.fill(cold(500), IFluidHandler.FluidAction.EXECUTE);
        helper.runAfterDelay(2 * SECOND + 2, () -> {
            final int cooled = rack.liquidCooledWatts();
            final int expected = Math.min(RackCoolant.mostWatts(), (load - air + KILOWATT - 1) / KILOWATT * KILOWATT);
            helper.assertTrue(cooled == expected && rack.thermalBudgetWatts() == air + cooled, "the water adds "
                    + expected + " W to its budget: " + cooled + " W, budget " + rack.thermalBudgetWatts() + " W");
            helper.succeed();
        });
    }

    private static void second(final RackCoolant coolant, final int overWatts) {
        for (int tick = 0; tick < SECOND; tick++) {
            coolant.tick(overWatts);
        }
    }

    private static FluidStack cold(final int millibuckets) {
        return new FluidStack(CoolingWater.COLD.get(), millibuckets);
    }

    /* The rack {@code block}, facing north, raised whole as a player's placing raises it. */
    private static ServerRackBlockEntity place(final GameTestHelper helper, final Block block) {
        final BlockState state = block.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH);
        helper.setBlock(RACK, state);
        block.setPlacedBy(helper.getLevel(), helper.absolutePos(RACK), state, null, ItemStack.EMPTY);
        if (!(helper.getBlockEntity(RACK) instanceof ServerRackBlockEntity rack)) {
            throw new IllegalStateException("no rack at " + RACK);
        }
        return rack;
    }

    /* The fluid port, if any, of the rack's top block {@code w} columns clockwise and {@code d} back, from a side. */
    private static IFluidHandler port(final GameTestHelper helper, final ServerRackBlockEntity rack, final int w,
                                      final int d, final Direction side) {
        final BlockPos at = ServerRackStructure.at(rack.getBlockPos(), Direction.NORTH, w, ServerRackStructure.HEIGHT
                - 1, d);
        return helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, at, side);
    }
}
