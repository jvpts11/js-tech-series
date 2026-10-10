/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import com.mojang.authlib.GameProfile;
import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.item.DiskItem;
import dev.jstech.computers.machine.FileCopyJobs;
import dev.jstech.computers.operation.payload.CopyProgressPayload;
import dev.jstech.computers.operation.payload.files.FileTransferPayloads;
import dev.jstech.computers.os.FilesystemKind;
import dev.jstech.computers.os.fs.CopyTiming;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.FileType;
import dev.jstech.computers.os.fs.StoredFile;
import dev.jstech.computers.os.media.MediaFormat;
import dev.jstech.computers.os.media.MediaReaderBlockEntity;
import dev.jstech.core.text.Text;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A file copy takes time: its size read at the pace of the slower volume, the file arriving when that time is up and
 * not before; a machine copies one file after another; paused copies stand still until taken up again; a cancelled
 * copy never arrives.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class FileCopyGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos PC = new BlockPos(2, 2, 2);
    private static final BlockPos DRIVE = new BlockPos(3, 2, 2);
    /** The longest file there is, so its copy onto a floppy takes long enough to be seen under way. */
    private static final int BIG_FILE_CHARS = StoredFile.MOST_CHARS;
    private static final String SOURCE = "big.txt";
    private static final String COPIED = "big - Copy.txt";
    /* A tick or two for the copy's end to be handled after its time. */
    private static final int SLACK = 3;

    private FileCopyGameTests() {
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void copy_ontoAFloppyArrivesWhenItsSizeAtTheFloppysPaceHasGone(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = withFloppy(helper);
        final ItemStack disk = computer.systemDisk();
        writeBigFile(helper, disk);
        final int ticks = expectedTicks(disk);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(computer.enabledEndpoints().contains(drive(helper)),
                        "the floppy drive links to the computer"))
                .thenExecute(() -> {
                    helper.assertTrue(ticks > SLACK, "the copy takes a while; it takes " + ticks + " ticks");
                    FileTransferPayloads.copy(helper.getLevel(), player(helper), helper.absolutePos(PC), SOURCE,
                            floppy(helper));
                    helper.assertTrue(!onFloppy(helper, SOURCE), "nothing arrives the moment the copy starts");
                    helper.assertTrue(FileCopyJobs.runningOn(helper.getLevel(), helper.absolutePos(PC)) == 1,
                            "the copy is under way");
                })
                .thenExecuteAfter(ticks - 2, () -> helper.assertTrue(!onFloppy(helper, SOURCE),
                        "just before its time it has not arrived"))
                .thenExecuteAfter(SLACK + 2, () -> {
                    helper.assertTrue(onFloppy(helper, SOURCE), "it has arrived once its time is up");
                    helper.assertTrue(FileCopyJobs.runningOn(helper.getLevel(), helper.absolutePos(PC)) == 0,
                            "and nothing is under way any more");
                })
                .thenSucceed();
    }

    /** A move onto a floppy that already holds a file of that name leaves both files as they were. */
    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void move_ontoANameAlreadyTakenLeavesBothFilesAlone(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = withFloppy(helper);
        final ItemStack disk = computer.systemDisk();
        DiskFilesystem.write(disk, "note.txt", FileType.TXT, "from the disk", Long.MAX_VALUE,
                FilesystemKind.HIERARCHICAL);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(computer.enabledEndpoints().contains(drive(helper)),
                        "the floppy drive links to the computer"))
                .thenExecute(() -> {
                    final ItemStack floppy = ((MediaReaderBlockEntity) helper.getBlockEntity(DRIVE)).mediaSlot()
                            .getStackInSlot(0);
                    DiskFilesystem.write(floppy, "note.txt", FileType.TXT, "from the floppy", Long.MAX_VALUE,
                            FilesystemKind.HIERARCHICAL);
                    FileTransferPayloads.move(helper.getLevel(), player(helper), helper.absolutePos(PC), "note.txt",
                            floppy(helper));
                })
                .thenWaitUntil(() -> helper.assertTrue(
                        FileCopyJobs.runningOn(helper.getLevel(), helper.absolutePos(PC)) == 0, "the move settles"))
                .thenExecute(() -> {
                    final ItemStack floppy = ((MediaReaderBlockEntity) helper.getBlockEntity(DRIVE)).mediaSlot()
                            .getStackInSlot(0);
                    helper.assertTrue(DiskFilesystem.read(floppy, "note.txt").orElse("").equals("from the floppy"),
                            "the floppy's own file is not written over");
                    helper.assertTrue(DiskFilesystem.read(computer.systemDisk(), "note.txt").isPresent(),
                            "and the file that was to move is still where it was");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 600)
    public static void copies_onOneMachineGoOneAfterAnother(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = withFloppy(helper);
        final ItemStack disk = computer.systemDisk();
        writeBigFile(helper, disk);
        final int ticks = expectedTicks(disk);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(computer.enabledEndpoints().contains(drive(helper)),
                        "the floppy drive links to the computer"))
                .thenExecute(() -> {
                    FileTransferPayloads.copy(helper.getLevel(), player(helper), helper.absolutePos(PC), SOURCE,
                            floppy(helper));
                    FileTransferPayloads.copy(helper.getLevel(), player(helper), helper.absolutePos(PC), SOURCE,
                            floppy(helper));
                    helper.assertTrue(FileCopyJobs.runningOn(helper.getLevel(), helper.absolutePos(PC)) == 2,
                            "both copies are under way");
                })
                .thenExecuteAfter(ticks + SLACK, () -> {
                    helper.assertTrue(onFloppy(helper, SOURCE), "the first has arrived");
                    helper.assertTrue(!onFloppy(helper, COPIED), "the second is still going");
                })
                .thenExecuteAfter(ticks, () -> helper.assertTrue(onFloppy(helper, COPIED),
                        "the second arrives after its own time"))
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void copy_calledOffNeverArrives(final GameTestHelper helper) {
        TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(PC);
        final AtomicInteger carried = new AtomicInteger();
        final long job = FileCopyJobs.start(helper.getLevel(), helper.absolutePos(PC), player(helper),
                new FileCopyJobs.Copy(CopyProgressPayload.COPY, "a.txt", Text.EMPTY, Text.EMPTY, 40L, 1.0), 40,
                carried::incrementAndGet);
        helper.assertTrue(job > 0L, "a copy that takes time is under way");
        FileCopyJobs.cancel(helper.getLevel(), helper.absolutePos(PC), job);
        helper.assertTrue(FileCopyJobs.runningOn(helper.getLevel(), helper.absolutePos(PC)) == 0,
                "a cancelled copy is no longer under way");
        helper.runAfterDelay(50, () -> {
            helper.assertTrue(carried.get() == 0, "and never carried out");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void copies_pausedStandStillAndGoOnWhenTakenUp(final GameTestHelper helper) {
        TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(PC);
        final AtomicInteger carried = new AtomicInteger();
        final BlockPos host = helper.absolutePos(PC);
        FileCopyJobs.start(helper.getLevel(), host, player(helper),
                new FileCopyJobs.Copy(CopyProgressPayload.COPY, "a.txt", Text.EMPTY, Text.EMPTY, 40L, 1.0), 40,
                carried::incrementAndGet);
        FileCopyJobs.pause(helper.getLevel(), host, true);
        helper.assertTrue(FileCopyJobs.pausedOn(helper.getLevel(), host), "the machine's copies are paused");
        // A copy asked for while the others stand waits with them, behind them.
        FileCopyJobs.start(helper.getLevel(), host, player(helper),
                new FileCopyJobs.Copy(CopyProgressPayload.COPY, "b.txt", Text.EMPTY, Text.EMPTY, 20L, 1.0), 20,
                carried::incrementAndGet);
        helper.startSequence()
                .thenExecuteAfter(60, () -> {
                    helper.assertTrue(carried.get() == 0, "nothing is carried out while they stand, past their time");
                    FileCopyJobs.pause(helper.getLevel(), host, false);
                    helper.assertTrue(!FileCopyJobs.pausedOn(helper.getLevel(), host), "taken up again");
                })
                .thenExecuteAfter(35, () -> helper.assertTrue(carried.get() == 0,
                        "the first still has the whole of its time to go, having stood from its start"))
                .thenExecuteAfter(SLACK + 5, () -> helper.assertTrue(carried.get() == 1, "the first has arrived"))
                .thenExecuteAfter(20, () -> helper.assertTrue(carried.get() == 2, "and the second after it"))
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void copy_thatTakesNoTimeIsCarriedOutAtOnce(final GameTestHelper helper) {
        TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(PC);
        final AtomicInteger carried = new AtomicInteger();
        final long job = FileCopyJobs.start(helper.getLevel(), helper.absolutePos(PC), player(helper),
                new FileCopyJobs.Copy(CopyProgressPayload.COPY, "tiny.txt", Text.EMPTY, Text.EMPTY, 0L, 80.0), 0,
                carried::incrementAndGet);
        helper.assertTrue(job == 0L && carried.get() == 1, "a copy of nothing is done before anyone sees it");
        helper.succeed();
    }

    /* A running computer with a floppy drive against it and a floppy in the drive. */
    private static PersonalComputerBlockEntity withFloppy(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final PersonalComputerBlockEntity computer = world.placeRunningPersonalComputer(PC);
        world.setBlock(DRIVE, ComputingModule.FLOPPY_DRIVE.get());
        world.blockEntity(DRIVE, MediaReaderBlockEntity.class).mediaSlot()
                .setStackInSlot(0, new ItemStack(ComputingModule.FLOPPY_DISK.get()));
        return computer;
    }

    private static void writeBigFile(final GameTestHelper helper, final ItemStack disk) {
        final DiskFilesystem.WriteResult wrote = DiskFilesystem.write(disk, SOURCE, FileType.TXT,
                "x".repeat(BIG_FILE_CHARS), Long.MAX_VALUE, FilesystemKind.HIERARCHICAL);
        helper.assertTrue(wrote == DiskFilesystem.WriteResult.OK, "the big file is written; " + wrote);
    }

    /* The ticks the big file takes from the disk to the floppy: its weight at the slower of the two paces. */
    private static int expectedTicks(final ItemStack disk) {
        final int tier = disk.getItem() instanceof DiskItem item ? item.spec().tier().speedMultiplier() : 1;
        return CopyTiming.ticks(DiskFilesystem.weightOf(disk, SOURCE),
                CopyTiming.slowest(CopyTiming.diskRate(tier), CopyTiming.mediaRate(MediaFormat.FLOPPY)));
    }

    private static long drive(final GameTestHelper helper) {
        return helper.absolutePos(DRIVE).asLong();
    }

    /* The floppy as the explorer names it. */
    private static String floppy(final GameTestHelper helper) {
        return "media:" + drive(helper);
    }

    private static boolean onFloppy(final GameTestHelper helper, final String name) {
        return helper.getBlockEntity(DRIVE) instanceof MediaReaderBlockEntity reader
                && DiskFilesystem.exists(reader.mediaSlot().getStackInSlot(0), name);
    }

    private static ServerPlayer player(final GameTestHelper helper) {
        return FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "copier"));
    }
}
