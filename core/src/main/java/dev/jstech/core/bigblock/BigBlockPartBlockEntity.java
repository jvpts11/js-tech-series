/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.bigblock;

import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.blockentity.ValueField;
import dev.jstech.core.moving.BlockMove;
import java.util.function.UnaryOperator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The shared base of a big block's parts: each part remembers the way to its origin, so a break or a use on the part
 * reaches the block entity at the origin. The way is kept from the part itself, not as a place in the world, so it
 * holds wherever the big block is carried whole, and it turns as the big block turns: on a moving structure of the
 * Core's it follows where the origin went, and a mod whose own mover turns blocks, as Create's contraptions do, turns
 * it with {@link #turn}.
 */
public abstract class BigBlockPartBlockEntity extends SyncedBlockEntity {

    /*
     * From this part to its origin. A plain Vec3i, never a BlockPos: a place a field holds follows the block it names
     * when blocks move, and this is no place. The player's game has it too, where picking a part gives its big block.
     */
    private final ValueField<Vec3i> toOrigin = fields().nullable("ToOrigin", Vec3i.CODEC).save().toClient();

    protected BigBlockPartBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
    }

    /** Where its big block's origin is, or none for a part not yet bound. */
    @Nullable
    public BlockPos originPos() {
        final Vec3i way = toOrigin.get();
        return way == null ? null : getBlockPos().offset(way);
    }

    public void setOrigin(final BlockPos pos) {
        final BlockPos at = getBlockPos();
        point(pos.getX() - at.getX(), pos.getY() - at.getY(), pos.getZ() - at.getZ());
    }

    /**
     * The part was turned with its big block by a mover that does not tell it where the origin went: {@code turned}
     * gives the way to the origin once turned as the blocks were. Nothing for a part not yet bound.
     */
    public void turn(final UnaryOperator<Vec3i> turned) {
        final Vec3i way = toOrigin.get();
        if (way != null) {
            final Vec3i to = turned.apply(way);
            point(to.getX(), to.getY(), to.getZ());
        }
    }

    /**
     * Moved with a moving structure: the way leads to where the origin went, if it moved too, or still to where it
     * stands, if it stayed.
     */
    @Override
    public void moved(final BlockPos was, final BlockMove move) {
        super.moved(was, move);
        final Vec3i way = toOrigin.get();
        if (way != null) {
            setOrigin(move.where(was.offset(way)));
        }
    }

    private void point(final int x, final int y, final int z) {
        toOrigin.set(new Vec3i(x, y, z));
    }
}
