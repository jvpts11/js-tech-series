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
import dev.jstech.core.multiblock.damage.CellMark;
import dev.jstech.core.multiblock.damage.DamageMark;
import dev.jstech.core.multiblock.damage.FormedDamage;
import dev.jstech.core.multiblock.shape.FormedPlace;
import dev.jstech.core.multiblock.shape.FormedShape;
import dev.jstech.core.multiblock.shape.FormedShapes;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * What a block of a formed structure merged into its terminal's model keeps: the block it was, given back when the
 * structure comes apart, the terminal it belongs to, and, for a structure of a declared formed model, which block of
 * the model it is, whose shape it takes. The block it was and its place in the model are sent to the players who see
 * it, so it breaks, sounds and shows its particles as that block would, and stands as the model does there. A block a
 * plan laid ({@code BuildPlan}) may take its shape from more than one model, its own and those of formed parts beside
 * it whose faces reach into it, as a rocket's skirt rises round the engine on it; and it never looks for its plan's
 * owner, which gives it back itself. It keeps, too, the strikes of damage that reached it ({@code FormedDamage}),
 * and takes its shape from its faces as they left them.
 */
public final class FormedPartBlockEntity extends SyncedBlockEntity {

    private final ValueField<BlockState> original;
    private final ValueField<BlockPos> terminal;
    private final ValueField<FormedPlace> place;
    private final ValueField<List<FormedPlace>> also;
    private final BoolField planned;
    private final BoolField drawsPart;
    private final ValueField<List<CellMark>> damage;
    /* The shape last worked out, and from which shape files, places and damage, so any of them changing redoes it. */
    private @Nullable VoxelShape shape;
    private List<Object> shapeFrom = List.of();
    /* The damage a player's game last saw it keep, to tell when the server sends another. */
    private List<CellMark> damageSeen = List.of();

    public FormedPartBlockEntity(final BlockPos pos, final BlockState state) {
        super(FormedParts.ENTITY.get(), pos, state);
        this.original = fields().value("Original", BlockState.CODEC, Blocks.AIR.defaultBlockState()).save()
                .toClient();
        // The players' games know whose it is too: a click on it there works its terminal, as the server does.
        this.terminal = fields().nullable("Terminal", BlockPos.CODEC).save().toClient();
        this.place = fields().nullable("Place", FormedPlace.CODEC).save().toClient();
        this.also = fields().value("Also", FormedPlace.CODEC.listOf(), List.<FormedPlace>of()).save().toClient();
        this.planned = fields().flag("Planned", false).save();
        // Whether it draws its whole formed model itself, no owner drawing it any more.
        this.drawsPart = fields().flag("DrawsPart", false).save().toClient();
        // The strikes laid on the models it is a block of, which reached it: what it is re-shaped and drawn by.
        this.damage = fields().value("Damage", CellMark.CODEC.listOf(), List.<CellMark>of()).save().toClient();
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

    /** Whether it draws its whole formed model itself ({@link FormedParts#drawFrom}). */
    public boolean drawsPart() {
        return this.drawsPart.get();
    }

    /** The strikes laid on the formed models it is a block of that reached it ({@link FormedDamage}). */
    public List<CellMark> damage() {
        final List<CellMark> kept = this.damage.get();
        return kept == null ? List.of() : kept;
    }

    /** Keeps the strike {@code mark} too, re-shaped by it from now on. */
    @ApiStatus.Internal
    public void mark(final CellMark mark) {
        final List<CellMark> more = new ArrayList<>(damage());
        more.add(mark);
        this.damage.set(List.copyOf(more));
        setChanged();
    }

    /** Lets go of the strike {@code mark}, as the block it lost is put back; whether it kept it. */
    @ApiStatus.Internal
    public boolean forget(final DamageMark mark) {
        final List<CellMark> left = damage().stream().filter(kept -> !kept.mark().equals(mark)).toList();
        if (left.size() == damage().size()) {
            return false;
        }
        this.damage.set(left);
        setChanged();
        return true;
    }

    /** Lets go of every strike laid on the model {@code model}; whether it kept any. */
    @ApiStatus.Internal
    public boolean unmark(final ResourceLocation model) {
        final List<CellMark> left = damage().stream().filter(mark -> !mark.model().equals(model)).toList();
        if (left.size() == damage().size()) {
            return false;
        }
        this.damage.set(left);
        setChanged();
        return true;
    }

    /* Draws its whole formed model itself from now on, or no longer. */
    void drawsPart(final boolean draws) {
        if (this.drawsPart.get() != draws) {
            this.drawsPart.set(draws);
            setChanged();
        }
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
        from.add(damage());
        if (this.shape != null && from.equals(this.shapeFrom)) {
            return this.shape;
        }
        VoxelShape out = Shapes.empty();
        boolean known = false;
        for (final FormedPlace at : all) {
            final Optional<FormedShape> file = FormedShapes.get(this.level, at.model());
            if (file.isPresent()) {
                // A block a strike reached takes its shape from its faces as the strike left them.
                final Optional<int[]> struck = FormedDamage.boxes(this.level, at, damage());
                out = Shapes.joinUnoptimized(out, struck.isPresent() ? FormedShape.shapeOf(struck.get(), at.turn())
                        : file.get().shape(at), BooleanOp.OR);
                known = true;
            }
        }
        this.shape = known ? out.optimize() : Shapes.block();
        this.shapeFrom = from;
        return this.shape;
    }

    /* On a player's game, a block keeping damage that comes into sight has what is drawn from it redone. */
    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level != null && this.level.isClientSide() && !damage().isEmpty()) {
            this.damageSeen = damage();
            FormedDamage.changed();
        }
    }

    /* On a player's game, a block keeping damage that goes has what is drawn from it redone. */
    @Override
    public void setRemoved() {
        super.setRemoved();
        if (this.level != null && this.level.isClientSide() && !damage().isEmpty()) {
            FormedDamage.changed();
        }
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

    /* A player's game told of other damage redoes what it draws from it. */
    @Override
    protected void afterClientUpdate() {
        super.afterClientUpdate();
        if (!damage().equals(this.damageSeen)) {
            this.damageSeen = damage();
            FormedDamage.changed();
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
