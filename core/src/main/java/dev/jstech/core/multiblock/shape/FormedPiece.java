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
import org.jetbrains.annotations.Nullable;

/**
 * A piece of a formed model a player takes for one thing, a door, a seat, a console, however many blocks it stands in:
 * aimed at from any of them, the whole piece is outlined, as the game outlines a block, in place of the part of the
 * model in the block aimed at. Its shape is a few convex solids round it, a seat its foot, its seat with the arms, its
 * back and its headrest, each outlined by its sharp edges. Pure, so it is tested without the game.
 *
 * @param name   what it is, as its model names it
 * @param solids its convex solids, each its faces as triangles, nine numbers to one, in the model's own space, facing
 *               out
 */
public record FormedPiece(String name, List<float[]> solids) {

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
            SOLID.listOf().fieldOf("solids").forGetter(FormedPiece::solids)).apply(instance, FormedPiece::new));

    public FormedPiece {
        if (solids.isEmpty()) {
            throw new IllegalArgumentException("a piece has a solid at least: " + name);
        }
        final List<float[]> copied = new ArrayList<>(solids.size());
        for (final float[] solid : solids) {
            if (solid.length == 0 || solid.length % TRIANGLE != 0) {
                throw new IllegalArgumentException("a solid's faces are nine numbers each: " + name);
            }
            copied.add(solid.clone());
        }
        solids = List.copyOf(copied);
    }

    /** Whether the point {@code at}, in the model's space, is on the piece: within a solid, or {@link #MARGIN} off. */
    public boolean holds(final double[] at) {
        for (final float[] solid : this.solids) {
            if (within(solid, at)) {
                return true;
            }
        }
        return false;
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

    /* Whether {@code at} is behind every face of the convex {@code solid}, or no further than the margin before one. */
    private static boolean within(final float[] solid, final double[] at) {
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
            if (ahead > MARGIN) {
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
