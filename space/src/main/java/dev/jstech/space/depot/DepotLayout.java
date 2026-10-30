/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.depot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;

/**
 * How a propellant depot is built, as the approved one stands: a welded sphere, built as a cube of Sphere Shell of odd
 * edge from three to nine blocks, its outside whole; a column of Tank Leg under each of its four bottom corners down to
 * the ground, all as tall; a Vent Stack on the middle of its top; and the Transfer Pump on the ground against a leg,
 * which finds it, holds its propellant and draws it whole.
 */
public final class DepotLayout {

    /** The smallest and the largest sphere, by its edge in blocks. */
    public static final int SMALLEST = 3;
    public static final int LARGEST = 9;
    /** How many millibuckets a cubic block holds. */
    public static final double CUBIC_BLOCK = 1000.0;

    private DepotLayout() {
    }

    /** The depot whose transfer pump stands at {@code pump}, if one is whole there. */
    public static Optional<Depot> find(final BlockGetter level, final BlockPos pump) {
        for (final Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos up = pump.relative(side);
            if (!is(level, up, DepotBlocks.TANK_LEG.get())) {
                continue;
            }
            while (is(level, up, DepotBlocks.TANK_LEG.get())) {
                up = up.above();
            }
            final Optional<Depot> found = cubeFrom(level, up);
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
    }

    /** Every piece of {@code depot}: its sphere's shell blocks, its legs and its vent stack. */
    public static List<BlockPos> pieces(final BlockGetter level, final Depot depot) {
        final List<BlockPos> out = new ArrayList<>();
        final BlockPos low = depot.low();
        final int n = depot.edge();
        for (int x = 0; x < n; x++) {
            for (int y = 0; y < n; y++) {
                for (int z = 0; z < n; z++) {
                    final BlockPos at = low.offset(x, y, z);
                    if (is(level, at, DepotBlocks.SPHERE_SHELL.get())) {
                        out.add(at);
                    }
                }
            }
        }
        for (final BlockPos corner : depot.corners()) {
            for (int k = 1; k <= depot.legs(); k++) {
                out.add(corner.below(k));
            }
        }
        out.add(depot.vent());
        return out;
    }

    /* The depot whose sphere has its bottom corner at {@code corner}, if it is whole. */
    private static Optional<Depot> cubeFrom(final BlockGetter level, final BlockPos corner) {
        if (!is(level, corner, DepotBlocks.SPHERE_SHELL.get())) {
            return Optional.empty();
        }
        final int east = run(level, corner, Direction.EAST);
        final int west = run(level, corner, Direction.WEST);
        final int south = run(level, corner, Direction.SOUTH);
        final int north = run(level, corner, Direction.NORTH);
        final int n = east + west + 1;
        if (east > 0 && west > 0 || south > 0 && north > 0 || south + north + 1 != n
                || run(level, corner, Direction.UP) + 1 != n || n < SMALLEST || n > LARGEST || n % 2 == 0) {
            return Optional.empty();
        }
        final BlockPos low = corner.offset(-west, 0, -north);
        for (int x = 0; x < n; x++) {
            for (int y = 0; y < n; y++) {
                for (int z = 0; z < n; z++) {
                    final boolean outside = x == 0 || y == 0 || z == 0 || x == n - 1 || y == n - 1 || z == n - 1;
                    if (outside && !is(level, low.offset(x, y, z), DepotBlocks.SPHERE_SHELL.get())) {
                        return Optional.empty();
                    }
                }
            }
        }
        int legs = -1;
        for (final BlockPos bottom : new Depot(low, n, 0).corners()) {
            int k = 0;
            while (k < LARGEST * 2 && is(level, bottom.below(k + 1), DepotBlocks.TANK_LEG.get())) {
                k++;
            }
            if (k == 0 || legs >= 0 && k != legs || level.getBlockState(bottom.below(k + 1)).isAir()) {
                return Optional.empty();
            }
            legs = k;
        }
        final Depot depot = new Depot(low, n, legs);
        return is(level, depot.vent(), DepotBlocks.VENT_STACK.get()) ? Optional.of(depot) : Optional.empty();
    }

    /* How many shell blocks follow {@code from} along {@code way}, at most a sphere's edge. */
    private static int run(final BlockGetter level, final BlockPos from, final Direction way) {
        int k = 0;
        while (k < LARGEST && is(level, from.relative(way, k + 1), DepotBlocks.SPHERE_SHELL.get())) {
            k++;
        }
        return k;
    }

    private static boolean is(final BlockGetter level, final BlockPos at, final Block block) {
        return level.getBlockState(at).is(block);
    }

    /**
     * A whole depot.
     *
     * @param low  its sphere's lowest corner, the least of its places along each axis
     * @param edge its sphere's edge, in blocks
     * @param legs how tall its legs are, under the sphere
     */
    public record Depot(BlockPos low, int edge, int legs) {

        public static final Codec<Depot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockPos.CODEC.fieldOf("low").forGetter(Depot::low),
                Codec.INT.fieldOf("edge").forGetter(Depot::edge),
                Codec.INT.fieldOf("legs").forGetter(Depot::legs)
        ).apply(instance, Depot::new));

        /** How much its sphere holds, in millibuckets: a ball as wide as the cube it is built in. */
        public int capacity() {
            return (int) Math.round(Math.PI / 6 * this.edge * this.edge * this.edge * CUBIC_BLOCK);
        }

        /** Its sphere's four bottom corners. */
        public List<BlockPos> corners() {
            final int far = this.edge - 1;
            return List.of(this.low, this.low.offset(far, 0, 0), this.low.offset(0, 0, far),
                    this.low.offset(far, 0, far));
        }

        /** Where its vent stack stands, on the middle of the sphere's top. */
        public BlockPos vent() {
            return this.low.offset(this.edge / 2, this.edge, this.edge / 2);
        }

        /** The same depot with its places counted from {@code from}. */
        public Depot from(final BlockPos from) {
            return new Depot(this.low.subtract(from), this.edge, this.legs);
        }
    }
}
