/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import dev.jstech.computers.block.ServerRackStructure;
import dev.jstech.computers.plant.CduBlockEntity;
import dev.jstech.computers.plant.CduPartBlock;
import dev.jstech.computers.plant.CoolingPlant;
import dev.jstech.computers.plant.CoolingWater;
import dev.jstech.core.energy.EnergyPorts;
import dev.jstech.tests.JsTests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The CDU (computers/docs/design/temperature.md): placed whole, a 2 by 3 by 2 cabinet; its four water ports in its
 * roof, as one faces its front the plant's COLD inlet at the front right and HOT outlet at the back right, the racks'
 * COLD outlet at the front left and HOT inlet at the back left, its energy at the bottom of its back left; and each
 * second it cools as much of the racks' water as the plant's cold water carries away, up to 64 mB.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class CduGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos CDU = new BlockPos(2, 1, 2);
    private static final int TOP = ServerRackStructure.HEIGHT - 1;

    private CduGameTests() {
    }

    @GameTest(template = ARENA)
    public static void cdu_standsWholeAsTwelveBlocks(final GameTestHelper helper) {
        final CduBlockEntity cdu = place(helper);
        int parts = 0;
        for (final BlockPos pos : ServerRackStructure.partPositions(cdu.getBlockPos(), Direction.NORTH)) {
            parts += helper.getLevel().getBlockState(pos).getBlock() instanceof CduPartBlock
                    && cdu.getBlockPos().equals(CduPartBlock.origin(helper.getLevel(), pos)) ? 1 : 0;
        }
        helper.assertTrue(parts == ServerRackStructure.BLOCK_COUNT - 1, "eleven parts round its origin, each bound "
                + "to it: " + parts);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void cdu_takesItsWaterInItsRoofAndItsEnergyAtItsBack(final GameTestHelper helper) {
        final CduBlockEntity cdu = place(helper);
        final IFluidHandler plantIn = port(helper, cdu, 0, 0);
        final IFluidHandler plantOut = port(helper, cdu, 0, 1);
        final IFluidHandler racksOut = port(helper, cdu, 1, 0);
        final IFluidHandler racksIn = port(helper, cdu, 1, 1);
        helper.assertTrue(plantIn != null && fills(plantIn, CoolingWater.COLD.get()) && fills(plantIn, Fluids.WATER)
                && !fills(plantIn, CoolingWater.HOT.get()), "the plant's COLD inlet takes cold water, or plain water");
        helper.assertTrue(racksIn != null && fills(racksIn, CoolingWater.HOT.get())
                && !fills(racksIn, CoolingWater.COLD.get()), "the racks' HOT inlet takes their hot water");
        helper.assertTrue(plantOut != null && racksOut != null && !fills(plantOut, CoolingWater.HOT.get())
                && !fills(racksOut, CoolingWater.COLD.get()), "its two outlets take nothing in");
        final BlockPos entry = ServerRackStructure.at(cdu.getBlockPos(), Direction.NORTH, 1, 0, 1);
        helper.assertTrue(helper.getLevel().getCapability(EnergyPorts.PORT, entry, Direction.SOUTH) != null
                && helper.getLevel().getCapability(EnergyPorts.PORT, entry, Direction.EAST) == null,
                "its energy is taken at the bottom of its back left, on its back");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void cdu_coolsTheRacksWaterWithThePlants(final GameTestHelper helper) {
        final CduBlockEntity cdu = place(helper);
        final int water = 2 * CduBlockEntity.MOST_A_SECOND;
        pour(port(helper, cdu, 1, 1), CoolingWater.HOT.get(), water);
        pour(port(helper, cdu, 0, 0), CoolingWater.COLD.get(), water);
        helper.succeedWhen(() -> {
            final int racks = held(port(helper, cdu, 1, 0), CoolingWater.COLD.get());
            final int plant = held(port(helper, cdu, 0, 1), CoolingWater.HOT.get());
            helper.assertTrue(racks == water && plant == water, "the racks' 128 mB went back cold and the plant's "
                    + "came out hot, a millibucket for a millibucket: " + racks + " and " + plant);
        });
    }

    /* The CDU, facing north, raised whole as a player's placing raises it. */
    private static CduBlockEntity place(final GameTestHelper helper) {
        final Block block = CoolingPlant.CDU.get();
        final BlockState state = block.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH);
        helper.setBlock(CDU, state);
        block.setPlacedBy(helper.getLevel(), helper.absolutePos(CDU), state, null, ItemStack.EMPTY);
        if (!(helper.getBlockEntity(CDU) instanceof CduBlockEntity cdu)) {
            throw new IllegalStateException("no CDU at " + CDU);
        }
        return cdu;
    }

    /* The water port of its top block {@code w} columns clockwise and {@code d} back, from above. */
    private static IFluidHandler port(final GameTestHelper helper, final CduBlockEntity cdu, final int w,
                                      final int d) {
        return helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK,
                ServerRackStructure.at(cdu.getBlockPos(), Direction.NORTH, w, TOP, d), Direction.UP);
    }

    private static boolean fills(final IFluidHandler port, final Fluid fluid) {
        return port.fill(new FluidStack(fluid, 10), IFluidHandler.FluidAction.SIMULATE) > 0;
    }

    private static void pour(final IFluidHandler port, final Fluid fluid, final int amount) {
        if (port == null || port.fill(new FluidStack(fluid, amount), IFluidHandler.FluidAction.EXECUTE) != amount) {
            throw new IllegalStateException("the port took no " + amount + " mB");
        }
    }

    private static int held(final IFluidHandler port, final Fluid fluid) {
        return port == null ? 0 : port.drain(new FluidStack(fluid, Integer.MAX_VALUE),
                IFluidHandler.FluidAction.SIMULATE).getAmount();
    }
}
