/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer;

/**
 * Changes to files counted as they come, and handed over once they have stopped for a while: an editor saving a file,
 * or a build copying a hundred, sets off one reload when it is done, not one for each file.
 */
final class ResourceChanges {

    private final long quietMillis;
    private int changed;
    private long lastChange;

    ResourceChanges(final long quietMillis) {
        this.quietMillis = quietMillis;
    }

    /** Counts a file that changed at {@code nowMillis}. */
    synchronized void changed(final long nowMillis) {
        this.changed++;
        this.lastChange = nowMillis;
    }

    /**
     * How many files changed, once none has for {@link #quietMillis} by {@code nowMillis}, and forgets them; nought
     * while there are none, or while they are still changing.
     */
    synchronized int take(final long nowMillis) {
        if (this.changed == 0 || nowMillis - this.lastChange < this.quietMillis) {
            return 0;
        }
        final int taken = this.changed;
        this.changed = 0;
        return taken;
    }
}
