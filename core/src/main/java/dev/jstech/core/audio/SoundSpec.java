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
 * <p>A sound can instead be made as it plays, with no file of its own: a tune a synthesiser plays, a recording a
 * player keeps on a disk. It is still declared, so it has a subtitle and a channel and the player can turn it off
 * like any other; what it plays is handed over each time it is played.
 *
 * @param space    whether it comes from a place in the world or from the player's own screen
 * @param loop     whether it runs on until it is stopped, as a fan does, rather than playing once
 * @param channel  the channel it is mixed in, whose volume the player sets
 * @param files    the files it plays one of, picked at random each time; more than one keeps a sound from repeating
 * @param stream   whether its file is read as it plays rather than loaded whole, which a long loop wants
 * @param range    how many blocks it carries before it fades out, for a sound of the world
 * @param priority how much it matters beside other sounds when there are too many to play, higher first
 * @param made     whether it is made as it plays, from samples handed over each time, and so has no files
 * @param stereo   whether its files are stereo recordings: heard from a place in the world they are mixed down to one
 *                 channel as they play, and the speakers of a pair can each play one side
 */
public record SoundSpec(SoundSpace space, boolean loop, AudioChannel channel, List<ResourceLocation> files,
                        boolean stream, int range, int priority, boolean made, boolean stereo) {

    /** The priority a sound has when its declaration says nothing about it. */
    public static final int NORMAL = 50;

    /** A sound whose files hold one channel, as a sound of the world's files do. */
    public SoundSpec(final SoundSpace space, final boolean loop, final AudioChannel channel,
                     final List<ResourceLocation> files, final boolean stream, final int range, final int priority,
                     final boolean made) {
        this(space, loop, channel, files, stream, range, priority, made, false);
    }

    public SoundSpec {
        if (stereo && (made || loop)) {
            throw new IllegalArgumentException("a stereo recording is a file played once; what is made as it plays,"
                    + " or runs on, keeps to one channel so the world can place it");
        }
        files = List.copyOf(files);
        if (made && !files.isEmpty()) {
            throw new IllegalArgumentException("a sound made as it plays has no files of its own");
        }
        if (made && loop) {
            throw new IllegalArgumentException("a sound made as it plays runs as long as what it is handed");
        }
        if (!made && files.isEmpty()) {
            throw new IllegalArgumentException("a sound plays at least one file");
        }
        if (range < 1) {
            throw new IllegalArgumentException("a sound carries at least one block: " + range);
        }
    }
}
