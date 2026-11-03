/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.ship;

import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.blockentity.ValueField;
import dev.jstech.core.moving.BlockMove;
import dev.jstech.core.multiblock.shape.FormedPlace;
import java.util.List;
import java.util.Objects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.block.Rotation;
import org.jetbrains.annotations.Nullable;

/**
 * The block of its part's formed model a fixture's block entity keeps, saved and sent to the players, turned with its
 * ship as the ship's blocks turn.
 */
final class FixturePlace {

    private final ValueField<FormedPlace> place;
    /* Under what a fixture's block entity keeps its place, whatever kind of block entity it is. */
    private static final String KEY = "FixturePlace";

    FixturePlace(final SyncedBlockEntity entity) {
        this.place = entity.fields().nullable(KEY, FormedPlace.CODEC).save().toClient();
    }

    /** Writes {@code place} into {@code tag}, for a fixture whose block entity is of the game's own kinds. */
    static void write(final CompoundTag tag, final @Nullable FormedPlace place) {
        if (place != null) {
            FormedPlace.CODEC.encodeStart(NbtOps.INSTANCE, place).result().ifPresent(at -> tag.put(KEY, at));
        }
    }

    /** The place {@link #write} wrote into {@code tag}, if it wrote one. */
    static @Nullable FormedPlace read(final CompoundTag tag) {
        return tag.contains(KEY) ? FormedPlace.CODEC.parse(NbtOps.INSTANCE, tag.get(KEY)).result().orElse(null)
                : null;
    }

    /** The block it stands for, as a list: none, or that one. */
    List<FormedPlace> places() {
        final FormedPlace kept = this.place.get();
        return kept == null ? List.of() : List.of(kept);
    }

    /** Keeps {@code place}; whether that changed it. */
    boolean set(final @Nullable FormedPlace place) {
        if (Objects.equals(this.place.get(), place)) {
            return false;
        }
        this.place.set(place);
        return true;
    }

    /** Its block entity was moved with {@code move}: the place turns as the blocks did. */
    void moved(final BlockMove move) {
        final FormedPlace kept = this.place.get();
        if (kept != null && move.rotation() != Rotation.NONE) {
            this.place.set(kept.turned(move.rotation()));
        }
    }
}
