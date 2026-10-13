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
import net.minecraft.world.level.Level;

import java.util.function.IntSupplier;

/**
 * A value the server works out rather than holds (whether a machine runs, how full its buffer is), shown on the
 * client as the server last sent it. Declared {@link #toClient()}, it is checked each tick and sent when it changes;
 * declared {@link #toMenu()}, the open menu carries it. It is never saved: the server can always work it out again.
 */
public final class DerivedInt implements IField {

    private final FieldFlags flags;
    private final BlockEntityFields fields;
    private final IntSupplier value;
    private int mirror;
    private int sent;

    DerivedInt(final FieldFlags flags, final BlockEntityFields fields, final IntSupplier value) {
        this.flags = flags;
        this.fields = fields;
        this.value = value;
    }

    /** Sends the value to the players who see the block, whenever it changes. */
    public DerivedInt toClient() {
        flags.toClient();
        return this;
    }

    /** Shows the value to the menu open on the block. */
    public DerivedInt toMenu() {
        flags.toMenu();
        return this;
    }

    /** The value: worked out on the server, as last sent on the client. */
    public int getAsInt() {
        final Level level = fields.level();
        return level != null && level.isClientSide() ? mirror : value.getAsInt();
    }

    /** Whether the value is not zero, for a value that says yes or no. */
    public boolean isSet() {
        return getAsInt() != 0;
    }

    @Override
    public FieldFlags flags() {
        return flags;
    }

    @Override
    public void write(final CompoundTag tag, final HolderLookup.Provider registries) {
        // Writing is not sending: an update tag built between a change and the next poll must not mark the change
        // as sent, or the players already watching would never be told. At worst one redundant update goes out.
        tag.putInt(flags.key(), value.getAsInt());
    }

    @Override
    public void read(final CompoundTag tag, final HolderLookup.Provider registries) {
        mirror = tag.getInt(flags.key());
    }

    @Override
    public int menuGet(final int part) {
        return getAsInt();
    }

    @Override
    public void menuSet(final int part, final int to) {
        mirror = to;
    }

    /*
     * A change is counted as sent the moment it is found, not when a player's update is written: with nobody near
     * the block no update is written at all, and a value never marked sent would schedule a send every tick.
     */
    @Override
    public boolean pollChanged() {
        final int now = value.getAsInt();
        if (now == sent) {
            return false;
        }
        sent = now;
        return true;
    }
}
