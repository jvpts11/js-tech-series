/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.install;

import dev.jstech.computers.os.FileSystemType;
import dev.jstech.computers.os.disk.PartitionEntry;
import dev.jstech.computers.os.disk.PartitionRole;
import dev.jstech.computers.os.disk.PartitionScheme;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InstallPlanTest {

    private static final InstallPlan.Facts NTFS_UEFI = new InstallPlan.Facts(FileSystemType.NTFS,
            List.of(FileSystemType.FAT16, FileSystemType.FAT32, FileSystemType.NTFS), 0, PartitionScheme.GPT);
    private static final InstallPlan.Facts NTFS_BIOS = new InstallPlan.Facts(FileSystemType.NTFS,
            List.of(FileSystemType.FAT16, FileSystemType.FAT32, FileSystemType.NTFS), 0, PartitionScheme.MBR);
    private static final InstallPlan.Facts EXT3_BIOS = new InstallPlan.Facts(FileSystemType.EXT3,
            List.of(FileSystemType.EXT2, FileSystemType.EXT3), 1_024, PartitionScheme.MBR);
    private static final InstallPlan.Facts DOS = new InstallPlan.Facts(FileSystemType.FAT16,
            List.of(FileSystemType.FAT12, FileSystemType.FAT16), 0, PartitionScheme.MBR);
    private static final InstallPlan.Facts UFS_UEFI = new InstallPlan.Facts(FileSystemType.UFS,
            List.of(FileSystemType.UFS, FileSystemType.ZFS), 16_384, PartitionScheme.GPT);

    @Test
    void of_anEmptyDiskUnderUefi_takesItWholeWithAnEfiPartitionFirst() {
        final InstallerFlow flow = flow(InstallerStyle.FRAMES_11, "Frames 11", 20_480, NTFS_UEFI,
                List.of(blank(0, 600_000)));
        final InstallPlan.Plan plan = flow.plan();
        assertTrue(plan.ok());
        assertTrue(plan.wholeDisk());
        final PartitionEntry efi = plan.layout().find(1);
        assertNotNull(efi);
        assertEquals(PartitionRole.EFI, efi.role());
        assertEquals(InstallPlan.EFI_MB, efi.sizeMb());
        final PartitionEntry system = plan.system();
        assertNotNull(system);
        assertEquals(2, system.number());
        assertEquals(FileSystemType.NTFS, system.fs());
        assertEquals(600_000 - InstallPlan.EFI_MB, system.sizeMb());
    }

    @Test
    void of_anEmptyDiskUnderABios_makesTheSystemTheActivePartition() {
        final InstallerFlow flow = flow(InstallerStyle.FRAMES_XP, "Frames XP", 1_536, NTFS_BIOS,
                List.of(blank(0, 40_960)));
        final InstallPlan.Plan plan = flow.plan();
        assertTrue(plan.ok());
        assertEquals(1, plan.systemPartition());
        assertTrue(plan.system().active());
        assertNull(plan.swap());
    }

    @Test
    void of_linux_makesASwapAsBigAsTheMemoryAfterTheSystem() {
        final InstallerFlow flow = flow(InstallerStyle.DEBIAN, "Debian", 4_096, EXT3_BIOS,
                List.of(blank(0, 40_960)));
        final InstallPlan.Plan plan = flow.plan();
        assertTrue(plan.ok());
        final PartitionEntry swap = plan.swap();
        assertNotNull(swap);
        assertEquals(1_024, swap.sizeMb());
        assertEquals(plan.system().endMb(), swap.startMb());
    }

    @Test
    void of_mcDosOnABigDisk_makesAFat16PartitionOfTwoGigabytesAtMost() {
        final InstallerFlow flow = flow(InstallerStyle.MC_DOS, "MC-DOS", 8, DOS, List.of(blank(0, 8_192)));
        final InstallPlan.Plan plan = flow.plan();
        assertTrue(plan.ok());
        assertEquals(FileSystemType.FAT16, plan.system().fs());
        assertEquals(2_048, plan.system().sizeMb());
    }

    @Test
    void of_theFreeSpaceChosen_putsTheSystemThereAndLeavesTheOtherPartitions() {
        final InstallerFlow.Disk disk = disk(0, 40_960, PartitionScheme.MBR, List.of(
                part(1, 0, 20_480, 4_000, "ntfs", "Frames XP"),
                DiskSpot.free(20_480, 20_480)));
        final InstallerFlow flow = flow(InstallerStyle.FRAMES_11, "Frames 10", 16_000, NTFS_BIOS, List.of(disk));
        flow.place(20_480);
        final InstallPlan.Plan plan = flow.plan();
        assertTrue(plan.ok());
        assertFalse(plan.wholeDisk());
        assertEquals(2, plan.systemPartition());
        assertEquals(20_480, plan.system().startMb());
        assertNotNull(plan.layout().find(1));
        assertEquals(List.of(2), plan.made());
    }

    @Test
    void of_unpartitionedSpaceOnAFullMasterBootRecord_isRefused() {
        final InstallerFlow.Disk disk = disk(0, 40_960, PartitionScheme.MBR, List.of(
                part(1, 0, 8_192, 8_000, "ntfs", ""), part(2, 8_192, 8_192, 8_000, "ntfs", ""),
                part(3, 16_384, 8_192, 8_000, "ntfs", ""), part(4, 24_576, 8_192, 8_000, "ntfs", ""),
                DiskSpot.free(32_768, 8_192)));
        final InstallerFlow flow = flow(InstallerStyle.FRAMES_XP, "Frames XP", 1_536, NTFS_BIOS, List.of(disk));
        flow.place(32_768);
        assertEquals(InstallPlan.Problem.TABLE_FULL, flow.plan().problem());
        assertFalse(flow.canContinue());
    }

    @Test
    void of_aPartitionHoldingAnotherSystem_wantsItFormattedFirst() {
        final InstallerFlow.Disk disk = disk(0, 40_960, PartitionScheme.MBR, List.of(
                part(1, 0, 40_960, 30_000, "ext3", "Ubuntu")));
        final InstallerFlow flow = flow(InstallerStyle.FRAMES_XP, "Frames XP", 1_536, NTFS_BIOS, List.of(disk));
        flow.place(0);
        assertEquals(InstallPlan.Problem.NEEDS_FORMAT, flow.plan().problem());
        flow.formatAs("ntfs");
        final InstallPlan.Plan plan = flow.plan();
        assertTrue(plan.ok());
        assertEquals(FileSystemType.NTFS, plan.system().fs());
        assertEquals(List.of(1), plan.made());
    }

    @Test
    void of_keepingAPartitionItLivesOn_installsBesideItsFiles() {
        final InstallerFlow.Disk disk = disk(0, 40_960, PartitionScheme.MBR, List.of(
                part(1, 0, 14_336, 2_560, "ntfs", ""), DiskSpot.free(14_336, 26_624)));
        final InstallerFlow flow = flow(InstallerStyle.FRAMES_XP, "Frames XP", 1_536, NTFS_BIOS, List.of(disk));
        flow.place(0);
        flow.formatAs(Placement.KEEP);
        final InstallPlan.Plan plan = flow.plan();
        assertTrue(plan.ok());
        assertEquals(1, plan.systemPartition());
        assertTrue(plan.made().isEmpty());
    }

    @Test
    void of_theEfiPartitionChosen_isNoPlaceForASystem() {
        final InstallerFlow.Disk disk = disk(0, 600_000, PartitionScheme.GPT, List.of(
                new DiskSpot(1, 0, 512, 506, "fat32", "", "", PartitionRole.EFI, false),
                part(2, 512, 599_488, 500_000, "ntfs", "")));
        final InstallerFlow flow = flow(InstallerStyle.FRAMES_11, "Frames 11", 20_480, NTFS_UEFI, List.of(disk));
        flow.place(0);
        assertEquals(InstallPlan.Problem.NOT_FOR_A_SYSTEM, flow.plan().problem());
    }

    @Test
    void of_ubuntuBesideFramesXp_shrinksItsPartitionAndMakesTheRootAndTheSwapInTheRoomFreed() {
        final InstallerFlow.Disk disk = disk(0, 40_960, PartitionScheme.MBR, List.of(
                part(1, 0, 40_960, 31_232, "ntfs", "Frames XP")));
        final InstallerFlow flow = flow(InstallerStyle.UBUNTU, "Ubuntu", 8_192, EXT3_BIOS, List.of(disk));
        flow.shrinkTo(20_480);
        final InstallPlan.Plan plan = flow.plan();
        assertTrue(plan.ok());
        assertEquals(1, plan.shrunk());
        assertEquals(20_480, plan.layout().find(1).sizeMb());
        assertEquals(FileSystemType.EXT3, plan.system().fs());
        assertEquals(20_480, plan.system().startMb());
        assertEquals(19_456, plan.system().sizeMb());
        assertEquals(1_024, plan.swap().sizeMb());
        assertEquals(3, plan.layout().partitions().size());
    }

    @Test
    void of_shrinkingBelowWhatItHolds_isRefused() {
        final InstallerFlow.Disk disk = disk(0, 40_960, PartitionScheme.MBR, List.of(
                part(1, 0, 40_960, 31_232, "ntfs", "Frames XP")));
        final InstallerFlow flow = flow(InstallerStyle.UBUNTU, "Ubuntu", 8_192, EXT3_BIOS, List.of(disk));
        flow.shrinkTo(4_096);
        assertEquals(InstallPlan.Problem.CANNOT_SHRINK, flow.plan().problem());
    }

    @Test
    void of_ubuntuLeftToItself_beside_shrinksTheOtherSystemByHalfItsFreeRoom() {
        final InstallerFlow.Disk disk = disk(0, 40_960, PartitionScheme.MBR, List.of(
                part(1, 0, 40_960, 31_232, "ntfs", "Frames XP")));
        final InstallerFlow flow = flow(InstallerStyle.UBUNTU, "Ubuntu", 8_192, EXT3_BIOS, List.of(disk));
        final long to = InstallPlan.shrinkFor(flow, disk);
        assertEquals(40_960 - 31_232 / 2, to);
        final InstallPlan.Plan plan = flow.plan();
        assertTrue(plan.ok());
        assertEquals(to, plan.layout().find(1).sizeMb());
    }

    @Test
    void of_framesLeftToItselfOnADiskAnotherSystemFills_findsNoRoom() {
        final InstallerFlow.Disk disk = disk(0, 40_960, PartitionScheme.MBR, List.of(
                part(1, 0, 40_960, 31_232, "ext3", "Ubuntu")));
        final InstallerFlow flow = flow(InstallerStyle.FRAMES_XP, "Frames XP", 1_536, NTFS_BIOS, List.of(disk));
        assertEquals(InstallPlan.Problem.NO_ROOM, flow.plan().problem());
    }

    @Test
    void of_theSameSystemAlreadyThere_isPutBackWhereItIs() {
        final InstallerFlow.Disk disk = disk(0, 40_960, PartitionScheme.MBR, List.of(
                part(1, 0, 20_480, 100, "ntfs", "Frames XP"), DiskSpot.free(20_480, 20_480)));
        final InstallerFlow flow = flow(InstallerStyle.FRAMES_XP, "Frames XP", 1_536, NTFS_BIOS, List.of(disk));
        final InstallPlan.Plan plan = flow.plan();
        assertTrue(plan.ok());
        assertEquals(1, plan.systemPartition());
        assertTrue(plan.made().isEmpty());
    }

    @Test
    void mirror_onZfs_laysTheSecondDiskOutAsTheFirst() {
        final InstallerFlow flow = flow(InstallerStyle.BSD_INSTALL, "FreeBSD", 2_048, UFS_UEFI,
                List.of(blank(0, 600_000), blank(1, 600_000)));
        flow.formatAs("zfs");
        flow.mirrorOn(1);
        final InstallPlan.Plan first = flow.plan();
        final InstallPlan.Plan second = InstallPlan.mirror(flow);
        assertTrue(first.ok());
        assertNotNull(second);
        assertTrue(second.ok());
        assertEquals(1, second.slot());
        assertEquals(FileSystemType.ZFS, second.system().fs());
        assertEquals(first.layout().partitions(), second.layout().partitions());
    }

    @Test
    void mirror_onUfs_isNone() {
        final InstallerFlow flow = flow(InstallerStyle.BSD_INSTALL, "FreeBSD", 2_048, UFS_UEFI,
                List.of(blank(0, 600_000), blank(1, 600_000)));
        flow.mirrorOn(1);
        assertNull(InstallPlan.mirror(flow));
    }

    private static InstallerFlow flow(final InstallerStyle style, final String name, final int footprintMb,
                                      final InstallPlan.Facts facts, final List<InstallerFlow.Disk> disks) {
        final InstallerFlow flow = InstallerFlow.quoted(style, "jsc:test", name, footprintMb, 100, disks, "PC",
                List.of(), "");
        flow.setFacts(facts);
        flow.select(disks.get(0).slot());
        return flow;
    }

    private static InstallerFlow.Disk blank(final int slot, final int sizeMb) {
        return new InstallerFlow.Disk(slot, "Disk " + slot, sizeMb, sizeMb, "", 1);
    }

    private static InstallerFlow.Disk disk(final int slot, final int sizeMb, final PartitionScheme scheme,
                                           final List<DiskSpot> spots) {
        long free = 0L;
        String holds = "";
        for (final DiskSpot spot : spots) {
            free += spot.freeMb();
            holds = holds.isEmpty() ? spot.holds() : holds;
        }
        return new InstallerFlow.Disk(slot, "Disk " + slot, sizeMb, (int) free, holds, 1, scheme, spots);
    }

    private static DiskSpot part(final int number, final long startMb, final long sizeMb, final long freeMb,
                                 final String fs, final String holds) {
        return new DiskSpot(number, startMb, sizeMb, freeMb, fs, holds, "", PartitionRole.DATA, number == 1);
    }
}
