/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.audio;

import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Where a system sends its sound: out of the machine's monitors, out of its speakers, or out of both. A choice with
 * nothing linked to play it gives way to what is linked, as a system moves to the device that is left when one is
 * unplugged, so choosing never silences a machine that has somewhere to play.
 */
public enum SoundOutput {

    MONITOR("monitor"),
    SPEAKERS("speakers"),
    BOTH("both");

    private final String id;

    SoundOutput(final String id) {
        this.id = id;
    }

    /** The output under that name, case aside, or null when there is none by it. */
    @Nullable
    public static SoundOutput byId(@Nullable final String id) {
        final String wanted = id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
        for (final SoundOutput each : values()) {
            if (each.id.equals(wanted)) {
                return each;
            }
        }
        return null;
    }

    /** The name it is typed and saved by. */
    public String id() {
        return id;
    }

    /** Whether the monitors play, given whether the machine has any speakers and any monitors. */
    public boolean monitorsPlay(final boolean hasMonitors, final boolean hasSpeakers) {
        return hasMonitors && (this != SPEAKERS || !hasSpeakers);
    }

    /** Whether the speakers play, given whether the machine has any speakers and any monitors. */
    public boolean speakersPlay(final boolean hasMonitors, final boolean hasSpeakers) {
        return hasSpeakers && (this != MONITOR || !hasMonitors);
    }
}
