/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.backup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BackupPartTest {

    @Test
    void of_readsBackTheSetAMaskWasMadeOf() {
        final Set<BackupPart> parts = EnumSet.of(BackupPart.ITEMS, BackupPart.SCRIPTS, BackupPart.IMAGES);
        assertEquals(parts, BackupPart.of(BackupPart.mask(parts)));
    }

    @Test
    void mask_isNoughtForNothingAndEveryBitForEverything() {
        assertEquals(0, BackupPart.mask(EnumSet.noneOf(BackupPart.class)));
        assertTrue(BackupPart.of(0).isEmpty());
        assertEquals(EnumSet.allOf(BackupPart.class), BackupPart.of(-1));
    }
}
