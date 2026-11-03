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
import java.util.List;
import java.util.OptionalLong;
import org.jetbrains.annotations.Nullable;

/**
 * A piece of a formed model a player takes for one thing, a door, a seat, a console, however many blocks it stands in:
 * aimed at from any of them, the whole piece is outlined, as the game outlines a block, in place of the part of the
 * model in the block aimed at; and used from any of them, it is used through the block it works by, where it names
 * one, as a seat whose back stands in the block over it is sat in from its back. Its shape is a few convex solids round
 * it, a seat its foot, its seat with the arms, its back and its headrest, each outlined by its sharp edges. A door is a
 * piece with a leaf: used, it shuts or opens ({@link FormedDoors}), and its leaf, while it is shut, is solid in the
 * blocks it stands in, the model drawing its leaf as the door stands; while the leaf is open, air goes through the
 * blocks it stands in, and while it is shut they keep the rooms on its two sides apart, each its own. A piece may ask
 * for rooms beside it, as an air vent does: the block it works by asks for them while it stands, so a ship's sections
 * either side of a door are rooms. Pure, so it is tested without the game.
 *
 * @param name   what it is, as its model names it
 * @param solids its convex solids, each its faces as triangles, nine numbers to one, in the model's own space, facing
 *               out
 * @param uses   the block of the model it works by, its place's three numbers as {@link MeshCells#key} reads them;
 *               none for a piece only outlined
 * @param shut   a door's leaf where it stands shut, its convex solids as {@code solids} are, the sixteenths whose
 *               middles they hold solid while it is; none for a piece that is no door
 * @param moves  the bones of the model that move with a door, its leaf and what its opening lets down, a stair, so no
 *               block takes a shape from them; none for a piece that moves none
 * @param startsShut whether a door stands shut as its structure forms, an airlock's outer door; open, else
 * @param rooms  the blocks of the model it asks for rooms at, each its place's three numbers; none for a piece that
 *               asks for none. Only a piece working by a block of its own asks.
 */
