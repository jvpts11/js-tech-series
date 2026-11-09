/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.portable;

/**
 * A portable's start, as its screen shows it from the moment it is switched on (computers/docs/design/wireless.md):
 * its maker's mark, then its system's own start, then its lock. Every screen that shows the device plays it from the
 * same game time, so the hand and the device held up agree.
 *
 * <p>Pure: no part of the game.
 */
public final class PortableBoot {

    /** How long the maker's mark shows, in ticks. */
    public static final long MARK_TICKS = 40L;
    /** How long the system's own start runs after it. */
    public static final long SYSTEM_TICKS = 60L;

    private PortableBoot() {
    }

    /** Where a device switched on at {@code onSince} (-1 for one off) has got to at {@code now}. */
    public static Stage stage(final long onSince, final long now) {
        if (onSince < 0) {
            return Stage.OFF;
        }
        final long since = now - onSince;
        if (since < MARK_TICKS) {
            return Stage.MARK;
        }
        return since < MARK_TICKS + SYSTEM_TICKS ? Stage.SYSTEM : Stage.READY;
    }

    /** How far through its system's start a device is at {@code now}, from 0 to 1. */
    public static float progress(final long onSince, final long now) {
        final long into = now - onSince - MARK_TICKS;
        return Math.max(0.0F, Math.min(1.0F, (float) into / SYSTEM_TICKS));
    }

    /** What a device's screen shows. */
    public enum Stage {
        /** Dark: switched off. */
        OFF,
        /** Its maker's mark. */
        MARK,
        /** Its system's own start. */
        SYSTEM,
        /** Its system, from the lock on. */
        READY
    }
}
