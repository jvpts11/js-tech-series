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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.jetbrains.annotations.Nullable;

/**
 * A frame of the umbilical tower's red truss, as the approved tower stands: one great truss round a dark shaft, not a
 * cage of small ones. Formed, each frame draws its own piece of it, by where it stands: a frame at a corner the thick
 * column there; a frame along a face the girder across the whole face every two blocks, its stretch of the face's one
 * long diagonal, which leans the other way each level as the approved tower's do, the dark wall of the shaft behind it
 * with its rails and lines, and every eight blocks the grated floor round the shaft. It faces into the shaft, a corner
 * along its face; its bay says the rest. On the tower's top the frames are the crane's house, and the row of frames
 * over it, reaching out past the tower over the rocket and a little past its back, the crane's boom; the boom's tip
 * over the rocket draws the whole crane, house, boom, cable and hook, as the approved launcher draws it.
 */
public final class TowerFrameBlock extends LauncherPieceBlock implements EntityBlock {

    private static final MapCodec<TowerFrameBlock> CODEC = simpleCodec(TowerFrameBlock::new);

    /**
     * Where a formed frame stands in its truss: its place along its face (the three between the corners, then a
     * corner, which leans its other face's diagonal either way), whether its level is an odd one from the tower's
     * foot, whether it is the upper block of its level, which way its diagonal leans, and whether the grated floor
     * crosses it.
     */
    public static final IntegerProperty BAY = IntegerProperty.create("bay", 0, 79);
    /** The bay of the boom's tip, which draws the crane. */
    public static final int TIP = 1;
    /* The tower's width between the outsides of its walls, so a wall finds the one across the shaft. */
    private static final int ACROSS = 4;
    /* How many blocks a level of the truss is, and how many between the grated floors. */
    private static final int LEVEL = 2;
    private static final int FLOORS = 8;
    private static final int FACES = 3;
    /* The longest row of frames walked along. */
    private static final int LONGEST = 64;

    public TowerFrameBlock(final BlockBehaviour.Properties properties) {
        super(properties, Kind.TOWER);
    }

    /**
     * The bay of a frame: {@code slot} its place, 0 to 2 the three along its face between the corners, 3 a corner
     * whose other face's diagonal leans as it would and 4 one whose leans back; {@code odd} whether its level is an
     * odd one from the tower's foot, so with {@code upper} which of four blocks it is, the shaft's lamps every fourth;
     * {@code upper} whether it is the upper block of its level; {@code lean} whether its face's diagonal leans back;
     * {@code floor} whether the grated floor crosses it, at the foot of a lower block or the head of an upper one.
     */
    public static int bay(final int slot, final boolean odd, final boolean upper, final boolean lean,
                          final boolean floor) {
        return slot * 16 + (odd ? 8 : 0) + (upper ? 4 : 0) + (lean ? 2 : 0) + (floor ? 1 : 0);
    }

    /** Whether {@code state} is a formed boom's tip, which draws the crane. */
    public static boolean tip(final BlockState state) {
        return state.getValue(FORMED) && state.getValue(ROLE) == Role.END && state.getValue(BAY) == TIP;
    }

