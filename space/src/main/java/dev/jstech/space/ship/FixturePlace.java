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
import net.minecraft.world.level.block.Rotation;
import org.jetbrains.annotations.Nullable;

/**
 * The block of its part's formed model a fixture's block entity keeps, saved and sent to the players, turned with its
 * ship as the ship's blocks turn.
 */
final class FixturePlace {

    private final ValueField<FormedPlace> place;

    FixturePlace(final SyncedBlockEntity entity) {
        this.place = entity.fields().nullable("FixturePlace", FormedPlace.CODEC).save().toClient();
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
