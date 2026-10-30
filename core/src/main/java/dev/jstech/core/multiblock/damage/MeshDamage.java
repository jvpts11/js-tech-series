/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock.damage;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.Nullable;

/**
 * Lays stamps of damage on a model's faces: round each strike the faces are cut finer, only within its reach, then
 * the faces of the hull there are pushed by the stamp's field and those within its hole's rim cut away. Only the hull
 * is struck: the faces lying no more than {@link #SKIN} under the outermost face right over them, looking down the way
 * the surface faced, its skins and not what stands behind them, so a hole in a cabin's wall never cuts the seats
 * inside. The faces given never change; what the strikes leave is made from them, the same every time for the same
 * strikes. Pure, so it is tested without the game.
 */
public final class MeshDamage {

    /** How deep under the outermost face right over it a face may lie and still be struck, in blocks. */
    public static final double SKIN = 0.22;
    /* The height map's step across, in blocks, and the most steps it takes to a side. */
    private static final double STEP = 0.02;
    private static final int MOST_STEPS = 512;
    /* How many times the faces round a strike are cut finer at most, and how fine, a share of its size. */
    private static final int ROUNDS = 6;
    private static final double FINEST = 1.0 / 7;
    /* The most triangles cutting finer makes, so a huge strike on a dense model stops short of filling the memory. */
    private static final int MOST_TRIANGLES = 400_000;
    /* How far round a strike the heights are mapped, and how deep a face lies past being struck, shares of its size. */
    private static final double MAPPED = 3;
    private static final double DEEPEST = 2;
    /* How far outside a triangle a point of the height map may lie and still be read as under it, a share. */
    private static final double SLACK = 0.02;
    /* How wide the fade at the edge of a strike's reach is, a share of its size. */
    private static final double FADE = 0.5;
    /* The petals: how far out they bend, how deep the shallowest is against the deepest, and how many to a turn. */
    private static final double PETALS_OUT_TO = 1.35;
    private static final double PETAL_SHALLOW = 0.55;
    private static final double PETAL_DEEP = 0.45;
    private static final double PETAL_WAVES = 3.5;
    /* How far in torn petals curl over the hole, and from how far out, shares of the size. */
    private static final double CURL = 0.18;
    private static final double CURL_FROM = 1.1;
    /* How wide a crater is, and the sag of a melted rim, shares of the size. */
    private static final double CRATER = 0.6;
    private static final double SAG = 0.5;
    /* A dent: how fast it fades, and its folds: how deep against the dent, how many and how they twist round. */
    private static final double DENT_FADE = 2.0;
    private static final double SHALLOW_DENT_FADE = 3.0;
    private static final double FOLD = 0.2;
    private static final double FOLD_FADE = 1.2;
    private static final double FOLD_WAVES = 8;
    private static final double FOLD_TWIST = 1.3;
    /* The most strikes a triangle tells it lies under, one to a bit. */
    private static final int MOST_TOLD = Long.SIZE;
    private static final int CORNERS = 3;
    private static final int AXES = 3;
    private static final int TRIANGLE = CORNERS * AXES;

    private MeshDamage() {
    }

    /**
     * The triangles {@code corners} as {@code strikes} leave them, laid one after another.
     *
     * @param corners {@code stride} numbers to a corner, three corners to a triangle; a corner's first three numbers
     *                are its place in the model's space, in blocks, and the rest (a point on a texture, say) are shared
     *                out in proportion as a face is cut finer
     * @param stride  how many numbers each corner has, three or more
     * @param tags    a number for each triangle (the block it belongs to, say), kept by the pieces it is cut into; or
     *                none, for all naught
     * @param strikes the stamps laid on them, in the order they struck
     * @return the triangles as the strikes left them, where each corner was before them, and each one's tag
     */
    public static Damaged apply(final float[] corners, final int stride, final int @Nullable [] tags,
                                final List<Strike> strikes) {
        Objects.requireNonNull(corners, "corners");
        if (stride < AXES || corners.length % (stride * CORNERS) != 0) {
            throw new IllegalArgumentException("the corners come " + stride + " numbers to a corner, three to a "
                    + "triangle, and a corner has its place first");
        }
        final int count = corners.length / (stride * CORNERS);
        if (tags != null && tags.length != count) {
            throw new IllegalArgumentException("a tag is needed for each of the " + count + " triangles");
        }
        Soup soup = Soup.of(corners, stride, tags);
        for (int i = 0; i < strikes.size(); i++) {
            soup = lay(soup, new StrikeFrame(strikes.get(i)), i);
        }
        return soup.damaged();
    }

