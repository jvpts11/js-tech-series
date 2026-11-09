/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.portable;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PortableStorageTest {

    @Test
    void items_areTheStorageOverTheEraItemSizeLessTheSystem() {
        assertEquals(4L, PortableStorage.items(PortableModel.HIGHTIDE_POCKET));
        assertEquals(8L, PortableStorage.items(PortableModel.HIGHTIDE_POCKET_PRO));
        assertEquals(112L, PortableStorage.items(PortableModel.QPHONE_5));
        assertEquals(480L, PortableStorage.items(PortableModel.SUNSUNG_NEBULA_S10));
        assertEquals(1_000L, PortableStorage.items(PortableModel.QPAD_PRO));
    }

    @Test
    void capacity_countsThousandthsOfAnItem() {
        assertEquals(112_000L, PortableStorage.capacity(PortableModel.QPHONE_5));
    }

    @Test
    void pictureWeight_roundsUpAsAFileOnADiskOfItsEra() {
        // A Standard disk weighs 256 MB to an item, so a thousandth of an item is 268,435 bytes.
        assertEquals(1L, PortableStorage.pictureWeight(PortableModel.QPHONE_5, 1L));
        assertEquals(4L, PortableStorage.pictureWeight(PortableModel.QPHONE_5, 1_000_000L));
        assertEquals(0L, PortableStorage.pictureWeight(PortableModel.QPHONE_5, 0L));
    }
}
