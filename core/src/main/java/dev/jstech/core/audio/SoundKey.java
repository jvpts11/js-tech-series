/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio;

import dev.jstech.core.text.TextKey;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

/**
 * A sound as the code knows it: declared once, with what it is and what its subtitle says, and played through
 * {@link Audio} on the server or the client's audio engine, never through the game's own calls. Everything the game
 * needs to know about it (the registered event, the entry in {@code sounds.json}, the subtitle in the language
 * files) is made from the one declaration.
 *
 * @param id       what the sound is known by, which is also its event's id
 * @param spec     what it is
 * @param subtitle what the subtitle says while it plays, in the player's language
 * @param event    the registered event, there once the game has registered it
 */
public record SoundKey(ResourceLocation id, SoundSpec spec, TextKey subtitle, Supplier<SoundEvent> event) {

    /** The key a subtitle is translated under, in the game's own form: {@code subtitles.<namespace>.<path>}. */
    public static String subtitleKey(final ResourceLocation id) {
        return "subtitles." + id.getNamespace() + "." + id.getPath().replace('/', '.');
    }
}
