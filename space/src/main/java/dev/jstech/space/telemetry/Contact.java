/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.telemetry;

import dev.jstech.space.exploration.ReliefCoverage;
import java.util.Objects;

/**
 * Whether a ground system hears a satellite or a probe, as in reality. A geostationary satellite is always in sight of
 * an antenna under it that hears that far. One in a low or a polar orbit only talks while it passes over an antenna:
 * its data gathers aboard and comes down on the pass. The Moon takes a medium antenna, the other worlds a large one. A
 * relay satellite passes the signal on all the time, as NASA's TDRS have served the International Space Station since
 * 1983. A ground system that does not work hears nothing.
 *
 * <p>Pure: no game types.
 */
public final class Contact {

    /** How far north and south a polar orbit's ground track swings, in degrees; a low one's as the mapping passes'. */
    public static final double POLAR_SWING = 85;
    /** How far from the point under a passing satellite an antenna hears it, in degrees of its world. */
    public static final double FOOTPRINT = 20;
    /** How far from the point under a geostationary satellite it is in sight, in degrees of its world. */
    public static final double IN_SIGHT = 70;
    private static final double ROUND = 360;
    private static final double HALF_ROUND = 180;

    private Contact() {
    }

    /** Whether {@code system} hears {@code target} at the game's tick {@code tick}. */
    public static boolean heard(final GroundSystem system, final Target target, final long tick) {
        if (!system.works() || system.reach().isEmpty()) {
            return false;
        }
        if (target.relayed()) {
            return true;
        }
        return switch (target.where()) {
            case DEEP_SPACE -> system.reach().get().covers(AntennaSize.Reach.DEEP);
            case MOON -> system.reach().get().covers(AntennaSize.Reach.FAR);
            case GEOSTATIONARY -> anyWithin(system, AntennaSize.Reach.FAR, target.longitude(), 0, IN_SIGHT);
            case LOW, POLAR -> {
                final double[] under = underIt(target, tick);
                yield anyWithin(system, AntennaSize.Reach.NEAR, under[0], under[1], FOOTPRINT);
            }
        };
    }

    /**
     * The point of its world under {@code target}, in a low or polar orbit, at the game's tick {@code tick}: longitude
     * and latitude, in degrees. With each pass the world has turned a little under the orbit.
     */
    public static double[] underIt(final Target target, final long tick) {
        final long pass = Math.floorDiv(tick, ReliefCoverage.PASS_TICKS);
        final double along = ROUND * Math.floorMod(tick, ReliefCoverage.PASS_TICKS) / ReliefCoverage.PASS_TICKS;
        final double swing = target.where() == Where.POLAR ? POLAR_SWING : ReliefCoverage.TRACK;
        final double longitude = wrap(target.longitude() + pass * ReliefCoverage.PASS_SHIFT + along);
        return new double[] {longitude, swing * Math.sin(Math.toRadians(along))};
    }

    /** How far apart two points of a world are, in degrees of a great circle. */
    public static double apart(final double longitude, final double latitude, final double otherLongitude,
                               final double otherLatitude) {
        final double a = Math.toRadians(latitude);
        final double b = Math.toRadians(otherLatitude);
        final double turn = Math.toRadians(otherLongitude - longitude);
        final double cos = Math.sin(a) * Math.sin(b) + Math.cos(a) * Math.cos(b) * Math.cos(turn);
        return Math.toDegrees(Math.acos(Math.clamp(cos, -1, 1)));
    }

    /* Whether an antenna of {@code system} hearing as far as {@code wanted} stands within {@code within} of a point. */
    private static boolean anyWithin(final GroundSystem system, final AntennaSize.Reach wanted,
                                     final double longitude, final double latitude, final double within) {
        for (final GroundSystem.Device antenna : system.antennas()) {
            if (antenna.size().isPresent() && antenna.size().get().reach().covers(wanted)
                    && apart(antenna.longitude(), antenna.latitude(), longitude, latitude) <= within) {
                return true;
            }
        }
        return false;
    }

    private static double wrap(final double degrees) {
        final double turned = (degrees + HALF_ROUND) % ROUND;
        return (turned < 0 ? turned + ROUND : turned) - HALF_ROUND;
    }

    /** Where a satellite or a probe is, seen from a ground system's world. */
    public enum Where {
        LOW,
        POLAR,
        GEOSTATIONARY,
        MOON,
        DEEP_SPACE
    }

    /**
     * A satellite or a probe a ground system may hear.
     *
     * @param where     where it is
     * @param longitude for a geostationary one, the longitude it hangs over; for one in a low or a polar orbit, where
     *                  its ground track crossed the equator going north on its first pass; in degrees east
     * @param relayed   whether a relay satellite passes its signal on
     */
    public record Target(Where where, double longitude, boolean relayed) {

        public Target {
            Objects.requireNonNull(where, "where");
        }
    }
}
