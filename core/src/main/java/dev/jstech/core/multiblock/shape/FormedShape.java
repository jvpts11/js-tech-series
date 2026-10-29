/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock.shape;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The shapes of the blocks of one formed model, as its shape file holds them: for each block of its structure that
 * merges into it, the boxes it collides with, is outlined by before the model's own outline is known, and is aimed at
 * through, in sixteenths of the block, as the model stands facing north. Worked out by the data run from the model's
 * faces, since a server has no models, and sent to the players. Each block's shape turned each way a structure can
 * stand is worked out once, the first time a block stands that way, and shared by every block of every structure of
 * the model that stands so.
 *
 * <pre>{@code
 * {"origin": [-0.5, 0.0, -0.5], "apart": [[0, 0, 0]],
 *  "cells": [{"at": [0, 1, 0], "boxes": [0, 0, 0, 16, 2, 16, ...]}, ...]}
 * }</pre>
 */
public final class FormedShape {

    private final double[] origin;
    private final Set<Long> apart;
    private final Map<Long, int[]> cells;
    private final Map<Turned, VoxelShape> turned = new ConcurrentHashMap<>();

    private static final int AXES = 3;
    private static final double SIXTEENTH = 1.0 / MeshVoxels.FINE;
    private static final Codec<List<Integer>> TRIPLE = Codec.INT.listOf().comapFlatMap(list -> list.size() == AXES
            ? DataResult.success(list) : DataResult.error(() -> "a block's place is three numbers: " + list),
            list -> list);
    private static final Codec<Cell> CELL = RecordCodecBuilder.create(instance -> instance.group(
            TRIPLE.fieldOf("at").forGetter(Cell::at),
            Codec.INT.listOf().fieldOf("boxes").forGetter(Cell::boxes)).apply(instance, Cell::new));
    /** How its file is read and written. */
    public static final Codec<FormedShape> CODEC = RecordCodecBuilder.<Raw>create(instance -> instance.group(
                    Codec.DOUBLE.listOf().fieldOf("origin").forGetter(Raw::origin),
                    TRIPLE.listOf().optionalFieldOf("apart", List.of()).forGetter(Raw::apart),
                    CELL.listOf().fieldOf("cells").forGetter(Raw::cells)).apply(instance, Raw::new))
            .comapFlatMap(FormedShape::build, FormedShape::raw);

    /**
     * The shapes of a model's blocks.
     *
     * @param origin where the lowest corner of the block {@code (0, 0, 0)} is in the model's space
     * @param apart  the blocks of its structure that stand for themselves, as its terminal does
     * @param cells  each merging block's boxes, by {@link MeshCells#key}, {@link MeshVoxels#BOX} numbers a box
     */
    public FormedShape(final double[] origin, final Set<Long> apart, final Map<Long, int[]> cells) {
        if (origin.length != AXES) {
            throw new IllegalArgumentException("an origin is three numbers");
        }
        for (final int[] boxes : cells.values()) {
            if (boxes.length % MeshVoxels.BOX != 0) {
                throw new IllegalArgumentException("a block's boxes are six numbers each");
            }
        }
        this.origin = origin.clone();
        this.apart = Collections.unmodifiableSet(new LinkedHashSet<>(apart));
        this.cells = Collections.unmodifiableMap(new TreeMap<>(cells));
    }

    /** Where the lowest corner of the block {@code (0, 0, 0)} is in the model's space. */
    public double[] origin() {
        return this.origin.clone();
    }

    /** The blocks of its structure that stand for themselves, as its terminal does. */
    public Set<Long> apart() {
        return this.apart;
    }

    /** Each merging block's boxes, by its key. */
    public Map<Long, int[]> cells() {
        return this.cells;
    }

    /** The boxes of the block {@code cell}, as the model stands facing north, in sixteenths. */
    public Optional<int[]> boxes(final long cell) {
        return Optional.ofNullable(this.cells.get(cell)).map(int[]::clone);
    }

    /** The shape of the block {@code place}, as its structure stands; a whole block for one the file has not. */
    public VoxelShape shape(final FormedPlace place) {
        final int[] boxes = this.cells.get(place.cell());
        if (boxes == null) {
            return Shapes.block();
        }
        return this.turned.computeIfAbsent(new Turned(place.cell(), place.orientation()),
                any -> voxels(CellSpace.boxes(boxes, place.turn())));
    }

    private static VoxelShape voxels(final int[] boxes) {
        VoxelShape out = Shapes.empty();
        for (int b = 0; b + MeshVoxels.BOX <= boxes.length; b += MeshVoxels.BOX) {
            out = Shapes.joinUnoptimized(out, Shapes.box(boxes[b] * SIXTEENTH, boxes[b + 1] * SIXTEENTH,
                    boxes[b + 2] * SIXTEENTH, boxes[b + 3] * SIXTEENTH, boxes[b + 4] * SIXTEENTH,
                    boxes[b + 5] * SIXTEENTH), BooleanOp.OR);
        }
        return out.optimize();
    }

    private static DataResult<FormedShape> build(final Raw raw) {
        if (raw.origin().size() != AXES) {
            return DataResult.error(() -> "an origin is three numbers: " + raw.origin());
        }
        final Set<Long> apart = new LinkedHashSet<>();
        raw.apart().forEach(at -> apart.add(MeshCells.key(at.get(0), at.get(1), at.get(2))));
        final Map<Long, int[]> cells = new TreeMap<>();
        for (final Cell cell : raw.cells()) {
            if (cell.boxes().size() % MeshVoxels.BOX != 0) {
                return DataResult.error(() -> "a block's boxes are six numbers each: " + cell.at());
            }
            for (final int value : cell.boxes()) {
                if (value < 0 || value > MeshVoxels.FINE) {
                    return DataResult.error(() -> "a box is within its block, 0 to 16: " + cell.at());
                }
            }
            cells.put(MeshCells.key(cell.at().get(0), cell.at().get(1), cell.at().get(2)),
                    cell.boxes().stream().mapToInt(Integer::intValue).toArray());
        }
        return DataResult.success(new FormedShape(new double[] {raw.origin().get(0), raw.origin().get(1),
            raw.origin().get(2)}, apart, cells));
    }

    private static Raw raw(final FormedShape shape) {
        final List<List<Integer>> apart = new ArrayList<>();
        shape.apart.forEach(key -> apart.add(List.of(MeshCells.x(key), MeshCells.y(key), MeshCells.z(key))));
        final List<Cell> cells = new ArrayList<>();
        shape.cells.forEach((key, boxes) -> cells.add(new Cell(List.of(MeshCells.x(key), MeshCells.y(key),
                MeshCells.z(key)), Arrays.stream(boxes).boxed().toList())));
        return new Raw(List.of(shape.origin[0], shape.origin[1], shape.origin[2]), apart, cells);
    }

    /* A block's shape standing one way. */
    private record Turned(long cell, int orientation) {
    }

    /* A block's boxes, as its file holds them. */
    private record Cell(List<Integer> at, List<Integer> boxes) {

        Cell {
            at = List.copyOf(at);
            boxes = List.copyOf(boxes);
        }
    }

    /* A shape file as it reads. */
    private record Raw(List<Double> origin, List<List<Integer>> apart, List<Cell> cells) {

        Raw {
            origin = List.copyOf(origin);
            apart = List.copyOf(apart);
            cells = List.copyOf(cells);
        }
    }
}
