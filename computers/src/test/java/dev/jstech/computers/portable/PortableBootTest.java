/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.portable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class PortableBootTest {

    @Test
    void stage_goesFromTheMarkToTheSystemToItsLock() {
        assertSame(PortableBoot.Stage.OFF, PortableBoot.stage(-1L, 500L));
        assertSame(PortableBoot.Stage.MARK, PortableBoot.stage(100L, 100L));
        assertSame(PortableBoot.Stage.MARK, PortableBoot.stage(100L, 100L + PortableBoot.MARK_TICKS - 1));
        assertSame(PortableBoot.Stage.SYSTEM, PortableBoot.stage(100L, 100L + PortableBoot.MARK_TICKS));
        assertSame(PortableBoot.Stage.READY, PortableBoot.stage(100L, 100L + PortableBoot.MARK_TICKS
                + PortableBoot.SYSTEM_TICKS));
    }

    @Test
    void progress_runsFromNoneToWholeThroughTheSystemsStart() {
        assertEquals(0.0F, PortableBoot.progress(0L, PortableBoot.MARK_TICKS));
        assertEquals(0.5F, PortableBoot.progress(0L, PortableBoot.MARK_TICKS + PortableBoot.SYSTEM_TICKS / 2));
        assertEquals(1.0F, PortableBoot.progress(0L, 10_000L));
    }
}
