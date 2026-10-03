/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.HardwareItems;
import dev.jstech.computers.block.RedstoneInterfaceBlock;
import dev.jstech.computers.blockentity.HubBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.blockentity.RedstoneInterfaceBlockEntity;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestCables;
import dev.jstech.tests.testkit.TestWorldBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * The Redstone Interface: linked to a computer, it reads the signal coming into its lens's face, or emits its strength
 * there and nowhere else; with no computer it reads and emits nothing; it takes a device port; and its name is unique
 * among its computer's interfaces.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class RedstoneInterfaceGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos PC = new BlockPos(2, 2, 2);
    /** The interface east of the computer, its back against it and its lens facing east. */
    private static final BlockPos SENSOR = PC.east();
    private static final BlockPos FRONT = SENSOR.east();
    /* Long enough for the interface to link and settle, and a little more. */
    private static final int SETTLE_TICKS = 10;

    private RedstoneInterfaceGameTests() {
    }

    /** Linked, it reads the signal at its lens, and nothing once the source is gone. */
    @GameTest(template = ARENA)
    public static void linked_readsTheSignalAtItsLens(final GameTestHelper helper) {
        vintagePc(helper);
        placeSensor(helper, SENSOR, Direction.EAST);
        helper.setBlock(FRONT, Blocks.REDSTONE_BLOCK);
        helper.startSequence()
                .thenExecuteAfter(SETTLE_TICKS, () -> {
                    final RedstoneInterfaceBlockEntity sensor = sensor(helper, SENSOR);
                    helper.assertTrue(sensor.live(), "the interface links to the computer against it");
                    helper.assertTrue(sensor.reading() == 15,
                            "it reads the block of redstone; got " + sensor.reading());
                    helper.assertTrue(sensor.shownStrength() == 15, "its lens shows it");
                })
                .thenExecute(() -> helper.setBlock(FRONT, Blocks.AIR))
                .thenExecuteAfter(SETTLE_TICKS, () -> helper.assertTrue(sensor(helper, SENSOR).reading() == 0,
                        "and nothing once it is gone"))
                .thenSucceed();
    }

    /** It reads only the face its lens is on: a source beside it is not read. */
    @GameTest(template = ARENA)
    public static void reads_onlyAtItsLens(final GameTestHelper helper) {
        vintagePc(helper);
        placeSensor(helper, SENSOR, Direction.EAST);
        helper.setBlock(SENSOR.north(), Blocks.REDSTONE_BLOCK);
        helper.startSequence()
                .thenExecuteAfter(SETTLE_TICKS, () -> helper.assertTrue(sensor(helper, SENSOR).reading() == 0,
                        "a source beside the lens is not read"))
                .thenSucceed();
    }

    /** With no computer it has no power: it reads nothing, and set to emit, emits nothing. */
    @GameTest(template = ARENA)
    public static void withoutAComputer_readsAndEmitsNothing(final GameTestHelper helper) {
        final BlockPos alone = new BlockPos(5, 2, 5);
        placeSensor(helper, alone, Direction.EAST);
        helper.setBlock(alone.east(), Blocks.REDSTONE_BLOCK);
        final BlockPos lamp = alone.west();
        helper.startSequence()
                .thenExecuteAfter(SETTLE_TICKS, () -> {
                    helper.assertTrue(!sensor(helper, alone).live(), "no computer links it");
                    helper.assertTrue(sensor(helper, alone).reading() == 0, "it reads nothing");
                })
                .thenExecute(() -> {
                    helper.setBlock(alone.east(), Blocks.AIR);
                    placeSensor(helper, alone, Direction.WEST);
                    helper.setBlock(lamp, Blocks.REDSTONE_LAMP);
                    sensor(helper, alone).emit(15, "");
                })
                .thenExecuteAfter(SETTLE_TICKS, () -> {
                    helper.assertTrue(sensor(helper, alone).output() == 0, "set to emit, it emits nothing");
                    helper.assertTrue(!helper.getBlockState(lamp).getValue(RedstoneLampBlock.LIT),
                            "the lamp at its lens stays dark");
                })
                .thenSucceed();
    }

    /** Emitting, it powers the block at its lens with its strength, and nothing beside it. */
    @GameTest(template = ARENA)
    public static void emits_itsStrengthAtItsLensOnly(final GameTestHelper helper) {
        vintagePc(helper);
        placeSensor(helper, SENSOR, Direction.EAST);
        helper.setBlock(FRONT, Blocks.REDSTONE_WIRE);
        final BlockPos beside = SENSOR.north();
        helper.setBlock(beside, Blocks.REDSTONE_LAMP);
        helper.startSequence()
                .thenExecuteAfter(SETTLE_TICKS, () -> sensor(helper, SENSOR).emit(5, ""))
                .thenExecuteAfter(SETTLE_TICKS, () -> {
                    final int wire = helper.getBlockState(FRONT).getValue(RedStoneWireBlock.POWER);
                    helper.assertTrue(wire == 5, "the wire at its lens carries its strength; got " + wire);
                    helper.assertTrue(!helper.getBlockState(beside).getValue(RedstoneLampBlock.LIT),
                            "a lamp beside it stays dark");
                })
                .thenExecute(() -> sensor(helper, SENSOR).emit(15, ""))
                .thenExecuteAfter(SETTLE_TICKS, () -> helper.assertTrue(
                        helper.getBlockState(FRONT).getValue(RedStoneWireBlock.POWER) == 15,
                        "and follows it when it changes"))
                .thenExecute(() -> sensor(helper, SENSOR).read(""))
                .thenExecuteAfter(SETTLE_TICKS, () -> helper.assertTrue(
                        helper.getBlockState(FRONT).getValue(RedStoneWireBlock.POWER) == 0,
                        "set to read, it emits nothing"))
                .thenSucceed();
    }

    /** Its computer taken away, it goes quiet: the lamp it was lighting goes dark. */
    @GameTest(template = ARENA)
    public static void lostComputer_goesQuiet(final GameTestHelper helper) {
        vintagePc(helper);
        placeSensor(helper, SENSOR, Direction.EAST);
        helper.setBlock(FRONT, Blocks.REDSTONE_LAMP);
        helper.startSequence()
                .thenExecuteAfter(SETTLE_TICKS, () -> sensor(helper, SENSOR).emit(15, ""))
                .thenExecuteAfter(SETTLE_TICKS, () -> helper.assertTrue(
                        helper.getBlockState(FRONT).getValue(RedstoneLampBlock.LIT), "the lamp lights"))
                .thenExecute(() -> helper.setBlock(PC, Blocks.AIR))
                .thenExecuteAfter(SETTLE_TICKS, () -> {
                    helper.assertTrue(!sensor(helper, SENSOR).live(), "the interface is unlinked");
                    helper.assertTrue(!helper.getBlockState(FRONT).getValue(RedstoneLampBlock.LIT),
                            "and the lamp goes dark");
                    helper.assertTrue(sensor(helper, SENSOR).emits() && sensor(helper, SENSOR).strength() == 15,
                            "keeping its setting for when it is cabled again");
                })
                .thenSucceed();
    }

    /**
     * It takes its cable on its back, where its wires go into a gland, whichever of the six ways it faces; a cable
     * against another face is no link.
     */
    @GameTest(template = ARENA)
    public static void linksOverACableOnItsBackOnly(final GameTestHelper helper) {
        TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(PC);
        // Two cables east, and the interface after them with its back to the last.
        TestCables.lay(helper, PC.east(), ComputingModule.VINTAGE_PERIPHERAL_CABLE);
        TestCables.lay(helper, PC.east(2), ComputingModule.VINTAGE_PERIPHERAL_CABLE);
        final BlockPos east = PC.east(3);
        placeSensor(helper, east, Direction.EAST);
        // A cable up from the computer, and one looking up with its back on it.
        TestCables.lay(helper, PC.above(), ComputingModule.VINTAGE_PERIPHERAL_CABLE);
        final BlockPos up = PC.above(2);
        placeSensor(helper, up, Direction.UP);
        // A cable north, and one beside it looking east: the cable touches its side, not its back.
        TestCables.lay(helper, PC.north(), ComputingModule.VINTAGE_PERIPHERAL_CABLE);
        final BlockPos beside = PC.north(2);
        placeSensor(helper, beside, Direction.EAST);
        helper.startSequence()
                .thenExecuteAfter(SETTLE_TICKS, () -> {
                    helper.assertTrue(sensor(helper, east).live(), "the run to its back links it");
                    helper.assertTrue(sensor(helper, up).live(), "looking up, its back is below it");
                    helper.assertTrue(!sensor(helper, beside).live(), "a cable at its side does not");
                })
                .thenSucceed();
    }

    /** A row of interfaces hangs from a chain of hubs, each against the hub behind it, each hub on the next. */
    @GameTest(template = ARENA)
    public static void row_hangsFromAChainOfHubs(final GameTestHelper helper) {
        final BlockPos computer = new BlockPos(6, 2, 3);
        TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(computer);
        final int count = 5;
        for (int i = 1; i <= count; i++) {
            helper.setBlock(computer.west(i), ComputingModule.STANDARD_HUB.get());
            placeSensor(helper, computer.west(i).north(), Direction.NORTH);
        }
        helper.startSequence()
                .thenExecuteAfter(SETTLE_TICKS * 2, () -> {
                    for (int i = 1; i <= count; i++) {
                        final boolean hubLinked = helper.getBlockEntity(computer.west(i)) instanceof HubBlockEntity hub
                                && hub.ownerPos() != null;
                        helper.assertTrue(hubLinked, "the hub " + i + " west of the computer links");
                        helper.assertTrue(sensor(helper, computer.west(i).north()).live(),
                                "the interface against hub " + i + " links");
                    }
                })
                .thenSucceed();
    }

    /** It takes a device port: with the Vintage board's two taken by drives, it waits. */
    @GameTest(template = ARENA)
    public static void takesADevicePort(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = vintagePc(helper);
        helper.setBlock(PC.west(), ComputingModule.FLOPPY_DRIVE.get());
        helper.setBlock(PC.south(), ComputingModule.FLOPPY_DRIVE.get());
        helper.startSequence()
                .thenExecuteAfter(SETTLE_TICKS, () -> placeSensor(helper, SENSOR, Direction.EAST))
                .thenExecuteAfter(SETTLE_TICKS, () -> {
                    helper.assertTrue(computer.linkedEndpoints().size() == 2, "the drives took both ports");
                    helper.assertTrue(!sensor(helper, SENSOR).live(), "the interface waits for a free one");
                })
                .thenExecute(() -> helper.setBlock(PC.south(), Blocks.AIR))
                .thenExecuteAfter(SETTLE_TICKS, () -> helper.assertTrue(sensor(helper, SENSOR).live(),
                        "and takes the one a drive leaves"))
                .thenSucceed();
    }

    /** A name another interface of the computer has, whatever its case, is refused; unnamed, it answers its word. */
    @GameTest(template = ARENA)
    public static void rename_refusesANameTaken(final GameTestHelper helper) {
        vintagePc(helper);
        placeSensor(helper, SENSOR, Direction.EAST);
        final BlockPos other = PC.north();
        placeSensor(helper, other, Direction.NORTH);
        helper.startSequence()
                .thenExecuteAfter(SETTLE_TICKS, () -> {
                    helper.assertTrue(sensor(helper, SENSOR).answersTo().equals("Redstone Interface"),
                            "unnamed, it answers to the word for it");
                    helper.assertTrue(sensor(helper, SENSOR).rename("Door"), "a free name is taken");
                    helper.assertTrue(!sensor(helper, other).rename("door"), "the same in other letters is refused");
                    helper.assertTrue(sensor(helper, other).name().isEmpty(), "and it keeps the name it had");
                    helper.assertTrue(sensor(helper, other).rename("Gate"), "another name is taken");
                })
                .thenSucceed();
    }

    /* A Vintage personal computer at PC, its board's two device ports free. */
    private static PersonalComputerBlockEntity vintagePc(final GameTestHelper helper) {
        helper.setBlock(PC, ComputingModule.VINTAGE_PERSONAL_COMPUTER.get());
        if (!(helper.getBlockEntity(PC) instanceof PersonalComputerBlockEntity computer)) {
            throw new IllegalStateException("no computer at " + PC);
        }
        final ItemStackHandler hardware = computer.getHardware();
        hardware.setStackInSlot(PersonalComputerBlockEntity.MOTHERBOARD_SLOT,
                new ItemStack(HardwareItems.MOTHERBOARD_BABYAT_VINTAGE.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.CPU_SLOT,
                new ItemStack(HardwareItems.CPU_INTEGRA_486SX.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.RAM_SLOTS_START,
                new ItemStack(HardwareItems.RAM_SIMM_4.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.PSU_SLOT, new ItemStack(HardwareItems.PSU_300.get()));
        return computer;
    }

    private static void placeSensor(final GameTestHelper helper, final BlockPos at, final Direction lens) {
        final Block block = ComputingModule.VINTAGE_REDSTONE_INTERFACE.get();
        final BlockState state = block.defaultBlockState().setValue(RedstoneInterfaceBlock.FACING, lens);
        helper.setBlock(at, state);
    }

    private static RedstoneInterfaceBlockEntity sensor(final GameTestHelper helper, final BlockPos at) {
        if (!(helper.getBlockEntity(at) instanceof RedstoneInterfaceBlockEntity sensor)) {
            throw new IllegalStateException("no Redstone Interface at " + at);
        }
        return sensor;
    }
}
