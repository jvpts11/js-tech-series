/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.ship;

/**
 * How a ship's autopilot flies it, a tick at a time, by what it holds at the helm as a pilot would: LAND turns it to a
 * landing spot, flies there high enough and slowing as it nears, comes over it hovering and sets it down; ORBIT opens
 * its throttle, pitches its nose up to the climb's angle and boosts where it may, up and over the line to space; DOCK,
 * in space, drifts it on its thrusters till its collar stands off the other ship's, in line with it, then closes
 * slowly till they touch, its flight turning it meanwhile to face the other's collar. Pure, as the flight is.
 */
public final class ShipAutopilots {

    /** The pitch ORBIT climbs at, in degrees, as the approved climb shows it. */
    public static final double CLIMB_PITCH = 55;
    /* How near the spot LAND slows to come over it, in metres; how high over it it flies till then. */
    private static final double NEAR = 40;
    private static final double CRUISE_HEIGHT = 30;
    /*
     * How high over the spot it hovers coming over it, in metres; how near over it it comes down, and how near it
     * stops creeping on.
     */
    private static final double HOVER_HEIGHT = 6;
    private static final double DESCENDS = 2.0;
    private static final double OVER = 1.0;
    /* How fast it closes on the spot near it, metres a second for each metre off, at least, and at most, hovering. */
    private static final double APPROACH = 0.25;
    private static final double LEAST_CREEP = 0.5;
    private static final double CREEP = 8;
    /* How far off its heading it turns at a whole hold, in degrees, and the least hold that still turns it. */
    private static final double TURN_WHOLE = 15;
    private static final double TURN_LEAST = 0.15;
    /* How far off a pitch it pitches back, in degrees. */
    private static final double PITCH_OFF = 1.5;
    /* The air's cruise, which the throttle's share is of. */
    private static final double CRUISE = ShipFlightModel.CRUISE_AIR;
    /*
     * DOCK: the gap off the other's collar it comes to first, in metres; the fastest it drifts, and closes the last
     * of the gap, in metres a second; how fast its thrusters change its drift, in metres a second squared; how near
     * in line, and in attitude, in degrees, it counts as aligned; how near it counts as touching; how fast it drifts
     * for each metre off.
     */
    private static final double GAP = 3;
    private static final double DRIFT = 2;
    private static final double CLOSING = 0.4;
    private static final double THRUSTERS = 0.5;
    private static final double IN_LINE = 0.3;
    private static final double ATTITUDE_IN_LINE = 2;
    private static final double TOUCHING = 0.15;
    private static final double DRIFT_GAIN = 0.3;

    private ShipAutopilots() {
    }

    /**
     * What LAND holds, the ship's flight at {@code state}, its nose at {@code heading} and {@code pitch}, in degrees,
     * the spot {@code dx}, {@code dy}, {@code dz} metres from the middle of its hull's floor.
     */
    public static ShipFlightModel.Held land(final ShipFlightModel.State state, final double heading,
                                            final double pitch, final double dx, final double dy, final double dz) {
        final double off = Math.hypot(dx, dz);
        final double height = -dy;
        final double toward = Math.toDegrees(Math.atan2(dx, -dz));
        final double[] turn = turn(heading, toward, off > OVER);
        final boolean slow = state.speed() < CREEP + 2;
        if (off > NEAR) {
            // On its way: as fast as the way left allows, high enough over the spot, its nose level otherwise.
            final double[] throttle = throttle(state, Math.min(CRUISE, off * APPROACH * 2) / CRUISE);
            final double up = height < CRUISE_HEIGHT ? 1 : pitch > PITCH_OFF ? 0 : pitch < -PITCH_OFF ? 1 : 0;
            final double down = height > 2 * CRUISE_HEIGHT && pitch > -10 ? 1 : pitch > PITCH_OFF ? 1 : 0;
            return held(throttle, turn, up, up > 0 ? 0 : down, false);
        }
        // Near: slowing as it nears, creeping on only as its nose points at the spot, coming down once over it.
        final double facing = Math.max(0, Math.cos(Math.toRadians(Math.IEEEremainder(toward - heading, 360))));
        final double creep = off > OVER ? Math.min(CREEP, Math.max(LEAST_CREEP, off * APPROACH)) * facing : 0;
        final double[] throttle = throttle(state, creep / CRUISE);
        if (!slow) {
            return held(throttle, turn, pitch < -PITCH_OFF ? 1 : 0, pitch > PITCH_OFF ? 1 : 0, false);
        }
        final double up = off > DESCENDS && height < HOVER_HEIGHT ? 1 : 0;
        final double down = off <= DESCENDS ? 1 : 0;
        return held(throttle, turn, up, down, false);
    }

