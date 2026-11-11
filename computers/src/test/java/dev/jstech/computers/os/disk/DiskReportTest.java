/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.disk;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiskReportTest {

    @Test
    void write_readsBackTheSameDisksAndPartitions() {
        final DiskReport report = new DiskReport(List.of(new DiskReport.Disk(0, 11, "Vaultis Swift SSD 2T",
                2_097_152L, PartitionScheme.GPT, List.of(
                        new DiskReport.Part(1, 0L, 512L, 6L, "fat32", "EFI", PartitionRole.EFI, false, false, ""),
                        new DiskReport.Part(2, 512L, 614_400L, 65_536L, "ntfs", "", PartitionRole.DATA, false, true,
                                "Frames 10")))), "");
        final DiskReport back = DiskReport.read(report.write());
        assertEquals(report, back);
    }

    @Test
    void read_keepsTheRefusal() {
        final DiskReport report = new DiskReport(List.of(), DiskReport.Refusal.NOT_EMPTY.name());
        assertEquals(DiskReport.Refusal.NOT_EMPTY, DiskReport.read(report.write()).refusal());
    }

    @Test
    void write_aLabelWithATab_comesBackWithASpace() {
        final DiskReport report = new DiskReport(List.of(new DiskReport.Disk(0, 0, "Disk", 100L,
                PartitionScheme.MBR, List.of(new DiskReport.Part(1, 0L, 100L, 0L, "fat16", "MY\tDATA",
                PartitionRole.DATA, true, false, "")))), "");
        assertEquals("MY DATA", DiskReport.read(report.write()).disks().get(0).parts().get(0).label());
    }

    @Test
    void disk_layout_isTheTableOfItsParts() {
        final DiskReport.Disk disk = new DiskReport.Disk(0, 0, "Disk", 40_960L, PartitionScheme.MBR, List.of(
                new DiskReport.Part(1, 0L, 20_480L, 0L, "ntfs", "", PartitionRole.DATA, true, false, "")));
        final DiskLayout layout = disk.layout();
        assertEquals(1, layout.partitions().size());
        assertEquals(20_480L, layout.freeMb());
        assertFalse(disk.empty());
        assertFalse(disk.inUse());
    }

    @Test
    void refusal_ofATableRefusal_isTheSameName() {
        for (final DiskLayout.Refusal refusal : DiskLayout.Refusal.values()) {
            assertEquals(refusal.name(), DiskReport.Refusal.of(refusal).name());
        }
    }

    @Test
    void parse_readsBackEveryCommand() {
        final DiskCommand create = DiskCommand.parse(DiskCommand.create(1, 2_048L, 4_096L, "ext4"));
        assertNotNull(create);
        assertEquals(DiskCommand.Verb.CREATE, create.verb());
        assertEquals(1, create.disk());
        assertEquals(2_048L, create.number());
        assertEquals(4_096L, create.amount());
        assertEquals("ext4", create.word());
        final DiskCommand convert = DiskCommand.parse(DiskCommand.convert(0, PartitionScheme.GPT));
        assertNotNull(convert);
        assertEquals("gpt", convert.word());
        assertEquals(DiskCommand.Verb.LOOK, DiskCommand.parse(DiskCommand.look()).verb());
    }

    @Test
    void parse_somethingElse_isNull() {
        assertNull(DiskCommand.parse("defrag:look"));
        assertNull(DiskCommand.parse("disks:explode/0/1/0/"));
        assertNull(DiskCommand.parse("disks:delete/zero/1/0/"));
        assertTrue(DiskCommand.names("disks:look"));
    }

    @Test
    void label_neverCarriesTheSeparator() {
        final DiskCommand label = DiskCommand.parse(DiskCommand.label(0, 3, "MY/DATA"));
        assertNotNull(label);
        assertEquals("MY DATA", label.word());
    }

    @Test
    void snapshot_keepsTheSlashesOfItsName() {
        final DiskCommand snapshot = DiskCommand.parse(DiskCommand.snapshot(1, 2, "/usr/home@day4"));
        assertNotNull(snapshot);
        assertEquals(DiskCommand.Verb.SNAPSHOT, snapshot.verb());
        assertEquals(2L, snapshot.number());
        assertEquals("/usr/home@day4", snapshot.word());
        final DiskCommand rollBack = DiskCommand.parse(DiskCommand.rollBack(0, 1, ".snapshots/day4", true));
        assertNotNull(rollBack);
        assertEquals(1L, rollBack.amount());
        assertEquals(".snapshots/day4", rollBack.word());
    }

    @Test
    void write_readsBackSnapshotsAndPoolsWithTheirPartitions() {
        final DiskReport.Part pooled = new DiskReport.Part(4, 1_064L, 40_960L, 2_048L, "zfs", "", PartitionRole.DATA,
                false, true, "FreeBSD").with(List.of(new DiskReport.Snap(258, "/usr/home@day4", 96_000L, 1_024L,
                        12L, false)), new DiskReport.Pool(true, true, -1, -1, 128L, 2, 96_500L, 64L, 3));
        final DiskReport.Part plain = new DiskReport.Part(1, 0L, 1_024L, 0L, "fat32", "", PartitionRole.EFI, false,
                false, "");
        final DiskReport report = new DiskReport(List.of(new DiskReport.Disk(0, 11, "Disk", 2_097_152L,
                PartitionScheme.GPT, List.of(plain, pooled))), "");
        final DiskReport read = DiskReport.read(report.write());
        assertEquals(report, read);
        final DiskReport.Part back = read.disk(0).part(4);
        assertNotNull(back.snapshot("/usr/home@day4"));
        assertTrue(back.pool().degraded());
        assertNull(read.disk(0).part(1).pool());
    }
}
