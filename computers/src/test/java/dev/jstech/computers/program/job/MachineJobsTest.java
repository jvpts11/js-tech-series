/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MachineJobsTest {

    private MachineJobs jobs;

    @BeforeEach
    void setUp() {
        this.jobs = new MachineJobs();
    }

    @Test
    void due_withCronOff_leavesAScheduledJobWaiting() {
        this.jobs.add("backup.sh", JobWhen.at(6));
        final List<MachineJobs.Job> due = this.jobs.due(0L, false);
        assertTrue(due.isEmpty());
        assertEquals(1, this.jobs.all().size(), "a job cron leaves waiting is not taken off the list");
    }

    @Test
    void due_withCronOff_stillRunsAJobPutInTheBackground() {
        this.jobs.add("cleanup.sh", JobWhen.AT_ONCE);
        final List<MachineJobs.Job> due = this.jobs.due(0L, false);
        assertEquals(1, due.size());
        assertEquals("cleanup.sh", due.getFirst().line());
        assertTrue(this.jobs.isEmpty(), "a job that runs once leaves the list once it has");
    }

    @Test
    void due_withCronOn_runsAScheduledJobAtItsHour() {
        this.jobs.add("backup.sh", JobWhen.at(6));
        final List<MachineJobs.Job> due = this.jobs.due(0L, true);
        assertEquals(1, due.size());
        assertEquals("backup.sh", due.getFirst().line());
    }

    @Test
    void due_switchedBackOn_runsWhatWasLeftWaiting() {
        this.jobs.add("backup.sh", JobWhen.at(6));
        assertTrue(this.jobs.due(0L, false).isEmpty());
        final List<MachineJobs.Job> due = this.jobs.due(0L, true);
        assertEquals(1, due.size(), "switching cron back on runs the job at the hour it is still due");
    }

    @Test
    void restore_neverHandsOutAnIdAKeptJobAlreadyHas() {
        this.jobs.restore(List.of(new MachineJobs.Job(7, "a.sh", JobWhen.AT_ONCE, -1L)), 1);

        assertEquals(8, this.jobs.nextId());
        assertEquals(8, this.jobs.add("b.sh", JobWhen.AT_ONCE).id());
    }

    @Test
    void roomForOneMore_countsTheJobsAlreadyHeld() {
        assertTrue(MachineJobs.roomForOneMore(16, 10, 5), "ten used and five jobs leave a megabyte for a sixth");
        assertFalse(MachineJobs.roomForOneMore(16, 10, 6), "six jobs fill what the programs leave");
        assertTrue(MachineJobs.roomForOneMore(0, 0, 1_000), "a machine that reports no memory is not held to it");
    }
}
