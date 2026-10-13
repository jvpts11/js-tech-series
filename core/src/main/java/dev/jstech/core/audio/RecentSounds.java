/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The sounds heard lately, each once and newest first: what a player looks through for the sound that has just
 * annoyed them. It keeps a fixed number, the oldest going first, so a long session costs no more than a short one.
 */
public final class RecentSounds {

    private final int capacity;
    private final LinkedHashMap<String, Long> heard = new LinkedHashMap<>();

    /** Keeps up to that many sounds. */
    public RecentSounds(final int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("room for at least one sound: " + capacity);
        }
        this.capacity = capacity;
    }

    /** Notes that a sound was heard at that time, which moves it to the front if it was heard before. */
    public synchronized void heard(final String sound, final long now) {
        heard.remove(sound);
        heard.put(sound, now);
        if (heard.size() > capacity) {
            final Iterator<String> oldest = heard.keySet().iterator();
            oldest.next();
            oldest.remove();
        }
    }

    /** The sounds heard at or after that time, newest first. */
    public synchronized List<String> since(final long from) {
        final List<String> out = new ArrayList<>();
        for (final Map.Entry<String, Long> one : heard.entrySet()) {
            if (one.getValue() >= from) {
                out.add(one.getKey());
            }
        }
        return out.reversed();
    }

    /** Forgets everything heard, as when the player leaves a world. */
    public synchronized void clear() {
        heard.clear();
    }
}
