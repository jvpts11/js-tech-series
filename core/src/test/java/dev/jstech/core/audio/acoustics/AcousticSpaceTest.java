/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio.acoustics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AcousticSpaceTest {

    @Test
    void reverb_isDryInTheOpenAir() {
        assertEquals(Reverb.DRY, AcousticSpace.OPEN.reverb());
        assertFalse(AcousticSpace.OPEN.reverb().heard());
    }

    @Test
    void reverb_ringsLongerInABiggerSpaceOfHarderWalls() {
        final Reverb cellar = new AcousticSpace(3, 1, 0.06, 0, 0).reverb();
        final Reverb cave = new AcousticSpace(20, 1, 0.06, 0, 0).reverb();
        final Reverb padded = new AcousticSpace(20, 1, 0.7, 0, 0).reverb();
        assertTrue(cave.decaySeconds() > cellar.decaySeconds(), cellar + " / " + cave);
        assertTrue(padded.decaySeconds() < cave.decaySeconds(), cave + " / " + padded);
        assertTrue(cave.reflectionsDelay() > cellar.reflectionsDelay(), "a far wall answers later");
    }

    @Test
    void reverb_ofASealedRoomFollowsEyringsFormula() {
        // A stone room of 4 by 4 by 3: 48 cubic metres of air, 80 square metres of walls.
        final Reverb room = new AcousticSpace(2, 0.5, 0.06, 0, 0).inRoom(48, 80).reverb();
        final double expected = 0.161 * 48 / (80 * -Math.log(1 - 0.06));
        assertEquals(expected, room.decaySeconds(), 1e-9);
        assertTrue(room.wet() > 0.9, "walled all round, the echo is loud: " + room.wet());
    }

    @Test
    void reverb_softWallsKeepLessOfTheHighs() {
        final Reverb stone = new AcousticSpace(8, 1, 0.06, 0, 0).reverb();
        final Reverb wool = new AcousticSpace(8, 1, 0.7, 0, 0).reverb();
        assertTrue(wool.highRatio() < stone.highRatio());
        assertTrue(wool.wet() < stone.wet(), "what soft walls send back is fainter");
    }

    @Test
    void reverb_isHeldToWhatTheSoundCardMakes() {
        final Reverb huge = new AcousticSpace(10_000, 1, 0.01, 0, 0).reverb();
        assertEquals(Reverb.LONGEST, huge.decaySeconds());
        assertEquals(0.3, huge.reflectionsDelay(), 1e-9);
        assertEquals(0.1, huge.lateDelay(), 1e-9);
    }

    @Test
    void constructor_refusesAShareOutOfRange() {
        assertThrows(IllegalArgumentException.class, () -> new AcousticSpace(1, 1.5, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new AcousticSpace(1, 1, -0.1, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new Reverb(30, 0.5, 0, 0, 1));
    }
}
