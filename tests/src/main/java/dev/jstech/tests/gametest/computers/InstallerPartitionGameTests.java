/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.operation.payload.firmware.FirmwarePayloads;
import dev.jstech.computers.os.FileSystemType;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.OsDisks;
import dev.jstech.computers.os.disk.DiskLayout;
import dev.jstech.computers.os.disk.DiskPartitions;
import dev.jstech.computers.os.disk.PartitionEntry;
import dev.jstech.computers.os.disk.PartitionRole;
import dev.jstech.computers.os.disk.ZfsMember;
import dev.jstech.computers.os.disk.ZfsMirrors;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.FileType;
import dev.jstech.computers.os.FilesystemKind;
import dev.jstech.computers.os.install.InstallerFlow;
import dev.jstech.computers.os.install.Installers;
import dev.jstech.computers.os.install.OsInstallJob;
import dev.jstech.computers.os.install.OsInstallRunner;
import dev.jstech.computers.os.media.MediaItem;
import dev.jstech.computers.os.media.MediaKind;
import dev.jstech.computers.os.media.MediaReaderBlockEntity;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.computers.EraKit;
import dev.jstech.tests.testkit.computers.EraNetwork;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Every installer's disk step (computers/docs/design/storage-devices.md): under UEFI the EFI system partition, the
 * system's own partition and its swap as big as the memory; the free space used beside a partition of files that is
 * left as it was; Ubuntu beside Frames XP, shrinking its partition; FreeBSD's Auto (ZFS) mirrored over two disks; and a
 * partition deleted from Frames XP's text phase.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class InstallerPartitionGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 8;
    private static final BlockPos AT = new BlockPos(2, 2, 2);
    private static final ResourceLocation DEBIAN = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "debian");
    private static final ResourceLocation UBUNTU = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "ubuntu");
    private static final ResourceLocation FREEBSD =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "freebsd");
    private static final ResourceLocation FRAMES_XP =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "frames_xp");
    private static final long FILES_MB = 102_400L;

    private InstallerPartitionGameTests() {
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void debianOnAnEmptyUefiDisk_makesTheEfiPartitionTheSystemAndItsSwap(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = mainframeWith(helper, DEBIAN, 1);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    install(helper, mainframe, null);
                    final ItemStack disk = mainframe.diskInSlot(0);
                    final DiskLayout layout = DiskPartitions.layout(disk);
                    helper.assertTrue(layout.partitions().size() == 3, "EFI, the system and the swap: " + layout);
                    final PartitionEntry efi = layout.find(1);
                    helper.assertTrue(efi != null && efi.role() == PartitionRole.EFI && efi.sizeMb() == 512L
                            && efi.fs() == FileSystemType.FAT32, "the EFI system partition, 512 MB of FAT32");
                    helper.assertTrue(OsDisks.systemsOn(DiskPartitions.view(disk, 2)).has(DEBIAN),
                            "Debian on the second partition");
                    final PartitionEntry swap = layout.find(3);
                    helper.assertTrue(swap != null && swap.swap() && swap.sizeMb() == mainframe.ramTotalMb(),
                            "and a swap as big as the memory: " + swap);
                    helper.assertTrue(DiskPartitions.volumes(disk).size() == 1,
                            "only the system's partition is a volume: neither the EFI one nor the swap");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void debianBesideAPartitionOfFiles_takesTheFreeSpaceAndLeavesTheFiles(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = mainframeWith(helper, DEBIAN, 1);
        // A partition of files at the start of the disk, the rest free.
        final ItemStack disk = mainframe.diskInSlot(0);
        final DiskLayout empty = DiskLayout.empty(DiskPartitions.layout(disk).scheme(),
                DiskPartitions.layout(disk).diskMb());
        DiskPartitions.relayout(disk, empty.create(0L, FILES_MB, FileSystemType.EXT4, PartitionRole.DATA).layout());
        final ItemStack files = DiskPartitions.view(disk, 1);
        DiskFilesystem.write(files, "notes.txt", FileType.TXT, "kept", Long.MAX_VALUE, FilesystemKind.HIERARCHICAL);
        DiskPartitions.commit(disk, files);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    install(helper, mainframe, null);
                    final ItemStack after = mainframe.diskInSlot(0);
                    final DiskLayout layout = DiskPartitions.layout(after);
                    helper.assertTrue(layout.find(1) != null && layout.find(1).sizeMb() == FILES_MB,
                            "the partition of files is as it was: " + layout);
                    helper.assertTrue(DiskFilesystem.read(DiskPartitions.view(after, 1), "notes.txt").isPresent(),
                            "with its file");
                    final PartitionEntry system = systemPartition(after, DEBIAN);
                    helper.assertTrue(system != null && system.startMb() >= FILES_MB,
                            "Debian went in the free space after it: " + system);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 600)
    public static void ubuntuBesideFramesXp_shrinksItsPartitionAndKeepsItsFiles(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final PersonalComputerBlockEntity pc = EraNetwork.bareComputer(world, EraKit.LEGACY, AT, 1);
        helper.assertTrue(pc.installOs(FRAMES_XP), "Frames XP over the whole of the disk");
        final ItemStack xpDisk = pc.diskInSlot(0);
        DiskFilesystem.write(xpDisk, "letter.txt", FileType.TXT, "kept", Long.MAX_VALUE,
                FilesystemKind.HIERARCHICAL);
        mediumBeside(helper, UBUNTU);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final long before = DiskPartitions.layout(pc.diskInSlot(0)).find(1).sizeMb();
                    install(helper, pc, null);
                    final ItemStack disk = pc.diskInSlot(0);
                    final DiskLayout layout = DiskPartitions.layout(disk);
                    final PartitionEntry xp = layout.find(1);
                    helper.assertTrue(xp != null && xp.fs() == FileSystemType.NTFS && xp.sizeMb() < before,
                            "Frames XP's partition was shrunk: " + layout);
                    helper.assertTrue(OsDisks.systemsOn(DiskPartitions.view(disk, 1)).has(FRAMES_XP)
                                    && DiskFilesystem.read(DiskPartitions.view(disk, 1), "letter.txt").isPresent(),
                            "and Frames XP is still on it, with its files");
                    final PartitionEntry ubuntu = systemPartition(disk, UBUNTU);
                    helper.assertTrue(ubuntu != null && ubuntu.fs() == FileSystemType.EXT3
                            && ubuntu.startMb() == xp.endMb(), "Ubuntu on ext3 in the room it freed: " + ubuntu);
                    helper.assertTrue(layout.partitions().stream().anyMatch(PartitionEntry::swap),
                            "with its swap beside it");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 600)
    public static void freeBsdAutoZfsMirror_laysBothDisksOutAndPairsThem(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = mainframeWith(helper, FREEBSD, 2);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    install(helper, mainframe, flow -> {
                        flow.formatAs(FileSystemType.ZFS.serializedName());
                        flow.mirrorOn(1);
                        flow.askErase(flow.targetSlot());
                        flow.confirmErase();
                    });
                    final ItemStack first = mainframe.diskInSlot(0);
                    final ItemStack second = mainframe.diskInSlot(1);
                    final PartitionEntry pool = systemPartition(first, FREEBSD);
                    helper.assertTrue(pool != null && pool.fs() == FileSystemType.ZFS, "FreeBSD on ZFS: " + pool);
                    final ZfsMember one = ZfsMirrors.memberOf(DiskPartitions.view(first, pool.number()));
                    final PartitionEntry other = DiskPartitions.layout(second).find(pool.number());
                    final ZfsMember two = other == null ? null
                            : ZfsMirrors.memberOf(DiskPartitions.view(second, other.number()));
                    helper.assertTrue(one != null && one.primary() && two != null && !two.primary()
                            && one.pool().equals(two.pool()), "the two disks are one mirrored pool");
                    helper.assertTrue(DiskPartitions.layout(second).partitions().equals(
                            DiskPartitions.layout(first).partitions()), "laid out the same");
                    helper.assertTrue(DiskPartitions.volumes(second).isEmpty(),
                            "the second disk holds nothing of its own");
                })
                .thenSucceed();
    }

    /** Once the copy has started, the disk it was prepared on is the disk it goes on, whatever is asked after. */
    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void aStartedCopy_staysOnTheDiskItWasPreparedOn(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = mainframeWith(helper, FREEBSD, 2);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    mainframe.setNeedsPost(false);
                    helper.assertTrue(FirmwarePayloads.beginInstall(helper.getLevel(), mainframe, -1L, -1) == null,
                            "the installer opens");
                    final InstallerFlow flow = mainframe.installer();
                    for (int page = 0; page < flow.style().stages().size() && !flow.started(); page++) {
                        flow.next();
                    }
                    helper.assertTrue(flow.started(), "the copy began");
                    final int prepared = flow.targetSlot();
                    final int other = prepared == 0 ? 1 : 0;
                    flow.select(other);
                    flow.askErase(other);
                    flow.confirmErase();
                    helper.assertTrue(flow.targetSlot() == prepared,
                            "it stays on disk " + prepared + "; got " + flow.targetSlot());
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void framesXpDeletesAPartition_andItsRoomIsUnpartitionedSpace(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final PersonalComputerBlockEntity pc = EraNetwork.bareComputer(world, EraKit.LEGACY, AT, 1);
        final ItemStack disk = pc.diskInSlot(0);
        final DiskLayout empty = DiskLayout.empty(DiskPartitions.layout(disk).scheme(),
                DiskPartitions.layout(disk).diskMb());
        final long half = empty.reachMb() / 2;
        final DiskLayout first = empty.create(0L, half, FileSystemType.NTFS, PartitionRole.DATA).layout();
        DiskPartitions.relayout(disk, first.create(half, half, FileSystemType.FAT32, PartitionRole.DATA).layout());
        mediumBeside(helper, FRAMES_XP);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    pc.setNeedsPost(false);
                    helper.assertTrue(FirmwarePayloads.beginInstall(helper.getLevel(), pc, -1L, -1) == null,
                            "Frames XP's setup opens");
                    final InstallerFlow flow = pc.installer();
                    helper.assertTrue(Installers.deletePartition(pc, flow, 0, 2), "D deletes the second partition");
                    final InstallerFlow.Disk listed = flow.diskAt(0);
                    helper.assertTrue(listed != null && listed.spotAt(half) != null && listed.spotAt(half).isFree(),
                            "and the list shows its room as unpartitioned space");
                    helper.assertTrue(DiskPartitions.layout(pc.diskInSlot(0)).find(2) == null,
                            "gone from the disk at once, as the real one did");
                    flow.place(half);
                    helper.assertTrue(flow.plan().ok() && flow.plan().system().startMb() == half,
                            "and Frames XP can go there");
                })
                .thenSucceed();
    }

    /* A Mainframe of the Standard, which starts by UEFI, with {@code disks} disks and a CD drive with a system. */
    private static MainframeBlockEntity mainframeWith(final GameTestHelper helper, final ResourceLocation system,
                                                      final int disks) {
        helper.setBlock(AT, ComputingModule.MAINFRAME.get());
        if (!(helper.getBlockEntity(AT) instanceof MainframeBlockEntity mainframe)) {
            throw new IllegalStateException("no Mainframe at " + AT);
        }
        final ItemStackHandler inv = mainframe.getInventory();
        inv.setStackInSlot(MainframeBlockEntity.MOTHERBOARD_SLOT,
                new ItemStack(ComputingModule.MOTHERBOARD_MTX_S_2011.get()));
        inv.setStackInSlot(MainframeBlockEntity.CPU_SLOTS_START, new ItemStack(ComputingModule.CPU_SERVO_2620.get()));
        inv.setStackInSlot(MainframeBlockEntity.RAM_SLOTS_START, new ItemStack(ComputingModule.RAM_DDR3_8192.get()));
        inv.setStackInSlot(MainframeBlockEntity.PSU_SLOT, new ItemStack(ComputingModule.PSU_650G.get()));
        inv.setStackInSlot(MainframeBlockEntity.GPU_SLOTS_START, new ItemStack(ComputingModule.GPU_HD_7970.get()));
        for (int d = 0; d < disks; d++) {
            inv.setStackInSlot(MainframeBlockEntity.DISK_SLOTS_START + d,
                    new ItemStack(ComputingModule.disk(StorageTier.HDD, DiskSize.GB_500)));
        }
        mainframe.togglePower();
        mediumBeside(helper, system);
        return mainframe;
    }

    /* A CD drive beside the machine with the system's installer in it. */
    private static void mediumBeside(final GameTestHelper helper, final ResourceLocation system) {
        final BlockPos readerPos = AT.east();
        helper.setBlock(readerPos, ComputingModule.CD_DRIVE.get());
        if (!(helper.getBlockEntity(readerPos) instanceof MediaReaderBlockEntity reader)) {
            throw new IllegalStateException("no reader at " + readerPos);
        }
        final ItemStack disc = new ItemStack(ComputingModule.CD_ROM.get());
        MediaItem.setKind(disc, MediaKind.OS_INSTALL);
        MediaItem.setPayload(disc, system);
        reader.mediaSlot().setStackInSlot(0, disc);
    }

    /*
     * Opens the installer, gives the answers {@code answers} gives (the installer's own on everything else), and
     * carries the copy to its end, as the machine's ticks would.
     */
    private static void install(final GameTestHelper helper, final IOsHost machine,
                                @Nullable final Consumer<InstallerFlow> answers) {
        machine.setNeedsPost(false);
        helper.assertTrue(FirmwarePayloads.beginInstall(helper.getLevel(), machine, -1L, -1) == null,
                "the installer opens");
        final InstallerFlow flow = machine.installer();
        helper.assertTrue(flow != null, "the machine opened an installer");
        if (answers != null) {
            answers.accept(flow);
        }
        for (int page = 0; page < flow.style().stages().size() && !flow.started(); page++) {
            flow.next();
        }
        helper.assertTrue(flow.started(), "every question was answered and the copy began: " + flow.plan());
        final OsInstallJob job = machine.installing();
        helper.assertTrue(job != null, "the machine took the copy on");
        // A page that asks partway through the copy (bsdinstall's services) is answered as it comes.
        for (int i = 0; i <= job.ticksTotal() * 2 && machine.installing() != null; i++) {
            final InstallerFlow now = machine.installer();
            if (now != null && now.stage().asks()) {
                now.next();
            }
            OsInstallRunner.tick(machine, helper.getLevel(), helper.absolutePos(AT));
        }
        helper.assertTrue(machine.installing() == null, "and the copy ended");
    }

    /* The partition of the disk the system is on, or null. */
    @Nullable
    private static PartitionEntry systemPartition(final ItemStack disk, final ResourceLocation system) {
        for (final PartitionEntry partition : DiskPartitions.layout(disk).partitions()) {
            if (OsDisks.systemsOn(DiskPartitions.view(disk, partition.number())).has(system)) {
                return partition;
            }
        }
        return null;
    }
}
