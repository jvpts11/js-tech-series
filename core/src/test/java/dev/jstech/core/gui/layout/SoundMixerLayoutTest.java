/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SoundMixerLayoutTest {

    private static final int SERIES_CHANNELS = 7;

    @Test
    void channels_fitWithoutCollidingOnTheSmallestScreenAndATypicalOne() {
        assertClean(SoundMixerLayout.channels(SoundMixerLayout.SMALLEST_WIDTH, SoundMixerLayout.SMALLEST_HEIGHT,
                SERIES_CHANNELS, 2));
        assertClean(SoundMixerLayout.channels(640, 360, SERIES_CHANNELS, 2));
    }

    @Test
    void channels_leaveRoomForAddonsOnATypicalScreen() {
        assertClean(SoundMixerLayout.channels(640, 360, 12, 2));
    }

    @Test
    void sounds_fitWithoutColliding() {
        assertClean(SoundMixerLayout.sounds(SoundMixerLayout.SMALLEST_WIDTH, SoundMixerLayout.SMALLEST_HEIGHT));
        assertClean(SoundMixerLayout.sounds(640, 360));
    }

    @Test
    void options_fitWithoutColliding() {
        assertClean(SoundMixerLayout.options(SoundMixerLayout.SMALLEST_WIDTH, SoundMixerLayout.SMALLEST_HEIGHT, 3));
        assertClean(SoundMixerLayout.options(640, 360, 2));
    }

    @Test
    void positions_matchTheApprovedScreen() {
        assertEquals(165, SoundMixerLayout.left(640));
        assertEquals(325, SoundMixerLayout.channelX(640, 1));
        assertEquals(111, SoundMixerLayout.channelY(6));
        assertEquals(144, SoundMixerLayout.resetY(SERIES_CHANNELS));
        assertEquals(180, SoundMixerLayout.channelsNoteY(SERIES_CHANNELS));
        assertEquals(122, SoundMixerLayout.OPTIONS_NOTE_TOP);
        assertEquals(332, SoundMixerLayout.doneY(360));
        assertEquals(322, SoundMixerLayout.listBottom(360));
    }

    private static void assertClean(final GuiLayout layout) {
        assertTrue(layout.isClean(), () -> "overlaps " + layout.overlaps() + ", out of bounds " + layout.outOfBounds());
    }
}