    /**
     * Whether {@code strike} reaches the triangle whose corners start at {@code first} in {@code corners},
     * {@code stride} numbers to a corner, its place first: whether any of it, not only a corner, lies within the reach
     * its faces are cut finer in, so a wide face struck in its middle is reached.
     */
    public static boolean reaches(final Strike strike, final float[] corners, final int stride, final int first) {
        final double reach = strike.size() * Strike.FINE_REACH;
        return distanceSquared(strike.x(), strike.y(), strike.z(), corners[first], corners[first + 1],
                corners[first + 2], corners[first + stride], corners[first + stride + 1], corners[first + stride + 2],
                corners[first + 2 * stride], corners[first + 2 * stride + 1], corners[first + 2 * stride + 2])
                < reach * reach;
    }

    /* Lays the strike {@code frame}, the strike {@code index} of those laid, on {@code soup}. */
    private static Soup lay(final Soup given, final StrikeFrame frame, final int index) {
        final Strike strike = frame.strike;
        final DamageStamp stamp = strike.stamp();
        final double size = strike.size();
        final Soup soup = finer(given, frame, size * Strike.FINE_REACH, size * FINEST);
        final int corners = soup.count * CORNERS;
        final double[] a = new double[corners];
        final double[] b = new double[corners];
        final double[] along = new double[corners];
        final double[] dist = new double[corners];
        final double[] angle = new double[corners];
        for (int c = 0; c < corners; c++) {
            final double x = soup.rest[c * AXES];
            final double y = soup.rest[c * AXES + 1];
            final double z = soup.rest[c * AXES + 2];
            a[c] = frame.a(x, y, z);
            b[c] = frame.b(x, y, z);
            along[c] = frame.along(x, y, z);
            dist[c] = Math.hypot(a[c], b[c]);
            angle[c] = Math.atan2(b[c], a[c]);
        }
        final boolean[] hull = hull(soup, a, b, along, dist, size);
        final boolean[] gone = new boolean[soup.count];
        for (int t = 0; t < soup.count; t++) {
            final int c = t * CORNERS;
            final boolean onHull = hull[c] && hull[c + 1] && hull[c + 2];
            if (onHull && index < MOST_TOLD) {
                soup.struck[t] |= 1L << index;
            }
            if (onHull && stamp.cuts()) {
                final double middle = (dist[c] + dist[c + 1] + dist[c + 2]) / CORNERS;
                final double rim = (frame.rim(angle[c]) + frame.rim(angle[c + 1]) + frame.rim(angle[c + 2])) / CORNERS;
                gone[t] = middle < rim;
            }
        }
        for (int c = 0; c < corners; c++) {
            if (!hull[c]) {
                continue;
            }
            final double fade = clamp((Strike.REACH * size - dist[c]) / (FADE * size));
            final double push = field(frame, dist[c], angle[c]) * fade * fade;
            for (int axis = 0; axis < AXES; axis++) {
                soup.pos[c * AXES + axis] += push * frame.out[axis];
            }
            if (stamp.curl() && dist[c] > 0) {
                final double inward = clamp((CURL_FROM * size - dist[c]) / size) * CURL * size / dist[c];
                for (int axis = 0; axis < AXES; axis++) {
                    soup.pos[c * AXES + axis] -= inward * (a[c] * frame.across[axis] + b[c] * frame.up[axis]);
                }
            }
        }
        return soup.without(gone);
    }

