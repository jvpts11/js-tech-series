/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.inventory;

import dev.jstech.core.blockentity.IFieldPart;
import dev.jstech.core.connect.RelativeFace;
import dev.jstech.core.id.StableNames;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * What each face of a block lets through, named from the block's own point of view (its front, back, left, right, top
 * and bottom), so turning the block keeps what each face does. A block keeps one as a field of its entity, saved and
 * sent to the players who see it, and hands its item handler out through it:
 *
 * <pre>{@code
 * private final FaceConfig faces = new FaceConfig(FaceMode.BOTH, () -> this.facesField.changed());
 * private final PartField facesField = fields().part("Faces", this.faces).save().toClient();
 *
 * IItemHandler itemsOn(Direction side) {
 *     return this.faces.handler(getBlockState(), side, this.inventory);
 * }
 * }</pre>
 */
public final class FaceConfig implements IFieldPart {

    private final Map<RelativeFace, FaceMode> modes = new EnumMap<>(RelativeFace.class);
    private final FaceMode standard;
    private final Runnable changed;

    private static final String KEY = "Faces";
    private static final StableNames<RelativeFace> FACES = StableNames.of(RelativeFace.class);
    private static final StableNames<FaceMode> MODES = StableNames.of(FaceMode.class);

    /**
     * @param standard what every face lets through until it is set
     * @param changed  what to do when a face is set, as marking the block's field changed
     */
    public FaceConfig(final FaceMode standard, final Runnable changed) {
        this.standard = Objects.requireNonNull(standard, "standard");
        this.changed = Objects.requireNonNull(changed, "changed");
    }

    /** What {@code face} lets through. */
    public FaceMode mode(final RelativeFace face) {
        return this.modes.getOrDefault(face, this.standard);
    }

    /** What the world's {@code side} of a block in {@code state} lets through. */
    public FaceMode mode(final BlockState state, final Direction side) {
        return mode(relative(state, side));
    }

    /** Sets what {@code face} lets through. */
    public void set(final RelativeFace face, final FaceMode mode) {
        if (mode(face) != Objects.requireNonNull(mode, "mode")) {
            this.modes.put(face, mode);
            this.changed.run();
        }
    }

    /** Moves {@code face} on to its next mode, and says which it is now. */
    public FaceMode cycle(final RelativeFace face) {
        final FaceMode next = mode(face).next();
        set(face, next);
        return next;
    }

    /**
     * The item handler the world's {@code side} of a block in {@code state} gives out: {@code inner}, taking in and
     * giving out only what the face lets through; null for a closed face.
     */
    public @Nullable IItemHandler handler(final BlockState state, final Direction side, final IItemHandler inner) {
        final FaceMode mode = mode(state, side);
        return mode == FaceMode.NONE ? null : new FaceItemHandler(inner, mode, ItemFilter.EVERYTHING);
    }

    @Override
    public void save(final CompoundTag tag, final HolderLookup.Provider registries) {
        final CompoundTag faces = new CompoundTag();
        for (final Map.Entry<RelativeFace, FaceMode> entry : this.modes.entrySet()) {
            faces.putString(entry.getKey().serializedName(), entry.getValue().serializedName());
        }
        tag.put(KEY, faces);
    }

    @Override
    public void load(final CompoundTag tag, final HolderLookup.Provider registries) {
        this.modes.clear();
        final CompoundTag faces = tag.getCompound(KEY);
        for (final String name : faces.getAllKeys()) {
            final RelativeFace face = FACES.find(name);
            final FaceMode mode = MODES.find(faces.getString(name));
            if (face != null && mode != null) {
                this.modes.put(face, mode);
            }
        }
    }

    @Override
    public void writeClient(final CompoundTag tag, final HolderLookup.Provider registries) {
        save(tag, registries);
    }

    @Override
    public void readClient(final CompoundTag tag, final HolderLookup.Provider registries) {
        load(tag, registries);
    }

    /* Which face of the block the world's side is; a block with no facing is taken as facing north. */
    private static RelativeFace relative(final BlockState state, final Direction side) {
        return RelativeFace.of(RelativeFace.facingOf(state).orElse(Direction.NORTH), side);
    }
}
