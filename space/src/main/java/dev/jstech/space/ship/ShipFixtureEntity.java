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
 * The block entity of a fixture that keeps nothing else, a bunk, an oxygen panel: only the block of its part's formed
 * model it stands for.
 */
public final class ShipFixtureEntity extends SyncedBlockEntity implements IFixtureEntity {

    private final FixturePlace place;

    public ShipFixtureEntity(final BlockPos pos, final BlockState state) {
        super(ShipKitBlocks.FIXTURE_ENTITY.get(), pos, state);
        this.place = new FixturePlace(this);
    }

    @Override
    public List<FormedPlace> places() {
        return this.place.places();
    }

    @Override
    public void place(final @Nullable FormedPlace place) {
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