    /* How far the field of {@code frame}'s stamp pushes a face {@code dist} out from its point at {@code angle}. */
    private static double field(final StrikeFrame frame, final double dist, final double angle) {
        final DamageStamp stamp = frame.strike.stamp();
        final double size = frame.strike.size();
        final double depth = stamp.depth() * size;
        final double q = dist / size;
        return switch (stamp.push()) {
            case PETALS_IN, PETALS_OUT -> {
                final double rim = frame.rim(angle);
                final double outer = PETALS_OUT_TO * size;
                final double band = clamp((outer - dist) / Math.max(outer - rim, 1.0e-6));
                final double petal = PETAL_SHALLOW + PETAL_DEEP * Math.abs(Math.cos(PETAL_WAVES * angle
                        + frame.petals));
                final double push = band * band * petal * depth;
                yield stamp.push() == DamageStamp.Push.PETALS_IN ? -push : push;
            }
            case CRATER -> dist < CRATER * size ? depth * Math.sin(dist / (CRATER * size) * Math.PI) : 0;
            case SAG -> -clamp(1 - (dist - frame.rim(angle)) / (SAG * size)) * depth;
            case FOLDS -> -depth * Math.exp(-DENT_FADE * q * q) + depth * FOLD
                    * Math.sin(FOLD_WAVES * q + FOLD_TWIST * Math.cos(2 * angle)) * Math.exp(-FOLD_FADE * q * q);
            case DENT -> -depth * Math.exp(-SHALLOW_DENT_FADE * q * q);
        };
    }

    /*
     * Which corners lie on the hull: no more than a skin under the outermost face right over them, looking down the
     * way the surface faced, mapped as the heights of the faces round the strike.
     */
    private static boolean[] hull(final Soup soup, final double[] a, final double[] b, final double[] along,
                                  final double[] dist, final double size) {
        final double mapped = MAPPED * size;
        final int side = Math.max(2, Math.min(MOST_STEPS, (int) Math.ceil(2 * mapped / STEP) + 1));
        final double step = 2 * mapped / (side - 1);
        final double[] height = new double[side * side];
        Arrays.fill(height, Double.NEGATIVE_INFINITY);
        final double[] gx = new double[CORNERS];
        final double[] gy = new double[CORNERS];
        for (int t = 0; t < soup.count; t++) {
            final int c = t * CORNERS;
            if (Math.min(dist[c], Math.min(dist[c + 1], dist[c + 2])) >= mapped) {
                continue;
            }
            for (int k = 0; k < CORNERS; k++) {
                gx[k] = (a[c + k] + mapped) / step;
                gy[k] = (b[c + k] + mapped) / step;
            }
            mapTriangle(height, side, gx, gy, along[c], along[c + 1], along[c + 2]);
        }
        final boolean[] out = new boolean[a.length];
        for (int c = 0; c < a.length; c++) {
            if (dist[c] >= mapped || along[c] <= -DEEPEST * size) {
                continue;
            }
            final int x = Math.clamp((int) Math.floor((a[c] + mapped) / step), 0, side - 2);
            final int y = Math.clamp((int) Math.floor((b[c] + mapped) / step), 0, side - 2);
            // A corner on a face's edge reads the highest of the four points round it, so it sees that face.
            final double over = Math.max(Math.max(height[x * side + y], height[(x + 1) * side + y]),
                    Math.max(height[x * side + y + 1], height[(x + 1) * side + y + 1]));
            out[c] = Math.max(over, along[c]) - along[c] < SKIN;
        }
        return out;
    }

