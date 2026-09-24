/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Keeps one sound from one source from being heard more than once in a short while.
 *
 * <p>Fifty Operations settling in the same tick are one chime, not fifty stacked on top of each other: whatever asks
 * for a sound passes through here with what it is and where it comes from, and only the first ask inside the
 * cooldown is let through. What it remembers is pruned as it goes, so a long session does not grow it without end.
 */
public final class SoundGate {

    private final Map<String, Long> lastHeard = new HashMap<>();
    private final int cooldownTicks;
    private long nextPrune;

    /** How many ticks apart the gate empties itself of what no longer matters. */
    private static final long PRUNE_EVERY = 200L;

    /** A gate that lets one sound from one source through at most once every {@code cooldownTicks} ticks. */
    public SoundGate(final int cooldownTicks) {
        if (cooldownTicks < 1) {
            throw new IllegalArgumentException("a cooldown is at least one tick: " + cooldownTicks);
        }
        this.cooldownTicks = cooldownTicks;
    }

    /**
     * Whether the sound {@code sound} from the source {@code source} may be heard at tick {@code now}; asking counts
     * as hearing it when the answer is yes.
     */
    public synchronized boolean allow(final String source, final String sound, final long now) {
        prune(now);
        final String key = source + '|' + sound;
        final Long last = lastHeard.get(key);
        if (last != null && now - last < cooldownTicks) {
            return false;
        }
        lastHeard.put(key, now);
        return true;
    }

    /** How many sounds the gate is remembering, for a test to watch it empty itself. */
    public synchronized int remembered() {
        return lastHeard.size();
    }

    private void prune(final long now) {
        if (now < nextPrune) {
            return;
        }
        nextPrune = now + PRUNE_EVERY;
        final Iterator<Long> heard = lastHeard.values().iterator();
        while (heard.hasNext()) {
            if (now - heard.next() >= cooldownTicks) {
                heard.remove();
            }
        }
    }
}
