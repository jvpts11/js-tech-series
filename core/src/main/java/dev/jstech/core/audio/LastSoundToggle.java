/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio;

import org.jetbrains.annotations.Nullable;

/**
 * What the key that turns off the last sound does: it turns off the sound the player last heard, and a second press
 * soon after brings that sound back, for when it was the wrong one.
 */
public final class LastSoundToggle {

    private final long undoWindow;
    private @Nullable String undoable;
    private long turnedOffAt;

    /** How long after a sound is turned off the key brings it back, in milliseconds, unless said otherwise. */
    public static final long DEFAULT_UNDO_MILLIS = 5_000L;

    /** A toggle whose second press within {@code undoWindow}, in the unit presses are timed in, undoes the first. */
    public LastSoundToggle(final long undoWindow) {
        this.undoWindow = undoWindow;
    }

    /** What a press did. */
    public enum Kind {
        /** Nothing had been heard, so nothing was turned off. */
        NOTHING,
        /** The last sound heard was turned off. */
        TURNED_OFF,
        /** The sound the press before turned off was turned back on. */
        BACK_ON
    }

    /**
     * What a press did and to which sound.
     *
     * @param kind  what it did
     * @param sound the sound's id, or null when it did nothing
     */
    public record Outcome(Kind kind, @Nullable String sound) {
    }

    /**
     * Presses the key.
     *
     * @param lastHeard the last sound heard, or null when none has been
     * @param now       the time of the press
     * @param prefs     the player's preferences, where the sound is turned off or back on
     */
    public synchronized Outcome press(@Nullable final String lastHeard, final long now, final AudioPrefs prefs) {
        final String back = undoable;
        undoable = null;
        if (back != null && now - turnedOffAt <= undoWindow) {
            prefs.setMuted(back, false);
            return new Outcome(Kind.BACK_ON, back);
        }
        if (lastHeard == null) {
            return new Outcome(Kind.NOTHING, null);
        }
        prefs.setMuted(lastHeard, true);
        undoable = lastHeard;
        turnedOffAt = now;
        return new Outcome(Kind.TURNED_OFF, lastHeard);
    }
}
