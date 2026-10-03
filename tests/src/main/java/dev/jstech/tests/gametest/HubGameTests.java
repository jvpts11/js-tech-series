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
import dev.jstech.computers.blockentity.HubBlockEntity;
import dev.jstech.computers.blockentity.MonitorBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.os.media.MediaReaderBlockEntity;
import dev.jstech.core.peripheral.PortKind;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestCables;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.util.OptionalLong;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * The hubs of the peripheral line: a hub takes one of its computer's device ports and offers its own, the devices
 * cabled to it taking those; a full hub leaves the next device waiting; no screen passes through one; the cable after
 * a hub reaches as far again; a hub may hang from another; and a hub taken away drops the devices behind it.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class HubGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos PC = new BlockPos(2, 2, 2);
    /* Long enough for a hub to link, then the devices behind it, and a little more. */
    private static final int LINK_TICKS = 10;

    private HubGameTests() {
    }

    /** The Vintage board's two ports: the hub takes one and offers two, and a third device behind it waits. */
    @GameTest(template = ARENA)
    public static void vintageHub_offersTwoPortsForTheOneItTakes(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = vintagePc(helper);
        final BlockPos hub = PC.east();
        helper.setBlock(hub, ComputingModule.VINTAGE_HUB.get());
        final BlockPos onTheBoard = PC.west();
        helper.setBlock(onTheBoard, ComputingModule.FLOPPY_DRIVE.get());
        final BlockPos[] behind = {hub.east(), hub.south(), hub.north()};
        for (final BlockPos drive : behind) {
            helper.setBlock(drive, ComputingModule.FLOPPY_DRIVE.get());
        }
        helper.startSequence()
                .thenExecuteAfter(LINK_TICKS, () -> {
                    final long hubAt = helper.absolutePos(hub).asLong();
                    helper.assertTrue(hubLinked(helper, hub), "the hub links on one of the board's ports");
                    helper.assertTrue(driveLinked(helper, onTheBoard), "a drive takes the board's other port");
                    helper.assertTrue(computer.portsInUse(PortKind.DEVICE) == 2, "the board's two ports are taken");
                    helper.assertTrue(linkedDrives(helper, behind) == 2,
                            "two drives take the hub's two ports and the third waits; linked "
                                    + linkedDrives(helper, behind));
                    helper.assertTrue(computer.portsInUseThrough(hubAt) == 2, "both on the hub's ports");
                })
                .thenSucceed();
    }

    /** A monitor against a hub is not linked, as no hub carries a video signal; a drive beside it is. */
    @GameTest(template = ARENA)
    public static void hub_passesNoScreen(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer =
                TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(PC);
        final BlockPos hub = PC.east();
        helper.setBlock(hub, ComputingModule.STANDARD_HUB.get());
        final BlockPos monitor = hub.east();
        helper.setBlock(monitor, facing(ComputingModule.MONITOR.get(), Direction.EAST));
        final BlockPos drive = hub.south();
        helper.setBlock(drive, ComputingModule.DVD_DRIVE.get());
        helper.startSequence()
                .thenExecuteAfter(LINK_TICKS, () -> {
                    helper.assertTrue(computer.ports(PortKind.VIDEO) > 0, "the computer has a video output free");
                    helper.assertTrue(!(helper.getBlockEntity(monitor) instanceof MonitorBlockEntity screen
                            && screen.ownerPos() != null), "the monitor behind the hub is not linked");
                    helper.assertTrue(driveLinked(helper, drive), "the drive behind the hub is");
                })
                .thenSucceed();
    }

    /** Eight Vintage cables reach the hub, and the cable after it counts afresh, so a ninth reaches a drive. */
    @GameTest(template = ARENA)
    public static void hub_startsTheCablesReachAgain(final GameTestHelper helper) {
        vintagePc(helper);
        // Five cables east, three south: eight, the whole reach of the Vintage cable, then the hub.
        for (int x = 3; x <= 7; x++) {
            TestCables.lay(helper, new BlockPos(x, 2, 2), ComputingModule.VINTAGE_PERIPHERAL_CABLE);
        }
        for (int z = 3; z <= 5; z++) {
            TestCables.lay(helper, new BlockPos(7, 2, z), ComputingModule.VINTAGE_PERIPHERAL_CABLE);
        }
        final BlockPos hub = new BlockPos(6, 2, 5);
        helper.setBlock(hub, ComputingModule.VINTAGE_HUB.get());
        TestCables.lay(helper, new BlockPos(5, 2, 5), ComputingModule.VINTAGE_PERIPHERAL_CABLE);
        // Facing west, its back and its port toward the cable.
        final BlockPos drive = new BlockPos(4, 2, 5);
        helper.setBlock(drive, facing(ComputingModule.FLOPPY_DRIVE.get(), Direction.WEST));
        helper.startSequence()
                .thenExecuteAfter(LINK_TICKS, () -> {
                    helper.assertTrue(hubLinked(helper, hub), "eight cables reach the hub");
                    helper.assertTrue(driveLinked(helper, drive), "and one more after it reaches the drive");
                })
                .thenSucceed();
    }

    /** A hub hangs from another, and the devices behind the second take its ports. */
    @GameTest(template = ARENA)
    public static void hub_hangsFromAnotherHub(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = vintagePc(helper);
        final BlockPos first = PC.east();
        final BlockPos second = first.east();
        helper.setBlock(first, ComputingModule.VINTAGE_HUB.get());
        helper.setBlock(second, ComputingModule.VINTAGE_HUB.get());
        final BlockPos[] behind = {second.east(), second.south()};
        for (final BlockPos drive : behind) {
            helper.setBlock(drive, ComputingModule.FLOPPY_DRIVE.get());
        }
        helper.startSequence()
                .thenExecuteAfter(LINK_TICKS, () -> {
                    final long firstAt = helper.absolutePos(first).asLong();
                    final long secondAt = helper.absolutePos(second).asLong();
                    helper.assertTrue(hubLinked(helper, second), "the second hub links");
                    helper.assertTrue(computer.hubOf(secondAt).equals(OptionalLong.of(firstAt)),
                            "on a port of the first");
                    helper.assertTrue(linkedDrives(helper, behind) == 2, "both drives behind the second link");
                    helper.assertTrue(computer.portsInUseThrough(secondAt) == 2, "on the second hub's ports");
                    helper.assertTrue(computer.portsInUse(PortKind.DEVICE) == 1, "the board gives only the first");
                })
                .thenSucceed();
    }

    /** Taking the hub away drops the devices behind it and frees the board's port. */
    @GameTest(template = ARENA)
    public static void brokenHub_dropsTheDevicesBehindIt(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = vintagePc(helper);
        final BlockPos hub = PC.east();
        helper.setBlock(hub, ComputingModule.VINTAGE_HUB.get());
        final BlockPos drive = hub.east();
        helper.setBlock(drive, ComputingModule.FLOPPY_DRIVE.get());
        helper.startSequence()
                .thenExecuteAfter(LINK_TICKS, () -> helper.assertTrue(driveLinked(helper, drive),
                        "the drive links through the hub"))
                .thenExecute(() -> helper.setBlock(hub, Blocks.AIR))
                .thenExecuteAfter(LINK_TICKS, () -> {
                    helper.assertTrue(!driveLinked(helper, drive), "the drive is dropped with the hub");
                    helper.assertTrue(computer.linkedEndpoints().isEmpty(), "and the computer holds nothing");
                })
                .thenSucceed();
    }

    /* A Vintage personal computer at PC, its board's two device ports free. */
    private static PersonalComputerBlockEntity vintagePc(final GameTestHelper helper) {
        helper.setBlock(PC, ComputingModule.VINTAGE_PERSONAL_COMPUTER.get());
        final PersonalComputerBlockEntity computer = entity(helper, PC, PersonalComputerBlockEntity.class);
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

    private static BlockState facing(final Block block, final Direction facing) {
        return block.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, facing);
    }

    private static boolean hubLinked(final GameTestHelper helper, final BlockPos at) {
        return helper.getBlockEntity(at) instanceof HubBlockEntity hub
                && helper.absolutePos(PC).equals(hub.ownerPos());
    }

    private static boolean driveLinked(final GameTestHelper helper, final BlockPos at) {
        return helper.getBlockEntity(at) instanceof MediaReaderBlockEntity drive
                && helper.absolutePos(PC).equals(drive.ownerPos());
    }

    private static int linkedDrives(final GameTestHelper helper, final BlockPos[] drives) {
        int linked = 0;
        for (final BlockPos drive : drives) {
            if (driveLinked(helper, drive)) {
                linked++;
            }
        }
        return linked;
    }

    private static <T> T entity(final GameTestHelper helper, final BlockPos at, final Class<T> type) {
        final Object entity = helper.getBlockEntity(at);
        if (!type.isInstance(entity)) {
            throw new IllegalStateException("no " + type.getSimpleName() + " at " + at);
        }
        return type.cast(entity);
    }
}
