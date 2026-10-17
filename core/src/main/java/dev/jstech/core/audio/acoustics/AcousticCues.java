/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio.acoustics;

import dev.jstech.core.audio.AudioChannels;
import dev.jstech.core.audio.SoundCue;
import dev.jstech.core.registry.CoreItems;

/**
 * The moments of how sound travels that make a sound of their own: the ring in the ears after a blast, and a blow
 * striking a living thing, which sounds as what it strikes is made of. Each is a cue bound to nothing, silent until a
 * resource pack or the series' sound pass gives it its sound.
 */
public final class AcousticCues {

    /** The ring in the ears after a blast close by, heard from inside the player's own head. */
    public static final SoundCue RINGING = CoreItems.CONTENT.cue("ears/ringing").onScreen()
            .channel(AudioChannels.AMBIENCE).register();
    /**
     * A blow or a shot striking a living thing, at it, its sound picked by the kind of thing struck ({@code
     * SoundContext.ENTITY}): a golem clangs, a skeleton clacks, as a pack binds it.
     */
    public static final SoundCue IMPACT = CoreItems.CONTENT.cue("impact/entity").world().range(16).register();

    private AcousticCues() {
    }

    /** Declares the cues while the Core's content is declared. */
    public static void declare() {
        // Loading the class declares them.
    }
}
