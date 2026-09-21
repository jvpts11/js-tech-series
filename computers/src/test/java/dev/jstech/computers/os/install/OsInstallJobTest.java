/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.install;

import dev.jstech.computers.os.media.MediaFormat;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OsInstallJobTest {

    /**
     * What the catalogue gives these two. Frames 95 is the one the comparisons are made with, because it is the
     * size that still moves between the floor and the ceiling; anything as big as Frames XP sits at the ceiling
     * whatever it is read from, which is a fact of its own and has a test of its own below.
     */
    private static final int FRAMES_95_MB = 48;
    private static final int FRAMES_XP_MB = 1_536;

    private static OsInstallJob beginning() {
        return OsInstallJob.beginning("jsc:frames_95", 0, 12L, FRAMES_95_MB, MediaFormat.CD, 2);
    }

    @Test
    void beginning_takesAsLongAsTheSameBytesWouldAsAProgram() {
        assertEquals(SetupTiming.ticks(FRAMES_95_MB, MediaFormat.CD, false, 2), beginning().ticksTotal());
    }

    @Test
    void beginning_onASlowerMedium_takesLonger() {
        final OsInstallJob disc = OsInstallJob.beginning("jsc:frames_95", 0, 12L, FRAMES_95_MB, MediaFormat.CD, 2);
        final OsInstallJob floppy =
                OsInstallJob.beginning("jsc:frames_95", 0, 12L, FRAMES_95_MB, MediaFormat.FLOPPY, 2);
        assertTrue(floppy.ticksTotal() > disc.ticksTotal(), "a floppy is a floppy");
    }

    @Test
    void beginning_onANewerMachine_isQuicker() {
        final OsInstallJob legacy = OsInstallJob.beginning("jsc:frames_95", 0, 12L, FRAMES_95_MB, MediaFormat.CD, 2);
        final OsInstallJob standard = OsInstallJob.beginning("jsc:frames_95", 0, 12L, FRAMES_95_MB, MediaFormat.CD, 4);
        assertTrue(standard.ticksTotal() < legacy.ticksTotal(), "what unpacks and writes it is the computer");
    }

    @Test
    void beginning_aBigSystemOnTheDiscItShipsOn_stopsAtTheCeilingAProgramStopsAt() {
        assertEquals(SetupTiming.MAX_SECONDS * SetupTiming.TICKS_PER_SECOND,
                OsInstallJob.beginning("jsc:frames_xp", 0, 12L, FRAMES_XP_MB, MediaFormat.CD, 2).ticksTotal());
    }

    @Test
    void beginning_theSameSystemOffSomethingFastEnough_comesInUnderTheCeiling() {
        assertTrue(OsInstallJob.beginning("jsc:frames_xp", 0, 12L, FRAMES_XP_MB, MediaFormat.USB, 4).ticksTotal()
                        < SetupTiming.MAX_SECONDS * SetupTiming.TICKS_PER_SECOND,
                "a stick on a modern machine is quick enough that the ceiling never comes into it");
    }

    @Test
    void tick_countsDownAndSaysWhenItIsDone() {
        final OsInstallJob job = new OsInstallJob("jsc:mc_dos", -1, 0L, 3, 3);
        assertFalse(job.tick(), "not on the first");
        assertFalse(job.tick(), "nor the second");
        assertTrue(job.tick(), "the third is the last");
        assertTrue(job.finished());
    }

    @Test
    void tick_pastTheEnd_staysDone() {
        final OsInstallJob job = new OsInstallJob("jsc:mc_dos", -1, 0L, 1, 1);
        job.tick();
        job.tick();
        assertEquals(0, job.ticksLeft());
        assertTrue(job.finished());
    }

    @Test
    void permille_walksFromNothingToTheWhole() {
        final OsInstallJob job = new OsInstallJob("jsc:mc_dos", -1, 0L, 4, 4);
        assertEquals(0, job.permille());
        job.tick();
        job.tick();
        assertEquals(500, job.permille());
        job.tick();
        job.tick();
        assertEquals(1000, job.permille());
    }

    @Test
    void aJobBroughtBackHalfWay_keepsWhereItWas() {
        final OsInstallJob job = new OsInstallJob("jsc:debian", 2, 99L, 100, 40);
        assertEquals("jsc:debian", job.osId());
        assertEquals(2, job.targetSlot());
        assertEquals(99L, job.readerPos());
        assertEquals(600, job.permille());
    }

    /*
     * A drive's position is packed into a long, and anything west or north of the world's origin packs into a
     * negative one. Reading "no drive" as "less than zero" meant that half the world's machines never noticed the
     * medium being taken out of them, which is the kind of thing that only shows up where the world put you.
     */
    @Test
    void aReaderWestOfTheOrigin_isStillAReader() {
        assertTrue(new OsInstallJob("jsc:debian", 0, -12_229_062L, 100, 100).hasReader());
        assertTrue(new OsInstallJob("jsc:debian", 0, 0L, 100, 100).hasReader());
        assertTrue(new OsInstallJob("jsc:debian", 0, -1L, 100, 100).hasReader());
    }

    @Test
    void aJobReadFromNoDrive_saysSo() {
        assertFalse(new OsInstallJob("jsc:debian", 0, OsInstallJob.NO_READER, 100, 100).hasReader());
    }

    @Test
    void aJobOfNoLength_isStillOneTick() {
        assertEquals(1, new OsInstallJob("jsc:mc_dos", -1, 0L, 0, 0).ticksTotal());
    }
}
