/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.ship;

import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.moving.BlockMove;
import dev.jstech.core.multiblock.shape.FormedPlace;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * What a block of a let-down boarding stair keeps: which block of its airlock's model it stands as, beside the hull,
 * and how the airlock stands, so it holds the stair's treads within it, turned with the ship.
 */
public final class ShipStairEntity extends SyncedBlockEntity {

    private final FixturePlace place;

    public ShipStairEntity(final BlockPos pos, final BlockState state) {
        super(ShipStairs.ENTITY.get(), pos, state);
        this.place = new FixturePlace(this);
    }

    /** The block of the airlock's model it stands as, if it was told. */
    public @Nullable FormedPlace place() {
        final List<FormedPlace> kept = this.place.places();
        return kept.isEmpty() ? null : kept.getFirst();
    }

    /** Stands as the block {@code place} of its airlock's model. */
    public void place(final FormedPlace place) {
        if (this.place.set(place)) {
            setChanged();
        }
    }

    @Override
    public void moved(final BlockPos was, final BlockMove move) {
        super.moved(was, move);
        this.place.moved(move);
    }
}
