/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.mcp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.PriorityQueue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The way a player walks to a place, as a player would find it: block to block along the ground, up a step, a slab or
 * a stair, up a block with a jump, down a drop of three at most, and through a door, the player's own box fitting
 * every place it stands. Whether the player fits is asked as the game asks it, so the blocks of a moving structure
 * are ground and walls too, and a ship's deck is walked as the world's. A place with no way to it gives the way to the
 * place nearest it that has one, and says so.
 */
final class PlayPaths {

    /* How far a way may lead, in blocks across, and how many places it may weigh before it gives up. */
    private static final int MOST_ACROSS = 64;
    private static final int MOST_PLACES = 4_000;
    /* The heights a player stands at in a block are tried in eighths of it; a step up a whole block needs a jump. */
    private static final int EIGHTHS = 8;
    private static final double JUMP = 1.0 + 1e-3;
    private static final int MOST_DROP = 3;
    private static final double BELOW = 1.0 / EIGHTHS;

    private PlayPaths() {
    }

    /**
     * The way from where {@code player} stands to {@code goal}: the places to stand at on it, the first the next
     * after where they stand; {@code reached} says whether it reaches the goal, or the place nearest it.
     */
    static Way find(final Level level, final Player player, final BlockPos goal) {
        final BlockPos start = player.blockPosition();
        if (start.equals(goal)) {
            // Already in the block: the way is to its middle.
            return new Way(List.of(new Spot(new Vec3(goal.getX() + 0.5, player.getY(), goal.getZ() + 0.5), goal,
                    false)), true);
        }
        final Map<BlockPos, Step> steps = new HashMap<>();
        final PriorityQueue<Step> open = new PriorityQueue<>((a, b) -> Double.compare(a.cost() + a.left(),
                b.cost() + b.left()));
        final Step first = new Step(start, player.getY(), false, 0, distance(start, goal), null);
        steps.put(start, first);
        open.add(first);
        Step nearest = first;
        while (!open.isEmpty() && steps.size() < MOST_PLACES) {
            final Step at = open.poll();
            if (at.left() < nearest.left()) {
                nearest = at;
            }
            if (at.place().equals(goal)) {
                return new Way(path(at), true);
            }
            for (final Direction side : Direction.Plane.HORIZONTAL) {
                for (int up = 1; up >= -MOST_DROP; up--) {
                    final BlockPos next = at.place().relative(side).above(up);
                    if (Math.abs(next.getX() - start.getX()) > MOST_ACROSS
                            || Math.abs(next.getZ() - start.getZ()) > MOST_ACROSS) {
                        continue;
                    }
                    final Step stepped = stepTo(level, player, at, next, goal);
                    if (stepped == null) {
                        continue;
                    }
                    final Step known = steps.get(next);
                    if (known == null || stepped.cost() < known.cost()) {
                        steps.put(next, stepped);
                        open.add(stepped);
                    }
                    // The highest place a side leads to is the one stood at: one below it is under its floor.
                    break;
                }
            }
        }
        return new Way(path(nearest), false);
    }

    /* The step from {@code at} to {@code next}, a door's place or one the player stands in; null where it is none. */
    private static Step stepTo(final Level level, final Player player, final Step at, final BlockPos next,
                               final BlockPos goal) {
        final BlockState state = level.getBlockState(next);
        if (state.getBlock() instanceof DoorBlock door) {
            // A door is walked through at its floor, opened first when it is shut; one no hand opens is a wall.
            final boolean shut = !state.getValue(DoorBlock.OPEN);
            return state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER && next.getY() == at.place().getY()
                    && (!shut || door.type().canOpenByHand()) ? new Step(next, next.getY(), shut, at.cost() + 1,
                    distance(next, goal), at) : null;
        }
        final OptionalDouble stand = standAt(level, player, next);
        if (stand.isEmpty() || stand.getAsDouble() - at.height() > JUMP
                || at.height() - stand.getAsDouble() > MOST_DROP) {
            return null;
        }
        // Going up, the player's head passes over where it stood: the box must fit there at the higher height.
        if (stand.getAsDouble() > at.height() && !fits(level, player, at.place(), stand.getAsDouble())) {
            return null;
        }
        return new Step(next, stand.getAsDouble(), false, at.cost() + 1 + Math.abs(stand.getAsDouble() - at.height()),
                distance(next, goal), at);
    }

    /* The height the player stands at in the block {@code place}: the lowest their box fits at with ground under it. */
    private static OptionalDouble standAt(final Level level, final Player player, final BlockPos place) {
        for (int eighth = 0; eighth < EIGHTHS; eighth++) {
            final double height = place.getY() + (double) eighth / EIGHTHS;
            if (fits(level, player, place, height) && !fits(level, player, place, height - BELOW)) {
                return OptionalDouble.of(height);
            }
        }
        return OptionalDouble.empty();
    }

    private static boolean fits(final Level level, final Player player, final BlockPos place, final double height) {
        final double half = player.getBbWidth() / 2;
        final AABB box = new AABB(place.getX() + 0.5 - half, height, place.getZ() + 0.5 - half,
                place.getX() + 0.5 + half, height + player.getBbHeight(), place.getZ() + 0.5 + half);
        return level.noCollision(player, box);
    }

    private static double distance(final BlockPos from, final BlockPos to) {
        return Math.abs(from.getX() - to.getX()) + Math.abs(from.getY() - to.getY()) + Math.abs(from.getZ()
                - to.getZ());
    }

    private static List<Spot> path(final Step last) {
        final List<Spot> out = new ArrayList<>();
        for (Step at = last; at != null && at.from() != null; at = at.from()) {
            out.add(new Spot(new Vec3(at.place().getX() + 0.5, at.height(), at.place().getZ() + 0.5), at.place(),
                    at.door()));
        }
        Collections.reverse(out);
        return out;
    }

    /**
     * A place on the way: where the player stands in it, its block, and whether it is a door to open first.
     *
     * @param at    where the player stands, their feet's middle
     * @param place its block
     * @param door  whether it is a door's place
     */
    record Spot(Vec3 at, BlockPos place, boolean door) {
    }

    /**
     * A way found.
     *
     * @param spots   the places stood at on it, the next first
     * @param reached whether it reaches the goal; else it ends at the place nearest it
     */
    record Way(List<Spot> spots, boolean reached) {
    }

    /* A place weighed: where it is, the height stood at in it, how far it took, how far is left, and whence. */
    private record Step(BlockPos place, double height, boolean door, double cost, double left, Step from) {
    }
}
