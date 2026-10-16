/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.vein;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

/**
 * Where the veins of a world lie: the world cut into zones three chunks a side, each holding at most one vein, of a
 * kind picked by the kinds' frequencies, at a place, height and size of its own, worked out from the world's seed
 * alone. Any chunk finds every vein that reaches it without asking another, the same on any thread and every time for
 * the same seed. Other bodies in the ground lie the same way in zones of their own size and salt (reservoirs). Pure.
 */
public final class VeinField {

    private final List<Kind> kinds;
    private final int zone;
    private final long salt;
    private final int margin;
    private final int reach;

    /** A vein zone's side, in blocks: three chunks. */
    public static final int ZONE = 48;

    /* Two odd numbers of no pattern that spread neighbouring zones far apart, and the veins' own salt. */
    private static final long MIX_X = -7_046_029_254_386_353_131L;
    private static final long MIX_Z = -4_417_276_706_812_531_889L;
    private static final long SALT = 5_573_394_017_221_771_109L;
    private static final double LEAST_SIZE = 0.75;
    private static final double SIZE_SPREAD = 0.5;

    /** The veins of {@code kinds}, in zones three chunks a side. */
    public VeinField(final List<Kind> kinds) {
        this(kinds, ZONE, SALT);
    }

    /**
     * Bodies of {@code kinds} in zones {@code zone} blocks a side, {@code salt} keeping them apart from the veins and
     * from other bodies on the same seed; each middle keeps a quarter of a zone from its zone's edges, so neighbours
     * seldom touch.
     */
    public VeinField(final List<Kind> kinds, final int zone, final long salt) {
        if (zone < 4) {
            throw new IllegalArgumentException("a zone is at least four blocks across, not " + zone);
        }
        this.kinds = List.copyOf(kinds);
        this.zone = zone;
        this.salt = salt;
        this.margin = zone / 4;
        int most = 0;
        for (final Kind kind : this.kinds) {
            most = Math.max(most, (int) Math.ceil(kind.radius() * (LEAST_SIZE + SIZE_SPREAD)));
        }
        this.reach = most;
    }

    /** A zone's side, in blocks. */
    public int zone() {
        return this.zone;
    }

    /** The vein of the zone {@code zx, zz}, or none if it holds none. */
    public Vein in(final long seed, final int zx, final int zz) {
        if (this.kinds.isEmpty()) {
            return null;
        }
        final SplittableRandom random = new SplittableRandom(seed ^ this.salt ^ zx * MIX_X ^ zz * MIX_Z);
        double total = 0;
        for (final Kind kind : this.kinds) {
            total += kind.frequency();
        }
        final double scale = total > 1 ? 1 / total : 1;
        final double roll = random.nextDouble();
        double reached = 0;
        for (int i = 0; i < this.kinds.size(); i++) {
            final Kind kind = this.kinds.get(i);
            reached += kind.frequency() * scale;
            if (roll < reached) {
                final int x = zx * this.zone + this.margin + random.nextInt(this.zone - 2 * this.margin);
                final int z = zz * this.zone + this.margin + random.nextInt(this.zone - 2 * this.margin);
                final int y = kind.minY() + random.nextInt(Math.max(1, kind.maxY() - kind.minY() + 1));
                final double size = LEAST_SIZE + SIZE_SPREAD * random.nextDouble();
                return new Vein(i, x, y, z, kind.radius() * size, kind.thickness() * size, random.nextLong());
            }
        }
        return null;
    }

    /** Every vein whose reach touches the columns from {@code x0, z0} to {@code x1, z1}, for {@code seed}. */
    public List<Vein> reaching(final long seed, final int x0, final int z0, final int x1, final int z1) {
        final List<Vein> out = new ArrayList<>();
        final int lastX = Math.floorDiv(x1 + this.reach, this.zone);
        final int lastZ = Math.floorDiv(z1 + this.reach, this.zone);
        for (int zx = Math.floorDiv(x0 - this.reach, this.zone); zx <= lastX; zx++) {
            for (int zz = Math.floorDiv(z0 - this.reach, this.zone); zz <= lastZ; zz++) {
                final Vein vein = in(seed, zx, zz);
                if (vein != null && vein.x() + vein.radius() >= x0 && vein.x() - vein.radius() <= x1
                        && vein.z() + vein.radius() >= z0 && vein.z() - vein.radius() <= z1) {
                    out.add(vein);
                }
            }
        }
        return out;
    }

    /** The veins within {@code radius} blocks of {@code x, z} across, nearest first. */
    public List<Vein> near(final long seed, final int x, final int z, final int radius) {
        final List<Vein> found = new ArrayList<>(reaching(seed, x - radius, z - radius, x + radius, z + radius));
        found.removeIf(vein -> Math.hypot(vein.x() - x, vein.z() - z) > radius + vein.radius());
        found.sort((a, b) -> Double.compare(Math.hypot(a.x() - x, a.z() - z), Math.hypot(b.x() - x, b.z() - z)));
        return found;
    }

    /**
     * A kind of vein, as the field places it.
     *
     * @param frequency how many zones hold one, 0 to 1; when the kinds' together pass 1, each is a share of every zone
     * @param minY      the lowest its middle lies
     * @param maxY      the highest
     * @param radius    how far across it reaches from its middle, at its usual size
     * @param thickness how far up and down
     */
    public record Kind(double frequency, int minY, int maxY, double radius, double thickness) {
    }

    /**
     * A vein where it lies.
     *
     * @param kind      which of the field's kinds it is
     * @param x         its middle
     * @param y         and its height
     * @param z         and along
     * @param radius    how far across it reaches
     * @param thickness how far up and down
     * @param seed      what picks its own blocks
     */
    public record Vein(int kind, int x, int y, int z, double radius, double thickness, long seed) {

        /**
         * How much of the vein's density holds at {@code px, py, pz}: all of it at its middle, thinning to none at its
         * edge.
         */
        public double share(final double px, final double py, final double pz) {
            final double dx = (px - this.x) / this.radius;
            final double dy = (py - this.y) / this.thickness;
            final double dz = (pz - this.z) / this.radius;
            final double reach = dx * dx + dy * dy + dz * dz;
            return reach >= 1 ? 0 : 1 - reach;
        }

        /** How many blocks it turns to ore at {@code density}: its volume, by the share of it that holds. */
        public int blocks(final double density) {
            // The mean of 1 - r^2 over a ball is two fifths.
            return (int) Math.round(4.0 / 3.0 * Math.PI * this.radius * this.radius * this.thickness * density * 0.4);
        }
    }
}
