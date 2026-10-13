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
 * A true-or-false a block entity holds, declared with where it goes: {@link #save()}, {@link #toClient()},
 * {@link #toMenu()} (where it is 1 or 0). Setting it to a new value marks what its destinations ask.
 */
public final class BoolField implements IField {

    private final FieldFlags flags;
    private boolean value;

    BoolField(final FieldFlags flags, final boolean initial) {
        this.flags = flags;
        this.value = initial;
    }

    /** Keeps the value in the save. */
    public BoolField save() {
        flags.save();
        return this;
    }

    /** Sends the value to the players who see the block. */
    public BoolField toClient() {
        flags.toClient();
        return this;
    }

    /** Shows the value to the menu open on the block. */
    public BoolField toMenu() {
        flags.toMenu();
        return this;
    }

    public boolean get() {
        return value;
    }

    public void set(final boolean to) {
        if (to != value) {
            value = to;
            flags.changed();
        }
    }

    @Override
    public FieldFlags flags() {
        return flags;
    }

    @Override
    public void write(final CompoundTag tag, final HolderLookup.Provider registries) {
        tag.putBoolean(flags.key(), value);
    }

    @Override
    public void read(final CompoundTag tag, final HolderLookup.Provider registries) {
        value = tag.getBoolean(flags.key());
    }

    @Override
    public int menuGet(final int part) {
        return value ? 1 : 0;
    }

    @Override
    public void menuSet(final int part, final int to) {
        value = to != 0;
    }
}
