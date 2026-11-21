/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock.shape;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The piece of a formed model a point is on, as a look at it finds it ({@link FormedPiece}): the model, the piece, a
 * seat, a console or a door, by the name its model gives it, and the box round the whole piece, however many blocks it
 * stands in; so whatever tells what is aimed at, to a player or to an agent, tells the thing drawn there and not only
 * the block it is in. Any mod's formed models, standing in the world or aboard a moving structure's yard.
 */
public final class FormedPieceAim {

    private static final int AXES = 3;

    private FormedPieceAim() {
    }

    /**
     * The piece of a formed model the point {@code at} of the block at {@code pos} is on; none for a point on no piece,
     * or a block of no formed model.
     *
     * @param at the point, in the same space as {@code pos}
     */
    public static Optional<Aimed> at(final Level level, final BlockPos pos, final Vec3 at) {
        if (!(level.getBlockEntity(pos) instanceof IFormedPlaced placed)) {
            return Optional.empty();
        }
        final double[] own = new double[AXES];
        for (final FormedPlace place : placed.places()) {
            final Optional<FormedShape> shape = FormedShapes.get(level, place.model());
            if (shape.isEmpty()) {
                continue;
            }
            CellSpace.toModel(new double[] {at.x - pos.getX(), at.y - pos.getY(), at.z - pos.getZ()}, place.turn(),
                    own);
            final Optional<FormedPiece> piece = shape.get().pieceAt(place, own);
            if (piece.isPresent()) {
                return Optional.of(new Aimed(place.model(), piece.get().name(), box(pos, place,
                        shape.get().pieceBox(place, piece.get()))));
            }
        }
        return Optional.empty();
    }

    /**
     * The piece called {@code piece} of the formed model the block at {@code pos} is a block of, wherever in the
     * model it stands; none for a model with no piece of that name, or a block of no formed model.
     */
    public static Optional<Aimed> named(final Level level, final BlockPos pos, final String piece) {
        if (!(level.getBlockEntity(pos) instanceof IFormedPlaced placed)) {
            return Optional.empty();
        }
        for (final FormedPlace place : placed.places()) {
            final Optional<FormedShape> shape = FormedShapes.get(level, place.model());
            final Optional<FormedPiece> found = shape.flatMap(each -> each.piece(piece));
            if (found.isPresent()) {
                return Optional.of(new Aimed(place.model(), piece, box(pos, place, shape.get().pieceBox(place,
                        found.get()))));
            }
        }
        return Optional.empty();
    }

    /* The box {@code local} of the block at {@code pos}, as its model stands facing north, as the block stands. */
    private static AABB box(final BlockPos pos, final FormedPlace place, final double[] local) {
        final double[] low = new double[AXES];
        final double[] high = new double[AXES];
        // A block's turns only swap and mirror its axes, so the box's two corners turned are still its corners.
        CellSpace.toWorld(new double[] {local[0], local[1], local[2]}, place.turn(), low);
        CellSpace.toWorld(new double[] {local[3], local[4], local[5]}, place.turn(), high);
        return new AABB(pos.getX() + Math.min(low[0], high[0]), pos.getY() + Math.min(low[1], high[1]),
                pos.getZ() + Math.min(low[2], high[2]), pos.getX() + Math.max(low[0], high[0]),
                pos.getY() + Math.max(low[1], high[1]), pos.getZ() + Math.max(low[2], high[2]));
    }

    /**
     * A piece aimed at.
     *
     * @param model the formed model it is a piece of
     * @param piece its name, as its model gives it
     * @param box   the box round the whole piece, in the same space as the block it was aimed at in
     */
    public record Aimed(ResourceLocation model, String piece, AABB box) {
    }
}
