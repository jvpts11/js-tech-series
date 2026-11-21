/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.chain.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.chain.machine.PrebuilderCues.Sound;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PrebuilderCuesTest {

    /* Two trips, at 0.08 and 0.16 of the loop, each 0.134 long, taken at 0.27 and seated at 0.76 of it. */
    private static final PrebuilderCues JOB = PrebuilderCues.of(new float[] {0.08F, 0.16F}, 0.134F, 0.27F, 0.76F,
            0.3F);

    @Test
    void of_hearsThePressThenEachTripsArmTwiceAndItsPartThenThePressAgain() {
        final List<Sound> all = JOB.crossed(PrebuilderCues.notYet(), 1);
        assertEquals(Sound.PRESS, all.getFirst(), "the lift's press first: " + all);
        assertEquals(Sound.PRESS, all.getLast(), "and again as the computer sinks: " + all);
        assertEquals(4, all.stream().filter(Sound.ARM::equals).count(), "an arm sets off and carries, a trip each");
        assertEquals(2, all.stream().filter(Sound.PART::equals).count(), "a part into its seat, a trip each");
    }

    @Test
    void crossed_hearsEachCueOnceAsTheLoopGoesOn() {
        final List<Sound> heard = new ArrayList<>();
        float at = PrebuilderCues.notYet();
        for (int tick = 0; tick <= 40; tick++) {
            final float now = tick * 0.01F;
            heard.addAll(JOB.crossed(at, now));
            at = now;
        }
        assertEquals(JOB.crossed(PrebuilderCues.notYet(), 1), heard, "tick by tick, every cue once and in order");
    }

    @Test
    void crossed_aMomentBehindIsANewJobHeardFromItsStart() {
        final List<Sound> again = JOB.crossed(0.35F, 0.01F);
        assertTrue(again.size() == 1 && again.getFirst() == Sound.PRESS, "a new job's press is heard: " + again);
    }

    @Test
    void crossed_standingStillHearsNothing() {
        assertTrue(JOB.crossed(0.2F, 0.2F).isEmpty());
    }
}
