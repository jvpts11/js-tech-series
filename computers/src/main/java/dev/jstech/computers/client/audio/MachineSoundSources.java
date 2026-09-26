/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.audio;

import dev.jstech.core.audio.IAudible;
import dev.jstech.core.client.audio.SoundDirector;

/**
 * Hands a machine's running sounds to the client's sound director, and takes them back when the machine goes.
 *
 * <p>The block entities that call this are shared by both sides, so they call it only on the client; this class
 * is where the client's own sound classes are named, so a dedicated server never loads them.
 */
public final class MachineSoundSources {

    private MachineSoundSources() {
    }

    public static void track(final IAudible source) {
        SoundDirector.track(source);
    }

    public static void untrack(final IAudible source) {
        SoundDirector.untrack(source);
    }
}
