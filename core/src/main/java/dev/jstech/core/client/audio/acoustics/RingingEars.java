/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.audio.acoustics;

import dev.jstech.core.audio.SoundContext;
import dev.jstech.core.audio.acoustics.AcousticCues;
import dev.jstech.core.audio.acoustics.EarRing;
import dev.jstech.core.client.audio.AudioEngine;
import dev.jstech.core.client.audio.AudioPrefsStore;

/**
 * The player's ears, ringing after a blast close by: while they ring, every sound comes muffled and dull, coming back
 * as the ringing fades. A player who turned it off on the Sound Mixer never has it.
 */
final class RingingEars {

    private static EarRing ring = EarRing.NONE;
    private static int left;

    private RingingEars() {
    }

    /** Rings the ears, harder or longer than they ring now, or as they ring now if that is more. */
    static synchronized void ring(final EarRing blast) {
        if (!AudioPrefsStore.prefs().ringingEars() || !blast.rings()) {
            return;
        }
        final boolean fresh = !ringing();
        ring = ringing() ? ring.louder(blast, left) : blast;
        left = ring.ticks();
        if (fresh) {
            AudioEngine.playCue(AcousticCues.RINGING, SoundContext.EMPTY, 0, 0, 0, (float) ring.strength(), 1F);
        }
    }

    /** A tick of the ears coming back. */
    static synchronized void tick() {
        if (left > 0) {
            left--;
        }
    }

    /** Stops the ringing at once, as the player leaves a world or turns it off. */
    static synchronized void stop() {
        ring = EarRing.NONE;
        left = 0;
    }

    /** Whether the ears ring now. */
    static synchronized boolean ringing() {
        return left > 0 && ring.rings();
    }

    /** What is left of a sound's loudness, from 1. */
    static synchronized float heard() {
        return (float) ring.heardAt(left);
    }

    /** What is left of a sound's high notes, from 1. */
    static synchronized float highs() {
        return (float) ring.highsAt(left);
    }
}
