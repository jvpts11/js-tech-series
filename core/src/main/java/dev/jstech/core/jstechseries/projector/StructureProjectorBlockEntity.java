/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.projector;

import dev.jstech.core.blockentity.BoolField;
import dev.jstech.core.blockentity.IntField;
import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.blockentity.ValueField;
import dev.jstech.core.multiblock.IStructureShape;
import dev.jstech.core.multiblock.MultiblockPattern;
import dev.jstech.core.multiblock.Orientation;
import dev.jstech.core.multiblock.Projectables;
import dev.jstech.core.multiblock.ShapePreview;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A Structure Projector: the multiblock it shows and how, saved and sent to the players who see it, and where it
 * shows it, in front of the projector, its near side a block away, its middle across from it, its bottom level with
 * it. What is placed, missing or wrong is worked out on each player's side from the world they see, so the projector
 * does no work on the server.
 */
public final class StructureProjectorBlockEntity extends SyncedBlockEntity {

    private final ValueField<ResourceLocation> chosen =
            fields().nullable("Chosen", ResourceLocation.CODEC).save().toClient();
    private final IntField turn = fields().integer("Turn", 0).save().toClient();
    private final BoolField mirrored = fields().flag("Mirrored", false).save().toClient();
    private final IntField layer = fields().integer("Layer", 0).save().toClient();
    private final BoolField missingOnly = fields().flag("MissingOnly", false).save().toClient();
    private final BoolField on = fields().flag("On", true).save().toClient();

    /* The projectors loaded on this player's side, for what draws their ghosts; never filled on a server. */
    private static final Set<StructureProjectorBlockEntity> SHOWN =
            Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    public StructureProjectorBlockEntity(final BlockPos pos, final BlockState state) {
        super(StructureProjector.ENTITY.get(), pos, state);
    }

    /** The projectors loaded on this player's side. */
    public static Set<StructureProjectorBlockEntity> shown() {
        synchronized (SHOWN) {
            return Set.copyOf(SHOWN);
        }
    }

    /** What it shows and how. */
    public ProjectorSettings settings() {
        return new ProjectorSettings(Optional.ofNullable(this.chosen.get()), this.turn.get(), this.mirrored.get(),
                this.layer.get(), this.missingOnly.get(), this.on.get());
    }

    /** Shows as {@code to} says: a multiblock that cannot be shown is not chosen, and one that cannot mirror is not. */
    public void apply(final ProjectorSettings to) {
        final ResourceLocation id = to.chosen().filter(chosen -> Projectables.get(chosen).isPresent()).orElse(null);
        this.chosen.set(id);
        this.turn.set(to.turn());
        this.mirrored.set(to.mirrored() && id != null && mirrors(Projectables.get(id).orElseThrow()));
        this.layer.set(to.layer());
        this.missingOnly.set(to.missingOnly());
        this.on.set(to.on());
    }

    /** The multiblock it shows, when one is chosen and can still be shown. */
    public Optional<IStructureShape> shape() {
        return Optional.ofNullable(this.chosen.get()).flatMap(Projectables::get);
    }

    /** How the multiblock it shows stands: its turn, and mirrored when it is. */
    public Orientation orientation() {
        final int index = Math.floorMod(this.turn.get(), ProjectorSettings.TURNS);
        return this.mirrored.get() ? Orientation.turnsAndMirrors().get(ProjectorSettings.TURNS + index)
                : Orientation.turns().get(index);
    }

    /** What it shows laid out as it stands, every layer, its terminal's block in when known, when one is chosen. */
    public Optional<ShapePreview> preview() {
        return Optional.ofNullable(this.chosen.get()).flatMap(id -> Projectables.preview(id, 0))
                .map(preview -> preview.turned(orientation()));
    }

    /** Where the terminal of what it shows stands: in front of it. */
    public BlockPos terminalOf(final ShapePreview preview) {
        final Direction facing = getBlockState().hasProperty(HorizontalDirectionalBlock.FACING)
                ? getBlockState().getValue(HorizontalDirectionalBlock.FACING) : Direction.NORTH;
        final int axis = facing.getAxis() == Direction.Axis.X ? 0 : 2;
        final int sign = facing.getAxisDirection().getStep();
        final int[] at = preview.anchor(this.worldPosition.getX(), this.worldPosition.getY(),
                this.worldPosition.getZ(), axis, sign);
        return new BlockPos(at[0], at[1], at[2]);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level != null && this.level.isClientSide()) {
            SHOWN.add(this);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        SHOWN.remove(this);
    }

    /* Whether a multiblock may be shown mirrored: a pattern that says so; a shape by rule is the same both ways. */
    private static boolean mirrors(final IStructureShape shape) {
        return shape instanceof MultiblockPattern pattern && pattern.mirrors();
    }
}
