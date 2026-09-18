/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.boot;

import dev.jstech.computers.os.Platform;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class BootSplashTest {

    /** Each edition of the family comes up behind its own picture, read off where it sits in that family. */
    @Test
    void of_givesEachFramesEditionItsOwnPicture() {
        assertSame(BootSplash.FRAMES_95, BootSplash.of(Platform.FRAMES, 1));
        assertSame(BootSplash.FRAMES_XP, BootSplash.of(Platform.FRAMES, 2));
        assertSame(BootSplash.FRAMES_11, BootSplash.of(Platform.FRAMES, 3));
    }

    /** Everything else reads out its own start instead, which is what those systems really did. */
    @Test
    void of_leavesEveryOtherPlatformWithTheWordsItPrints() {
        for (final Platform platform : Platform.values()) {
            if (platform == Platform.FRAMES) {
                continue;
            }
            assertSame(BootSplash.PLAIN, BootSplash.of(platform, 1), platform.name());
            assertSame(BootSplash.PLAIN, BootSplash.of(platform, 3), platform.name());
        }
    }

    /**
     * A family the mod does not ship gets the plain one rather than a picture that is not its own. There is no
     * fourth edition, and an addon that says it is the fourth of the family is not one of these three.
     */
    @Test
    void of_aPlaceNoEditionHolds_isThePlainOne() {
        assertSame(BootSplash.PLAIN, BootSplash.of(Platform.FRAMES, 0));
        assertSame(BootSplash.PLAIN, BootSplash.of(Platform.FRAMES, 4));
        assertSame(BootSplash.PLAIN, BootSplash.of(Platform.FRAMES, -1));
    }

    /** A machine with nothing installed is still asked, and answers rather than falling over. */
    @Test
    void of_noSystem_isThePlainOne() {
        assertSame(BootSplash.PLAIN, BootSplash.of(null, 1));
    }

    /** The name is what crosses the wire, so it has to survive the round trip and refuse nothing. */
    @Test
    void byName_readsBackEveryPictureAndFallsBackToThePlainOne() {
        for (final BootSplash splash : BootSplash.values()) {
            assertSame(splash, BootSplash.byName(splash.serializedName()));
        }
        assertSame(BootSplash.PLAIN, BootSplash.byName("something-nothing-answers-to"));
        assertSame(BootSplash.PLAIN, BootSplash.byName(""));
    }

    /** The names are the stable ones, since a save and a packet both carry them. */
    @Test
    void serializedName_isTheOneWrittenDown() {
        assertEquals("plain", BootSplash.PLAIN.serializedName());
        assertEquals("frames_95", BootSplash.FRAMES_95.serializedName());
        assertEquals("frames_xp", BootSplash.FRAMES_XP.serializedName());
        assertEquals("frames_11", BootSplash.FRAMES_11.serializedName());
    }
}
