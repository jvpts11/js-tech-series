/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.launcher;

import com.mojang.serialization.MapCodec;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * A frame of the umbilical tower's red truss, as the approved tower stands: one great truss round a dark shaft, not a
 * cage of small ones. Formed, each frame draws its own piece of it, by where it stands: a frame at a corner the thick
 * column there; a frame along a face the girder across the whole face every two blocks, its stretch of the face's one
 * long diagonal, which leans the other way each level as the approved tower's do, the dark wall of the shaft behind it
 * with its rails and lines, and every eight blocks the grated floor round the shaft. It faces into the shaft, a corner
 * along its face; its bay says the rest.
 */
public final class TowerFrameBlock extends LauncherPieceBlock {

    private static final MapCodec<TowerFrameBlock> CODEC = simpleCodec(TowerFrameBlock::new);

    /**
     * Where a formed frame stands in its truss: its place along its face (the three between the corners, then a
     * corner, which leans its other face's diagonal either way), whether it is the upper block of its level, which
     * way its diagonal leans, and whether the grated floor is at its foot.
     */
    public static final IntegerProperty BAY = IntegerProperty.create("bay", 0, 39);
    /* The tower's width between the outsides of its walls, so a wall finds the one across the shaft. */
    private static final int ACROSS = 4;
    /* How many blocks a level of the truss is, and how many between the grated floors. */
    private static final int LEVEL = 2;
    private static final int FLOORS = 8;
    private static final int FACES = 3;

    public TowerFrameBlock(final BlockBehaviour.Properties properties) {
        super(properties, Kind.TOWER);
    }

    /**
     * The bay of a frame: {@code slot} its place, 0 to 2 the three along its face between the corners, 3 a corner
     * whose other face's diagonal leans as it would and 4 one whose leans back; {@code upper} whether it is the upper
     * block of its level; {@code lean} whether its face's diagonal leans back; {@code floor} whether the grated floor
     * is at its foot.
     */
    public static int bay(final int slot, final boolean upper, final boolean lean, final boolean floor) {
        return slot * 8 + (upper ? 4 : 0) + (lean ? 2 : 0) + (floor ? 1 : 0);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BAY);
    }

    /*
     * A frame of the tower's walls: whether it is a corner or which of the three along its face, from its frames
     * beside it; its level and the lean of its diagonals counted from the foot of its face's corner, which no doorway
     * breaks.
     */
    @Override
    protected BlockState shaped(final BlockGetter level, final BlockPos pos, final BlockState state) {
        if (state.getValue(ROLE) != Role.BODY) {
            return state;
        }
        final Set<Direction> along = EnumSet.noneOf(Direction.class);
        Direction inward = null;
        for (final Direction way : Direction.Plane.HORIZONTAL) {
            if (tower(level, pos.relative(way))) {
                along.add(way);
            } else if (tower(level, pos.relative(way, ACROSS))) {
                inward = way;
            }
        }
        Direction facing = inward;
        if (inward == null) {
            for (final Direction way : along) {
                if (along.contains(way.getClockWise())) {
                    facing = way;
                }
            }
        }
        if (facing == null) {
            return state;
        }
        final boolean corner = inward == null;
        int place = 0;
        BlockPos foot = pos;
        if (!corner) {
            // Its place from the face's corner on its left as seen from outside, or from the one on its right.
            final BlockPos left = end(level, pos, facing.getCounterClockWise());
            final BlockPos right = end(level, pos, facing.getClockWise());
            if (isCorner(level, left)) {
                place = left.distManhattan(pos);
                foot = left;
            } else {
                place = ACROSS - right.distManhattan(pos);
                foot = right;
            }
            if (place < 1 || place > FACES) {
                return state;
            }
        }
        int base = foot.getY();
        while (tower(level, new BlockPos(foot.getX(), base - 1, foot.getZ()))) {
            base--;
        }
        final int high = pos.getY() - base;
        final boolean odd = high / LEVEL % 2 == 1;
        final boolean lean = odd ^ leansBack(facing);
        final int slot = corner ? FACES + (odd ^ leansBack(facing.getClockWise()) ? 1 : 0) : place - 1;
        return state.setValue(FACING, facing).setValue(BAY, bay(slot, high % LEVEL == 1, lean,
                high % FLOORS == 0));
    }

    /*
     * Whether a face looking into the shaft {@code way} runs its levels' diagonals the other way: the approved tower's
     * diagonals rise toward the east on its north and south faces and toward the south on its east and west ones.
     */
    private static boolean leansBack(final Direction way) {
        return way == Direction.SOUTH || way == Direction.WEST;
    }

    /* The last frame from {@code pos} toward {@code way} along the wall. */
    private static BlockPos end(final BlockGetter level, final BlockPos pos, final Direction way) {
        BlockPos at = pos;
        for (int step = 0; step < ACROSS && tower(level, at.relative(way)); step++) {
            at = at.relative(way);
        }
        return at;
    }

    /* Whether the frame at {@code pos} is a corner of the tower: frames beside it on two sides at a right angle. */
    private static boolean isCorner(final BlockGetter level, final BlockPos pos) {
        for (final Direction way : Direction.Plane.HORIZONTAL) {
            if (tower(level, pos.relative(way)) && tower(level, pos.relative(way.getClockWise()))) {
                return true;
            }
        }
        return false;
    }

    private static boolean tower(final BlockGetter level, final BlockPos pos) {
        return kindOf(level, pos) == Kind.TOWER;
    }
}
