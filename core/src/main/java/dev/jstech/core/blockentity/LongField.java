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
 * A long a block entity holds, declared with where it goes: {@link #save()}, {@link #toClient()}, {@link #toMenu()}.
 * A menu carries ints, so there the value takes two: its high half, then its low half.
 */
public final class LongField implements IField {

    private final FieldFlags flags;
    private long value;

    LongField(final FieldFlags flags, final long initial) {
        this.flags = flags;
        this.value = initial;
    }

    /** Keeps the value in the save. */
    public LongField save() {
        flags.save();
        return this;
    }

    /** Sends the value to the players who see the block. */
    public LongField toClient() {
        flags.toClient();
        return this;
    }

    /** Shows the value to the menu open on the block. */
    public LongField toMenu() {
        flags.toMenu();
        return this;
    }

    public long get() {
        return value;
    }

    public void set(final long to) {
        if (to != value) {
            value = to;
            flags.changed();
        }
    }

    public void add(final long delta) {
        set(value + delta);
    }

    @Override
    public FieldFlags flags() {
        return flags;
    }

    @Override
    public void write(final CompoundTag tag, final HolderLookup.Provider registries) {
        tag.putLong(flags.key(), value);
    }

    @Override
    public void read(final CompoundTag tag, final HolderLookup.Provider registries) {
        value = tag.getLong(flags.key());
    }

    @Override
    public int menuSlots() {
        return 2;
    }

    @Override
    public int menuGet(final int part) {
        return part == 0 ? (int) (value >>> 32) : (int) value;
    }

    @Override
    public void menuSet(final int part, final int to) {
        value = part == 0
                ? ((long) to << 32) | (value & 0xFFFF_FFFFL)
                : (value & 0xFFFF_FFFF_0000_0000L) | (to & 0xFFFF_FFFFL);
    }
}
