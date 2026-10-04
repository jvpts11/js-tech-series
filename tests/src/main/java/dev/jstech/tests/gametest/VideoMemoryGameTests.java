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
import dev.jstech.computers.block.MonitorBlock;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.MonitorBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.hardware.ComputerBuild;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.monitor.VideoMemory;
import dev.jstech.computers.os.RamLedger;
import dev.jstech.computers.os.VramLedger;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestWorldBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * The graphics card as a resource: what a lit monitor and a graphics window hold of its memory, a monitor that does
 * not fit staying dark until there is room, graphics on a processor's die borrowing the system's memory, a
 * Mainframe's card queues no faster than their cards, and a server's cards adding to what it sends out.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class VideoMemoryGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos PC = new BlockPos(4, 2, 2);

    private VideoMemoryGameTests() {
    }

    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void bigScreen_staysDarkUntilACardHasRoomForIt(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = sharedGraphicsPc(helper);
        // Forty Standard monitors need two and a half gigabytes; the shared memory gives graphics at most 1792 MB.
        final BlockPos[] wall = wall(PC.north(), 8, 5);
        for (final BlockPos member : wall) {
            helper.setBlock(member, facing(Direction.SOUTH));
        }
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(starvedSomewhere(helper, wall),
                        "the screen is linked and finds no room in the shared memory"))
                .thenExecute(() -> {
                    for (final BlockPos member : wall) {
                        helper.assertTrue(!entity(helper, member).lit(), "the screen stays dark");
                    }
                    computer.getHardware().setStackInSlot(PersonalComputerBlockEntity.GPU_SLOTS_START,
                            new ItemStack(ComputingModule.GPU_HD_7970.get()));
                })
                .thenWaitUntil(() -> helper.assertTrue(entity(helper, wall[0]).lit(),
                        "a three-gigabyte card has room for it, and it lights"))
                .thenExecute(() -> helper.assertTrue(!starvedSomewhere(helper, wall), "and nothing says it is starved"))
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void sharedGraphics_holdTheSystemsMemoryForWhatTheyShow(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = TestWorldBuilder.forGameTest(helper)
                .placeRunningPersonalComputer(PC);
        // Without its card the machine draws with its processor's graphics, from its own memory.
        computer.getHardware().setStackInSlot(PersonalComputerBlockEntity.GPU_SLOTS_START, ItemStack.EMPTY);
        helper.setBlock(PC.east(), facing(Direction.WEST));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(entity(helper, PC.east()).lit(), "the monitor lights"))
                .thenExecute(() -> {
                    final VideoMemory.State state = VideoMemory.of(helper.getLevel(), computer);
                    helper.assertTrue(state.shared(), "the processor's graphics share the system's memory");
                    helper.assertTrue(state.ledger().totalKb() == 1792L * VramLedger.KB_PER_MB,
                            "up to 1792 MB of eight gigabytes; had " + state.ledger().totalKb() + " KB");
                    helper.assertTrue(computer.ramLedger().usedMb(RamLedger.Kind.GRAPHICS) == 64,
                            "the lit Standard monitor's 64 MB are held of the system's RAM; held "
                                    + computer.ramLedger().usedMb(RamLedger.Kind.GRAPHICS));
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void mainframeQueues_runNoFasterThanTheirCards(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = TestWorldBuilder.forGameTest(helper).placeRunningMainframe(PC);
        mainframe.getInventory().setStackInSlot(MainframeBlockEntity.GPU_SLOTS_START,
                new ItemStack(ComputingModule.GPU_HD_7970.get()));
        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    final long[] speeds = mainframe.queueSpeeds();
                    helper.assertTrue(speeds.length == 2, "the processor's queue and the card's; had " + speeds.length);
                    helper.assertTrue(speeds[0] == mainframe.currentBuild().totalCapacity(),
                            "the processor's queue runs at its capacity");
                    helper.assertTrue(speeds[1] <= speeds[0] && speeds[1] >= 1L,
                            "the card's queue runs no faster than the processor; " + speeds[1] + " of " + speeds[0]);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void serverCards_addAThreadsTwentiethToWhatItSendsOut(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = TestWorldBuilder.forGameTest(helper)
                .placeRunningPersonalComputer(PC);
        final ComputerBuild build = computer.currentBuild();
        // The HD 7970's 2048 cores are 8192 threads, a twentieth of an item each.
        helper.assertTrue(build.serverGpuBonus() == 410L, "8192 threads add 410 it/t; added " + build.serverGpuBonus());
        helper.assertTrue(build.serverGpuBonusPercent() > 0, "which the server's assembly shows as a share");
        helper.succeed();
    }

    /* A Standard machine with a Centro c5 4590 and eight gigabytes and no card, switched on. */
    private static PersonalComputerBlockEntity sharedGraphicsPc(final GameTestHelper helper) {
        helper.setBlock(PC, ComputingModule.PERSONAL_COMPUTER.get());
        final PersonalComputerBlockEntity computer = (PersonalComputerBlockEntity) helper.getBlockEntity(PC);
        final ItemStackHandler hardware = computer.getHardware();
        hardware.setStackInSlot(PersonalComputerBlockEntity.MOTHERBOARD_SLOT,
                new ItemStack(HardwareItems.MOTHERBOARD_ATX_STANDARD_LGA1150.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.CPU_SLOT,
                new ItemStack(HardwareItems.CPU_INTEGRA_CENTRO_C5_4590.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.RAM_SLOTS_START,
                new ItemStack(ComputingModule.RAM_DDR3_8192.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.PSU_SLOT, new ItemStack(ComputingModule.PSU_650G.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.DISK_SLOTS_START,
                new ItemStack(ComputingModule.disk(StorageTier.SSD, DiskSize.GB_500)));
        computer.togglePower();
        return computer;
    }

    private static boolean starvedSomewhere(final GameTestHelper helper, final BlockPos[] wall) {
        for (final BlockPos member : wall) {
            if (entity(helper, member).starved()) {
                return true;
            }
        }
        return false;
    }

    private static BlockPos[] wall(final BlockPos start, final int wide, final int tall) {
        final BlockPos[] cells = new BlockPos[wide * tall];
        for (int v = 0; v < tall; v++) {
            for (int u = 0; u < wide; u++) {
                cells[v * wide + u] = start.east(u).above(v);
            }
        }
        return cells;
    }

    private static BlockState facing(final Direction facing) {
        return ComputingModule.MONITOR.get().defaultBlockState().setValue(MonitorBlock.FACING, facing);
    }

    private static MonitorBlockEntity entity(final GameTestHelper helper, final BlockPos at) {
        if (!(helper.getBlockEntity(at) instanceof MonitorBlockEntity monitor)) {
            throw new IllegalStateException("no monitor at " + at);
        }
        return monitor;
    }
}
