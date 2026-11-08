/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.defrag;

import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefragJobTest {

    @Test
    void advance_spreadsTheWorkOverThePass() {
        final DefragJob job = new DefragJob(UUID.randomUUID(), 0, 10, 95, 5, 50L, 0);
        int due = 0;
        for (int tick = 0; tick < 10; tick++) {
            final int now = job.advance();
            for (int i = 0; i < now; i++) {
                job.tried(1L, 0L);
            }
            due += now;
        }
        assertEquals(5, due);
        assertTrue(job.done());
        assertEquals(100, job.percent());
    }

    @Test
    void advance_standsStillWhilePaused() {
        final DefragJob job = new DefragJob(UUID.randomUUID(), 0, 10, 95, 5, 50L, 0);
        job.paused(true);
        assertEquals(0, job.advance());
        assertEquals(0, job.ticksDone());
        job.paused(false);
        job.advance();
        assertEquals(1, job.ticksDone());
    }

    @Test
    void tried_keepsTheTally() {
        final DefragJob job = new DefragJob(UUID.randomUUID(), 1, 10, 95, 2, 20L, 4);
        job.tried(9L, 1L);
        job.tried(8L, 2L);
        assertEquals(17L, job.recovered());
        assertEquals(3L, job.lost());
        assertEquals(2, job.tried());
        assertFalse(job.done());
    }
}
