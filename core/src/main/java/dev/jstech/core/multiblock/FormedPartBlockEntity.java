/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock;

import dev.jstech.core.blockentity.BoolField;
import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.blockentity.ValueField;
import dev.jstech.core.moving.BlockMove;
import dev.jstech.core.multiblock.shape.FormedPlace;
import dev.jstech.core.multiblock.shape.FormedShape;
import dev.jstech.core.multiblock.shape.FormedShapes;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * What a block of a formed structure merged into its terminal's model keeps: the block it was, given back when the
 * structure comes apart, the terminal it belongs to, and, for a structure of a declared formed model, which block of
 * the model it is, whose shape it takes. The block it was and its place in the model are sent to the players who see
 * it, so it breaks, sounds and shows its particles as that block would, and stands as the model does there. A block a
 * plan laid ({@code BuildPlan}) may take its shape from more than one model, its own and those of formed parts beside
 * it whose faces reach into it, as a rocket's skirt rises round the engine on it; and it never looks for its plan's
 * owner, which gives it back itself.
 */
public final class FormedPartBlockEntity extends SyncedBlockEntity {

    private final ValueField<BlockState> original;
    private final ValueField<BlockPos> terminal;
    private final ValueField<FormedPlace> place;
    private final ValueField<List<FormedPlace>> also;
    private final BoolField planned;
    /* The shape last worked out, and from which shape files and places, so a new file or a turn works it again. */
    private @Nullable VoxelShape shape;
    private List<Object> shapeFrom = List.of();

    public FormedPartBlockEntity(final BlockPos pos, final BlockState state) {
        super(FormedParts.ENTITY.get(), pos, state);
        this.original = fields().value("Original", BlockState.CODEC, Blocks.AIR.defaultBlockState()).save()
                .toClient();
        // The players' games know whose it is too: a click on it there works its terminal, as the server does.
        this.terminal = fields().nullable("Terminal", BlockPos.CODEC).save().toClient();
        this.place = fields().nullable("Place", FormedPlace.CODEC).save().toClient();
        this.also = fields().value("Also", FormedPlace.CODEC.listOf(), List.<FormedPlace>of()).save().toClient();
        this.planned = fields().flag("Planned", false).save();
    }

    /** The block it was before its structure formed. */
    public BlockState original() {
        return this.original.get();
    }

    /** The terminal of the structure it is part of, or the owner of the plan that laid it; none for one not told. */
    public @Nullable BlockPos terminal() {
        return this.terminal.get();
    }

    /** Which block of its structure's formed model it is, or none for a structure of no declared model. */
    public @Nullable FormedPlace place() {
        return this.place.get();
    }

    /** The blocks of other formed models whose faces reach into it, which it takes its shape from too. */
    public List<FormedPlace> also() {
        final List<FormedPlace> more = this.also.get();
        return more == null ? List.of() : more;
    }

    /** Every block of a formed model it takes its shape from: its own first, then those reaching into it. */
    public List<FormedPlace> places() {
        final List<FormedPlace> all = new ArrayList<>();
        final FormedPlace own = this.place.get();
        if (own != null) {
            all.add(own);
        }
        all.addAll(also());
        return List.copyOf(all);
    }

    /** Whether a plan laid it, which gives it back itself, so it never looks for its owner. */
    public boolean planned() {
        return this.planned.get();
    }

    /**
     * The shape it stands as: its part of its structure's formed model, as the structure stands, with the parts of the
     * models reaching into it; a whole block for a structure of no declared model, or while no shape file is known.
     */
    public VoxelShape shape() {
        final List<FormedPlace> all = places();
        if (all.isEmpty() || this.level == null) {
            return Shapes.block();
        }
        final List<Object> from = new ArrayList<>();
        for (final FormedPlace at : all) {
            from.add(at);
            from.add(FormedShapes.get(this.level, at.model()).orElse(null));
        }
        if (this.shape != null && from.equals(this.shapeFrom)) {
            return this.shape;
        }
        VoxelShape out = Shapes.empty();
        boolean known = false;
        for (final FormedPlace at : all) {
            final Optional<FormedShape> file = FormedShapes.get(this.level, at.model());
            if (file.isPresent()) {
                out = Shapes.joinUnoptimized(out, file.get().shape(at), BooleanOp.OR);
                known = true;
            }
        }
        this.shape = known ? out.optimize() : Shapes.block();
        this.shapeFrom = from;
        return this.shape;
    }

    /* Moved with a moving structure: its places in the models turn as the blocks turned. */
    @Override
    public void moved(final BlockPos was, final BlockMove move) {
        super.moved(was, move);
        if (move.rotation() == Rotation.NONE) {
            return;
        }
        final FormedPlace at = this.place.get();
        if (at != null) {
            this.place.set(at.turned(move.rotation()));
        }
        if (!also().isEmpty()) {
            this.also.set(also().stream().map(reaching -> reaching.turned(move.rotation())).toList());
        }
    }

    /* Told what it was, whose it is and which block of the model it is, as it merges. */
    void merged(final BlockState was, final BlockPos owner, @Nullable final FormedPlace in) {
        this.original.set(was);
        this.terminal.set(owner.immutable());
        this.place.set(in);
    }

    /* Told it was laid by a plan, with the blocks of other models reaching into it. */
    void laid(final List<FormedPlace> reaching) {
        this.planned.set(true);
        this.also.set(List.copyOf(reaching));
    }

    /* Told its plan's owner is somewhere else now. */
    void owned(final BlockPos owner) {
        this.terminal.set(owner.immutable());
    }

    /* Told which blocks of other formed models reach into it now. */
    void reached(final List<FormedPlace> reaching) {
        this.also.set(List.copyOf(reaching));
    }
}
