/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.input;

/**
 * A key held over one thing until it has been held long enough, so a stray press does nothing. The hold starts again
 * when the thing under it changes or the key is let go; once it is done, the key has to be let go before another
 * hold starts, so a key still down when a page it opened is closed does not open the page again.
 */
public final class KeyHold {

    private final long holdMillis;
    private String over = "";
    private long since;
    private boolean spent;

    /** A hold done after {@code holdMillis} milliseconds. */
    public KeyHold(final long holdMillis) {
        if (holdMillis <= 0L) {
            throw new IllegalArgumentException("a hold takes some time, not " + holdMillis + " ms");
        }
        this.holdMillis = holdMillis;
    }

    /**
     * Follows the key for one moment.
     *
     * @param thing what the key is held over, or empty for nothing it can be held over
     * @param held  whether the key is down
     * @param now   the time, in milliseconds
     * @return whether the hold over {@code thing} has just been done
     */
    public boolean follow(final String thing, final boolean held, final long now) {
        if (!held) {
            this.spent = false;
            this.over = "";
            return false;
        }
        if (this.spent || thing.isEmpty()) {
            this.over = "";
            return false;
        }
        if (!thing.equals(this.over)) {
            this.over = thing;
            this.since = now;
            return false;
        }
        if (now - this.since < this.holdMillis) {
            return false;
        }
        this.over = "";
        this.spent = true;
        return true;
    }

    /** How far the hold has come, from 0 to 1, and 0 while the key is held over nothing. */
    public float progress(final long now) {
        if (this.over.isEmpty()) {
            return 0.0F;
        }
        return Math.max(0.0F, Math.min(1.0F, (now - this.since) / (float) this.holdMillis));
    }

    /** What the key is held over, or empty. */
    public String over() {
        return this.over;
    }
}
