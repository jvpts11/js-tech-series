/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.universe;

/**
 * What is seen on the way up from a world to its space, from the build limit to the line where the world becomes its
 * space ({@link SpaceClimbs#ABOVE_THE_LIMIT}).
 *
 * <p>For the first half the eye is the climber's own: the loaded blocks below, and past them, out to the horizon, the
 * far map of the ground as the world's generator makes it ({@link FarRings}), on a ground that bends just enough for
 * its horizon to lie within the map's reach, under the game's clouds and a haze that matches the game's fog. From the
 * middle up the blocks give way to the far map's own picture of them, too far below by then to tell apart, and the eye
 * rises ever faster, the ground bending into a world, until near the line the real ground melts, under a veil of haze,
 * into the world's painted sphere, never seen through, and the line is crossed with the very picture its space shows.
 *
 * <p>How far up a climber is goes from 0 at the build limit to 1 at the line. Pure: no game types.
 */
public final class ClimbView {

    /** How far from the eye the climb's horizon lies at most, in blocks: within the far map's widest ring. */
    public static final double REACH = 0.9 * FarRings.reach();
    /** Where on the way up the loaded blocks give way to the far map's picture of them. */
    public static final double BLOCKS_GIVE_WAY = 0.5;

    private ClimbView() {
    }

    /** How far up the climb a height is, from 0 at the build limit {@code limit} to 1 at the line. */
    public static double share(final double height, final double limit) {
        return Math.max(0, Math.min(1, (height - limit) / SpaceClimbs.ABOVE_THE_LIMIT));
    }

    /**
     * How far up the climb one at height {@code y} in a system's space, over a world's low orbit, is drawn from: the
     * low orbits' own height is the line, and the floor under them is where one going down through it arrives in the
     * world, so on the way down to it the space shows the climb's own pictures, and the world takes over with the very
     * picture there.
     */
    public static double shareInSpace(final double y) {
        final double arrives = 1 - (double) SpaceClimbs.ARRIVE_UNDER / SpaceClimbs.ABOVE_THE_LIMIT;
        final double down = (SystemLayout.ORBIT_Y - y) / SpaceClimbs.UNDER_THE_FLOOR;
        return 1 - (1 - arrives) * Math.max(0, Math.min(1, down));
    }

    /**
     * The climb as seen from {@code height} over the place at {@code longitude} and {@code latitude}, in degrees, of
     * {@code body}'s world, {@code width} blocks round, its build limit at {@code limit} and its sea at {@code sea}.
     */
    public static Climb at(final SolarBody body, final double longitude, final double latitude, final double height,
                           final double limit, final double sea, final double width) {
        final double share = share(height, limit);
        final double[] centre = SystemLayout.centre(body);
        final double radius = SystemLayout.radius(body);
        final double[] spot = SystemLayout.orbitSpot(body, longitude, latitude);
        final double[] arrival = {spot[0], SystemLayout.ORBIT_Y, spot[1]};
        final double[] outward = minus(arrival, centre);
        final double away = length(outward);
        final double[] upAtArrival = scale(outward, 1 / away);
        final double high = (away - radius) / radius;
        // The eye's height over the sea, in blocks: the climber's own, then rising by a factor to where the ground,
        // bent so its horizon stays within reach, looks from as high as the arrival does from its world; going on at
        // the climber's own pace where the blocks give way and ever faster after.
        final double given = Math.max(1, limit + BLOCKS_GIVE_WAY * SpaceClimbs.ABOVE_THE_LIMIT - sea);
        final double rise = Math.log(Math.max(1, REACH * Math.sqrt(high / 2) / given));
        final double pace = Math.min(rise, (1 - BLOCKS_GIVE_WAY) * SpaceClimbs.ABOVE_THE_LIMIT / given);
        final double past = (share - BLOCKS_GIVE_WAY) / (1 - BLOCKS_GIVE_WAY);
        final double eye = share <= BLOCKS_GIVE_WAY ? Math.max(1, height - sea)
                : given * Math.exp(pace * past + (rise - pace) * past * past);
        final double bend = Math.min(width / (2 * Math.PI), REACH * REACH / (2 * eye));
        final double turn = smooth(BLOCKS_GIVE_WAY, 1, share);
        final double[] up = slerp(new double[] {0, 1, 0}, upAtArrival, turn);
        final double[] space = plus(centre, scale(up, radius * (1 + eye / bend)));
        return new Climb(share, eye, bend, up, space, share >= BLOCKS_GIVE_WAY, 1 - smooth(0.80, 0.94, share),
                bump(0.76, 0.98, share), smooth(0.02, 0.6, share), smooth(0.2, 0.85, share),
                smooth(0.5, 0.95, share), smooth(0.3, 0.85, share), smooth(0.6, 0.92, share),
                smooth(BLOCKS_GIVE_WAY, 0.78, share), smooth(0.8, 0.92, share));
    }

