/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiskSizeTest {

    @Test
    void capacityItems_oneTerabyteIs4096Items() {
        // 1 TB = 1,048,576 MB / 256 MB per item = 4,096 items.
        assertEquals(4_096L, DiskSize.TB_1.capacityItems());
    }

    @Test
    void capacityItems_smallestIs2000Items() {
        // 500 GB = 512,000 MB / 256 MB = 2,000 items.
        assertEquals(2_000L, DiskSize.GB_500.capacityItems());
    }

    @Test
    void capacityItems_doublesWithSize() {
        assertEquals(DiskSize.TB_1.capacityItems() * 2, DiskSize.TB_2.capacityItems());
        assertEquals(DiskSize.TB_2.capacityItems() * 2, DiskSize.TB_4.capacityItems());
        assertEquals(DiskSize.TB_4.capacityItems() * 2, DiskSize.TB_8.capacityItems());
    }

    @Test
    void capacityItems_ofTheHeliumDisksFollowTheirTerabytes() {
        assertEquals(DiskSize.TB_4.capacityItems() * 3, DiskSize.TB_12.capacityItems());
        assertEquals(DiskSize.TB_8.capacityItems() * 2, DiskSize.TB_16.capacityItems());
        assertEquals(DiskSize.TB_4.capacityItems() * 5, DiskSize.TB_20.capacityItems());
        assertEquals(DiskSize.TB_12.capacityItems() * 2, DiskSize.TB_24.capacityItems());
    }

    @Test
    void comesAs_everySizeIsAHardDisk() {
        for (final DiskSize size : DiskSize.values()) {
            assertTrue(size.comesAs(StorageTier.HDD), () -> size + " should come as a hard disk");
        }
    }

    @Test
    void comesAs_theHeliumSizesAreHardDisksOnly() {
        assertFalse(DiskSize.TB_12.comesAs(StorageTier.SSD));
        assertFalse(DiskSize.TB_24.comesAs(StorageTier.NVME));
        assertTrue(DiskSize.TB_8.comesAs(StorageTier.NVME));
    }

    @Test
    void capacityMb_matchesDiskSpec() {
        // A disk built from a size yields capacityMb = items × 256, regardless of tier.
        final DiskSpec hdd = new DiskSpec(StorageTier.HDD, dev.jstech.core.tier.HardwareEra.STANDARD,
                DiskSize.TB_1.capacityItems(), 6);
        final DiskSpec nvme = new DiskSpec(StorageTier.NVME, dev.jstech.core.tier.HardwareEra.STANDARD,
                DiskSize.TB_1.capacityItems(), 5);
        assertEquals(hdd.capacityMb(), nvme.capacityMb());
        assertEquals(1_048_576L, hdd.capacityMb());
    }
}
