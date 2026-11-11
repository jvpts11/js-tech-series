/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.backup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TapeDriveTest {

    private TapeDrive drive;

    @BeforeEach
    void setUp() {
        this.drive = new TapeDrive();
    }

    @Test
    void start_windsTheTapeThenReadsOnceItIsThere() {
        assertTrue(drive.start(3, 2L, TapeDrive.State.READING));
        assertEquals(TapeDrive.State.WINDING, drive.state());
        assertEquals(3, drive.tape());
        assertFalse(drive.tick(), "one tick of two");
        assertEquals(500, drive.permille());
        assertTrue(drive.tick(), "and there");
        assertEquals(TapeDrive.State.READING, drive.state());
        assertTrue(drive.there());
    }

    @Test
    void start_refusesASecondJobWhileOneIsUnderWay() {
        drive.start(0, 5L, TapeDrive.State.WRITING);
        assertFalse(drive.start(1, 1L, TapeDrive.State.READING));
        assertEquals(0, drive.tape());
    }

    @Test
    void start_withNoWindIsThereAtOnce() {
        assertTrue(drive.start(2, 0L, TapeDrive.State.WRITING));
        assertTrue(drive.there());
        assertEquals(1000, drive.permille());
    }

    @Test
    void finish_freesTheDriveForTheNextJob() {
        drive.start(4, 1L, TapeDrive.State.READING);
        drive.tick();
        drive.finish();
        assertFalse(drive.busy());
        assertEquals(-1, drive.tape());
        assertTrue(drive.start(5, 1L, TapeDrive.State.WRITING));
    }

    @Test
    void start_refusesToStartIdleOrWinding() {
        assertFalse(drive.start(0, 1L, TapeDrive.State.IDLE));
        assertFalse(drive.start(0, 1L, TapeDrive.State.WINDING));
    }
}
