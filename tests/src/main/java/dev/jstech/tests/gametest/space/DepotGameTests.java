/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.space;

import dev.jstech.core.matter.IMatterHandler;
import dev.jstech.core.matter.MatterCapabilities;
import dev.jstech.space.depot.DepotBlocks;
import dev.jstech.space.depot.DepotPieceBlock;
import dev.jstech.space.depot.TransferPumpBlockEntity;
import dev.jstech.space.flight.FlightComputerBlockEntity;
import dev.jstech.space.hangar.RocketDesign;
import dev.jstech.space.launcher.LauncherBlocks;
import dev.jstech.space.launcher.LauncherDeckBlockEntity;
import dev.jstech.space.life.OxygenBottleItem;
import dev.jstech.space.life.SuitSupplies;
import dev.jstech.space.pad.PadBlocks;
import dev.jstech.space.pad.PadServicePortBlockEntity;
import dev.jstech.space.rocket.RocketKitBlocks;
import dev.jstech.space.rocket.RocketStack;
import dev.jstech.space.rocket.SkeletonPropellants;
import dev.jstech.space.rocket.SkeletonRocketParts;
import dev.jstech.space.rocket.TankLoad;
import dev.jstech.tests.JsTests;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The propellant depot and the pad's fuelling: a sphere of Sphere Shell three blocks wide on its four legs, its vent
 * stack on top and its transfer pump against a leg, forms and holds as much as a ball that wide, liquid oxygen or
 * liquid methane and nothing else, and lets it boil away when a plate goes; a depot of liquid oxygen fills an Oxygen
 * Bottle; and the pad's service port, its lines filled from the depot, fuels the rocket standing on the launcher over
 * it to its propellant's mix, its pumps spending energy.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class DepotGameTests {

    private static final String TALL = "tall";
    private static final String BATCH = "jsspace_depot";
    /* The sphere's lowest corner, two blocks of legs over the ground at y 1, and the pump against a leg. */
    private static final BlockPos LOW = new BlockPos(4, 3, 4);
    private static final int EDGE = 3;
    private static final BlockPos PUMP = new BlockPos(3, 1, 4);
    /* The pad's port, the launcher's measuring arm over it, and the rocket's flight computer up its axis. */
    private static final BlockPos PORT = new BlockPos(2, 1, 2);
    private static final BlockPos ARM = new BlockPos(5, 7, 5);
    private static final BlockPos COMPUTER = new BlockPos(6, 11, 6);
    private static final int OXYGEN = 10_000;

    private DepotGameTests() {
    }

    @GameTest(template = TALL, batch = BATCH, timeoutTicks = 200)
    public static void depot_formsAndHoldsAsMuchAsItsSphere(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        depot(helper);
        final BlockPos pump = helper.absolutePos(PUMP);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(pump(level, pump).depot().isPresent(), "the depot forms"))
                .thenExecute(() -> {
                    helper.assertTrue(level.getBlockState(helper.absolutePos(LOW)).getValue(DepotPieceBlock.FORMED),
                            "its pieces formed");
                    final IFluidHandler pipe = level.getCapability(Capabilities.FluidHandler.BLOCK, pump, Direction.UP);
                    helper.assertTrue(pipe != null && pipe.getTankCapacity(0) == (int) Math.round(Math.PI / 6 * 27
                            * 1000), "it holds as much as a ball three blocks wide: " + pipe.getTankCapacity(0));
                    helper.assertTrue(pipe.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE)
                            == 0, "a propellant's liquid only");
                    pipe.fill(new FluidStack(SkeletonPropellants.LIQUID_OXYGEN.get(), OXYGEN),
                            IFluidHandler.FluidAction.EXECUTE);
                    helper.assertTrue(pump(level, pump).amount() == OXYGEN, "liquid oxygen it takes");
                    level.setBlockAndUpdate(helper.absolutePos(LOW.above()), Blocks.AIR.defaultBlockState());
                })
                .thenWaitUntil(() -> helper.assertTrue(pump(level, pump).depot().isEmpty()
                        && pump(level, pump).amount() == 0, "a plate gone, it falls apart and its liquid boils away"))
                .thenSucceed();
    }

    @GameTest(template = TALL, batch = BATCH, timeoutTicks = 200)
    public static void depot_ofLiquidOxygenFillsAnOxygenBottle(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        depot(helper);
        final BlockPos pump = helper.absolutePos(PUMP);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(pump(level, pump).depot().isPresent(), "the depot forms"))
                .thenExecute(() -> {
                    pump(level, pump).liquid().fill(new FluidStack(SkeletonPropellants.LIQUID_OXYGEN.get(), OXYGEN),
                            IFluidHandler.FluidAction.EXECUTE);
                    final IMatterHandler gas = level.getCapability(MatterCapabilities.GAS, pump, Direction.UP);
                    helper.assertTrue(gas != null, "its pump gives gas");
                    final ItemStack bottle = new ItemStack(SuitSupplies.OXYGEN_BOTTLE.get());
                    final int seconds = OxygenBottleItem.fillFromGas(bottle, gas);
                    helper.assertTrue(seconds == OxygenBottleItem.FULL && OxygenBottleItem.air(bottle)
                            == OxygenBottleItem.FULL, "a bottle held to it fills: " + seconds);
                    helper.assertTrue(pump(level, pump).amount() < OXYGEN, "boiling off some of its liquid oxygen");
                })
                .thenSucceed();
    }

    @GameTest(template = TALL, batch = BATCH, timeoutTicks = 300)
    public static void port_fuelsTheRocketOnThePadOverIt(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        helper.setBlock(PORT, PadBlocks.PAD_SERVICE_PORT.get());
        helper.setBlock(ARM, LauncherBlocks.HOLD_DOWN_ARM.get());
        helper.setBlock(COMPUTER, RocketKitBlocks.FLIGHT_COMPUTER.get());
        final BlockPos port = helper.absolutePos(PORT);
        final BlockPos computer = helper.absolutePos(COMPUTER);
        if (level.getBlockEntity(helper.absolutePos(ARM)) instanceof LauncherDeckBlockEntity arm) {
            arm.deck(new LauncherDeckBlockEntity.Deck(Direction.EAST, 1, new BlockPos(1, 0, 1), 6, 8, 6));
        }
        final RocketStack stack = RocketStack.of(part("sea_level_engine_3"), part("tank_3"), part("capsule_3"));
        if (level.getBlockEntity(computer) instanceof FlightComputerBlockEntity rocket) {
            rocket.carry(new RocketDesign("PAD", stack), new BlockPos(0, -4, 0), List.of());
        }
        final IFluidHandler lines = level.getCapability(Capabilities.FluidHandler.BLOCK, port, Direction.UP);
        final IEnergyStorage cable = level.getCapability(Capabilities.EnergyStorage.BLOCK, port, Direction.UP);
        helper.assertTrue(lines != null && cable != null, "the port takes its lines by pipe and energy by cable");
        lines.fill(new FluidStack(SkeletonPropellants.LIQUID_OXYGEN.get(), PadServicePortBlockEntity.LINE),
                IFluidHandler.FluidAction.EXECUTE);
        lines.fill(new FluidStack(SkeletonPropellants.LIQUID_METHANE.get(), PadServicePortBlockEntity.LINE),
                IFluidHandler.FluidAction.EXECUTE);
        for (int k = 0; k < 30; k++) {
            cable.receiveEnergy(PadServicePortBlockEntity.DELUGE_ENERGY, false);
        }
        helper.assertTrue(lines.drain(1000, IFluidHandler.FluidAction.SIMULATE).isEmpty(),
                "nothing is drawn back out of its lines");
        helper.startSequence()
                .thenWaitUntil(() -> {
                    final TankLoad load = level.getBlockEntity(computer) instanceof FlightComputerBlockEntity rocket
                            ? rocket.tanks().get(1) : null;
                    helper.assertTrue(load != null && load.kilograms().size() == 2 && load.total() > 100,
                            "the rocket's tank takes both liquids: " + load);
                })
                .thenExecute(() -> {
                    final PadServicePortBlockEntity lineEnds = (PadServicePortBlockEntity) level.getBlockEntity(port);
                    helper.assertTrue(lineEnds != null && lineEnds.oxygen() < PadServicePortBlockEntity.LINE
                            && lineEnds.methane() < PadServicePortBlockEntity.LINE
                            && lineEnds.energy() < PadServicePortBlockEntity.DELUGE_ENERGY,
                            "from the port's lines, its pumps spending energy");
                })
                .thenSucceed();
    }

    /* A depot three blocks wide: its sphere on two-block legs over stone, its vent stack, its pump against a leg. */
    private static void depot(final GameTestHelper helper) {
        for (int x = 0; x < EDGE; x++) {
            for (int y = 0; y < EDGE; y++) {
                for (int z = 0; z < EDGE; z++) {
                    helper.setBlock(LOW.offset(x, y, z), DepotBlocks.SPHERE_SHELL.get());
                }
            }
        }
        for (final int x : new int[] {0, EDGE - 1}) {
            for (final int z : new int[] {0, EDGE - 1}) {
                helper.setBlock(LOW.offset(x, -3, z), Blocks.STONE);
                helper.setBlock(LOW.offset(x, -2, z), DepotBlocks.TANK_LEG.get());
                helper.setBlock(LOW.offset(x, -1, z), DepotBlocks.TANK_LEG.get());
            }
        }
        helper.setBlock(LOW.offset(EDGE / 2, EDGE, EDGE / 2), DepotBlocks.VENT_STACK.get());
        helper.setBlock(PUMP, DepotBlocks.TRANSFER_PUMP.get());
    }

    private static TransferPumpBlockEntity pump(final ServerLevel level, final BlockPos at) {
        if (level.getBlockEntity(at) instanceof TransferPumpBlockEntity pump) {
            return pump;
        }
        throw new IllegalStateException("No transfer pump at " + at);
    }

    private static RocketStack.Part part(final String path) {
        return new RocketStack.Part(SkeletonRocketParts.id(path), 1);
    }
}
