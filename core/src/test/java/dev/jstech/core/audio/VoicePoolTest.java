/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VoicePoolTest {

    private static final long FOREVER = Long.MAX_VALUE;

    private VoicePool pool;

    @BeforeEach
    void setUp() {
        pool = new VoicePool();
    }

    @Test
    void claim_takesFreeVoicesAndStealsNothing() {
        assertEquals(List.of(), pool.claim(9, "song", 2, 0L, FOREVER));
        assertEquals(List.of(), pool.claim(9, "chord", 7, 0L, FOREVER));
        assertEquals(9, pool.held(0L));
    }

    @Test
    void claim_stealsTheOldestWhenNoVoiceIsFree() {
        pool.claim(9, "song", 2, 0L, FOREVER);
        pool.claim(9, "chord", 7, 1L, FOREVER);
        assertEquals(List.of("song"), pool.claim(9, "beep", 1, 2L, FOREVER), "the first to start gives way");
        assertFalse(pool.holds("song", 2L));
        assertTrue(pool.holds("chord", 2L) && pool.holds("beep", 2L));
    }

    @Test
    void claim_onOneVoiceCutsEachSoundOffWithTheNext() {
        pool.claim(1, "first", 1, 0L, FOREVER);
        assertEquals(List.of("first"), pool.claim(1, "second", 1, 1L, FOREVER));
        assertEquals(List.of("second"), pool.claim(1, "third", 1, 2L, FOREVER));
    }

    @Test
    void claim_givesASoundThatWantsTooManyEveryVoiceThereIs() {
        pool.claim(4, "a", 1, 0L, FOREVER);
        pool.claim(4, "b", 1, 0L, FOREVER);
        assertEquals(List.of("a", "b"), pool.claim(4, "huge", 10, 1L, FOREVER));
        assertEquals(4, pool.held(1L));
    }

    @Test
    void claim_countsNothingThatHasRunOut() {
        pool.claim(2, "short", 2, 0L, 20L);
        assertEquals(List.of(), pool.claim(2, "later", 2, 20L, FOREVER), "a sound over by now frees its voices");
    }

    @Test
    void claim_underAnIdAlreadyHeldReplacesIt() {
        pool.claim(2, "song", 2, 0L, FOREVER);
        assertEquals(List.of(), pool.claim(2, "song", 2, 5L, FOREVER), "a sound starting again takes back its own");
        assertEquals(2, pool.held(5L));
    }

    @Test
    void claim_onADeviceWithNoVoicesTakesNothing() {
        assertEquals(List.of(), pool.claim(0, "silent", 1, 0L, FOREVER));
        assertEquals(0, pool.held(0L));
    }

    @Test
    void release_freesTheVoices() {
        pool.claim(2, "song", 2, 0L, FOREVER);
        pool.release("song");
        assertEquals(0, pool.held(0L));
    }
}
