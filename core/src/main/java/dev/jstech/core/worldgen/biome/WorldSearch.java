/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.biome;

import java.util.Optional;

/**
 * A search across the whole of a bounded world for the place nearest a start where something holds, for a world too
 * big to read every few blocks of: first on a coarse grid, a few thousand steps across the world's longer side, ring
 * after ring out from the start until a place holds and no ring left could hold a nearer one; then finer and finer
 * round the place found, down to a step of a few dozen blocks, keeping the place nearest the start; and last a fine
 * step past it, into what it found, when that holds too, so the place lies inside rather than at the very edge, where
 * what a place holds can be read either way. What is smaller than a coarse step can fall between its points: the
 * search is for what spreads over a step or more, as the zones a map lays, and finds its nearest place to within about
 * two fine steps.
 */
public final class WorldSearch {

    /* How many steps the coarse grid takes across the world's longer side. */
    private static final int ACROSS = 2048;
    /* The finest step, the one the game's own biome search takes. */
    private static final int FINEST = 32;
    /* How much finer each round round the place found is than the last. */
    private static final int FINER = 16;

    private WorldSearch() {
    }

    /**
     * The place nearest the start where {@code probe} holds, inside the bounds, all given in blocks; the start is taken
     * inside them when it lies past them.
     *
     * @param probe what is asked of a place
     * @param fromX the start's x
     * @param fromZ the start's z
     * @param minX  the bounds' west end, the westmost column inside them
     * @param maxX  their east end, the eastmost column inside them
     * @param minZ  their north end
     * @param maxZ  their south end
     * @return the place, or empty when it holds at none the search reads
     * @throws IllegalArgumentException when the bounds hold no column
     */
    public static Optional<Found> nearest(final IProbe probe, final int fromX, final int fromZ, final int minX,
                                          final int maxX, final int minZ, final int maxZ) {
        if (maxX < minX || maxZ < minZ) {
            throw new IllegalArgumentException("the bounds run from west to east and north to south; got " + minX
                    + ".." + maxX + " by " + minZ + ".." + maxZ);
        }
        final Area area = new Area(probe, Math.clamp(fromX, minX, maxX), Math.clamp(fromZ, minZ, maxZ), minX, maxX,
                minZ, maxZ);
        final long span = Math.max((long) maxX - minX, (long) maxZ - minZ) + 1;
        final int step = (int) Math.max(FINEST, Math.ceilDiv(span, ACROSS));
        area.rings(step);
        if (area.best == null) {
            return Optional.empty();
        }
        int around = step;
        while (around > FINEST) {
            final int finer = Math.max(FINEST, around / FINER);
            area.square(area.best, around, finer);
            around = finer;
        }
        return Optional.of(area.inward(area.best));
    }

    /** What the search asks of a place. */
    @FunctionalInterface
    public interface IProbe {

        /** Whether what is searched for holds at the column {@code x}, {@code z}. */
        boolean holds(int x, int z);
    }

    /**
     * A place found.
     *
     * @param x its column's x
     * @param z its column's z
     */
    public record Found(int x, int z) {

        /** The square of its distance from the column {@code fromX}, {@code fromZ}. */
        public long distanceSquared(final int fromX, final int fromZ) {
            final long dx = (long) this.x - fromX;
            final long dz = (long) this.z - fromZ;
            return dx * dx + dz * dz;
        }
    }

    /* The bounds searched, from a start, and the place nearest it found so far. */
    private static final class Area {

        private final IProbe probe;
        private final int fromX;
        private final int fromZ;
        private final int minX;
        private final int maxX;
        private final int minZ;
        private final int maxZ;
        private Found best;
        private long bestDistance = Long.MAX_VALUE;

        private Area(final IProbe probe, final int fromX, final int fromZ, final int minX, final int maxX,
                     final int minZ, final int maxZ) {
            this.probe = probe;
            this.fromX = fromX;
            this.fromZ = fromZ;
            this.minX = minX;
            this.maxX = maxX;
            this.minZ = minZ;
            this.maxZ = maxZ;
        }

        /*
         * Reads the grid of {@code step} ring by ring out from the start, until past the bounds or past the place
         * found: a ring's places all lie at least its number of steps away.
         */
        private void rings(final int step) {
            final long reach = Math.ceilDiv(Math.max(Math.max((long) fromX - minX, (long) maxX - fromX),
                    Math.max((long) fromZ - minZ, (long) maxZ - fromZ)), step);
            for (long ring = 0; ring <= reach; ring++) {
                final long near = ring * step;
                if (best != null && near * near > bestDistance) {
                    return;
                }
                if (ring == 0) {
                    read(fromX, fromZ);
                    continue;
                }
                row(ring, -ring, step);
                row(ring, ring, step);
                column(ring, -ring, step);
                column(ring, ring, step);
            }
        }

        /* The ring's row {@code j} steps north or south of the start, as much of it as lies inside the bounds. */
        private void row(final long ring, final long j, final int step) {
            final long z = fromZ + j * step;
            if (z < minZ || z > maxZ) {
                return;
            }
            final long from = Math.max(-ring, Math.ceilDiv((long) minX - fromX, step));
            final long to = Math.min(ring, Math.floorDiv((long) maxX - fromX, step));
            for (long i = from; i <= to; i++) {
                read(fromX + i * step, z);
            }
        }

        /* The ring's column {@code i} steps west or east of the start, but its corners, which its rows read. */
        private void column(final long ring, final long i, final int step) {
            final long x = fromX + i * step;
            if (x < minX || x > maxX) {
                return;
            }
            final long from = Math.max(-ring + 1, Math.ceilDiv((long) minZ - fromZ, step));
            final long to = Math.min(ring - 1, Math.floorDiv((long) maxZ - fromZ, step));
            for (long j = from; j <= to; j++) {
                read(x, fromZ + j * step);
            }
        }

        /* Reads the square {@code around} blocks round {@code centre} every {@code step} blocks. */
        private void square(final Found centre, final int around, final int step) {
            for (long x = Math.max(minX, (long) centre.x() - around); x <= Math.min(maxX, (long) centre.x() + around);
                 x += step) {
                for (long z = Math.max(minZ, (long) centre.z() - around);
                     z <= Math.min(maxZ, (long) centre.z() + around); z += step) {
                    read(x, z);
                }
            }
        }

        /*
         * The place a fine step past {@code found}, away from the start, when it holds too: the nearest place found
         * lies at the edge of what holds, where a biome source reads two biomes that tie either way.
         */
        private Found inward(final Found found) {
            final double dx = (double) found.x() - fromX;
            final double dz = (double) found.z() - fromZ;
            final double length = Math.sqrt(dx * dx + dz * dz);
            if (length == 0) {
                return found;
            }
            final long x = Math.round(found.x() + dx / length * FINEST);
            final long z = Math.round(found.z() + dz / length * FINEST);
            if (x < minX || x > maxX || z < minZ || z > maxZ || !probe.holds((int) x, (int) z)) {
                return found;
            }
            return new Found((int) x, (int) z);
        }

        /* Asks the place, and keeps it when it holds and lies nearer than the best yet. */
        private void read(final long x, final long z) {
            final Found here = new Found((int) x, (int) z);
            final long distance = here.distanceSquared(fromX, fromZ);
            if (distance < bestDistance && probe.holds(here.x(), here.z())) {
                best = here;
                bestDistance = distance;
            }
        }
    }
}
