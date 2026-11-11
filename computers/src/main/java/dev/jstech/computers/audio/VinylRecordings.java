/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.audio;

import dev.jstech.core.audio.media.MediaId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * The recordings a Vinyl RW Drive made from a music disc, and the disc's song each came from
 * (computers/docs/design/peripherals.md): only a song that came from a disc goes onto a vinyl, and the cut is that disc
 * itself. A recording is known by the sound it holds, so a copy of its file is the same recording; one brought from
 * elsewhere, never recorded from a disc here, is not.
 */
public final class VinylRecordings extends SavedData {

    private final Map<String, Entry> bySound = new HashMap<>();

    private static final String FILE = "jsc_vinyl_recordings";
    private static final String SOUND = "Sound";
    private static final String SONG = "Song";
    private static final String DAY = "Day";
    private static final String ALL = "Recordings";

    private VinylRecordings() {
    }

    /** The world's recordings, kept with the overworld. */
    public static VinylRecordings of(final ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(VinylRecordings::new, VinylRecordings::load), FILE);
    }

    /** Notes that {@code media} was recorded from the disc of {@code song} on world day {@code day}. */
    public void recorded(final MediaId media, final ResourceLocation song, final long day) {
        bySound.put(key(media), new Entry(song, day));
        setDirty();
    }

    /** The disc's song and the day a recording came from, or empty for one that came from no disc. */
    public Optional<Entry> of(final MediaId media) {
        return Optional.ofNullable(bySound.get(key(media)));
    }

    @Override
    public CompoundTag save(final CompoundTag tag, final HolderLookup.Provider registries) {
        final CompoundTag all = new CompoundTag();
        int index = 0;
        for (final Map.Entry<String, Entry> entry : bySound.entrySet()) {
            final CompoundTag one = new CompoundTag();
            one.putString(SOUND, entry.getKey());
            one.putString(SONG, entry.getValue().song().toString());
            one.putLong(DAY, entry.getValue().day());
            all.put(Integer.toString(index++), one);
        }
        tag.put(ALL, all);
        return tag;
    }

    private static VinylRecordings load(final CompoundTag tag, final HolderLookup.Provider registries) {
        final VinylRecordings out = new VinylRecordings();
        final CompoundTag all = tag.getCompound(ALL);
        for (final String index : all.getAllKeys()) {
            final CompoundTag one = all.getCompound(index);
            final ResourceLocation song = ResourceLocation.tryParse(one.getString(SONG));
            if (song != null) {
                out.bySound.put(one.getString(SOUND), new Entry(song, one.getLong(DAY)));
            }
        }
        return out;
    }

    /* A recording's sound, by what it holds and how. */
    private static String key(final MediaId media) {
        return media.hash() + "." + media.format();
    }

    /**
     * Where a recording came from.
     *
     * @param song the jukebox song of the disc it was recorded from
     * @param day  the world's day it was recorded on, from 1
     */
    public record Entry(ResourceLocation song, long day) {
    }
}
