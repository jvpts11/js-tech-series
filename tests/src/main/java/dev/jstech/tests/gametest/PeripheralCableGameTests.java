/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.MonitorBlockEntity;
import dev.jstech.computers.blockentity.PatternEncoderBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.os.media.MediaReaderBlockEntity;
import dev.jstech.core.cable.CableBlockEntity;
import dev.jstech.core.cable.CableType;
import dev.jstech.core.multipart.ModelLayout;
import dev.jstech.core.multipart.PlacedModel;
import dev.jstech.core.peripheral.PortKind;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestCables;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The peripheral cables: one for each era, in the Core's cable block, each reaching its era's run. A port takes its
 * era's cable and every earlier one, so a newer computer takes an older device's cable and an older device never takes
 * a newer cable, the drives and the encoders alike; cables of two eras never join; and each cable ends in the plug of
 * the port it enters.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class PeripheralCableGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos PC = new BlockPos(0, 2, 0);
    /* Long enough for a peripheral's next tick to try its link, and a little more. */
    private static final int LINK_TICKS = 10;

    private PeripheralCableGameTests() {
    }

    /** A Standard computer takes the cables of its era and of every earlier one, and a monitor links over each. */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void standardComputer_takesItsErasCableAndEveryEarlierOne(final GameTestHelper helper) {
        final List<Supplier<CableType>> cables = List.of(ComputingModule.VINTAGE_PERIPHERAL_CABLE,
                ComputingModule.LEGACY_PERIPHERAL_CABLE, ComputingModule.TRANSITION_PERIPHERAL_CABLE,
                ComputingModule.PERIPHERAL_CABLE);
        final PersonalComputerBlockEntity computer = standardPcWithCard(helper);
        // Each monitor on its own run off a face of the computer, a monitor taking every era's cable.
        final BlockPos[][] runs = {
                {new BlockPos(1, 2, 0), new BlockPos(2, 2, 0)},
                {new BlockPos(0, 2, 1), new BlockPos(0, 2, 2)},
                {new BlockPos(0, 3, 0), new BlockPos(0, 4, 0)},
                {new BlockPos(1, 2, 1), new BlockPos(2, 2, 1)}};
        final BlockPos[] monitors = {new BlockPos(3, 2, 0), new BlockPos(0, 2, 3), new BlockPos(1, 4, 0),
                new BlockPos(3, 2, 1)};
        // A monitor faces out of its back, where its port is: each one's faces its run.
        final Direction[] facing = {Direction.WEST, Direction.NORTH, Direction.WEST, Direction.WEST};
        // The fourth run does not touch the computer, only the first two runs, which are of other eras and never join
        // it, so its monitor stays unlinked.
        for (int i = 0; i < cables.size(); i++) {
            for (final BlockPos at : runs[i]) {
                TestCables.lay(helper, at, cables.get(i));
            }
            helper.setBlock(monitors[i], monitorFacing(facing[i]));
        }
        helper.startSequence()
                .thenExecuteAfter(LINK_TICKS, () -> {
                    helper.assertTrue(linked(helper, monitors[0]), "a monitor over the Vintage cable links");
                    helper.assertTrue(linked(helper, monitors[1]), "a monitor over the Legacy cable links");
                    helper.assertTrue(linked(helper, monitors[2]), "a monitor over the Transition cable links");
                    helper.assertTrue(!linked(helper, monitors[3]),
                            "a run of one era never joins a run of another, so the last monitor is not reached");
                    helper.assertTrue(computer.portsInUse(PortKind.VIDEO) == 3, "three video outputs in use");
                })
                .thenSucceed();
    }

    /** A Standard computer never takes the Advanced cable: a port takes no newer cable than its era's. */
    @GameTest(template = ARENA)
    public static void standardComputer_neverTakesANewerCable(final GameTestHelper helper) {
        standardPcWithCard(helper);
        TestCables.lay(helper, new BlockPos(1, 2, 0), ComputingModule.ADVANCED_PERIPHERAL_CABLE);
        final BlockPos monitor = new BlockPos(2, 2, 0);
        helper.setBlock(monitor, monitorFacing(Direction.WEST));
        helper.startSequence()
                .thenExecuteAfter(LINK_TICKS, () -> {
                    helper.assertTrue(!linked(helper, monitor), "the Advanced cable does not reach the computer");
                    helper.assertTrue(!cable(helper, new BlockPos(1, 2, 0)).crosses(
                                    ComputingModule.ADVANCED_PERIPHERAL_CABLE.get(), Direction.WEST),
                            "and does not plug into it");
                })
                .thenSucceed();
    }

    /** A monitor takes its cable on its back, never on its glass, and the cable is drawn plugged into that back. */
    @GameTest(template = ARENA)
    public static void monitor_takesTheCableOnlyOnItsBack(final GameTestHelper helper) {
        standardPcWithCard(helper);
        final BlockPos run = new BlockPos(1, 2, 0);
        TestCables.lay(helper, run, ComputingModule.PERIPHERAL_CABLE);
        final BlockPos monitor = new BlockPos(2, 2, 0);
        // A monitor faces out of its back, so facing east its glass is turned to the cable.
        helper.setBlock(monitor, monitorFacing(Direction.EAST));
        helper.startSequence()
                .thenExecuteAfter(LINK_TICKS, () -> {
                    helper.assertTrue(!linked(helper, monitor), "a cable against the glass does not link the monitor");
                    helper.assertTrue(!plugsAt(helper, run, Direction.EAST).contains(plug("hdmi")),
                            "nor plugs into the glass; got " + plugsAt(helper, run, Direction.EAST));
                })
                .thenExecute(() -> helper.setBlock(monitor, monitorFacing(Direction.WEST)))
                .thenExecuteAfter(LINK_TICKS, () -> {
                    helper.assertTrue(linked(helper, monitor), "turned with its back to the cable, it links");
                    helper.assertTrue(plugsAt(helper, run, Direction.EAST).contains(plug("hdmi")),
                            "and the cable plugs into its back; got " + plugsAt(helper, run, Direction.EAST));
                })
                .thenSucceed();
    }

    /** A Vintage drive takes only the Vintage cable on its back. */
    @GameTest(template = ARENA)
    public static void vintageDrive_neverTakesANewerCable(final GameTestHelper helper) {
        standardPcWithCard(helper);
        final BlockPos olderRun = new BlockPos(1, 2, 0);
        final BlockPos olderDrive = new BlockPos(2, 2, 0);
        final BlockPos newerRun = new BlockPos(0, 2, 1);
        final BlockPos newerDrive = new BlockPos(0, 2, 2);
        TestCables.lay(helper, olderRun, ComputingModule.VINTAGE_PERIPHERAL_CABLE);
        helper.setBlock(olderDrive, facingBlock(ComputingModule.FLOPPY_DRIVE.get(), Direction.EAST));
        TestCables.lay(helper, newerRun, ComputingModule.LEGACY_PERIPHERAL_CABLE);
        helper.setBlock(newerDrive, facingBlock(ComputingModule.FLOPPY_DRIVE.get(), Direction.SOUTH));
        helper.startSequence()
                .thenExecuteAfter(LINK_TICKS, () -> {
                    helper.assertTrue(driveLinked(helper, olderDrive), "the Vintage cable links the floppy drive");
                    helper.assertTrue(!driveLinked(helper, newerDrive), "the Legacy cable does not fit it");
                })
                .thenSucceed();
    }

    /**
     * A Pattern Encoder takes the device port of its era on its back: the cable of its era and every earlier one, as
     * the drives do, and never a newer one.
     */
    @GameTest(template = ARENA)
    public static void encoders_takeTheirErasCableAndEveryEarlierOne(final GameTestHelper helper) {
        standardPcWithCard(helper);
        // A Legacy cable east to a Legacy encoder, its back to the cable.
        TestCables.lay(helper, new BlockPos(1, 2, 0), ComputingModule.LEGACY_PERIPHERAL_CABLE);
        final BlockPos ownEra = new BlockPos(2, 2, 0);
        helper.setBlock(ownEra, facingBlock(ComputingModule.LEGACY_PATTERN_ENCODER.get(), Direction.EAST));
        // A Transition cable south to another Legacy encoder: a newer cable than its port takes.
        TestCables.lay(helper, new BlockPos(0, 2, 1), ComputingModule.TRANSITION_PERIPHERAL_CABLE);
        final BlockPos newer = new BlockPos(0, 2, 2);
        helper.setBlock(newer, facingBlock(ComputingModule.LEGACY_PATTERN_ENCODER.get(), Direction.SOUTH));
        // A Vintage cable up and over to a Standard encoder: an older cable, which its port takes.
        TestCables.lay(helper, new BlockPos(0, 3, 0), ComputingModule.VINTAGE_PERIPHERAL_CABLE);
        TestCables.lay(helper, new BlockPos(0, 3, 1), ComputingModule.VINTAGE_PERIPHERAL_CABLE);
        final BlockPos older = new BlockPos(0, 3, 2);
        helper.setBlock(older, facingBlock(ComputingModule.PATTERN_ENCODER.get(), Direction.SOUTH));
        helper.startSequence()
                .thenExecuteAfter(LINK_TICKS, () -> {
                    helper.assertTrue(encoderLinked(helper, ownEra), "the Legacy encoder takes the Legacy cable");
                    helper.assertTrue(!encoderLinked(helper, newer), "and not the Transition one");
                    helper.assertTrue(encoderLinked(helper, older), "the Standard encoder takes the Vintage cable");
                })
                .thenSucceed();
    }

    /** A run reaches its era's cables and no more: eight for the Vintage cable, so a ninth is too far. */
    @GameTest(template = ARENA)
    public static void vintageRun_reachesEightCables(final GameTestHelper helper) {
        standardPcWithCard(helper);
        // Eight cables east along the first row, then a drive with its back to the last.
        for (int x = 1; x <= 7; x++) {
            TestCables.lay(helper, new BlockPos(x, 2, 0), ComputingModule.VINTAGE_PERIPHERAL_CABLE);
        }
        TestCables.lay(helper, new BlockPos(7, 2, 1), ComputingModule.VINTAGE_PERIPHERAL_CABLE);
        final BlockPos near = new BlockPos(7, 2, 2);
        helper.setBlock(near, facingBlock(ComputingModule.FLOPPY_DRIVE.get(), Direction.SOUTH));
        helper.startSequence()
                .thenExecuteAfter(LINK_TICKS, () -> helper.assertTrue(driveLinked(helper, near),
                        "eight cables are within the Vintage cable's reach"))
                // One cable more puts the drive past the reach.
                .thenExecute(() -> {
                    helper.setBlock(near, Blocks.AIR);
                    TestCables.lay(helper, near, ComputingModule.VINTAGE_PERIPHERAL_CABLE);
                    helper.setBlock(new BlockPos(7, 2, 3),
                            facingBlock(ComputingModule.FLOPPY_DRIVE.get(), Direction.SOUTH));
                })
                .thenExecuteAfter(LINK_TICKS, () -> helper.assertTrue(!driveLinked(helper, new BlockPos(7, 2, 3)),
                        "nine cables are past it"))
                .thenSucceed();
    }

    /** The cable ends in the plug of the port it enters: the video plug at a screen, the device plug at a drive. */
    @GameTest(template = ARENA)
    public static void plug_isThePortsOwn(final GameTestHelper helper) {
        standardPcWithCard(helper);
        final BlockPos toScreen = new BlockPos(1, 2, 0);
        final BlockPos toDrive = new BlockPos(0, 2, 1);
        TestCables.lay(helper, toScreen, ComputingModule.PERIPHERAL_CABLE);
        helper.setBlock(new BlockPos(2, 2, 0), monitorFacing(Direction.WEST));
        TestCables.lay(helper, toDrive, ComputingModule.PERIPHERAL_CABLE);
        helper.setBlock(new BlockPos(0, 2, 2), facingBlock(ComputingModule.DVD_DRIVE.get(), Direction.SOUTH));
        helper.startSequence()
                .thenExecuteAfter(LINK_TICKS, () -> {
                    helper.assertTrue(plugsAt(helper, toScreen, Direction.EAST).contains(plug("hdmi")),
                            "an HDMI plug into the screen; got " + plugsAt(helper, toScreen, Direction.EAST));
                    helper.assertTrue(plugsAt(helper, toDrive, Direction.SOUTH).contains(plug("usb3")),
                            "a USB 3 plug into the drive; got " + plugsAt(helper, toDrive, Direction.SOUTH));
                    helper.assertTrue(plugsAt(helper, toScreen, Direction.WEST).contains(plug("usb3")),
                            "and the device plug into the computer; got " + plugsAt(helper, toScreen, Direction.WEST));
                })
                .thenSucceed();
    }

    /* A Standard personal computer with a graphics card, for its video outputs. */
    private static PersonalComputerBlockEntity standardPcWithCard(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer =
                TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(PC);
        computer.getHardware().setStackInSlot(PersonalComputerBlockEntity.GPU_SLOTS_START,
                new ItemStack(ComputingModule.GPU_HD_7970.get()));
        return computer;
    }

    private static BlockState monitorFacing(final Direction facing) {
        return facingBlock(ComputingModule.MONITOR.get(), facing);
    }

    private static BlockState facingBlock(final Block block, final Direction facing) {
        return block.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, facing);
    }

    private static boolean linked(final GameTestHelper helper, final BlockPos at) {
        return helper.getBlockEntity(at) instanceof MonitorBlockEntity monitor
                && helper.absolutePos(PC).equals(monitor.ownerPos());
    }

    private static boolean encoderLinked(final GameTestHelper helper, final BlockPos at) {
        return helper.getBlockEntity(at) instanceof PatternEncoderBlockEntity encoder
                && helper.absolutePos(PC).equals(encoder.ownerPos());
    }

    private static boolean driveLinked(final GameTestHelper helper, final BlockPos at) {
        return helper.getBlockEntity(at) instanceof MediaReaderBlockEntity drive
                && helper.absolutePos(PC).equals(drive.ownerPos());
    }

    private static CableBlockEntity cable(final GameTestHelper helper, final BlockPos at) {
        return TestCables.cable(helper, at);
    }

    /* The plug models the cable at {@code at} draws on {@code face}. */
    private static List<ResourceLocation> plugsAt(final GameTestHelper helper, final BlockPos at,
                                                  final Direction face) {
        final ModelLayout layout = cable(helper, at).getModelData().get(ModelLayout.PROPERTY);
        return layout == null ? List.of() : layout.models().stream()
                .filter(placed -> placed.facing() == face)
                .map(PlacedModel::model)
                .toList();
    }

    private static ResourceLocation plug(final String name) {
        return ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "block/cable/plug/" + name);
    }
}
