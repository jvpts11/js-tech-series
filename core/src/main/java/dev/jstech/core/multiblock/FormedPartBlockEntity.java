/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock;

import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.blockentity.ValueField;
import dev.jstech.core.moving.BlockMove;
import dev.jstech.core.multiblock.shape.FormedPlace;
import dev.jstech.core.multiblock.shape.FormedShape;
import dev.jstech.core.multiblock.shape.FormedShapes;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * What a block of a formed structure merged into its terminal's model keeps: the block it was, given back when the
 * structure comes apart, the terminal it belongs to, and, for a structure of a declared formed model, which block of
 * the model it is, whose shape it takes. The block it was and its place in the model are sent to the players who see
 * it, so it breaks, sounds and shows its particles as that block would, and stands as the model does there.
 */
public final class FormedPartBlockEntity extends SyncedBlockEntity {

    private final ValueField<BlockState> original;
    private final ValueField<BlockPos> terminal;
    private final ValueField<FormedPlace> place;
    /* The shape last worked out, from which shape file and for which place, so a new file or a turn works it again. */
    private @Nullable VoxelShape shape;
    private @Nullable FormedShape shapeFrom;
    private @Nullable FormedPlace shapeFor;

    public FormedPartBlockEntity(final BlockPos pos, final BlockState state) {
        super(FormedParts.ENTITY.get(), pos, state);
        this.original = fields().value("Original", BlockState.CODEC, Blocks.AIR.defaultBlockState()).save()
                .toClient();
        // The players' games know whose it is too: a click on it there works its terminal, as the server does.
        this.terminal = fields().nullable("Terminal", BlockPos.CODEC).save().toClient();
        this.place = fields().nullable("Place", FormedPlace.CODEC).save().toClient();
    }

    /** The block it was before its structure formed. */
    public BlockState original() {
        return this.original.get();
    }

    /** The terminal of the structure it is part of, or none for one not yet told. */
    public @Nullable BlockPos terminal() {
        return this.terminal.get();
    }

    /** Which block of its structure's formed model it is, or none for a structure of no declared model. */
    public @Nullable FormedPlace place() {
        return this.place.get();
    }

    /**
     * The shape it stands as: its part of its structure's formed model, as the structure stands; a whole block for a
     * structure of no declared model, or while the model's shape file is not known.
     */
    public VoxelShape shape() {
        final FormedPlace at = this.place.get();
        if (at == null || this.level == null) {
            return Shapes.block();
        }
        final Optional<FormedShape> file = FormedShapes.get(this.level, at.model());
        if (file.isEmpty()) {
            return Shapes.block();
        }
        if (this.shape == null || this.shapeFrom != file.get() || !at.equals(this.shapeFor)) {
            this.shape = file.get().shape(at);
            this.shapeFrom = file.get();
            this.shapeFor = at;
        }
        return this.shape;
    }

    /* Moved with a moving structure: its place in the model turns as the blocks turned. */
    @Override
    public void moved(final BlockPos was, final BlockMove move) {
        super.moved(was, move);
        final FormedPlace at = this.place.get();
        if (at != null && move.rotation() != Rotation.NONE) {
            this.place.set(at.turned(move.rotation()));
        }
    }

    /* Told what it was, whose it is and which block of the model it is, as it merges. */
    void merged(final BlockState was, final BlockPos owner, @Nullable final FormedPlace in) {
        this.original.set(was);
        this.terminal.set(owner.immutable());
        this.place.set(in);
    }
}
