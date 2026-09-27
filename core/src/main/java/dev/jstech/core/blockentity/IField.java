/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.blockentity;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

/**
 * One declared field of a block entity, as {@link BlockEntityFields} drives it: written to and read from a tag under
 * its key (the save and the client's update use the same form unless the field says otherwise), and seen by a menu as
 * a run of ints.
 */
interface IField {

    /** Where this field goes, and under which key. */
    FieldFlags flags();

    /** Writes the field's value under its key, for the save. */
    void write(CompoundTag tag, HolderLookup.Provider registries);

    /** Reads the field's value from under its key, which is present; nothing is marked changed. */
    void read(CompoundTag tag, HolderLookup.Provider registries);

    /** Writes what the players who see the block are sent; the saved form unless the field says otherwise. */
    default void writeClient(final CompoundTag tag, final HolderLookup.Provider registries) {
        write(tag, registries);
    }

    /** Reads what the server sent; the saved form unless the field says otherwise. */
    default void readClient(final CompoundTag tag, final HolderLookup.Provider registries) {
        read(tag, registries);
    }

    /**
     * Whether the field lives under its one key, so it is read only when the key is there. A part that writes keys of
     * its own is read every time and looks for them itself.
     */
    default boolean keyed() {
        return true;
    }

    /** The key was not in a save or an update: a field that may hold nothing holds nothing. */
    default void absent() {
    }

    /** How many ints the field takes in a menu's data. */
    default int menuSlots() {
        return 1;
    }

    /** The {@code part}-th int of the field's value, for the menu. */
    default int menuGet(final int part) {
        return 0;
    }

    /** Takes the {@code part}-th int of the value from the menu, on the client; nothing is marked changed. */
    default void menuSet(final int part, final int value) {
    }

    /** Checks, on the server, whether what the players see of this field changed since it was last sent. */
    default boolean pollChanged() {
        return false;
    }
}
