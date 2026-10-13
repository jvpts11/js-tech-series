/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.audio.mixer;

import dev.jstech.core.JsCore;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;

/** The Sound Mixer's colours, the game's own greys for what its options say: {@code jscore:audio/sound_mixer}. */
@PaletteHolder
final class SoundMixerPalette {

    static final Palette<Colours> PALETTE = Palettes.declare(JsCore.MODID, "audio/sound_mixer",
            new Colours(0xFFA0A0A0, 0xFF808080, 0xFFFFFFFF, 0xFF808080, 0xFF909090, 0xFF606060, 0xFF141414));

    private SoundMixerPalette() {
    }

    /** The colours as the loaded palette has them now. */
    static Colours get() {
        return PALETTE.get();
    }

    /**
     * The mixer's colours.
     *
     * @param note    a note under the options, and the count of sounds turned off
     * @param hint    the search field's hint
     * @param name    a sound's name in the list
     * @param nameOff the name of a sound the player turned off
     * @param id      a sound's id under its name
     * @param idOff   the id of a sound the player turned off
     * @param ground  what the list's text is written on, which its shadow is worked out against
     */
    record Colours(int note, int hint, int name, int nameOff, int id, int idOff, int ground) {
    }
}
