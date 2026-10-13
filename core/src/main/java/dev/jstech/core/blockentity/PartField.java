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
 * A {@link IFieldPart} declared among a block entity's fields, with where it goes: {@link #save()},
 * {@link #toClient()}. The part writes its own keys, so it is read every time and looks for them itself.
 */
public final class PartField implements IField {

    private final FieldFlags flags;
    private final IFieldPart part;

    PartField(final FieldFlags flags, final IFieldPart part) {
        this.flags = flags;
        this.part = part;
    }

    /** Keeps the part in the save. */
    public PartField save() {
        flags.save();
        return this;
    }

    /** Sends the players what the part says they see of it. */
    public PartField toClient() {
        flags.toClient();
        return this;
    }

    /** The part changed: the save is marked changed and the players are sent it, as its destinations ask. */
    public void changed() {
        flags.changed();
    }

    @Override
    public FieldFlags flags() {
        return flags;
    }

    @Override
    public void write(final CompoundTag tag, final HolderLookup.Provider registries) {
        part.save(tag, registries);
    }

    @Override
    public void read(final CompoundTag tag, final HolderLookup.Provider registries) {
        part.load(tag, registries);
    }

    @Override
    public void writeClient(final CompoundTag tag, final HolderLookup.Provider registries) {
        part.writeClient(tag, registries);
    }

    @Override
    public void readClient(final CompoundTag tag, final HolderLookup.Provider registries) {
        part.readClient(tag, registries);
    }

    @Override
    public boolean keyed() {
        return false;
    }

    @Override
    public int menuSlots() {
        return 0;
    }
}
