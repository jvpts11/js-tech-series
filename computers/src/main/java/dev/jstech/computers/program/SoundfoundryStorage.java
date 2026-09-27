/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

/**
 * Soundfoundry's state as a machine's save keeps it, under the one compound it is written to. Nothing is written for
 * a machine that never used it.
 */
final class SoundfoundryStorage {

    private static final String KEY = "Soundfoundry";

    private SoundfoundryStorage() {
    }

    static void save(final SoundfoundryState state, final CompoundTag tag) {
        if (state.size() == 0 && !state.shuffle() && !state.repeat()
                && state.volume() == SoundfoundryState.DEFAULT_VOLUME && state.balance() == 0) {
            return;
        }
        final CompoundTag s = new CompoundTag();
        final ListTag songs = new ListTag();
        for (final String song : state.songs()) {
            songs.add(StringTag.valueOf(song));
        }
        s.put("Songs", songs);
        s.putInt("Current", state.current());
        s.putBoolean("Shuffle", state.shuffle());
        s.putBoolean("Repeat", state.repeat());
        s.putInt("Volume", state.volume());
        s.putInt("Balance", state.balance());
        tag.put(KEY, s);
    }

    /** Puts the state back as the tag has it; a tag without one leaves it as a machine that never used it. */
    static void load(final SoundfoundryState state, final CompoundTag tag) {
        final CompoundTag s = tag.getCompound(KEY);
        state.clear();
        final List<String> songs = new ArrayList<>();
        for (final Tag song : s.getList("Songs", Tag.TAG_STRING)) {
            songs.add(song.getAsString());
        }
        state.add(songs);
        state.select(s.contains("Current") ? s.getInt("Current") : -1);
        state.setShuffle(s.getBoolean("Shuffle"));
        state.setRepeat(s.getBoolean("Repeat"));
        state.setVolume(s.contains("Volume") ? s.getInt("Volume") : SoundfoundryState.DEFAULT_VOLUME);
        state.setBalance(s.getInt("Balance"));
    }
}
