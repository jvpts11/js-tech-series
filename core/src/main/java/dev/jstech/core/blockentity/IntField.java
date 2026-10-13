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
 * An int a block entity holds, declared with where it goes: {@link #save()}, {@link #toClient()}, {@link #toMenu()}.
 * Setting it to a new value marks the save changed and schedules the players' update, as its destinations ask; on
 * the client it holds what the server last sent.
 */
public final class IntField implements IField {

    private final FieldFlags flags;
    private int value;

    IntField(final FieldFlags flags, final int initial) {
        this.flags = flags;
        this.value = initial;
    }

    /** Keeps the value in the save. */
    public IntField save() {
        flags.save();
        return this;
    }

    /** Sends the value to the players who see the block. */
    public IntField toClient() {
        flags.toClient();
        return this;
    }

    /** Shows the value to the menu open on the block. */
    public IntField toMenu() {
        flags.toMenu();
        return this;
    }

    public int get() {
        return value;
    }

    public void set(final int to) {
        if (to != value) {
            value = to;
            flags.changed();
        }
    }

    public void add(final int delta) {
        set(value + delta);
    }

    @Override
    public FieldFlags flags() {
        return flags;
    }

    @Override
    public void write(final CompoundTag tag, final HolderLookup.Provider registries) {
        tag.putInt(flags.key(), value);
    }

    @Override
    public void read(final CompoundTag tag, final HolderLookup.Provider registries) {
        value = tag.getInt(flags.key());
    }

    @Override
    public int menuGet(final int part) {
        return value;
    }

    @Override
    public void menuSet(final int part, final int to) {
        value = to;
    }
}
