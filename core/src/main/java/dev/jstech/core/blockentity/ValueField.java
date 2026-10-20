/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.blockentity;

import dev.jstech.core.log.CoreLogs;
import com.mojang.serialization.Codec;
import dev.jstech.core.moving.BlockMove;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Objects;

/**
 * A value of any type a codec writes (a name, a position, an id), declared with where it goes: {@link #save()},
 * {@link #toClient()}. A value declared to be able to hold nothing is left out of the save and the update while it
 * holds nothing, and holds nothing again when it is missing from them.
 *
 * @param <T> the value's type
 */
public final class ValueField<T> implements IField {

    private final FieldFlags flags;
    private final Codec<T> codec;
    private final boolean nullable;
    private @Nullable T value;

    private static final Logger LOGGER = CoreLogs.SAVES.logger();

    ValueField(final FieldFlags flags, final Codec<T> codec, final @Nullable T initial, final boolean nullable) {
        this.flags = flags;
        this.codec = codec;
        this.nullable = nullable;
        this.value = initial;
    }

    /** Keeps the value in the save. */
    public ValueField<T> save() {
        flags.save();
        return this;
    }

    /** Sends the value to the players who see the block. */
    public ValueField<T> toClient() {
        flags.toClient();
        return this;
    }

    /** The value; null only for a value declared to be able to hold nothing. */
    public @Nullable T get() {
        return value;
    }

    /**
     * Sets the value.
     *
     * @throws IllegalArgumentException when {@code to} is null for a value that must always hold one
     */
    public void set(final @Nullable T to) {
        if (to == null && !nullable) {
            throw new IllegalArgumentException("field '" + flags.key() + "' always holds a value");
        }
        if (!Objects.equals(to, value)) {
            value = to;
            flags.changed();
        }
    }

    @Override
    public FieldFlags flags() {
        return flags;
    }

    /**
     * Its block moved with others, a moving structure made or set down: a place it holds of a block that moved too
     * follows that block, in this dimension or another; a block's look it holds turns as the blocks turned; any other
     * value, or a place of a block that stayed, is kept as it is.
     */
    @SuppressWarnings("unchecked")
    void moved(final BlockMove move) {
        if (value instanceof BlockPos place && move.movedHere(place)) {
            set((T) move.where(place));
        } else if (value instanceof GlobalPos place && place.dimension() == move.from().dimension()
                && move.movedHere(place.pos())) {
            set((T) GlobalPos.of(move.to().dimension(), move.where(place.pos())));
        } else if (value instanceof BlockState look && move.rotation() != Rotation.NONE) {
            set((T) look.rotate(move.rotation()));
        }
    }

    @Override
    public void write(final CompoundTag tag, final HolderLookup.Provider registries) {
        if (value == null) {
            return;
        }
        codec.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), value)
                .resultOrPartial(problem -> LOGGER.error("field '{}' could not be written: {}", flags.key(), problem))
                .ifPresent(written -> tag.put(flags.key(), written));
    }

    @Override
    public void read(final CompoundTag tag, final HolderLookup.Provider registries) {
        final Tag stored = tag.get(flags.key());
        codec.parse(registries.createSerializationContext(NbtOps.INSTANCE), stored)
                .resultOrPartial(problem -> LOGGER.error("field '{}' could not be read: {}", flags.key(), problem))
                .ifPresent(read -> value = read);
    }

    @Override
    public void absent() {
        if (nullable) {
            value = null;
        }
    }

    @Override
    public int menuSlots() {
        return 0;
    }
}
