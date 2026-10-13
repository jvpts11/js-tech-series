/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class RecentSoundsTest {

    @Test
    void since_givesEachSoundOnceNewestFirst() {
        final RecentSounds recent = new RecentSounds(8);
        recent.heard("a", 10);
        recent.heard("b", 20);
        recent.heard("a", 30);
        recent.heard("c", 40);
        assertEquals(List.of("c", "a", "b"), recent.since(0));
    }

    @Test
    void since_leavesOutWhatWasHeardBefore() {
        final RecentSounds recent = new RecentSounds(8);
        recent.heard("old", 5);
        recent.heard("new", 50);
        assertEquals(List.of("new"), recent.since(40));
    }

    @Test
    void heard_forgetsTheOldestPastItsRoom() {
        final RecentSounds recent = new RecentSounds(2);
        recent.heard("a", 1);
        recent.heard("b", 2);
        recent.heard("c", 3);
        assertEquals(List.of("c", "b"), recent.since(0));
    }

    @Test
    void clear_forgetsEverything() {
        final RecentSounds recent = new RecentSounds(4);
        recent.heard("a", 1);
        recent.clear();
        assertTrue(recent.since(0).isEmpty());
    }

    @Test
    void constructor_needsRoomForOne() {
        assertThrows(IllegalArgumentException.class, () -> new RecentSounds(0));
    }
}
