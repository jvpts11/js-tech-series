/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock.shape;

import dev.jstech.core.JsCore;
import dev.jstech.core.data.DataRegistry;
import dev.jstech.core.multiblock.IStructureShape;
import dev.jstech.core.multiblock.MultiblockPattern;
import dev.jstech.core.multiblock.MultiblockPatterns;
import dev.jstech.core.multiblock.Orientation;
import dev.jstech.core.multiblock.PatternById;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * The shapes of formed structures' blocks, from their formed models: a model declared formed from a pattern
 * ({@code ModelBuilder.formedFrom}) has its shape file written by the data run, under
 * {@code data/<namespace>/formed_shape/}, and each block of its structure that merges into it takes the shape of its
 * own part of the model, as the structure stands: collided with, outlined and aimed at as it is drawn, on the ground
 * and aboard a moving structure. A structure of no declared model merges as before, each block whole.
 */
public final class FormedShapes {

    /** Every way a structure can stand, by the place a merged block keeps. */
    public static final List<Orientation> ORIENTATIONS = List.copyOf(Orientation.lyingAndMirrors());
    /*
     * The shape files, sent to the players too, whose own collisions need them. A file's id is its model's: the shape
     * file of jsmod:press is data/jsmod/formed_shape/press.json.
     */
    private static final DataRegistry<FormedShape> FILES = DataRegistry.builder(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "formed_shape"), "formed_shape", FormedShape.CODEC)
            .synced().register();
    /* The models declared formed, each with the pattern it is formed from, and the other way round. */
    private static final Map<ResourceLocation, ResourceLocation> PATTERNS =
            Collections.synchronizedMap(new LinkedHashMap<>());
    private static final Map<ResourceLocation, ResourceLocation> MODELS =
            Collections.synchronizedMap(new LinkedHashMap<>());
    /* Where the lowest corner of a pattern's terminal block is in its formed model's space. */
    private static final double[] PATTERN_ORIGIN = {-0.5, 0.0, -0.5};
    private static volatile @Nullable IFormedMeshes meshes;

    private FormedShapes() {
    }

    /** Loads the registry of shape files, from the Core's constructor. */
    public static void register() {
        // The registry is made when this class loads; loading it while the game starts is all this does.
    }

    /**
     * Declares the model {@code model} the formed model of the structure of the pattern {@code pattern}, as its
     * declaration says.
     *
     * @throws IllegalStateException when the pattern has a formed model declared already
     */
    public static void declare(final ResourceLocation model, final ResourceLocation pattern) {
        synchronized (MODELS) {
            final ResourceLocation before = MODELS.get(pattern);
            if (before != null && !before.equals(model)) {
                throw new IllegalStateException("the pattern " + pattern + " is formed into " + before
                        + " already, not also into " + model);
            }
            MODELS.put(pattern, model);
            PATTERNS.put(model, pattern);
        }
    }

    /** The models declared formed by the mod of that namespace, each with its pattern, for its data run. */
    public static Map<ResourceLocation, ResourceLocation> declaredBy(final String namespace) {
        final Map<ResourceLocation, ResourceLocation> out = new LinkedHashMap<>();
        synchronized (MODELS) {
            PATTERNS.forEach((model, pattern) -> {
                if (model.getNamespace().equals(namespace)) {
                    out.put(model, pattern);
                }
            });
        }
        return out;
    }

    /** Where the lowest corner of a pattern's terminal block is in its formed model's space. */
    public static double[] patternOrigin() {
        return PATTERN_ORIGIN.clone();
    }

    /** The formed model of the structure of {@code shape}, if one is declared. */
    public static Optional<ResourceLocation> modelOf(final IStructureShape shape) {
        final Optional<ResourceLocation> pattern = shape instanceof PatternById byId ? Optional.of(byId.id())
                : shape instanceof MultiblockPattern drawn ? MultiblockPatterns.idOf(drawn) : Optional.empty();
        return pattern.map(MODELS::get);
    }

    /**
     * Which block of its formed model the block at {@code pos} is, for a structure of {@code shape} whose terminal is
     * at {@code terminal}, standing the way of {@link #ORIENTATIONS} {@code orientation}; none for a structure of no
     * declared model.
     */
    public static Optional<FormedPlace> place(final IStructureShape shape, final int orientation,
                                              final BlockPos terminal, final BlockPos pos) {
        if (orientation < 0 || orientation >= ORIENTATIONS.size()) {
            return Optional.empty();
        }
        return modelOf(shape).map(model -> {
            final int[] cell = CellSpace.cellOf(pos.getX() - terminal.getX(), pos.getY() - terminal.getY(),
                    pos.getZ() - terminal.getZ(), ORIENTATIONS.get(orientation));
            return new FormedPlace(model, orientation, cell[0], cell[1], cell[2]);
        });
    }

    /** The shapes of the blocks of the formed model {@code model}, on the side {@code level} is on. */
    public static Optional<FormedShape> get(final Level level, final ResourceLocation model) {
        return Optional.ofNullable(FILES.entries(level).get(model));
    }

    /** The faces of the block {@code place} in its own space, where its model is known; none on a server. */
    public static Optional<float[]> triangles(final Level level, final FormedPlace place) {
        final IFormedMeshes source = meshes;
        if (source == null || !level.isClientSide()) {
            return Optional.empty();
        }
        return get(level, place.model()).map(shape -> source.triangles(place, shape));
    }

    /** The lines of the outline of the block {@code place} in its own space, where its model is known. */
    public static Optional<float[]> lines(final Level level, final FormedPlace place) {
        final IFormedMeshes source = meshes;
        if (source == null || !level.isClientSide()) {
            return Optional.empty();
        }
        return get(level, place.model()).map(shape -> source.lines(place, shape));
    }

    /** Hands over where the models' faces are known, once, from the Core's client. */
    @ApiStatus.Internal
    public static void meshes(final IFormedMeshes source) {
        meshes = source;
    }

    /** Where the models' faces are known now, on a player's game; none on a server. */
    @ApiStatus.Internal
    public static @Nullable IFormedMeshes meshes() {
        return meshes;
    }
}
