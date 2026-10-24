/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.industrial;

import dev.jstech.core.fluid.LineValveBlockEntity;
import dev.jstech.core.fluid.ReliefValveBlockEntity;
import dev.jstech.core.hazard.GasClouds;
import dev.jstech.core.jstechseries.Element;
import dev.jstech.core.jstechseries.Elements;
import dev.jstech.core.machine.ComparatorReads;
import dev.jstech.core.machine.MachineStatus;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MatterState;
import dev.jstech.core.matter.MatterStack;
import dev.jstech.core.matter.MatterTank;
import dev.jstech.industrial.line.IndustrialTank;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestCableTypes;
import dev.jstech.tests.TestFluidEnds;
import dev.jstech.tests.TestFluidMachines;
import dev.jstech.tests.TestMaterials;
import dev.jstech.tests.TestMatterEnds;
import dev.jstech.tests.testkit.TestCables;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * J's Industrial's tanks, pumps and J's Core's valves: a tank holds the room inside its walls, a block a cubic metre,
 * one content at a time, refusing more full, and keeps it while apart; a gas vessel its volume times its pressure,
 * refusing what it is not made for; a cold liquid boils off; a water pump draws water without end, a drainage pump the
 * fluid around it; a valve lets through one way, by hand, by a signal or one fluid alone, counting its flow; and a
 * relief valve lets a vessel's excess out above its pressure.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class IndustrialFluidGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos LOW = new BlockPos(1, 1, 1);
    private static final BlockPos TERMINAL = new BlockPos(2, 2, 1);
    private static final BlockPos SOURCE = new BlockPos(1, 2, 2);
    private static final BlockPos VALVE = new BlockPos(3, 2, 2);
    private static final BlockPos SINK = new BlockPos(5, 2, 2);
    private static final BlockPos PUMP = new BlockPos(3, 2, 3);
    private static final long FILL = 4_000L;
    private static final double NEAR = 16;

    private IndustrialFluidGameTests() {
    }

    @GameTest(template = ARENA)
    public static void tank_holdsTheRoomInsideItsWallsAndRefusesMoreFull(final GameTestHelper helper) {
        final IndustrialTank tank = box(helper, 3, TestFluidMachines.TANK_CASING.get(),
                TestFluidMachines.LIQUID_TANK.get());
        helper.succeedWhen(() -> {
            helper.assertTrue(tank.capacity() == 1_000L, "a box three a side holds a block, a cubic metre: "
                    + tank.capacity());
            helper.assertTrue(tank.liquid().fill(new FluidStack(Fluids.WATER, 1_500), IFluidHandler.FluidAction.EXECUTE)
                    == 1_000, "it takes no more than its room");
            helper.assertTrue(tank.liquid().fill(new FluidStack(Fluids.WATER, 1), IFluidHandler.FluidAction.SIMULATE)
                    == 0, "full, it refuses");
            tank.control().setReads(ComparatorReads.INPUT);
            helper.assertTrue(tank.strength() == 15, "a comparator reads it full");
        });
    }

    @GameTest(template = ARENA)
    public static void tank_ofABiggerBoxHoldsMoreAndOneContent(final GameTestHelper helper) {
        final IndustrialTank tank = box(helper, 4, TestFluidMachines.TANK_CASING.get(),
                TestFluidMachines.LIQUID_TANK.get());
        helper.succeedWhen(() -> {
            helper.assertTrue(tank.capacity() == 8_000L, "four a side, eight blocks inside: " + tank.capacity());
            tank.liquid().fill(new FluidStack(Fluids.WATER, 100), IFluidHandler.FluidAction.EXECUTE);
            helper.assertTrue(tank.liquid().fill(new FluidStack(Fluids.LAVA, 100), IFluidHandler.FluidAction.SIMULATE)
                    == 0, "holding water, it takes no lava");
        });
    }

    @GameTest(template = ARENA)
    public static void vessel_holdsItsVolumeTimesItsPressureAndRefusesWhatItIsNotMadeFor(final GameTestHelper helper) {
        final IndustrialTank vessel = box(helper, 3, TestFluidMachines.VESSEL_CASING.get(),
                TestFluidMachines.GAS_VESSEL.get());
        helper.succeedWhen(() -> {
            helper.assertTrue(vessel.capacity() == 20_000L && vessel.gas().capacity() == 20_000L,
                    "a cubic metre at twenty atmospheres holds twenty of gas: " + vessel.capacity());
            helper.assertTrue(vessel.gas().insert(new MatterStack(Elements.of(Element.CHLORINE), 10), true) == 0,
                    "it is not made for chlorine");
            helper.assertTrue(vessel.gas().insert(new MatterStack(Elements.of(Element.HYDROGEN), 10), true) == 10,
                    "it holds hydrogen");
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void tank_keepsWhatItHoldsWhileApart(final GameTestHelper helper) {
        final IndustrialTank tank = box(helper, 3, TestFluidMachines.TANK_CASING.get(),
                TestFluidMachines.LIQUID_TANK.get());
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(tank.capacity() == 1_000L, "it stands whole"))
                .thenExecute(() -> tank.liquid().fill(new FluidStack(Fluids.WATER, 500),
                        IFluidHandler.FluidAction.EXECUTE))
                .thenExecute(() -> helper.setBlock(LOW, Blocks.AIR))
                .thenWaitUntil(() -> helper.assertTrue(tank.status() == MachineStatus.NOT_FORMED,
                        "a corner gone undoes it"))
                .thenExecute(() -> helper.assertTrue(tank.liquid().getFluidAmount() == 500,
                        "and it keeps its water"))
                .thenExecute(() -> helper.setBlock(LOW, TestFluidMachines.TANK_CASING.get()))
                .thenWaitUntil(() -> helper.assertTrue(tank.status() != MachineStatus.NOT_FORMED
                        && tank.liquid().getFluidAmount() == 500, "whole again, with its water"))
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void tank_aColdLiquidBoilsOff(final GameTestHelper helper) {
        final IndustrialTank tank = box(helper, 3, TestFluidMachines.TANK_CASING.get(),
                TestFluidMachines.LIQUID_TANK.get());
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(tank.capacity() == 1_000L, "it stands whole"))
                .thenExecute(() -> tank.liquid().fill(new FluidStack(TestMaterials.LIQUID_NITROGEN.get(), 1_000),
                        IFluidHandler.FluidAction.EXECUTE))
                .thenWaitUntil(() -> helper.assertTrue(tank.liquid().getFluidAmount() < 1_000,
                        "liquid nitrogen in a bare tank boils off as the place's heat leaks in"))
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void waterPump_drawsWaterWithoutEndFromASourceBesideIt(final GameTestHelper helper) {
        helper.setBlock(PUMP, TestFluidMachines.WATER_PUMP.get());
        helper.setBlock(PUMP.east(), Blocks.WATER);
        final TestFluidMachines.WaterPump pump = helper.getBlockEntity(PUMP);
        helper.onEachTick(() -> pump.power().generate(pump.power().capacity()));
        helper.succeedWhen(() -> {
            helper.assertTrue(pump.tank().getFluidAmount() >= 1_000 || pump.tank().getFluidAmount()
                    == pump.tank().getCapacity(), "it fills with water: " + pump.tank().getFluidAmount());
            helper.assertTrue(helper.getLevel().getFluidState(helper.absolutePos(PUMP.east()))
                    .isSourceOfType(Fluids.WATER), "and the source stays");
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void drainagePump_drainsTheFluidAroundIt(final GameTestHelper helper) {
        helper.setBlock(PUMP, TestFluidMachines.DRAINAGE_PUMP.get());
        final BlockPos pool = PUMP.west(2);
        helper.setBlock(pool.below(), Blocks.STONE);
        helper.setBlock(pool, Blocks.LAVA);
        final TestFluidMachines.DrainagePump pump = helper.getBlockEntity(PUMP);
        helper.onEachTick(() -> pump.power().generate(pump.power().capacity()));
        helper.succeedWhen(() -> {
            helper.assertTrue(pump.tank().getFluid().getFluid() == Fluids.LAVA
                    && pump.tank().getFluidAmount() >= 1_000, "it takes the lava, a bucket a block: "
                    + pump.status() + ", " + pump.tank().getFluidAmount());
            helper.assertFalse(helper.getLevel().getFluidState(helper.absolutePos(pool)).isSource(),
                    "and the lava block is gone");
        });
    }

    @GameTest(template = ARENA)
    public static void checkValve_letsThroughOneWayAndCountsItsFlow(final GameTestHelper helper) {
        final LineValveBlockEntity valve = valveLine(helper, TestFluidMachines.CHECK_VALVE.get(), Direction.EAST);
        helper.succeedWhen(() -> {
            helper.assertTrue(TestFluidEnds.tankAt(helper.getLevel(), helper.absolutePos(SINK)).getFluidAmount() > 0,
                    "water goes through it the way it faces");
            helper.assertTrue(valve.passed() > 0, "and it counts what passed: " + valve.passed());
        });
    }

    @GameTest(template = ARENA)
    public static void checkValve_letsNothingBack(final GameTestHelper helper) {
        valveLine(helper, TestFluidMachines.CHECK_VALVE.get(), Direction.WEST);
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(TestFluidEnds.tankAt(helper.getLevel(), helper.absolutePos(SINK)).getFluidAmount() == 0,
                    "facing the source, it lets nothing through");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void handValve_isShutUntilOpened(final GameTestHelper helper) {
        final LineValveBlockEntity valve = valveLine(helper, TestFluidMachines.HAND_VALVE.get(), Direction.EAST);
        helper.startSequence()
                .thenIdle(20)
                .thenExecute(() -> helper.assertTrue(TestFluidEnds.tankAt(helper.getLevel(),
                        helper.absolutePos(SINK)).getFluidAmount() == 0, "shut, it lets nothing through"))
                .thenExecute(valve::turn)
                .thenWaitUntil(() -> helper.assertTrue(TestFluidEnds.tankAt(helper.getLevel(),
                        helper.absolutePos(SINK)).getFluidAmount() > 0, "opened by hand, it does"))
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void signalValve_opensWithRedstone(final GameTestHelper helper) {
        valveLine(helper, TestFluidMachines.SIGNAL_VALVE.get(), Direction.EAST);
        helper.startSequence()
                .thenIdle(20)
                .thenExecute(() -> helper.assertTrue(TestFluidEnds.tankAt(helper.getLevel(),
                        helper.absolutePos(SINK)).getFluidAmount() == 0, "with no signal, it is shut"))
                .thenExecute(() -> helper.setBlock(VALVE.above(), Blocks.REDSTONE_BLOCK))
                .thenWaitUntil(() -> helper.assertTrue(TestFluidEnds.tankAt(helper.getLevel(),
                        helper.absolutePos(SINK)).getFluidAmount() > 0, "a signal opens it"))
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void filterValve_letsOnlyItsFluidThrough(final GameTestHelper helper) {
        final LineValveBlockEntity valve = valveLine(helper, TestFluidMachines.CHECK_VALVE.get(), Direction.EAST);
        valve.setFilter(Optional.of(ResourceLocation.withDefaultNamespace("lava")));
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(TestFluidEnds.tankAt(helper.getLevel(), helper.absolutePos(SINK)).getFluidAmount() == 0,
                    "set to lava, it lets no water through");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA)
    public static void gasValve_letsGasThrough(final GameTestHelper helper) {
        final Material hydrogen = Elements.of(Element.HYDROGEN);
        TestMatterEnds.clear(helper.getLevel(), helper.absolutePos(SOURCE));
        TestMatterEnds.clear(helper.getLevel(), helper.absolutePos(SINK));
        helper.setBlock(SOURCE, Blocks.CHISELED_COPPER);
        helper.setBlock(SINK, Blocks.WAXED_CHISELED_COPPER);
        TestCables.lay(helper, SOURCE.east(), TestCableTypes.GAS_LINE);
        helper.setBlock(VALVE, TestFluidMachines.GAS_VALVE.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.EAST));
        TestCables.lay(helper, VALVE.east(), TestCableTypes.GAS_LINE);
        TestMatterEnds.tankAt(helper.getLevel(), helper.absolutePos(SOURCE), MatterState.GAS)
                .insert(new MatterStack(hydrogen, FILL), false);
        helper.succeedWhen(() -> helper.assertTrue(TestMatterEnds.amountAt(helper.getLevel(),
                helper.absolutePos(SINK), MatterState.GAS, hydrogen) > 0, "hydrogen goes through a gas valve"));
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void reliefValve_letsTheExcessOutAboveItsPressure(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final Material nitrogen = Elements.of(Element.NITROGEN);
        TestMatterEnds.clear(level, helper.absolutePos(SOURCE));
        helper.setBlock(SOURCE, Blocks.COPPER_GRATE);
        final MatterTank vessel = TestMatterEnds.tankAt(level, helper.absolutePos(SOURCE), MatterState.GAS);
        vessel.insert(new MatterStack(nitrogen, 30_000L), false);
        helper.setBlock(SOURCE.east(), TestFluidMachines.RELIEF_VALVE.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.EAST));
        final ReliefValveBlockEntity valve = helper.getBlockEntity(SOURCE.east());
        helper.succeedWhen(() -> {
            helper.assertTrue(valve.vented() > 0, "above ten atmospheres, it lets the vessel's gas out");
            helper.assertTrue(vessel.pressureNow() <= TestFluidMachines.RELIEF_AT + 1,
                    "down to its pressure: " + vessel.pressureNow());
            clearClouds(level, helper.absolutePos(SOURCE));
        });
    }

    /* A box of {@code casing}, {@code side} blocks a side, its terminal of {@code terminal} on a face. */
    private static IndustrialTank box(final GameTestHelper helper, final int side, final Block casing,
                                      final Block terminal) {
        final int high = LOW.getX() + side - 1;
        for (int x = LOW.getX(); x <= high; x++) {
            for (int y = LOW.getY(); y <= high; y++) {
                for (int z = LOW.getZ(); z <= high; z++) {
                    final boolean shell = x == LOW.getX() || x == high || y == LOW.getY() || y == high
                            || z == LOW.getZ() || z == high;
                    if (shell) {
                        helper.setBlock(new BlockPos(x, y, z), casing);
                    }
                }
            }
        }
        helper.setBlock(TERMINAL, terminal);
        return helper.getBlockEntity(TERMINAL);
    }

    /* A line from a store of water through a valve of {@code valve}, facing {@code facing}, to an empty store. */
    private static LineValveBlockEntity valveLine(final GameTestHelper helper, final Block valve,
                                                  final Direction facing) {
        TestFluidEnds.clear(helper.getLevel(), helper.absolutePos(SOURCE));
        TestFluidEnds.clear(helper.getLevel(), helper.absolutePos(SINK));
        helper.setBlock(SOURCE, Blocks.CHISELED_TUFF);
        helper.setBlock(SINK, Blocks.CHISELED_TUFF_BRICKS);
        TestCables.lay(helper, SOURCE.east(), TestCableTypes.PLAIN_PIPE);
        helper.setBlock(VALVE, valve.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, facing));
        TestCables.lay(helper, VALVE.east(), TestCableTypes.PLAIN_PIPE);
        TestFluidEnds.tankAt(helper.getLevel(), helper.absolutePos(SOURCE)).fill(new FluidStack(Fluids.WATER,
                (int) FILL), IFluidHandler.FluidAction.EXECUTE);
        return helper.getBlockEntity(VALVE);
    }

    private static void clearClouds(final ServerLevel level, final BlockPos at) {
        final Vec3 centre = Vec3.atCenterOf(at);
        GasClouds.STATE.set(level, new GasClouds.Book(GasClouds.of(level).stream()
                .filter(cloud -> cloud.centre().distanceTo(centre) > NEAR).toList()));
    }
}
