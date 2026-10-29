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
import dev.jstech.core.moving.BlockAboard;
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
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * The shapes of formed structures' blocks, from their formed models: a model declared formed from a pattern
 * ({@code ModelBuilder.formedFrom}) has its shape file written by the data run, under
 * {@code data/<namespace>/formed_shape/}, and each block of its structure that merges into it takes the shape of its
 * own part of the model, as the structure stands: collided with, outlined and aimed at as it is drawn, on the ground
 * and aboard a moving structure. A structure of no declared model merges as before, each block whole. A model of blocks
 * a plan lays ({@link #declareLaid}) is shaped the same way from the blocks it says, its faces free to reach into the
 * blocks of the parts beside it.
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
    /* What a kind of block does aboard as a block of a formed model, by the model, then the block. */
    private static final Map<ResourceLocation, Map<ResourceLocation, BlockAboard>> ABOARD =
            Collections.synchronizedMap(new LinkedHashMap<>());
    /* The models of blocks laid by a plan, with their own blocks and those of others their faces may reach into. */
    private static final Map<ResourceLocation, Laid> LAID = Collections.synchronizedMap(new LinkedHashMap<>());
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

    /**
     * Declares that the block {@code block}, as a block of the formed model {@code model}, does aboard a moving
     * structure what {@code aboard} says, in place of what it does alone: the same seat a couch in one model and a
     * chair in another. Written into the model's shape file by the data run.
     */
    public static void declareAboard(final ResourceLocation model, final ResourceLocation block,
                                     final BlockAboard aboard) {
        ABOARD.computeIfAbsent(model, any -> Collections.synchronizedMap(new LinkedHashMap<>())).put(block, aboard);
    }

    /**
     * Declares the model {@code model} the formed model of blocks a plan lays, rather than ones a pattern finds: its
     * own blocks {@code own}, and the blocks of other parts beside it its faces may reach into, {@code reach}, as a
     * rocket's interstage skirt rises round the engine standing on it; each a key of {@link MeshCells#key}, from the
     * block whose lowest corner is at {@code origin} in the model's space, in blocks. Of its own blocks, those in
     * {@code hollow} are meant to have none of its faces, as the corners of a square of blocks round a round hull, and
     * merge with no shape at all. Its data run writes the shapes of every block its faces fall in, and stops when any
     * other of its own blocks has no face of it, or a face lies more than a block outside them all.
     */
    public static void declareLaid(final ResourceLocation model, final double[] origin, final Set<Long> own,
                                   final Set<Long> reach, final Set<Long> hollow) {
        LAID.put(model, new Laid(origin.clone(), Set.copyOf(own), Set.copyOf(reach), Set.copyOf(hollow)));
    }

    /** The models of laid blocks declared by the mod of that namespace, for its data run. */
    public static Map<ResourceLocation, Laid> laidBy(final String namespace) {
        final Map<ResourceLocation, Laid> out = new LinkedHashMap<>();
        synchronized (LAID) {
            LAID.forEach((model, laid) -> {
                if (model.getNamespace().equals(namespace)) {
                    out.put(model, laid);
                }
            });
        }
        return out;
    }

    /** What each kind of block does aboard as a block of the formed model {@code model}, as declared. */
    public static Map<ResourceLocation, BlockAboard> aboardDeclared(final ResourceLocation model) {
        final Map<ResourceLocation, BlockAboard> declared = ABOARD.get(model);
        return declared == null ? Map.of() : Map.copyOf(declared);
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

    /** What {@code block} does aboard as the block {@code place} of a formed model, if its model's file says. */
    public static Optional<BlockAboard> aboard(final Level level, final FormedPlace place, final Block block) {
        return get(level, place.model()).flatMap(shape -> shape.aboard(block));
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

    /**
     * The blocks of a model a plan lays.
     *
     * @param origin where the lowest corner of the block {@code (0, 0, 0)} is in the model's space, in blocks
     * @param own    its own blocks, each needing a face of it but the hollow ones
     * @param reach  the blocks of other parts beside it its faces may fall in
     * @param hollow its own blocks meant to have none of its faces, which merge with no shape
     */
    public record Laid(double[] origin, Set<Long> own, Set<Long> reach, Set<Long> hollow) {

        /** Its origin, a copy. */
        @Override
        public double[] origin() {
            return this.origin.clone();
        }
    }
}
