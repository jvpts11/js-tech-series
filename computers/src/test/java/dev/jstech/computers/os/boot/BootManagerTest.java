/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.boot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.os.Platform;
import org.junit.jupiter.api.Test;

class BootManagerTest {

    @Test
    void of_givesFreeBsdItsLoaderAndTheFirstFamiliesNone() {
        assertEquals(BootManager.LOADER, BootManager.of(Platform.FREEBSD));
        assertEquals(BootManager.GRUB, BootManager.of(Platform.LINUX));
        assertEquals(BootManager.KICKMGR, BootManager.of(Platform.FRAMES));
        assertEquals(BootManager.NONE, BootManager.of(Platform.MC_DOS));
        assertEquals(BootManager.NONE, BootManager.of(Platform.MC_NET));
    }

    @Test
    void named_findsEveryManagerByTheNameItTravelsUnder() {
        for (final BootManager manager : BootManager.values()) {
            assertEquals(manager, BootManager.named(manager.serializedName()));
        }
    }

    @Test
    void named_readsANameNobodyHasAsNone() {
        assertEquals(BootManager.NONE, BootManager.named("lilo"));
        assertEquals(BootManager.NONE, BootManager.named(""));
    }

    @Test
    void listsSystems_isFalseOnlyForTheLoader() {
        assertFalse(BootManager.LOADER.listsSystems());
        assertTrue(BootManager.GRUB.listsSystems());
        assertTrue(BootManager.KICKMGR.listsSystems());
    }

    @Test
    void label_namesAnotherSystemTheWayEachManagerDoes() {
        assertEquals("Fedora Boot Manager (on /dev/sdb1)", BootManager.GRUB.label("Fedora", "/dev/sdb1", 1));
        assertEquals("Frames XP (Disk 1)", BootManager.KICKMGR.label("Frames XP", "/dev/sdb1", 1));
        assertEquals("FreeBSD", BootManager.LOADER.label("FreeBSD", "/dev/ada0", 0));
    }
}
