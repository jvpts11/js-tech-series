/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.audio;

import dev.jstech.core.audio.SoundCue;

/** The moments a system has a sound of its own for: coming up to its desktop, going down, and an error box. */
public enum SystemSound {

    STARTUP,
    SHUTDOWN,
    ERROR;

    /** The cue this moment raises, which picks the sound by the system the machine runs. */
    public SoundCue cue() {
        return switch (this) {
            case STARTUP -> ComputingSounds.SYSTEM_STARTUP;
            case SHUTDOWN -> ComputingSounds.SYSTEM_SHUTDOWN;
            case ERROR -> ComputingSounds.SYSTEM_ERROR;
        };
    }

    /**
     * How long it holds a voice of the machine's sound hardware, in ticks: as long as the longest chime a system of
     * the series plays for it, six seconds to come up or go down and one for an error.
     */
    public int voiceTicks() {
        return switch (this) {
            case STARTUP, SHUTDOWN -> 120;
            case ERROR -> 20;
        };
    }
}
