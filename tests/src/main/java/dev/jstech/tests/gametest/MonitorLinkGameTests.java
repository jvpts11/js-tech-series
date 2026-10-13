/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.block.MonitorBlock;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.MonitorBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestCables;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static dev.jstech.tests.testkit.NetworkFixtures.formRunningMainframe;
import static dev.jstech.tests.testkit.NetworkFixtures.monitorFacing;
import static dev.jstech.tests.testkit.NetworkFixtures.placeRunningPC;

/**
 * GameTests for a monitor linking to a computer by cable: auto-linking and unlinking, the screen lighting up, and
 * linking through a Mainframe part face.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class MonitorLinkGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 4;

    private MonitorLinkGameTests() {
    }

    @GameTest(template = ARENA)
    public static void monitor_autoLinksAndUnlinksOnCableBreak(final GameTestHelper helper) {
        final BlockPos pc = new BlockPos(1, 2, 2);
        final BlockPos cable = new BlockPos(2, 2, 2);
        final BlockPos mon = new BlockPos(3, 2, 2);
        final PersonalComputerBlockEntity computer = placeRunningPC(helper, pc);
        // A Standard graphics card gives the computer four video outputs, one for each monitor.
        computer.getHardware().setStackInSlot(PersonalComputerBlockEntity.GPU_SLOTS_START,
                new ItemStack(ComputingModule.GPU_HD_7970.get()));
        TestCables.lay(helper, cable, ComputingModule.PERIPHERAL_CABLE);
        // Facing west, out of its back, where its video port is, it takes the cable.
        helper.setBlock(mon, monitorFacing(Direction.WEST));
        if (!(helper.getBlockEntity(mon) instanceof MonitorBlockEntity monitor)) {
            helper.fail("no monitor");
            return;
        }
        helper.startSequence()
                .thenExecuteAfter(25, () -> {
                    helper.assertTrue(monitor.ownerPos() != null
                                    && monitor.ownerPos().equals(helper.absolutePos(pc)),
                            "monitor should auto-link to the PC over the peripheral cable");
                    helper.assertTrue(computer.linkedEndpoints().contains(helper.absolutePos(mon).asLong()),
                            "PC should list the monitor as a linked endpoint");
                })
                .thenExecute(() -> helper.setBlock(cable, Blocks.AIR))
                .thenExecuteAfter(25, () -> {
                    helper.assertTrue(monitor.ownerPos() == null,
                            "monitor should unlink when the peripheral cable is cut");
                    helper.assertTrue(computer.linkedEndpoints().isEmpty(),
                            "PC should drop the endpoint after the cable is cut");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void monitor_screenLightsUpAfterLinkingAndDarkensOnCut(final GameTestHelper helper) {
        final BlockPos pc = new BlockPos(1, 2, 2);
        final BlockPos cable = new BlockPos(2, 2, 2);
        final BlockPos mon = new BlockPos(3, 2, 2);
        final PersonalComputerBlockEntity computer = placeRunningPC(helper, pc);
        computer.getHardware().setStackInSlot(PersonalComputerBlockEntity.GPU_SLOTS_START,
                new ItemStack(ComputingModule.GPU_HD_7970.get()));
        TestCables.lay(helper, cable, ComputingModule.PERIPHERAL_CABLE);
        helper.setBlock(mon, monitorFacing(Direction.WEST));
        helper.startSequence()
                // The link is near-instant; the screen boots ~20 ticks later, so by 30 ticks it is lit.
                .thenExecuteAfter(30, () -> helper.assertTrue(
                        helper.getBlockState(mon).getValue(MonitorBlock.LIT),
                        "monitor screen should be lit after the boot delay once linked"))
                .thenExecute(() -> helper.setBlock(cable, Blocks.AIR))
                .thenExecuteAfter(SETTLE, () -> helper.assertFalse(
                        helper.getBlockState(mon).getValue(MonitorBlock.LIT),
                        "monitor screen should darken the moment the link is cut"))
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void monitor_linksThroughMainframePartFace(final GameTestHelper helper) {
        final BlockPos controller = new BlockPos(3, 2, 3);
        final Direction facing = Direction.NORTH;
        final MainframeBlockEntity be = formRunningMainframe(helper, controller, facing);
        // A graphics card gives the Mainframe the video outputs its monitors take.
        be.getInventory().setStackInSlot(MainframeBlockEntity.GPU_SLOTS_START,
                new ItemStack(ComputingModule.GPU_HD_7970.get()));
        // A cable on the far side-column part's outward face never touches the controller.
        final BlockPos farPart = controller.relative(facing.getClockWise());
        final BlockPos cable = farPart.relative(facing.getClockWise());
        final BlockPos mon = cable.relative(facing.getClockWise());
        TestCables.lay(helper, cable, ComputingModule.PERIPHERAL_CABLE);
        // Its back to the cable: a monitor faces out of its back, so it faces the Mainframe.
        helper.setBlock(mon, monitorFacing(facing.getCounterClockWise()));
        if (!(helper.getBlockEntity(mon) instanceof MonitorBlockEntity monitor)) {
            helper.fail("no monitor");
            return;
        }
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 4, () -> helper.assertTrue(
                        monitor.ownerPos() != null
                                && monitor.ownerPos().equals(helper.absolutePos(controller)),
                        "monitor must link to the Mainframe through a cable on a PART face"))
                .thenSucceed();
    }
}
