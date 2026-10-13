/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio;

import net.minecraft.resources.ResourceLocation;

/**
 * Something that happens and may be heard, raised by the code by name (a computer powering on, a drive failing),
 * with what is heard bound to it in the assets: {@code assets/<namespace>/sound_cues/<path>.json} holds the rules
 * that pick its sound by context. The mod ships its own bindings, generated from this declaration, and a resource
 * pack binds a cue to other sounds by shipping a file of the same name.
 *
 * <p>The server sends a cue, not a sound: each client picks the sound from the bindings its own packs give.
 *
 * @param id       what the code raises it by
 * @param space    whether it is heard from a place in the world or from the player's own screen
 * @param channel  the channel a sound bound to it is mixed in when that sound is not one the series declared
 * @param range    how many blocks away a player still hears it, for a cue of the world
 * @param defaults the rules it has when no pack binds it, which the generator writes as its file
 */
public record SoundCue(ResourceLocation id, SoundSpace space, AudioChannel channel, int range, SoundSet defaults) {

    public SoundCue {
        if (range < 1) {
            throw new IllegalArgumentException("a cue carries at least one block: " + range);
        }
    }

    /** Where its bindings are read from. */
    public ResourceLocation file() {
        return ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "sound_cues/" + id.getPath() + ".json");
    }
}
