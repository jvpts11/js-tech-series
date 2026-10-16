/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.connect;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

/**
 * CTM's pillar type: a block that stands as a column with its like, or lies as one when its like are beside it and
 * none above or below. Its ends are the texture itself; its sides are drawn from its companion's two by two, a short
 * column alone, the middle of a column, its bottom and its top, the column's way along the side's texture, turned a
 * quarter where the column lies across it.
 */
final class PackPillars {

    private static final int HALVES = 2;

    private PackPillars() {
    }

    /** The face {@code pieces} of a pillar, its like found by {@code joins} among {@code around}. */
    static BakedQuad draw(final QuadPieces pieces, final PackCandidates.Ctm ctm, final PackJoins.IJoin joins,
                          final PackSurroundings around, final int light) {
        final Direction face = pieces.quad().getDirection();
        final Direction.Axis axis = axis(around, joins, face);
        if (axis == null) {
            return face.getAxis() == Direction.Axis.Y ? pieces.whole(ctm.own(), QuadPieces.Cell.WHOLE, 0, light)
                    : pieces.whole(ctm.companion(0), cell(0, 0), 0, light);
        }
        if (face.getAxis() == axis) {
            // An end of the column: the texture itself.
            return pieces.whole(ctm.own(), QuadPieces.Cell.WHOLE, 0, light);
        }
        // The column runs up the side's texture, or across it, turned a quarter so it runs up the picture.
        final boolean across = pieces.right().getAxis() == axis;
        final Direction towards = across ? pieces.right() : pieces.up();
        final boolean ahead = joined(around, joins, towards, face);
        final boolean behind = joined(around, joins, towards.getOpposite(), face);
        final QuadPieces.Cell cell;
        if (ahead && behind) {
            cell = cell(0, 1);
        } else if (ahead) {
            cell = cell(1, 1);
        } else if (behind) {
            cell = cell(1, 0);
        } else {
            cell = cell(0, 0);
        }
        return pieces.whole(ctm.companion(0), cell, across ? 1 : 0, light);
    }

    /* The way the column runs: up and down before east and west, before north and south; none for one alone. */
    private static @Nullable Direction.Axis axis(final PackSurroundings around, final PackJoins.IJoin joins,
                                                 final Direction face) {
        if (joined(around, joins, Direction.UP, face) || joined(around, joins, Direction.DOWN, face)) {
            return Direction.Axis.Y;
        }
        if (joined(around, joins, Direction.EAST, face) || joined(around, joins, Direction.WEST, face)) {
            return Direction.Axis.X;
        }
        if (joined(around, joins, Direction.NORTH, face) || joined(around, joins, Direction.SOUTH, face)) {
            return Direction.Axis.Z;
        }
        return null;
    }

    private static boolean joined(final PackSurroundings around, final PackJoins.IJoin joins, final Direction way,
                                  final Direction face) {
        return joins.joins(around.at(way), around.extraAt(way), face);
    }

    private static QuadPieces.Cell cell(final int x, final int y) {
        return new QuadPieces.Cell((float) x / HALVES, (float) y / HALVES, 1.0F / HALVES, 1.0F / HALVES);
    }
}
