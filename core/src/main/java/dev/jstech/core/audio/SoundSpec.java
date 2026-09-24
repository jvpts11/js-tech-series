/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio;

import java.util.List;
import net.minecraft.resources.ResourceLocation;

/**
 * What a declared sound is: where it is heard from, whether it runs on, which channel it is mixed in, which files
 * it plays one of, how far it carries and how much it matters when too many sounds want to play at once.
 *
 * @param space    whether it comes from a place in the world or from the player's own screen
 * @param loop     whether it runs on until it is stopped, as a fan does, rather than playing once
 * @param channel  the channel it is mixed in, whose volume the player sets
 * @param files    the files it plays one of, picked at random each time; more than one keeps a sound from repeating
 * @param stream   whether its file is read as it plays rather than loaded whole, which a long loop wants
 * @param range    how many blocks it carries before it fades out, for a sound of the world
 * @param priority how much it matters beside other sounds when there are too many to play, higher first
 */
public record SoundSpec(SoundSpace space, boolean loop, AudioChannel channel, List<ResourceLocation> files,
                        boolean stream, int range, int priority) {

    /** The priority a sound has when its declaration says nothing about it. */
    public static final int NORMAL = 50;

    public SoundSpec {
        files = List.copyOf(files);
        if (files.isEmpty()) {
            throw new IllegalArgumentException("a sound plays at least one file");
        }
        if (range < 1) {
            throw new IllegalArgumentException("a sound carries at least one block: " + range);
        }
    }
}