    /** Only the boom's tip keeps anything: the crane it draws. */
    @Override
    public @Nullable BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return tip(state) ? new CraneBlockEntity(pos, state) : null;
    }

    /** Its tower formed or came apart: as the boom's tip it measures the crane, or forgets it. */
    @Override
    public void partFormed(final ServerLevel level, final BlockPos pos, final boolean formed) {
        super.partFormed(level, pos, formed);
        final BlockState now = level.getBlockState(pos);
        if (level.getBlockEntity(pos) instanceof CraneBlockEntity entity) {
            entity.crane(now.getBlock() == this && tip(now) ? measure(level, pos, now.getValue(FACING)
                    .getOpposite()) : null);
        }
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
     * A frame of the tower: the crane's house on its top, under the boom or not; the boom, its tip the end away from
     * the tower; or a frame of its walls: whether it is a corner or which of the three along its face, from its
     * frames beside it, its level and the lean of its diagonals counted from the foot of its face's corner, which no
     * doorway breaks.
     */
    @Override
    protected BlockState shaped(final BlockGetter level, final BlockPos pos, final BlockState state) {
        final Role role = role(level, pos);
        if (role == Role.END) {
            return boom(level, pos, state.setValue(ROLE, role));
        }
        if (role == Role.TOP) {
            return state.setValue(ROLE, role);
        }
        final BlockState body = state.setValue(ROLE, role);
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
            return body;
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
                return body;
            }
        }
        int base = foot.getY();
        while (tower(level, new BlockPos(foot.getX(), base - 1, foot.getZ()))) {
            base--;
        }
        final int high = pos.getY() - base;
        final boolean odd = high / LEVEL % 2 == 1;
        final boolean upper = high % LEVEL == 1;
        final boolean lean = odd ^ leansBack(facing);
        final int slot = corner ? FACES + (odd ^ leansBack(facing.getClockWise()) ? 1 : 0) : place - 1;
        // The floor lies on a level's line every eight blocks, a little of it in the block under the line.
        final boolean floor = upper ? high % FLOORS == FLOORS - 1 : high % FLOORS == 0;
        return body.setValue(FACING, facing).setValue(BAY, bay(slot, odd, upper, lean, floor));
    }

    /*
     * What a frame is in its tower: the boom, a row of frames on the tower's top that reaches out over nothing; the
     * house, a frame with nothing over it but the boom; or a frame of its walls.
     */
    private static Role role(final BlockGetter level, final BlockPos pos) {
        if (tower(level, pos.above())) {
            return onBoom(level, pos.above()) ? Role.TOP : Role.BODY;
        }
        if (onBoom(level, pos) || kindOf(level, pos.below()) == null) {
            return Role.END;
        }
        return Role.TOP;
    }

    /*
     * Whether the frame at {@code pos} is in a row of frames, any way along, one of which stands over nothing of the
     * launcher with nothing over it: a boom reaching out from the tower's top, not a wall over a doorway.
     */
    private static boolean onBoom(final BlockGetter level, final BlockPos pos) {
        for (final Direction way : Direction.Plane.HORIZONTAL) {
            BlockPos at = pos;
            for (int step = 0; step < LONGEST && tower(level, at); step++) {
                if (kindOf(level, at.below()) == null && !tower(level, at.above())) {
                    return true;
                }
                at = at.relative(way);
            }
        }
        return false;
    }

    /*
     * A frame of the boom: it faces along the boom toward its tip, the end farther from the tower under it; the tip
     * itself takes the tip's bay.
     */
    private static BlockState boom(final BlockGetter level, final BlockPos pos, final BlockState state) {
        Direction axis = null;
        for (final Direction way : Direction.Plane.HORIZONTAL) {
            if (axis == null && tower(level, pos.relative(way))) {
                axis = way;
            }
        }
        if (axis == null) {
            return state;
        }
        final int forward = steps(level, pos, axis);
        final int backward = steps(level, pos, axis.getOpposite());
        final BlockPos start = pos.relative(axis.getOpposite(), backward);
        final int length = backward + forward + 1;
        int lo = -1;
        int hi = -1;
        for (int i = 0; i < length; i++) {
            if (tower(level, start.relative(axis, i).below())) {
                lo = lo < 0 ? i : lo;
                hi = i;
            }
        }
        if (lo < 0) {
            return state;
        }
        // The tip is the end farther from the tower the boom stands on.
        final boolean tipAtStart = lo >= length - 1 - hi;
        final Direction toTip = tipAtStart ? axis.getOpposite() : axis;
        final int fromTip = tipAtStart ? backward : forward;
        return state.setValue(FACING, toTip).setValue(BAY, fromTip == 0 ? TIP : 0);
    }

    /* The crane, as its boom's tip at {@code tip} measures it, the tower {@code toTower} along the boom. */
    private static CraneBlockEntity.Crane measure(final BlockGetter level, final BlockPos tip,
                                                  final Direction toTower) {
        final int back = steps(level, tip, toTower);
        int near = -1;
        int far = -1;
        for (int i = 0; i <= back; i++) {
            if (tower(level, tip.relative(toTower, i).below())) {
                near = near < 0 ? i : near;
                far = i;
            }
        }
        return new CraneBlockEntity.Crane(toTower, near, far, back);
    }

    /* How many frames follow {@code pos} toward {@code way} in a row. */
    private static int steps(final BlockGetter level, final BlockPos pos, final Direction way) {
        int count = 0;
        while (count < LONGEST && tower(level, pos.relative(way, count + 1))) {
            count++;
        }
        return count;
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