public record FormedPiece(String name, List<float[]> solids, List<Integer> uses, List<float[]> shut,
                          List<String> moves, boolean startsShut, List<List<Integer>> rooms) {

    /** How far from its solids a point aimed at still counts as on the piece, in blocks. */
    public static final double MARGIN = 0.02;

    private static final int TRIANGLE = 9;
    private static final int AXES = 3;
    private static final Codec<float[]> SOLID = Codec.FLOAT.listOf().comapFlatMap(list -> list.isEmpty()
            || list.size() % TRIANGLE != 0 ? DataResult.error(() -> "a solid's faces are nine numbers each")
            : DataResult.success(floats(list)), solid -> {
                final List<Float> out = new ArrayList<>(solid.length);
                for (final float value : solid) {
                    out.add(value);
                }
                return out;
            });
    /** How a shape file holds it. */
    public static final Codec<FormedPiece> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(FormedPiece::name),
            SOLID.listOf().fieldOf("solids").forGetter(FormedPiece::solids),
            Codec.INT.listOf().optionalFieldOf("uses", List.of()).forGetter(FormedPiece::uses),
            SOLID.listOf().optionalFieldOf("shut", List.of()).forGetter(FormedPiece::shut),
            Codec.STRING.listOf().optionalFieldOf("moves", List.of()).forGetter(FormedPiece::moves),
            Codec.BOOL.optionalFieldOf("starts_shut", false).forGetter(FormedPiece::startsShut),
            Codec.INT.listOf().listOf().optionalFieldOf("rooms", List.of()).forGetter(FormedPiece::rooms))
            .apply(instance, FormedPiece::new));

    public FormedPiece {
        if (solids.isEmpty()) {
            throw new IllegalArgumentException("a piece has a solid at least: " + name);
        }
        if (!uses.isEmpty() && uses.size() != AXES) {
            throw new IllegalArgumentException("the block a piece works by is three numbers: " + name);
        }
        if (!shut.isEmpty() && uses.isEmpty()) {
            throw new IllegalArgumentException("a door works by a block of its own, which keeps whether it is shut: "
                    + name);
        }
        if (!rooms.isEmpty() && uses.isEmpty()) {
            throw new IllegalArgumentException("a piece asks for rooms by a block of its own: " + name);
        }
        if (rooms.stream().anyMatch(room -> room.size() != AXES)) {
            throw new IllegalArgumentException("a block a piece asks for a room at is three numbers: " + name);
        }
        uses = List.copyOf(uses);
        solids = copied(name, solids);
        shut = copied(name, shut);
        moves = List.copyOf(moves);
        rooms = rooms.stream().map(List::copyOf).toList();
    }

    /** A piece asking for no rooms. */
    public FormedPiece(final String name, final List<float[]> solids, final List<Integer> uses,
                       final List<float[]> shut, final List<String> moves, final boolean startsShut) {
        this(name, solids, uses, shut, moves, startsShut, List.of());
    }

    /**
     * A door standing open as its structure forms, moving no bone of its own, or a piece that is no door
     * ({@code shut} empty).
     */
    public FormedPiece(final String name, final List<float[]> solids, final List<Integer> uses,
                       final List<float[]> shut) {
        this(name, solids, uses, shut, List.of(), false);
    }

    /** A piece that is no door, working by the block {@code uses} names, or by none. */
    public FormedPiece(final String name, final List<float[]> solids, final List<Integer> uses) {
        this(name, solids, uses, List.of());
    }

    /** A piece only outlined, working by no block of its own. */
    public FormedPiece(final String name, final List<float[]> solids) {
        this(name, solids, List.of());
    }

    /** Whether it is a door, a leaf that shuts. */
    public boolean door() {
        return !this.shut.isEmpty();
    }

    /** Whether the point {@code at}, in the model's space, is within its leaf where it stands shut. */
    public boolean leafHolds(final double[] at) {
        for (final float[] solid : this.shut) {
            if (within(solid, at, 0)) {
                return true;
            }
        }
        return false;
    }

    /** The block of the model it works by, as {@link MeshCells#key} keys it; none for a piece only outlined. */
    public OptionalLong usesCell() {
        return this.uses.isEmpty() ? OptionalLong.empty()
                : OptionalLong.of(MeshCells.key(this.uses.get(0), this.uses.get(1), this.uses.get(2)));
    }

    /** The blocks of the model it asks for rooms at, as {@link MeshCells#key} keys them; none for none. */
    public List<Long> roomCells() {
        return this.rooms.stream().map(room -> MeshCells.key(room.get(0), room.get(1), room.get(2))).toList();
    }

    /** Whether the point {@code at}, in the model's space, is on the piece: within a solid, or {@link #MARGIN} off. */
    public boolean holds(final double[] at) {
        for (final float[] solid : this.solids) {
            if (within(solid, at, MARGIN)) {
                return true;
            }
        }
        return false;
    }

    /** Its leaf's corners' lowest and highest, in the model's space, six numbers; none for a piece that is no door. */
    public double[] leafBounds() {
        final double[] out = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY};
        for (final float[] solid : this.shut) {
            for (int k = 0; k < solid.length; k++) {
                final int axis = k % AXES;
                out[axis] = Math.min(out[axis], solid[k]);
                out[axis + AXES] = Math.max(out[axis + AXES], solid[k]);
            }
        }
        return out;
    }

    /** Its outline: each solid's sharp edges, {@link MeshOutline#LINE} numbers to a line, in the model's space. */
    public float[] lines() {
        final List<float[]> each = new ArrayList<>(this.solids.size());
        int length = 0;
        for (final float[] solid : this.solids) {
            final float[] lines = MeshOutline.lines(solid);
            each.add(lines);
            length += lines.length;
        }
        final float[] out = new float[length];
        int at = 0;
        for (final float[] lines : each) {
            System.arraycopy(lines, 0, out, at, lines.length);
            at += lines.length;
        }
        return out;
    }

    /** The piece of {@code pieces} holding the point {@code at}, the first that does; none for none. */
    public static @Nullable FormedPiece at(final List<FormedPiece> pieces, final double[] at) {
        for (final FormedPiece piece : pieces) {
            if (piece.holds(at)) {
                return piece;
            }
        }
        return null;
    }

    /* Copies of {@code solids}, each checked to be faces of nine numbers. */
    private static List<float[]> copied(final String name, final List<float[]> solids) {
        final List<float[]> copied = new ArrayList<>(solids.size());
        for (final float[] solid : solids) {
            if (solid.length == 0 || solid.length % TRIANGLE != 0) {
                throw new IllegalArgumentException("a solid's faces are nine numbers each: " + name);
            }
            copied.add(solid.clone());
        }
        return List.copyOf(copied);
    }

    /* Whether {@code at} is behind every face of the convex {@code solid}, or no further than {@code margin} before. */
    private static boolean within(final float[] solid, final double[] at, final double margin) {
        final double[][] corner = new double[3][AXES];
        for (int t = 0; t + TRIANGLE <= solid.length; t += TRIANGLE) {
            for (int c = 0; c < 3; c++) {
                for (int axis = 0; axis < AXES; axis++) {
                    corner[c][axis] = solid[t + c * AXES + axis];
                }
            }
            final double[] normal = MeshCells.normal(corner[0], corner[1], corner[2]);
            if (normal == null) {
                continue;
            }
            final double ahead = normal[0] * (at[0] - corner[0][0]) + normal[1] * (at[1] - corner[0][1])
                    + normal[2] * (at[2] - corner[0][2]);
            if (ahead > margin) {
                return false;
            }
        }
        return true;
    }

    private static float[] floats(final List<Float> list) {
        final float[] out = new float[list.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = list.get(i);
        }
        return out;
    }
}