    /** What ORBIT holds, the ship's flight at {@code state}, its nose at {@code pitch} degrees: up and away. */
    public static ShipFlightModel.Held orbit(final ShipFlightModel.State state, final double pitch) {
        final double up = pitch < CLIMB_PITCH - PITCH_OFF ? 1 : 0;
        final double down = pitch > CLIMB_PITCH + PITCH_OFF ? 1 : 0;
        return new ShipFlightModel.Held(state.throttle() < 1 ? 1 : 0, 0, 0, 0, up, down, true, true);
    }

    /**
     * What DOCK does: the other ship's collar's mouth {@code offset} metres from its own, along the world, the other's
     * collar facing {@code axis}, a unit vector, its own turned {@code attitudeOff} degrees off facing it, drifting at
     * {@code drift} metres a second; its drift next, and whether it is aligned and touches.
     */
    public static Docking dock(final double[] offset, final double[] axis, final double attitudeOff,
                               final double[] drift) {
        // Its collar comes to stand off the other's, a gap out along the way it faces; in line there, it closes.
        final double along = offset[0] * axis[0] + offset[1] * axis[1] + offset[2] * axis[2];
        final double[] lateral = {offset[0] - along * axis[0], offset[1] - along * axis[1],
                offset[2] - along * axis[2]};
        final double across = Math.sqrt(lateral[0] * lateral[0] + lateral[1] * lateral[1]
                + lateral[2] * lateral[2]);
        final double distance = Math.sqrt(offset[0] * offset[0] + offset[1] * offset[1] + offset[2] * offset[2]);
        final boolean touching = distance < TOUCHING;
        final boolean inLine = across < IN_LINE && attitudeOff < ATTITUDE_IN_LINE && along <= IN_LINE;
        final double sideways = across * DRIFT_GAIN > DRIFT ? DRIFT / across : DRIFT_GAIN;
        final double closing = cap((along - (inLine ? 0 : -GAP)) * DRIFT_GAIN, inLine ? CLOSING : DRIFT);
        final double[] next = new double[3];
        for (int i = 0; i < 3; i++) {
            final double wanted = touching ? 0 : lateral[i] * sideways + axis[i] * closing;
            next[i] = toward(drift[i], wanted, THRUSTERS * ShipFlightModel.TICK);
        }
        return new Docking(next, inLine, touching);
    }

    /* The left and right held to turn from {@code heading} to {@code wanted}, in degrees; none when not to turn. */
    private static double[] turn(final double heading, final double wanted, final boolean turning) {
        final double off = Math.IEEEremainder(wanted - heading, 360);
        if (!turning || Math.abs(off) < 0.5) {
            return new double[] {0, 0};
        }
        final double hold = Math.max(TURN_LEAST, Math.min(1, Math.abs(off) / TURN_WHOLE));
        // A heading grows turning right, east of north.
        return off > 0 ? new double[] {0, hold} : new double[] {hold, 0};
    }

    /* The forward and back held to bring the throttle to {@code share}: as much of a key as takes it there. */
    private static double[] throttle(final ShipFlightModel.State state, final double share) {
        final double off = share - state.throttle();
        return off >= 0 ? new double[] {Math.min(1, off / ShipFlightModel.OPENS), 0}
                : new double[] {0, Math.min(1, -off / ShipFlightModel.CLOSES)};
    }

    private static ShipFlightModel.Held held(final double[] throttle, final double[] turn, final double up,
                                             final double down, final boolean boost) {
        return new ShipFlightModel.Held(throttle[0], throttle[1], turn[0], turn[1], up, down, boost, true);
    }

    private static double cap(final double value, final double most) {
        return Math.max(-most, Math.min(most, value));
    }

    private static double toward(final double from, final double to, final double by) {
        return from < to ? Math.min(to, from + by) : Math.max(to, from - by);
    }

    /**
     * A tick of docking.
     *
     * @param drift    its drift on its thrusters next, in metres a second, across, up and along the world
     * @param aligned  whether its collar is in line with the other's, facing it, on the side it faces
     * @param touching whether its collar touches the other's
     */
    public record Docking(double[] drift, boolean aligned, boolean touching) {
    }
}
