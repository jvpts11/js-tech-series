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
 * A piece of a block entity's state that writes itself, under keys of its own (a computer's installed hardware, its
 * power, its session), declared as a field so the block entity still saves and sends nothing by hand. What the players
 * who see the block are sent may be less than what is saved, or other.
 */
public interface IFieldPart {

    /**
     * A part that is only saved, written by {@code save} and read back by {@code load}: the shape of most parts, whose
     * own class already knows how to write itself into a tag.
     */
    static IFieldPart of(final ITagIo save, final ITagIo load) {
        return new IFieldPart() {
            @Override
            public void save(final CompoundTag tag, final HolderLookup.Provider registries) {
                save.accept(tag, registries);
            }

            @Override
            public void load(final CompoundTag tag, final HolderLookup.Provider registries) {
                load.accept(tag, registries);
            }
        };
    }

    /** Writes the part into the save. */
    void save(CompoundTag tag, HolderLookup.Provider registries);

    /** Reads the part back from a save, looking for its own keys; a key that is missing leaves its default. */
    void load(CompoundTag tag, HolderLookup.Provider registries);

    /** Writes what the players who see the block are sent of this part; nothing unless the part says. */
    default void writeClient(final CompoundTag tag, final HolderLookup.Provider registries) {
    }

    /** Reads what the server sent of this part, on the client; nothing unless the part says. */
    default void readClient(final CompoundTag tag, final HolderLookup.Provider registries) {
    }

    /** One way of moving a part to or from a tag. */
    @FunctionalInterface
    interface ITagIo {
        void accept(CompoundTag tag, HolderLookup.Provider registries);
    }
}
