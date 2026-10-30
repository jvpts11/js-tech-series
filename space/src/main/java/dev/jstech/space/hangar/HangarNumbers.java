/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.hangar;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * The numbers a world's Rocket Hangars take, one each as each first forms, from 1 up, so the screen of one says which
 * it is: Rocket Hangar 1, Rocket Hangar 2. Kept with the overworld, the one dimension every server has.
 */
public final class HangarNumbers extends SavedData {

    private int last;

    private static final String FILE = "jsspace_hangars";
    private static final String LAST = "Last";

    private HangarNumbers() {
    }

    /** The next number for a hangar of {@code server}'s world, taken now. */
    public static int take(final MinecraftServer server) {
        final HangarNumbers numbers = server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(
                HangarNumbers::new, HangarNumbers::load), FILE);
        numbers.last++;
        numbers.setDirty();
        return numbers.last;
    }

    @Override
    public CompoundTag save(final CompoundTag tag, final HolderLookup.Provider registries) {
        tag.putInt(LAST, this.last);
        return tag;
    }

    private static HangarNumbers load(final CompoundTag tag, final HolderLookup.Provider registries) {
        final HangarNumbers loaded = new HangarNumbers();
        loaded.last = tag.getInt(LAST);
        return loaded;
    }
}
