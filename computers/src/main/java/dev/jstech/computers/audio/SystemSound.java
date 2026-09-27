/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.audio;

import dev.jstech.core.audio.SoundCue;
import dev.jstech.core.id.IStableName;

/**
 * The moments a system has a sound of its own for: coming up to its desktop, going down, an error box, a notice, the
 * bell of an action that goes nowhere, and a device plugged in or pulled out.
 */
public enum SystemSound implements IMachineCue, IStableName {

    STARTUP("startup"),
    SHUTDOWN("shutdown"),
    ERROR("error"),
    NOTIFY("notify"),
    BEEP("beep"),
    DEVICE_CONNECT("device_connect"),
    DEVICE_DISCONNECT("device_disconnect");

    private final String serializedName;

    SystemSound(final String serializedName) {
        this.serializedName = serializedName;
    }

    /** The cue this moment raises, which picks the sound by the system and the desktop the machine runs. */
    @Override
    public SoundCue cue() {
        return switch (this) {
            case STARTUP -> ComputingSounds.SYSTEM_STARTUP;
            case SHUTDOWN -> ComputingSounds.SYSTEM_SHUTDOWN;
            case ERROR -> ComputingSounds.SYSTEM_ERROR;
            case NOTIFY -> ComputingSounds.SYSTEM_NOTIFY;
            case BEEP -> ComputingSounds.SYSTEM_BEEP;
            case DEVICE_CONNECT -> ComputingSounds.SYSTEM_DEVICE_CONNECT;
            case DEVICE_DISCONNECT -> ComputingSounds.SYSTEM_DEVICE_DISCONNECT;
        };
    }

    /**
     * How long it holds a voice of the machine's sound hardware, in ticks: as long as the longest chime a system of
     * the series plays for it, six seconds to come up or go down, two for an error, a second and a half for a notice
     * and one for the rest.
     */
    @Override
    public int voiceTicks() {
        return switch (this) {
            case STARTUP, SHUTDOWN -> 120;
            case ERROR -> 40;
            case NOTIFY -> 30;
            case BEEP, DEVICE_CONNECT, DEVICE_DISCONNECT -> 20;
        };
    }

    @Override
    public String serializedName() {
        return serializedName;
    }
}
