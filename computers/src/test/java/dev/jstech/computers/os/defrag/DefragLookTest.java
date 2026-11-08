/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.defrag;

import dev.jstech.computers.os.Platform;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefragLookTest {

    @Test
    void of_followsTheSystemsFamily() {
        assertEquals(DefragLook.DOS, DefragLook.of(Platform.MC_DOS, 1));
        assertEquals(DefragLook.DOS, DefragLook.of(Platform.MC_NET, 1));
        assertEquals(DefragLook.FRAMES_95, DefragLook.of(Platform.FRAMES, 1));
        assertEquals(DefragLook.FRAMES_7, DefragLook.of(Platform.FRAMES, 3));
        assertEquals(DefragLook.FRAMES_11, DefragLook.of(Platform.FRAMES, 5));
        assertEquals(DefragLook.UNIX, DefragLook.of(Platform.UNIX, 1));
    }

    @Test
    void drive_namesTheDiskTheWayEachSystemDoes() {
        assertEquals("D:", DefragLook.FRAMES_XP.drive(1));
        assertEquals("sdb1", DefragLook.LINUX.drive(1));
        assertEquals("/dev/sdb1", DefragLook.LINUX.device(1));
        assertEquals("ada1p1", DefragLook.FREEBSD.drive(1));
        assertEquals("/dev/dsk/1s1", DefragLook.UNIX.device(1));
    }

    @Test
    void diskNamed_readsEveryFormOfTheName() {
        assertEquals(1, DefragLook.DOS.diskNamed("d:", 2));
        assertEquals(1, DefragLook.DOS.diskNamed("D", 2));
        assertEquals(1, DefragLook.LINUX.diskNamed("/dev/sdb1", 2));
        assertEquals(0, DefragLook.FREEBSD.diskNamed("ada0p1", 2));
        assertEquals(-1, DefragLook.LINUX.diskNamed("/dev/sdc1", 2));
    }

    @Test
    void scheduled_onlyOnTheEditionsThatHaveOne() {
        assertTrue(DefragLook.FRAMES_7.scheduled());
        assertTrue(DefragLook.FRAMES_10.scheduled());
        assertFalse(DefragLook.FRAMES_XP.scheduled());
        assertFalse(DefragLook.DOS.scheduled());
    }
}
