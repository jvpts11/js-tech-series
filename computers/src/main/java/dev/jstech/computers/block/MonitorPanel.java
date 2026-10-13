/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.block;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Flat monitors joined into one big screen, as it stands in the world: its bottom-left monitor as the front is seen,
 * which way its rows run, and how many monitors wide and tall it is.
 *
 * <p>The screen counts as one monitor on one video output: whichever of its monitors a cable or a computer reaches
 * links it, and the rest show what that one shows. The bottom-right monitor keeps the power button, the others show
 * none, so the whole has one bezel, one chin and one button.
 */
public record MonitorPanel(BlockPos origin, Direction right, Direction front, int width, int height) {

    /*
     * Bumped whenever a monitor is placed or taken away anywhere, on either side; a monitor works its panel out again
     * when this has moved since it last did, and otherwise keeps the one it has.
     */
    private static int generation;

    /** The monitor {@code u} across and {@code v} up from the bottom-left one. */
    public BlockPos at(final int u, final int v) {
        return origin.relative(right, u).above(v);
    }

    /** The monitor that keeps the power button: the bottom-right one. */
    public BlockPos buttonHolder() {
        return at(width - 1, 0);
    }

    /** Every monitor of the screen, row by row from the bottom-left one: the order a link is settled in. */
    public List<BlockPos> members() {
        final List<BlockPos> members = new ArrayList<>(width * height);
        for (int v = 0; v < height; v++) {
            for (int u = 0; u < width; u++) {
                members.add(at(u, v));
            }
        }
        return members;
    }

    public boolean contains(final BlockPos pos) {
        final int dy = pos.getY() - origin.getY();
        final int u = (pos.getX() - origin.getX()) * right.getStepX() + (pos.getZ() - origin.getZ()) * right.getStepZ();
        final BlockPos back = at(u, dy);
        return dy >= 0 && dy < height && u >= 0 && u < width && back.equals(pos);
    }

    /** A monitor was placed or taken away: every monitor works its panel out again. */
    public static void changed() {
        generation++;
    }

    /** The count that moves with every monitor placed or taken away. */
    public static int generation() {
        return generation;
    }

    /**
     * The screen the monitor at {@code pos} is part of, or null when it stands alone: a monitor that is not a flat
     * panel, or one whose neighbours do not fill a rectangle within the limit with it.
     */
    @Nullable
    public static MonitorPanel resolve(final BlockGetter level, final BlockPos pos) {
        final BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof MonitorBlock monitor) || !monitor.kind().flat()) {
            return null;
        }
        final Direction facing = state.getValue(MonitorBlock.FACING);
        // The screen faces the player who placed it, so its front is against the facing.
        final Direction front = facing.getOpposite();
        final Direction right = facing.getClockWise();
        final Set<Long> cells = new HashSet<>();
        final Set<BlockPos> seen = new HashSet<>();
        final Deque<BlockPos> open = new ArrayDeque<>();
        open.add(pos);
        seen.add(pos);
        while (!open.isEmpty()) {
            final BlockPos at = open.poll();
            final int u = (at.getX() - pos.getX()) * right.getStepX() + (at.getZ() - pos.getZ()) * right.getStepZ();
            cells.add(PanelShape.cell(u, at.getY() - pos.getY()));
            if (cells.size() > PanelShape.MAX_CELLS) {
                return null;
            }
            for (final BlockPos next : List.of(at.relative(right), at.relative(right.getOpposite()), at.above(),
                    at.below())) {
                if (seen.add(next) && sameScreen(level.getBlockState(next), monitor, facing)) {
                    open.add(next);
                }
            }
        }
        final PanelShape shape = PanelShape.of(cells);
        if (shape == null) {
            return null;
        }
        return new MonitorPanel(pos.relative(right, shape.left()).above(shape.bottom()), right, front, shape.width(),
                shape.height());
    }

    private static boolean sameScreen(final BlockState state, final MonitorBlock kind, final Direction facing) {
        return state.getBlock() instanceof MonitorBlock other && other.kind() == kind.kind()
                && state.getValue(MonitorBlock.FACING) == facing;
    }
}
