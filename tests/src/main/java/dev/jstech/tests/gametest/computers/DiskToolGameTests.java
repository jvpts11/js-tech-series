/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.EraStorage;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.machine.DiskTools;
import dev.jstech.computers.os.FileSystemType;
import dev.jstech.computers.os.FilesystemKind;
import dev.jstech.computers.os.disk.DiskCommand;
import dev.jstech.computers.os.disk.DiskPartitions;
import dev.jstech.computers.os.disk.DiskReport;
import dev.jstech.computers.os.disk.PartitionScheme;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.FileType;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.CliCommands;
import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.computers.program.cli.CliShell;
import dev.jstech.computers.program.tty.ITtyProcess;
import dev.jstech.computers.program.tty.ITtySink;
import dev.jstech.computers.registry.HardwareRegistry;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.util.Sizes;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.computers.TestCli;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The disk tools (computers/docs/design/storage-devices.md): the machine refuses to delete or format the partition its
 * system runs from, converts only an empty disk, writes a new table erasing the disk, and resizes by the file system's
 * rules; Linux's lsblk, fdisk -l and mkfs, and Frames' DiskPart, say so as their real ones do; and the machine starts
 * next from a volume chosen only when a system is on it.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class DiskToolGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos AT = new BlockPos(4, 2, 4);
    private static final ResourceLocation UBUNTU = ResourceLocation.fromNamespaceAndPath("jsc", "ubuntu");
    private static final ResourceLocation FRAMES_10 = ResourceLocation.fromNamespaceAndPath("jsc", "frames_10");

    private DiskToolGameTests() {
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void tools_refuseTheSystemsOwnPartitionAndConvertOnlyAnEmptyDisk(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = withSecondDisk(helper, FRAMES_10);
        helper.startSequence()
                .thenExecute(() -> {
                    helper.assertTrue(read(pc, DiskCommand.delete(0, 1)).refusal() == DiskReport.Refusal.MOUNTED,
                            "the partition the system runs from is never deleted");
                    helper.assertTrue(read(pc, DiskCommand.format(0, 1, "fat32")).refusal()
                            == DiskReport.Refusal.MOUNTED, "nor formatted");
                    helper.assertTrue(read(pc, DiskCommand.convert(1, PartitionScheme.MBR)).refusal()
                            == DiskReport.Refusal.NOT_EMPTY, "a disk with a partition is not converted");
                    final DiskReport erased = read(pc, DiskCommand.table(1, PartitionScheme.MBR));
                    helper.assertTrue(erased.refusal() == null && erased.disk(1).empty()
                            && erased.disk(1).scheme() == PartitionScheme.MBR, "a new table erases the disk");
                    helper.assertTrue(read(pc, DiskCommand.convert(1, PartitionScheme.GPT)).disk(1).scheme()
                            == PartitionScheme.GPT, "and an empty disk converts");
                    final DiskReport made = read(pc, DiskCommand.create(1, 0L, 1_024L, "exfat"));
                    helper.assertTrue(made.refusal() == null && made.disk(1).parts().size() == 1
                            && made.disk(1).parts().get(0).fileSystem() == FileSystemType.EXFAT,
                            "a partition is made with its file system");
                    helper.assertTrue(read(pc, DiskCommand.table(0, PartitionScheme.GPT)).refusal()
                            == DiskReport.Refusal.MOUNTED, "and the system's own disk is never erased");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void linux_mountedExt4OnlyGrowsAndMkfsRefusesIt(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = withSecondDisk(helper, UBUNTU);
        helper.startSequence()
                .thenExecute(() -> {
                    final DiskReport.Part root = DiskTools.report(pc, null).disk(0).part(1);
                    helper.assertTrue(root != null && root.mounted(), "the root is mounted");
                    helper.assertTrue(read(pc, DiskCommand.resize(0, 1, root.sizeMb() / 2)).refusal()
                                    == DiskReport.Refusal.SHRINKS_ONLY_UNMOUNTED,
                            "a mounted ext4 shrinks only unmounted");
                    final ServerCliComputer cli = new ServerCliComputer(pc, helper.getLevel());
                    final CliShell shell = CliCommands.shellFor(cli, 80);
                    helper.assertTrue(TestCli.contains(shell.run("mkfs.ext4 /dev/sda1", cli),
                            "is mounted; will not make a filesystem here!"), "mkfs refuses the root");
                    helper.assertTrue(TestCli.contains(shell.run("mkfs.ext4 /dev/sdb1", cli), "done"),
                            "and formats the second disk's partition");
                    helper.assertTrue(DiskPartitions.layout(pc.diskInSlot(1)).find(1).fs() == FileSystemType.EXT4,
                            "which is now ext4");
                    final CliShell.Response lsblk = shell.run("lsblk -f", cli);
                    helper.assertTrue(TestCli.contains(lsblk, "sdb1 ext4") && TestCli.contains(lsblk, " /"),
                            "lsblk lists it, and the root mounted at /");
                    helper.assertTrue(TestCli.contains(shell.run("fdisk -l /dev/sda", cli), "Disklabel type"),
                            "and fdisk -l prints the table");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void diskpart_selectsADiskAndSaysWhyItCannotConvertIt(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = withSecondDisk(helper, FRAMES_10);
        helper.startSequence()
                .thenExecute(() -> {
                    final ServerCliComputer cli = new ServerCliComputer(pc, helper.getLevel());
                    final CliShell shell = CliCommands.shellFor(cli, 80);
                    final ITtyProcess diskpart = shell.run("diskpart", cli).started();
                    helper.assertTrue(diskpart != null, "diskpart takes the terminal over");
                    final List<String> said = new ArrayList<>();
                    final ITtySink sink = new ITtySink() {
                        @Override
                        public void line(final CliLine line) {
                            said.add(line.text());
                        }

                        @Override
                        public void redraw(final CliLine line) {
                            said.add(line.text());
                        }
                    };
                    diskpart.begin(0L);
                    diskpart.advance(0L, sink);
                    diskpart.answer("list disk", 1L, sink);
                    diskpart.answer("select disk 1", 2L, sink);
                    diskpart.answer("convert gpt", 3L, sink);
                    final String all = String.join("\n", said);
                    helper.assertTrue(all.contains("Disk 1 is now the selected disk."), "selects the disk: " + all);
                    helper.assertTrue(all.contains("Virtual Disk Service error:")
                            && all.contains("Only an empty disk can be converted."), "and says why not: " + all);
                    diskpart.answer("exit", 4L, sink);
                    helper.assertTrue(diskpart.over(), "exit leaves it");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void startup_choosesOnlyAVolumeWithASystemAndStartsFromItsDisk(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = withSecondDisk(helper, FRAMES_10);
        helper.startSequence()
                .thenExecute(() -> {
                    helper.assertTrue(read(pc, DiskCommand.startup(1, 1)).refusal() == DiskReport.Refusal.NO_SYSTEM,
                            "a volume with no system on it is never started from");
                    final DiskReport.Disk own = DiskTools.report(pc, null).disk(0);
                    DiskReport.Part running = null;
                    for (final DiskReport.Part part : own.parts()) {
                        if (part.mounted() && !part.swap()) {
                            running = part;
                        }
                    }
                    helper.assertTrue(running != null, "the system runs from a partition of its own disk");
                    helper.assertTrue(read(pc, DiskCommand.startup(0, running.number())).refusal() == null,
                            "its own volume is chosen");
                    helper.assertTrue(pc.bootDiskSlot() == own.slot(),
                            "and its disk is the one the machine starts from: " + pc.bootDiskSlot());
                })
                .thenSucceed();
    }

    /* A running PC of the Standard with that system, and a second, plain disk beside its own. */
    /**
     * What is free on a partition is what its files leave of its size, in whole megabytes rounded up: a size that is
     * no whole number of items is held in more items than it has room for, and counting from those would call room
     * free that is not there, which a resize would then take away from what the partition holds.
     */
    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void freeSpace_isWhatTheFilesLeaveOfThePartition(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = withSecondDisk(helper, FRAMES_10);
        helper.startSequence()
                .thenExecute(() -> {
                    final long perItem = EraStorage.mbPerItem(HardwareRegistry.disk(pc.diskInSlot(1)).era());
                    final long size = perItem * 3 + perItem / 2 + 1;
                    read(pc, DiskCommand.table(1, PartitionScheme.GPT));
                    helper.assertTrue(read(pc, DiskCommand.create(1, 0L, size, "exfat")).refusal() == null,
                            "a partition of " + size + " MB is made");
                    final ItemStack volume = DiskPartitions.view(pc.diskInSlot(1), 1);
                    helper.assertTrue(DiskPartitions.sizeMbOf(volume) == size, "at the size asked for");
                    DiskFilesystem.write(volume, "notes.txt", FileType.TXT, "x".repeat(4_000), Long.MAX_VALUE,
                            FilesystemKind.HIERARCHICAL);
                    final long held = Sizes.ceilDiv(DiskFilesystem.filesWeight(volume) * perItem,
                            StorageKey.MB_EQ_PER_ITEM);
                    helper.assertTrue(held > 0 && DiskPartitions.freeMbOf(volume) == size - held,
                            "free is the size less what the file takes; got " + DiskPartitions.freeMbOf(volume)
                                    + " of " + size + ", the file taking " + held);
                })
                .thenSucceed();
    }

    private static PersonalComputerBlockEntity withSecondDisk(final GameTestHelper helper,
                                                             final ResourceLocation system) {
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper)
                .placeRunningPersonalComputer(AT, system);
        pc.getHardware().setStackInSlot(PersonalComputerBlockEntity.DISK_SLOTS_START + 1,
                new ItemStack(ComputingModule.disk(StorageTier.SSD, DiskSize.GB_500)));
        return pc;
    }

    private static DiskReport read(final PersonalComputerBlockEntity pc, final String path) {
        return DiskReport.read(DiskTools.reply(pc, pc.getLevel(), path));
    }
}