    /* Raises {@code height} to the triangle's height over each point of the map it covers. */
    private static void mapTriangle(final double[] height, final int side, final double[] gx, final double[] gy,
                                    final double h0, final double h1, final double h2) {
        final int x0 = Math.max((int) Math.floor(Math.min(gx[0], Math.min(gx[1], gx[2]))), 0);
        final int x1 = Math.min((int) Math.ceil(Math.max(gx[0], Math.max(gx[1], gx[2]))), side - 1);
        final int y0 = Math.max((int) Math.floor(Math.min(gy[0], Math.min(gy[1], gy[2]))), 0);
        final int y1 = Math.min((int) Math.ceil(Math.max(gy[0], Math.max(gy[1], gy[2]))), side - 1);
        final double d = (gy[1] - gy[2]) * (gx[0] - gx[2]) + (gx[2] - gx[1]) * (gy[0] - gy[2]);
        if (x0 > x1 || y0 > y1 || Math.abs(d) < 1.0e-12) {
            return;
        }
        for (int i = x0; i <= x1; i++) {
            for (int j = y0; j <= y1; j++) {
                final double l0 = ((gy[1] - gy[2]) * (i - gx[2]) + (gx[2] - gx[1]) * (j - gy[2])) / d;
                final double l1 = ((gy[2] - gy[0]) * (i - gx[2]) + (gx[0] - gx[2]) * (j - gy[2])) / d;
                final double l2 = 1 - l0 - l1;
                if (l0 >= -SLACK && l1 >= -SLACK && l2 >= -SLACK) {
                    final int at = i * side + j;
                    height[at] = Math.max(height[at], l0 * h0 + l1 * h1 + l2 * h2);
                }
            }
        }
    }

    /*
     * {@code soup} with its faces within {@code reach} of the strike's point cut in four, again and again, until no
     * side of one is longer than {@code finest}; the rest left as they were.
     */
    private static Soup finer(final Soup given, final StrikeFrame frame, final double reach, final double finest) {
        Soup soup = given;
        for (int round = 0; round < ROUNDS; round++) {
            final boolean[] split = new boolean[soup.count];
            int splits = 0;
            for (int t = 0; t < soup.count; t++) {
                if (soup.near(t, frame, reach) && soup.longestSide(t) > finest) {
                    split[t] = true;
                    splits++;
                }
            }
            if (splits == 0 || soup.count + 3 * splits > MOST_TRIANGLES) {
                return soup;
            }
            soup = soup.split(split, splits);
        }
        return soup;
    }

    private static double clamp(final double value) {
        return Math.clamp(value, 0, 1);
    }

    /*
     * How far, squared, the point p lies from the nearest point of the triangle a, b, c: found by which of the
     * triangle's corners, sides or face the point lies over, as a book of collisions finds it.
     */
    private static double distanceSquared(final double px, final double py, final double pz, final double ax,
                                          final double ay, final double az, final double bx, final double by,
                                          final double bz, final double cx, final double cy, final double cz) {
        final double abx = bx - ax;
        final double aby = by - ay;
        final double abz = bz - az;
        final double acx = cx - ax;
        final double acy = cy - ay;
        final double acz = cz - az;
        final double d1 = abx * (px - ax) + aby * (py - ay) + abz * (pz - az);
        final double d2 = acx * (px - ax) + acy * (py - ay) + acz * (pz - az);
        if (d1 <= 0 && d2 <= 0) {
            return squared(px - ax, py - ay, pz - az);
        }
        final double d3 = abx * (px - bx) + aby * (py - by) + abz * (pz - bz);
        final double d4 = acx * (px - bx) + acy * (py - by) + acz * (pz - bz);
        if (d3 >= 0 && d4 <= d3) {
            return squared(px - bx, py - by, pz - bz);
        }
        final double vc = d1 * d4 - d3 * d2;
        if (vc <= 0 && d1 >= 0 && d3 <= 0) {
            final double v = d1 / (d1 - d3);
            return squared(px - ax - v * abx, py - ay - v * aby, pz - az - v * abz);
        }
        final double d5 = abx * (px - cx) + aby * (py - cy) + abz * (pz - cz);
        final double d6 = acx * (px - cx) + acy * (py - cy) + acz * (pz - cz);
        if (d6 >= 0 && d5 <= d6) {
            return squared(px - cx, py - cy, pz - cz);
        }
        final double vb = d5 * d2 - d1 * d6;
        if (vb <= 0 && d2 >= 0 && d6 <= 0) {
            final double w = d2 / (d2 - d6);
            return squared(px - ax - w * acx, py - ay - w * acy, pz - az - w * acz);
        }
        final double va = d3 * d6 - d5 * d4;
        if (va <= 0 && d4 - d3 >= 0 && d5 - d6 >= 0) {
            final double w = (d4 - d3) / (d4 - d3 + d5 - d6);
            return squared(px - bx - w * (cx - bx), py - by - w * (cy - by), pz - bz - w * (cz - bz));
        }
        final double sum = va + vb + vc;
        if (sum == 0) {
            return Math.min(squared(px - ax, py - ay, pz - az), Math.min(squared(px - bx, py - by, pz - bz),
                    squared(px - cx, py - cy, pz - cz)));
        }
        final double v = vb / sum;
        final double w = vc / sum;
        return squared(px - ax - v * abx - w * acx, py - ay - v * aby - w * acy, pz - az - v * abz - w * acz);
    }