    /* A smooth step from 0 at {@code from} to 1 at {@code to}. */
    private static double smooth(final double from, final double to, final double value) {
        final double t = Math.max(0, Math.min(1, (value - from) / (to - from)));
        return t * t * (3 - 2 * t);
    }

    /* A rise and fall from 0 at {@code from} to 1 halfway and back to 0 at {@code to}. */
    private static double bump(final double from, final double to, final double value) {
        final double t = (value - from) / (to - from);
        return t <= 0 || t >= 1 ? 0 : Math.sin(Math.PI * t);
    }

    /* The direction {@code share} of the way round from {@code from} to {@code to}, both of length one. */
    private static double[] slerp(final double[] from, final double[] to, final double share) {
        final double cos = Math.max(-1, Math.min(1, from[0] * to[0] + from[1] * to[1] + from[2] * to[2]));
        final double angle = Math.acos(cos);
        if (angle < 1e-9) {
            return to.clone();
        }
        final double a = Math.sin((1 - share) * angle) / Math.sin(angle);
        final double b = Math.sin(share * angle) / Math.sin(angle);
        return new double[] {from[0] * a + to[0] * b, from[1] * a + to[1] * b, from[2] * a + to[2] * b};
    }

    private static double[] minus(final double[] a, final double[] b) {
        return new double[] {a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    private static double[] plus(final double[] a, final double[] b) {
        return new double[] {a[0] + b[0], a[1] + b[1], a[2] + b[2]};
    }

    private static double[] scale(final double[] a, final double by) {
        return new double[] {a[0] * by, a[1] * by, a[2] * by};
    }

    private static double length(final double[] a) {
        return Math.sqrt(a[0] * a[0] + a[1] * a[1] + a[2] * a[2]);
    }

    /**
     * The climb at one height.
     *
     * @param share       how far up it is, 0 at the build limit and 1 at the line
     * @param eyeHeight   how high the eye the ground is drawn from stands over the sea, in blocks
     * @param bend        the radius, in blocks, of the ground as it is drawn bending away
     * @param up          the ground's up under the eye, in the space's directions: straight up low, turning to the
     *                    arrival's own up near the line
     * @param space       where in the system's space the eye that sees the world's sphere stands
     * @param blocksGone  whether the loaded blocks have given way to the far map's picture of them
     * @param ground      how much of the real ground covers the painted sphere: all of it, then none past the veil
     * @param veil        how thick the veil of haze the real ground melts under is
     * @param skyCover    how much of the game's own sky the climb's sky covers
     * @param zenith      how far the sky overhead has gone dark
     * @param horizon     how far the sky at the horizon has gone dark
     * @param stars       how brightly the stars of space shine
     * @param sun         how much of the Sun of space is drawn
     * @param sphere      how much of the world's painted sphere and its air is drawn under the real ground
     * @param sunlit      how much the ground is lit by the Sun of space rather than as the game lights its blocks:
     *                    only under the veil, as the real ground melts into the sphere
     */
    public record Climb(double share, double eyeHeight, double bend, double[] up, double[] space, boolean blocksGone,
                        double ground, double veil, double skyCover, double zenith, double horizon, double stars,
                        double sun, double sphere, double sunlit) {

        public Climb {
            up = up.clone();
            space = space.clone();
        }
    }
}
