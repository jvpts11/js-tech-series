/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.hangar;

import dev.jstech.core.multiblock.FormedPartBlock;
import dev.jstech.core.multiblock.MultiblockPortBlock;
import dev.jstech.core.text.Text;
import dev.jstech.space.rocket.RocketKitBlocks;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * How a Rocket Hangar finds its bay, from its terminal set in a wall and looking in. Its shell is free, walls and roof
 * of any solid block, its bay doors in them; what stands in the bay, the hangar's own pieces and the rocket it builds,
 * is not its shell. From the block in front of the terminal it measures down to the floor, up to the roof, along its
 * wall to the walls each side and across to the far wall; the bay must be no smaller and no larger than the server
 * allows, closed on every side, with a line of crane rail along the top of two facing walls, the crane bridge spanning
 * the bay from one to the other, the hoist hanging under it, and a bay door from the floor up. The item ports set in
 * its shell are found as the shell is looked at.
 */
public final class HangarBays {

    /* The rockets' kit blocks, which stand in a bay as it builds them; gathered once, after they are registered. */
    private static @Nullable Set<Block> rocketKit;

    private HangarBays() {
    }

    /**
     * The bay of the hangar whose terminal stands at {@code terminal} looking {@code into}, or why there is none.
     *
     * @param limits the narrowest, the widest, the lowest and the tallest a bay may be
     */
    public static Result find(final Level level, final BlockPos terminal, final Direction into, final Limits limits) {
        final BlockPos front = terminal.relative(into);
        if (shell(level, front)) {
            return Result.refused(HangarTexts.NO_BAY.text());
        }
        final int floor = reach(level, front, Direction.DOWN, limits.tallest());
        final int roof = reach(level, front, Direction.UP, limits.tallest());
        final Direction side = into.getClockWise();
        final int right = reach(level, front, side, limits.widest());
        final int left = reach(level, front, side.getOpposite(), limits.widest());
        final int deep = reach(level, front, into, limits.widest());
        if (floor < 0 || roof < 0 || right < 0 || left < 0 || deep < 0) {
            return Result.refused(HangarTexts.TOO_LARGE.with(limits.widest(), limits.tallest()));
        }
        final BlockPos a = front.below(floor - 1).relative(side, right - 1);
        final BlockPos b = front.above(roof - 1).relative(side.getOpposite(), left - 1).relative(into, deep - 1);
        final BlockPos low = new BlockPos(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()),
                Math.min(a.getZ(), b.getZ()));
        final BlockPos high = new BlockPos(Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()),
                Math.max(a.getZ(), b.getZ()));
        final int width = high.getX() - low.getX() + 1;
        final int depth = high.getZ() - low.getZ() + 1;
        final int height = high.getY() - low.getY() + 1;
        if (Math.min(width, depth) < limits.narrowest() || height < limits.lowest()) {
            return Result.refused(HangarTexts.TOO_SMALL.with(width, depth, height, limits.narrowest(),
                    limits.lowest()));
        }
        if (Math.max(width, depth) > limits.widest() || height > limits.tallest()) {
            return Result.refused(HangarTexts.TOO_LARGE.with(limits.widest(), limits.tallest()));
        }
        final List<BlockPos> ports = new ArrayList<>();
        final BlockPos open = opening(level, low, high, ports);
        if (open != null) {
            return Result.refused(HangarTexts.OPEN.with(open.getX(), open.getY(), open.getZ()));
        }
        return crane(level, low, high).withPorts(ports);
    }

    /** Whether the block at {@code pos} is a piece of a hangar's shell: a bay door, or any solid block not in a bay. */
    public static boolean shell(final Level level, final BlockPos pos) {
        final BlockState state = level.getBlockState(pos);
        if (state.is(HangarBlocks.BAY_DOOR.get())) {
            return true;
        }
        return !contents(state) && state.isCollisionShapeFullBlock(level, pos);
    }

    /* Whether {@code state} is of what stands in a bay: the hangar's own pieces, or a rocket's, kit or formed. */
    private static boolean contents(final BlockState state) {
        final Block block = state.getBlock();
        if (rocketKit == null) {
            final Set<Block> made = Collections.newSetFromMap(new IdentityHashMap<>());
            RocketKitBlocks.all().forEach(kit -> made.add(kit.get()));
            rocketKit = made;
        }
        return block instanceof HangarPieceBlock && block != HangarBlocks.BAY_DOOR.get()
                || block == HangarBlocks.ASSEMBLY_CONSOLE.get() || block instanceof FormedPartBlock
                || rocketKit.contains(block);
    }

    /* How many steps from {@code from} toward {@code way} the shell is, or -1 for none within {@code most}. */
    private static int reach(final Level level, final BlockPos from, final Direction way, final int most) {
        final BlockPos.MutableBlockPos at = from.mutable();
        for (int step = 1; step <= most + 1; step++) {
            at.move(way);
            if (shell(level, at)) {
                return step;
            }
        }
        return -1;
    }

    /*
     * The first place of the bay's shell that is not shell: its walls, its floor and its roof; or none. The ports
     * found in the shell on the way are put in {@code ports}.
     */
    private static @Nullable BlockPos opening(final Level level, final BlockPos low, final BlockPos high,
                                              final List<BlockPos> ports) {
        final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int x = low.getX() - 1; x <= high.getX() + 1; x++) {
            for (int z = low.getZ() - 1; z <= high.getZ() + 1; z++) {
                final boolean edge = x < low.getX() || x > high.getX() || z < low.getZ() || z > high.getZ();
                for (int y = low.getY() - 1; y <= high.getY() + 1; y++) {
                    final boolean cap = y < low.getY() || y > high.getY();
                    // The corners where two walls meet are no one's to close.
                    final boolean corner = (x < low.getX() || x > high.getX()) && (z < low.getZ() || z > high.getZ());
                    if (!(edge || cap) || corner) {
                        continue;
                    }
                    if (!shell(level, at.set(x, y, z))) {
                        return at.immutable();
                    }
                    if (level.getBlockState(at).getBlock() instanceof MultiblockPortBlock) {
                        ports.add(at.immutable());
                    }
                }
            }
        }
        return null;
    }

    /* The crane: rails along the top of two facing walls, the bridge from one to the other, the hoist under it. */
    private static Result crane(final Level level, final BlockPos low, final BlockPos high) {
        final int top = high.getY();
        for (final Direction.Axis span : new Direction.Axis[] {Direction.Axis.Z, Direction.Axis.X}) {
            // Spanning north to south, the rails run along the north and south walls, east to west; and so on.
            final boolean alongX = span == Direction.Axis.Z;
            final int railFrom = alongX ? low.getX() : low.getZ();
            final int railTo = alongX ? high.getX() : high.getZ();
            final int nearWall = alongX ? low.getZ() : low.getX();
            final int farWall = alongX ? high.getZ() : high.getX();
            if (!line(level, top, alongX, railFrom, railTo, nearWall, HangarBlocks.CRANE_RAIL.get())
                    || !line(level, top, alongX, railFrom, railTo, farWall, HangarBlocks.CRANE_RAIL.get())) {
                continue;
            }
            for (int along = railFrom; along <= railTo; along++) {
                if (!line(level, top, !alongX, nearWall + 1, farWall - 1, along, HangarBlocks.CRANE_BRIDGE.get())) {
                    continue;
                }
                final BlockPos bridge = alongX ? new BlockPos(along, top, nearWall + 1)
                        : new BlockPos(nearWall + 1, top, along);
                for (int across = nearWall + 1; across <= farWall - 1; across++) {
                    final BlockPos under = alongX ? new BlockPos(along, top - 1, across)
                            : new BlockPos(across, top - 1, along);
                    if (level.getBlockState(under).is(HangarBlocks.CRANE_HOIST.get())) {
                        return door(level, low, high) ? Result.formed(new HangarBay(low, high, span, bridge, under))
                                : Result.refused(HangarTexts.NO_DOOR.text());
                    }
                }
                return Result.refused(HangarTexts.NO_HOIST.text());
            }
            return Result.refused(HangarTexts.NO_BRIDGE.text());
        }
        return Result.refused(HangarTexts.NO_RAILS.text());
    }

    /*
     * Whether every block of the line at height {@code y} from {@code from} to {@code to}, along X where
     * {@code alongX} or else along Z, at {@code at} on the other axis, is {@code block}.
     */
    private static boolean line(final Level level, final int y, final boolean alongX, final int from, final int to,
                                final int at, final Block block) {
        if (from > to) {
            return false;
        }
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int k = from; k <= to; k++) {
            pos.set(alongX ? k : at, y, alongX ? at : k);
            if (!level.getBlockState(pos).is(block)) {
                return false;
            }
        }
        return true;
    }

    /* Whether a bay door stands in a wall on the floor's level. */
    private static boolean door(final Level level, final BlockPos low, final BlockPos high) {
        final int y = low.getY();
        final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int x = low.getX(); x <= high.getX(); x++) {
            if (level.getBlockState(at.set(x, y, low.getZ() - 1)).is(HangarBlocks.BAY_DOOR.get())
                    || level.getBlockState(at.set(x, y, high.getZ() + 1)).is(HangarBlocks.BAY_DOOR.get())) {
                return true;
            }
        }
        for (int z = low.getZ(); z <= high.getZ(); z++) {
            if (level.getBlockState(at.set(low.getX() - 1, y, z)).is(HangarBlocks.BAY_DOOR.get())
                    || level.getBlockState(at.set(high.getX() + 1, y, z)).is(HangarBlocks.BAY_DOOR.get())) {
                return true;
            }
        }
        return false;
    }

    /**
     * How small and how large a bay may be.
     *
     * @param narrowest the narrowest across and deep, in blocks
     * @param widest    the widest across and deep
     * @param lowest    the lowest, floor to roof
     * @param tallest   the tallest
     */
    public record Limits(int narrowest, int widest, int lowest, int tallest) {
    }

    /**
     * What a hangar found: its bay, or why it has none.
     *
     * @param bay    the bay, if it formed
     * @param reason what the terminal says: the bay's size, or what is missing
     * @param ports  the ports set in its shell, where it formed
     */
    public record Result(Optional<HangarBay> bay, Text reason, List<BlockPos> ports) {

        public Result {
            ports = List.copyOf(ports);
        }

        static Result refused(final Text why) {
            return new Result(Optional.empty(), why, List.of());
        }

        static Result formed(final HangarBay bay) {
            return new Result(Optional.of(bay), HangarTexts.FORMED.with(bay.width(), bay.depth(), bay.height()),
                    List.of());
        }

        /* The same, with the ports of its shell where it formed. */
        Result withPorts(final List<BlockPos> found) {
            return this.bay.isPresent() ? new Result(this.bay, this.reason, found) : this;
        }
    }
}
