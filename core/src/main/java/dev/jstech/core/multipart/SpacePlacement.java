/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multipart;

import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Where a microblock goes when a player places it: against the face of the block clicked, in the space beyond it; or,
 * clicked on a part's side inside a space, in that same space, against the face that side looks to, as a slab placed
 * on a slab's top makes the top slab. A post goes on the edge of that face nearest the click, or through the middle
 * when the click is near the face's middle; a corner in the corner of that face nearest the click. A space is made
 * where nothing stands, and a part never goes where a player or a mob stands.
 */
public final class SpacePlacement {

    private static final double PIXELS = PartBox.SIDE;
    private static final double EDGE = 1.0e-4;
    private static final double MIDDLE = 8.0;
    private static final double MIDDLE_FROM = 4.0;
    private static final double MIDDLE_TO = 12.0;

    private SpacePlacement() {
    }

    /**
     * Where {@code piece} goes when placed on {@code clicked}'s {@code face} at {@code location}, or nothing when its
     * shape has no place there.
     */
    public static Optional<Placement> target(final Level level, final BlockPos clicked, final Vec3 location,
                                             final Direction face, final Microblock.MicroblockPiece piece) {
        final double[] local = {(location.x - clicked.getX()) * PIXELS, (location.y - clicked.getY()) * PIXELS,
                (location.z - clicked.getZ()) * PIXELS};
        final int normal = axisOf(face);
        final boolean onBoundary = local[normal] <= EDGE || local[normal] >= PIXELS - EDGE;
        final boolean inSpace = level.getBlockEntity(clicked) instanceof ISpaceHolder;
        final BlockPos pos;
        final Direction against;
        final double[] at = local.clone();
        if (inSpace && !onBoundary) {
            pos = clicked;
            against = face;
        } else {
            pos = clicked.relative(face);
            against = face.getOpposite();
            at[normal] -= face.getAxisDirection().getStep() * PIXELS;
        }
        final int side = against.get3DDataValue();
        try {
            return switch (piece.shape()) {
                case FACE, HOLLOW, FACADE -> Optional.of(new Placement(pos, new Microblock(piece,
                        SpaceSlots.face(side), SpaceSlots.Y)));
                case CORNER -> Optional.of(new Placement(pos, new Microblock(piece, corner(side, at),
                        SpaceSlots.Y)));
                case POST -> Optional.of(new Placement(pos, post(piece, side, at)));
            };
        } catch (final IllegalArgumentException wrong) {
            return Optional.empty();
        }
    }

    /**
     * Places {@code placement}'s microblock, making a space where nothing stands, on the server.
     *
     * @return whether it stands there now
     */
    public static boolean place(final Level level, final Placement placement) {
        final BlockPos pos = placement.pos();
        final Microblock block = placement.block();
        if (!level.isUnobstructed(null, voxels(block).move(pos.getX(), pos.getY(), pos.getZ()))) {
            return false;
        }
        return into(level, pos, parts -> parts.add(block));
    }

    /**
     * Mounts {@code device} on {@code face} of the space at {@code pos}, a cable's or a space of parts alone, making
     * a space where nothing stands; on the server.
     *
     * @return whether it is mounted there now
     */
    public static boolean mount(final Level level, final BlockPos pos, final Direction face, final IFacePart device) {
        return into(level, pos, parts -> parts.add(face, device));
    }

    /**
     * Hangs the game's small block {@code state} in the space at {@code pos}, making a space where nothing stands; on
     * the server.
     *
     * @return whether it hangs there now
     */
    public static boolean hang(final Level level, final BlockPos pos, final BlockState state) {
        return into(level, pos, parts -> parts.addSmall(state));
    }

    /* Puts a part into the space at {@code pos}, or into a new one where nothing stands, which goes if it fails. */
    private static boolean into(final Level level, final BlockPos pos, final Predicate<SpaceParts> put) {
        if (level.getBlockEntity(pos) instanceof ISpaceHolder holder) {
            return put.test(holder.parts());
        }
        final BlockState there = level.getBlockState(pos);
        if (!there.canBeReplaced() || !there.getFluidState().isEmpty()) {
            return false;
        }
        level.setBlock(pos, CoreSpaces.BLOCK.get().defaultBlockState(), Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof SpaceBlockEntity space && put.test(space.parts())) {
            return true;
        }
        level.setBlock(pos, there, Block.UPDATE_ALL);
        return false;
    }

    private static int corner(final int side, final double[] at) {
        final boolean[] far = new boolean[3];
        for (int axis = SpaceSlots.X; axis <= SpaceSlots.Z; axis++) {
            far[axis] = at[axis] >= MIDDLE;
        }
        far[SpaceSlots.faceAxis(side)] = SpaceSlots.faceFar(side);
        return SpaceSlots.corner(far[SpaceSlots.X], far[SpaceSlots.Y], far[SpaceSlots.Z]);
    }

    private static Microblock post(final Microblock.MicroblockPiece piece, final int side, final double[] at) {
        final int axis = SpaceSlots.faceAxis(side);
        final int[] across = SpaceSlots.across(axis);
        final double u = at[across[0]];
        final double v = at[across[1]];
        if (u > MIDDLE_FROM && u < MIDDLE_TO && v > MIDDLE_FROM && v < MIDDLE_TO) {
            return new Microblock(piece, SpaceSlots.CENTRE, axis);
        }
        // The nearest of the face's four edges: the edge runs along the across axis whose ends the click is not near.
        final double[] distance = {u, PIXELS - u, v, PIXELS - v};
        int nearest = 0;
        for (int k = 1; k < distance.length; k++) {
            if (distance[k] < distance[nearest]) {
                nearest = k;
            }
        }
        final int boundaryAxis = across[nearest / 2];
        final boolean boundaryFar = nearest % 2 == 1;
        final int edgeAxis = across[1 - nearest / 2];
        final int[] edgeAcross = SpaceSlots.across(edgeAxis);
        final boolean first = edgeAcross[0] == axis ? SpaceSlots.faceFar(side) : boundaryFar;
        final boolean second = edgeAcross[1] == axis ? SpaceSlots.faceFar(side)
                : edgeAcross[1] == boundaryAxis && boundaryFar;
        return new Microblock(piece, SpaceSlots.edge(edgeAxis, first, second), SpaceSlots.Y);
    }

    private static int axisOf(final Direction face) {
        return switch (face.getAxis()) {
            case X -> SpaceSlots.X;
            case Y -> SpaceSlots.Y;
            case Z -> SpaceSlots.Z;
        };
    }

    private static VoxelShape voxels(final Microblock block) {
        VoxelShape made = Shapes.empty();
        for (final PartBox box : block.boxes()) {
            made = Shapes.or(made, Shapes.box(box.x0() / PIXELS, box.y0() / PIXELS, box.z0() / PIXELS,
                    box.x1() / PIXELS, box.y1() / PIXELS, box.z1() / PIXELS));
        }
        return made;
    }

    /**
     * Where a microblock goes: the space, and the microblock in its place there.
     *
     * @param pos   the space
     * @param block the microblock, in its place
     */
    public record Placement(BlockPos pos, Microblock block) {
    }
}