    private static double squared(final double x, final double y, final double z) {
        return x * x + y * y + z * z;
    }

    /**
     * Triangles as strikes left them.
     *
     * @param corners {@code stride} numbers to a corner, three corners to a triangle, the place first
     * @param stride  how many numbers each corner has
     * @param rest    where each corner was before the strikes, three numbers to a corner
     * @param tags    each triangle's tag, as the triangle it was cut from had it
     * @param struck  for each triangle, a bit for each of the first 64 strikes whose hull it lies on, the first strike
     *                the lowest bit: the faces its edge's pixels are drawn over
     */
    public record Damaged(float[] corners, int stride, float[] rest, int[] tags, long[] struck) {

        public Damaged {
            corners = corners.clone();
            rest = rest.clone();
            tags = tags.clone();
            struck = struck.clone();
        }

        /** How many triangles there are. */
        public int triangles() {
            return this.tags.length;
        }
    }

    /* Triangles being worked: their places now and before, the rest of each corner's numbers, and their tags. */
    private static final class Soup {

        private final int stride;
        private final int extra;
        private final int count;
        private final double[] pos;
        private final double[] rest;
        private final float[] more;
        private final int[] tags;
        private final long[] struck;

        /* The four pieces of a triangle cut at the middles of its sides: 3 is the middle of 0-1, 4 of 1-2, 5 of 2-0. */
        private static final int[][] PIECES = {{0, 3, 5}, {3, 1, 4}, {5, 4, 2}, {3, 4, 5}};

        private Soup(final int stride, final int count) {
            this.stride = stride;
            this.extra = stride - AXES;
            this.count = count;
            this.pos = new double[count * TRIANGLE];
            this.rest = new double[count * TRIANGLE];
            this.more = new float[count * CORNERS * this.extra];
            this.tags = new int[count];
            this.struck = new long[count];
        }

        static Soup of(final float[] corners, final int stride, final int @Nullable [] tags) {
            final Soup soup = new Soup(stride, corners.length / (stride * CORNERS));
            for (int c = 0; c < soup.count * CORNERS; c++) {
                for (int axis = 0; axis < AXES; axis++) {
                    soup.pos[c * AXES + axis] = corners[c * stride + axis];
                    soup.rest[c * AXES + axis] = corners[c * stride + axis];
                }
                System.arraycopy(corners, c * stride + AXES, soup.more, c * soup.extra, soup.extra);
            }
            if (tags != null) {
                System.arraycopy(tags, 0, soup.tags, 0, tags.length);
            }
            return soup;
        }

        /* Whether any of triangle {@code t}, as it lay, came within {@code reach} of the strike's point. */
        boolean near(final int t, final StrikeFrame frame, final double reach) {
            final double[] r = this.rest;
            final int a = t * TRIANGLE;
            return distanceSquared(frame.px, frame.py, frame.pz, r[a], r[a + 1], r[a + 2], r[a + 3], r[a + 4],
                    r[a + 5], r[a + 6], r[a + 7], r[a + 8]) < reach * reach;
        }

