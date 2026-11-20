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
import dev.jstech.computers.plant.ChillerBlockEntity;
import dev.jstech.computers.plant.CoolingTowerBlockEntity;
import dev.jstech.computers.plant.CoolingWater;
import dev.jstech.core.machine.MachineStructure;
import dev.jstech.core.multiblock.PortKind;
import dev.jstech.tests.JsTests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The datacenter's cooling plant (computers/docs/design/temperature.md): the Cooling Tower cools the Chiller's Hot
 * Water into Cold Water, up to 200 mB a second, evaporating a millibucket of its make-up water for every ten; the
 * Chiller cools the racks' Hot Water into Cold Water, up to 120 mB a second, heating 25 mB of the tower's Cold Water
 * for every 20 it cools, and stands still without it; each port takes or gives its own water alone.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class CoolingPlantGameTests {

    private static final String ARENA = "empty";
    private static final int FORMS = 20;
    private static final int SECONDS = 20;

    private CoolingPlantGameTests() {
    }

    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void tower_coolsHotWaterIntoColdEvaporatingItsMakeUp(final GameTestHelper helper) {
        final BlockPos terminal = formed(helper, machine("cooling_tower"));
        helper.startSequence()
                .thenIdle(FORMS)
                .thenExecute(() -> {
                    final ChainMachineBlockEntity tower = machineAt(helper, terminal);
                    pour(helper, tower, CoolingWater.HOT.get(), 2 * CoolingTowerBlockEntity.MOST_A_SECOND);
                    pour(helper, tower, Fluids.WATER, 100);
                })
                .thenWaitUntil(() -> helper.assertTrue(held(helper, machineAt(helper, terminal),
                        CoolingWater.COLD.get()) == 2 * CoolingTowerBlockEntity.MOST_A_SECOND, "the 400 mB of hot "
                        + "water came out cold"))
                .thenExecute(() -> {
                    final ChainMachineBlockEntity tower = machineAt(helper, terminal);
                    final int left = tower.inputTank(0).getFluid().is(Fluids.WATER) ? tower.inputTank(0)
                            .getFluidAmount() : tower.inputTank(1).getFluidAmount();
                    helper.assertTrue(left == 100 - 2 * CoolingTowerBlockEntity.MOST_A_SECOND
                            / CoolingTowerBlockEntity.COOLED_A_MAKEUP, "a millibucket of make-up water evaporated "
                            + "for every ten cooled: " + left + " mB left");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void tower_eachPortTakesItsOwnWater(final GameTestHelper helper) {
        final BlockPos terminal = formed(helper, machine("cooling_tower"));
        helper.startSequence()
                .thenIdle(FORMS)
                .thenExecute(() -> {
                    final ChainMachineBlockEntity tower = machineAt(helper, terminal);
                    final IFluidHandler hot = port(helper, tower, PortKind.FLUID_INPUT, CoolingWater.HOT.get());
                    final IFluidHandler fill = port(helper, tower, PortKind.FLUID_INPUT, Fluids.WATER);
                    helper.assertTrue(hot != null && fill != null && hot != fill, "a HOT inlet and a FILL inlet");
                    helper.assertTrue(hot.fill(new FluidStack(Fluids.WATER, 10), IFluidHandler.FluidAction.SIMULATE)
                            == 0, "the HOT inlet takes no plain water");
                    helper.assertTrue(fill.fill(new FluidStack(CoolingWater.HOT.get(), 10),
                            IFluidHandler.FluidAction.SIMULATE) == 0, "the FILL inlet takes no hot water");
                    helper.assertTrue(port(helper, tower, PortKind.FLUID_OUTPUT, CoolingWater.COLD.get()) != null,
                            "a COLD outlet");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void chiller_coolsTheRacksWaterAndHeatsTheTowers(final GameTestHelper helper) {
        final BlockPos terminal = formed(helper, machine("chiller"));
        final int racks = 2 * ChillerBlockEntity.MOST_A_SECOND;
        final int tower = ChillerBlockEntity.towerShare(racks);
        helper.startSequence()
                .thenIdle(FORMS)
                .thenExecute(() -> {
                    final ChainMachineBlockEntity chiller = machineAt(helper, terminal);
                    pour(helper, chiller, CoolingWater.HOT.get(), racks);
                    pour(helper, chiller, CoolingWater.COLD.get(), tower);
                })
                .thenWaitUntil(() -> helper.assertTrue(held(helper, machineAt(helper, terminal),
                        CoolingWater.COLD.get()) == racks, "the racks' 240 mB came out cold"))
                .thenExecute(() -> {
                    final int heated = held(helper, machineAt(helper, terminal), CoolingWater.HOT.get());
                    helper.assertTrue(heated == tower, "it heated 25 mB of the tower's for every 20 it cooled: "
                            + heated + " of " + tower);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void chiller_takesPlainWaterAtTheTowersInletAsCold(final GameTestHelper helper) {
        final BlockPos terminal = formed(helper, machine("chiller"));
        final int racks = ChillerBlockEntity.MOST_A_SECOND;
        helper.startSequence()
                .thenIdle(FORMS)
                .thenExecute(() -> {
                    final ChainMachineBlockEntity chiller = machineAt(helper, terminal);
                    pour(helper, chiller, CoolingWater.HOT.get(), racks);
                    pour(helper, chiller, Fluids.WATER, ChillerBlockEntity.towerShare(racks));
                })
                .thenWaitUntil(() -> helper.assertTrue(held(helper, machineAt(helper, terminal),
                        CoolingWater.COLD.get()) == racks, "plain water filled its loop, the racks' 120 mB came out "
                        + "cold"))
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void chiller_standsStillWithoutTheTowersWater(final GameTestHelper helper) {
        final BlockPos terminal = formed(helper, machine("chiller"));
        helper.startSequence()
                .thenIdle(FORMS)
                .thenExecute(() -> pour(helper, machineAt(helper, terminal), CoolingWater.HOT.get(),
                        ChillerBlockEntity.MOST_A_SECOND))
                .thenIdle(3 * SECONDS)
                .thenExecute(() -> helper.assertTrue(held(helper, machineAt(helper, terminal), CoolingWater.COLD.get())
                        == 0, "with no cold water from the tower to carry its heat away, it cools nothing"))
                .thenSucceed();
    }

    /* Pours {@code amount} of {@code fluid} into the machine's inlet that takes it. */
    private static void pour(final GameTestHelper helper, final ChainMachineBlockEntity machine, final Fluid fluid,
                             final int amount) {
        final IFluidHandler inlet = port(helper, machine, PortKind.FLUID_INPUT, fluid);
        if (inlet == null || inlet.fill(new FluidStack(fluid, amount), IFluidHandler.FluidAction.EXECUTE) != amount) {
            throw new IllegalStateException("no inlet took " + amount + " mB of " + BuiltInRegistries.FLUID.getKey(
                    fluid));
        }
    }

    /* How much of {@code fluid} the machine's outlet that gives it holds. */
    private static int held(final GameTestHelper helper, final ChainMachineBlockEntity machine, final Fluid fluid) {
        final IFluidHandler outlet = port(helper, machine, PortKind.FLUID_OUTPUT, fluid);
        return outlet == null ? 0 : outlet.drain(new FluidStack(fluid, Integer.MAX_VALUE),
                IFluidHandler.FluidAction.SIMULATE).getAmount();
    }

    /* The machine's port of {@code kind} bound to {@code fluid}, as a pipe on it sees it, or none. */
    private static IFluidHandler port(final GameTestHelper helper, final ChainMachineBlockEntity machine,
                                      final PortKind kind, final Fluid fluid) {
        final MachineStructure structure = machine.structure();
        if (structure == null) {
            return null;
        }
        final ResourceLocation id = BuiltInRegistries.FLUID.getKey(fluid);
        for (final MachineStructure.PortAt port : structure.ports()) {
            if (port.kind() == kind && machine.portProducts(port.pos()).contains(id)) {
                return helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, port.pos(), Direction.UP);
            }
        }
        return null;
    }
}
