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
 * its key (the save and the client's update use the same form), and seen by a menu as a run of ints.
 */
interface IField {

    /** Where this field goes, and under which key. */
    FieldFlags flags();

    /** Writes the field's value under its key. */
    void write(CompoundTag tag, HolderLookup.Provider registries);

    /** Reads the field's value from under its key, which is present; nothing is marked changed. */
    void read(CompoundTag tag, HolderLookup.Provider registries);

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