        /* The longest side of triangle {@code t}, as it lay. */
        double longestSide(final int t) {
            double longest = 0;
            for (int k = 0; k < CORNERS; k++) {
                final int from = (t * CORNERS + k) * AXES;
                final int to = (t * CORNERS + (k + 1) % CORNERS) * AXES;
                final double dx = this.rest[to] - this.rest[from];
                final double dy = this.rest[to + 1] - this.rest[from + 1];
                final double dz = this.rest[to + 2] - this.rest[from + 2];
                longest = Math.max(longest, dx * dx + dy * dy + dz * dz);
            }
            return Math.sqrt(longest);
        }

        /* This, each triangle marked in {@code split} cut in four at the middles of its sides. */
        Soup split(final boolean[] split, final int splits) {
            final Soup out = new Soup(this.stride, this.count + 3 * splits);
            int to = 0;
            for (int t = 0; t < this.count; t++) {
                if (!split[t]) {
                    out.copy(to++, this, t);
                    continue;
                }
                // Corners 0, 1 and 2, then the middles of 0-1, 1-2 and 2-0, each piece three of them.
                for (final int[] piece : PIECES) {
                    for (int k = 0; k < CORNERS; k++) {
                        out.blend(to, k, this, t, piece[k]);
                    }
                    out.tags[to] = this.tags[t];
                    out.struck[to] = this.struck[t];
                    to++;
                }
            }
            return out;
        }

        /* This without the triangles marked in {@code gone}. */
        Soup without(final boolean[] gone) {
            int left = 0;
            for (final boolean g : gone) {
                left += g ? 0 : 1;
            }
            if (left == this.count) {
                return this;
            }
            final Soup out = new Soup(this.stride, left);
            int to = 0;
            for (int t = 0; t < this.count; t++) {
                if (!gone[t]) {
                    out.copy(to++, this, t);
                }
            }
            return out;
        }

        Damaged damaged() {
            final float[] corners = new float[this.count * CORNERS * this.stride];
            final float[] before = new float[this.count * TRIANGLE];
            for (int c = 0; c < this.count * CORNERS; c++) {
                for (int axis = 0; axis < AXES; axis++) {
                    corners[c * this.stride + axis] = (float) this.pos[c * AXES + axis];
                    before[c * AXES + axis] = (float) this.rest[c * AXES + axis];
                }
                System.arraycopy(this.more, c * this.extra, corners, c * this.stride + AXES, this.extra);
            }
            return new Damaged(corners, this.stride, before, this.tags, this.struck);
        }

        private void copy(final int to, final Soup from, final int t) {
            System.arraycopy(from.pos, t * TRIANGLE, this.pos, to * TRIANGLE, TRIANGLE);
            System.arraycopy(from.rest, t * TRIANGLE, this.rest, to * TRIANGLE, TRIANGLE);
            System.arraycopy(from.more, t * CORNERS * this.extra, this.more, to * CORNERS * this.extra,
                    CORNERS * this.extra);
            this.tags[to] = from.tags[t];
            this.struck[to] = from.struck[t];
        }

        /* Puts in corner {@code k} of triangle {@code to} the point {@code which} of triangle {@code t} of {@code from}. */
        private void blend(final int to, final int k, final Soup from, final int t, final int which) {
            final int first = which < CORNERS ? which : which - CORNERS;
            final int second = which < CORNERS ? which : (first + 1) % CORNERS;
            final int target = to * CORNERS + k;
            final int c1 = t * CORNERS + first;
            final int c2 = t * CORNERS + second;
            for (int axis = 0; axis < AXES; axis++) {
                this.pos[target * AXES + axis] = (from.pos[c1 * AXES + axis] + from.pos[c2 * AXES + axis]) / 2;
                this.rest[target * AXES + axis] = (from.rest[c1 * AXES + axis] + from.rest[c2 * AXES + axis]) / 2;
            }
            for (int e = 0; e < this.extra; e++) {
                this.more[target * this.extra + e] = (from.more[c1 * this.extra + e] + from.more[c2 * this.extra + e])
                        / 2;
            }
        }
    }
}
