/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.pad;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * The numbers a world gives its launch pads, as a launch site numbers its pads: each pad's service port is given the
 * next one when it first stands, and a number is never given again, so a pad taken down leaves its number behind it.
 */
public final class PadNumbers extends SavedData {

    private int given;

    private static final String FILE = "jsspace_pads";
    private static final String GIVEN = "Given";

    private PadNumbers() {
    }

    /** The next number {@code level} gives a pad, from 1. */
    public static int next(final ServerLevel level) {
        final PadNumbers numbers = level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(PadNumbers::new,
                PadNumbers::load), FILE);
        numbers.given++;
        numbers.setDirty();
        return numbers.given;
    }

    @Override
    public CompoundTag save(final CompoundTag tag, final HolderLookup.Provider registries) {
        tag.putInt(GIVEN, this.given);
        return tag;
    }

    private static PadNumbers load(final CompoundTag tag, final HolderLookup.Provider registries) {
        final PadNumbers loaded = new PadNumbers();
        loaded.given = Math.max(0, tag.getInt(GIVEN));
        return loaded;
    }
}
