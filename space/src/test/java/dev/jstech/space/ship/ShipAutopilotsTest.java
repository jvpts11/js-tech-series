/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.ship;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ShipAutopilotsTest {

    /* A ship of 40 tonnes on feet: 420 kN of air engines, 420 kN of bells, delta wings holding 400 kN. */
    private static final ShipFlightModel.Performance SHIP = new ShipFlightModel.Performance(40_000, 420_000,
            420_000, 400_000, 4.0e6, ShipFlightModel.Gear.LEGS);
    /* The same ship on wheels. */
    private static final ShipFlightModel.Performance WHEELED = new ShipFlightModel.Performance(40_000, 420_000,
            420_000, 400_000, 4.0e6, ShipFlightModel.Gear.WHEELS);
    /* The land under a runway, and how high over it a ship flies when its autopilot is engaged. */
    private static final double GROUND = 64;
    private static final double ALOFT = 40;
    /* How high over its beacon a ship landing on a runway passes, at the least, and how far off its line it stops. */
    private static final double OVER_THE_BEACON = 1.2;
    private static final double ON_THE_LINE = 4;
    /* How far off level its nose is, at the most, as it settles onto a runway, in degrees. */
    private static final double LEVEL = 2;
    private static final ShipFlightModel.State CRUISING = new ShipFlightModel.State(30, 0, 0.75, 1, 0, 0);
    private static final double[] UP = {0, 1, 0};
    private static final double[] STILL = {0, 0, 0};
    private static final int TICKS_A_SECOND = 20;

    @Test
    void orbit_opensTheThrottleAndBoosts() {
        final ShipFlightModel.Held held = ShipAutopilots.orbit(CRUISING, ShipAutopilots.CLIMB_PITCH);
        assertEquals(1, held.forward());
        assertTrue(held.boost());
        assertTrue(held.piloted());
    }

    @Test
    void orbit_pitchesUpToTheClimbAndHoldsIt() {
        assertEquals(1, ShipAutopilots.orbit(CRUISING, 0).up());
        final ShipFlightModel.Held there = ShipAutopilots.orbit(CRUISING, ShipAutopilots.CLIMB_PITCH);
        assertEquals(0, there.up());
        assertEquals(0, there.down());
        assertEquals(1, ShipAutopilots.orbit(CRUISING, ShipAutopilots.CLIMB_PITCH + 10).down());
    }

    @Test
    void orbit_flownClimbsAtTheClimbsAngle() {
        final Flight flight = new Flight(CRUISING, 0, 100, 0, 0, 0, false);
        for (int tick = 0; tick < 30 * TICKS_A_SECOND; tick++) {
            flight.fly(ShipAutopilots.orbit(flight.state, flight.pitch), 0);
        }
        assertEquals(ShipAutopilots.CLIMB_PITCH, flight.pitch, 3, "climbs at its angle");
        assertTrue(flight.y > 1000, "and has climbed: " + flight.y);
    }

    @Test
    void land_turnsTowardTheSpot() {
        final ShipFlightModel.Held east = ShipAutopilots.land(CRUISING, 0, 0, 200, 0, 0);
        assertTrue(east.right() > 0 && east.left() == 0, "a spot to the east, its nose north, turns right");
        final ShipFlightModel.Held west = ShipAutopilots.land(CRUISING, 0, 0, -200, 0, 0);
        assertTrue(west.left() > 0 && west.right() == 0, "a spot to the west turns left");
        final ShipFlightModel.Held ahead = ShipAutopilots.land(CRUISING, 0, 0, 0, 0, -200);
        assertEquals(0, ahead.left() + ahead.right(), "a spot ahead turns it not at all");
    }

    @Test
    void land_climbsOverTheSpotOnItsWayAndSlowsNearIt() {
        final ShipFlightModel.Held low = ShipAutopilots.land(CRUISING, 0, 0, 0, 0, -300);
        assertEquals(1, low.up(), "level with the spot, far off, it climbs to fly over");
        final ShipFlightModel.Held near = ShipAutopilots.land(CRUISING, 0, 0, 0, -20, -20);
        assertEquals(1, near.back(), "near the spot its throttle closes");
    }

    @Test
    void land_comesDownWhenOverTheSpot() {
        final ShipFlightModel.State hovering = new ShipFlightModel.State(0.5, 0, 0, 1, 0, 90);
        final ShipFlightModel.Held over = ShipAutopilots.land(hovering, 0, 0, 0.3, -6, 0.2);
        assertEquals(1, over.down());
        assertEquals(0, over.up());
    }

    @Test
    void land_flownSetsDownOnTheSpot() {
        final Flight flight = new Flight(ShipFlightModel.State.RESTING, 0, 64, 0, 0, 0, true);
        final double spotX = 220;
        final double spotZ = -160;
        for (int tick = 0; tick < 240 * TICKS_A_SECOND && !(flight.landed && tick > TICKS_A_SECOND); tick++) {
            flight.fly(ShipAutopilots.land(flight.state, flight.heading, flight.pitch, spotX - flight.x, 64 - flight.y,
                    spotZ - flight.z), 64);
        }
        assertTrue(flight.landed, "set down: " + flight);
        assertEquals(0, Math.hypot(spotX - flight.x, spotZ - flight.z), 2.0, "on the spot: " + flight);
    }

    @Test
    void land_flownFromHoveringHighJustOffTheSpotSetsDownOnIt() {
        // Hovering still, high over the land, a couple of metres short of the spot: it creeps over and comes down.
        final Flight flight = new Flight(new ShipFlightModel.State(0, 0, 0, 1, 0, 66), 0, 119, 0, 0, 0, false);
        final double spotZ = -2.2;
        for (int tick = 0; tick < 120 * TICKS_A_SECOND && !flight.landed; tick++) {
            flight.fly(ShipAutopilots.land(flight.state, flight.heading, flight.pitch, -flight.x, 64 - flight.y,
                    spotZ - flight.z), 64);
        }
        assertTrue(flight.landed, "set down: " + flight);
        assertEquals(0, Math.hypot(flight.x, spotZ - flight.z), 2.0, "on the spot: " + flight);
    }

    @Test
    void land_holdsItsThrottleAtTheShareItWantsToTheHundredth() {
        // Its throttle shut, a spot four metres ahead: it opens just to the creep it wants, not past it.
        ShipFlightModel.State state = new ShipFlightModel.State(0, 0, 0, 1, 0, 66);
        for (int tick = 0; tick < TICKS_A_SECOND; tick++) {
            final ShipFlightModel.Held held = ShipAutopilots.land(state, 0, 0, 0, -20, -4);
            state = new ShipFlightModel.State(0, 0, Math.clamp(state.throttle() + held.forward()
                    * ShipFlightModel.OPENS - held.back() * ShipFlightModel.CLOSES, 0.0, 1.0), 1, 0, 66);
        }
        assertEquals(1.0 / ShipFlightModel.CRUISE_AIR, state.throttle(), 0.002);
    }

    @Test
    void runway_flownFromBehindTouchesDownPastTheBeaconAndStopsOnTheStretch() {
        final Landing landing = runwayLanding(new Flight(CRUISING, 0, GROUND + ALOFT, 400, 0, 0, false, WHEELED), 0);
        landing.assertLanded();
    }

    @Test
    void runway_flownFromBeyondItsEndComesRoundToTheGate() {
        final Landing landing = runwayLanding(new Flight(CRUISING, 30, GROUND + ALOFT, -300, 180, 0, false, WHEELED),
                0);
        landing.assertLanded();
    }

    @Test
    void runway_flownFromTheSideLinesUpOnItsMiddle() {
        final Landing landing = runwayLanding(new Flight(CRUISING, 300, GROUND + ALOFT, -50, 270, 0, false, WHEELED),
                0);
        landing.assertLanded();
    }

    @Test
    void runway_flownOntoARunwayRunningAnyWayLandsAlongIt() {
        // A runway running south-east, the ship coming from the north-east.
        final Landing landing = runwayLanding(new Flight(CRUISING, 250, GROUND + ALOFT, -250, 200, 0, false,
                WHEELED), 135);
        landing.assertLanded();
    }

    @Test
    void runway_flownFromStandingOffItsStretchLiftsOffAndComesRound() {
        final Landing landing = runwayLanding(new Flight(ShipFlightModel.State.RESTING, -120, GROUND, 60, 90, 0, true,
                WHEELED), 0);
        landing.assertLanded();
    }

    @Test
    void runway_rollingOutShutsItsThrottleAndKeepsItsNoseAlong() {
        final ShipFlightModel.State rolling = new ShipFlightModel.State(8, 0, 0.4, 1, 0, 0);
        // On the runway 30 metres past its beacon, slow, its nose a little off the runway's way and down.
        final ShipFlightModel.Held held = ShipAutopilots.runway(rolling, 5, -3, 0, 0, 30, 0, true);
        assertTrue(held.back() > 0 && held.forward() == 0, "its throttle shuts");
        assertTrue(held.left() > 0, "its nose turns back along the runway");
        assertEquals(0, held.up() + held.down(), "never up nor down, which slow would lift it off");
    }

    @Test
    void runway_rollingOutSlowWithItsNoseDownStaysOnTheLand() {
        // Touched down nose down, as the glide leaves it, and slowed below the speed it hovers at, its nose still down
        // as the land holds it in the game.
        final Flight flight = new Flight(new ShipFlightModel.State(9.7, 0, 0.2, 1, 0, 0), 0, GROUND, -40, 0, -3, true,
                WHEELED);
        for (int tick = 0; tick < 10 * TICKS_A_SECOND; tick++) {
            flight.fly(ShipAutopilots.runway(flight.state, flight.heading, flight.pitch, -flight.x,
                    GROUND - flight.y, -flight.z, 0, flight.landed), GROUND);
            assertTrue(flight.landed, "it stays on the land as it slows: " + flight);
        }
        assertTrue(flight.state.speed() < 0.5, "and stops: " + flight);
    }

    @Test
    void runway_onTheLandOffItsStretchLiftsOff() {
        final ShipFlightModel.Held held = ShipAutopilots.runway(ShipFlightModel.State.RESTING, 0, 0, 200, 0, 200, 0,
                true);
        assertEquals(1, held.up());
        assertEquals(0, held.down());
    }

    @Test
    void onRunway_holdsTheStretchFromJustBehindItsBeaconToItsEnd() {
        // A runway running north from its beacon: the ship's beacon south of it is the runway behind it.
        assertTrue(ShipAutopilots.onRunway(0, 30, 0), "30 metres along");
        assertTrue(ShipAutopilots.onRunway(3, 30, 0), "a little off its line");
        assertFalse(ShipAutopilots.onRunway(10, 30, 0), "well off its line");
        assertFalse(ShipAutopilots.onRunway(0, -20, 0), "short of the beacon");
        assertFalse(ShipAutopilots.onRunway(0, ShipAutopilots.RUNWAY_LENGTH + 5, 0), "past its end");
        assertTrue(ShipAutopilots.onRunway(-30, 0, 90), "a runway running east");
    }

    @Test
    void dock_closesOnTheOtherCollarAndTouches() {
        double[] offset = {6, -9, 4};
        double[] drift = STILL;
        boolean aligned = false;
        boolean touching = false;
        for (int tick = 0; tick < 180 * TICKS_A_SECOND && !touching; tick++) {
            final ShipAutopilots.Docking docking = ShipAutopilots.dock(offset, UP, 0, drift);
            aligned |= docking.aligned();
            touching = docking.touching();
            drift = docking.drift();
            offset = new double[] {offset[0] - drift[0] * ShipFlightModel.TICK,
                    offset[1] - drift[1] * ShipFlightModel.TICK, offset[2] - drift[2] * ShipFlightModel.TICK};
        }
        assertTrue(aligned, "in line before it closed");
        assertTrue(touching, "and touches");
    }

    @Test
    void dock_standsOffTheGapTillItsAttitudeIsRight() {
        double[] offset = {0, -9, 0};
        double[] drift = STILL;
        for (int tick = 0; tick < 60 * TICKS_A_SECOND; tick++) {
            final ShipAutopilots.Docking docking = ShipAutopilots.dock(offset, UP, 30, drift);
            assertFalse(docking.aligned());
            assertFalse(docking.touching());
            drift = docking.drift();
            offset = new double[] {offset[0], offset[1] - drift[1] * ShipFlightModel.TICK, offset[2]};
        }
        assertEquals(-3, offset[1], 0.2, "a gap off, turned wrong");
    }

    @Test
    void dock_driftChangesNoFasterThanItsThrusters() {
        final double[] drift = {0.4, -0.2, 0};
        final ShipAutopilots.Docking docking = ShipAutopilots.dock(new double[] {50, -50, 50}, UP, 0, drift);
        for (int i = 0; i < 3; i++) {
            assertTrue(Math.abs(docking.drift()[i] - drift[i]) <= 0.5 * ShipFlightModel.TICK + 1e-12,
                    "part " + i + ": " + docking.drift()[i]);
        }
    }

    /*
     * {@code flight} flown onto a runway whose beacon stands at the middle of the land, running {@code runway} degrees
     * east of north, till it stands still on the runway's stretch, or four minutes pass.
     */
    private static Landing runwayLanding(final Flight flight, final double runway) {
        final double r = Math.toRadians(runway);
        Double touched = null;
        double touchPitch = Double.NaN;
        double lowestOver = Double.POSITIVE_INFINITY;
        boolean stopped = false;
        for (int tick = 0; tick < 240 * TICKS_A_SECOND && !stopped; tick++) {
            final boolean wasLanded = flight.landed;
            flight.fly(ShipAutopilots.runway(flight.state, flight.heading, flight.pitch, -flight.x, GROUND - flight.y,
                    -flight.z, runway, flight.landed), GROUND);
            final double along = flight.x * Math.sin(r) - flight.z * Math.cos(r);
            if (Math.abs(along) < 1.5 && Math.abs(flight.x * Math.cos(r) + flight.z * Math.sin(r)) < ON_THE_LINE) {
                lowestOver = Math.min(lowestOver, flight.y - GROUND);
            }
            if (flight.landed && !wasLanded && tick > 0) {
                touched = along;
                touchPitch = flight.pitch;
            }
            stopped = flight.landed && flight.state.speed() < 0.5 && ShipAutopilots.onRunway(-flight.x, -flight.z,
                    runway) && tick > TICKS_A_SECOND;
        }
        return new Landing(flight, runway, touched, touchPitch, lowestOver, stopped);
    }

    /*
     * How a ship landed on a runway: the flight as it ended, the runway's way, where along it it last touched down and
     * its pitch then, how low it passed over its beacon, and whether it came to a stop on its stretch.
     */
    private record Landing(Flight flight, double runway, Double touched, double touchPitch, double lowestOver,
                           boolean stopped) {

        void assertLanded() {
            final double r = Math.toRadians(this.runway);
            final double along = flight.x * Math.sin(r) - flight.z * Math.cos(r);
            final double across = flight.x * Math.cos(r) + flight.z * Math.sin(r);
            assertTrue(this.stopped, "stopped on the runway's stretch: " + flight + ", along " + along + ", across "
                    + across);
            assertTrue(this.touched != null && this.touched >= 0, "touched down past its beacon: " + this.touched);
            assertTrue(Math.abs(this.touchPitch) <= LEVEL, "flared, settling onto the runway level: "
                    + this.touchPitch);
            assertTrue(this.lowestOver >= OVER_THE_BEACON, "passed well over its beacon: " + this.lowestOver);
            assertTrue(Math.abs(across) < ON_THE_LINE, "on the runway's line: " + across);
            final double off = Math.abs(Math.IEEEremainder(flight.heading - this.runway, 360));
            assertTrue(off < 10, "its nose along the runway: " + flight.heading);
        }
    }

    /* A ship flown a tick at a time over flat land at a height, as its structure would fly it, its nose turned. */
    private static final class Flight {

        private final ShipFlightModel.Performance ship;
        private ShipFlightModel.State state;
        private double x;
        private double y;
        private double z;
        private double heading;
        private double pitch;
        private boolean landed;

        Flight(final ShipFlightModel.State state, final double x, final double y, final double z,
               final double heading, final double pitch, final boolean landed) {
            this(state, x, y, z, heading, pitch, landed, SHIP);
        }

        Flight(final ShipFlightModel.State state, final double x, final double y, final double z,
               final double heading, final double pitch, final boolean landed,
               final ShipFlightModel.Performance ship) {
            this.ship = ship;
            this.state = state;
            this.x = x;
            this.y = y;
            this.z = z;
            this.heading = heading;
            this.pitch = pitch;
            this.landed = landed;
        }

        void fly(final ShipFlightModel.Held held, final double ground) {
            final ShipFlightModel.Air air = new ShipFlightModel.Air(1.0, 1.0, this.y - ground >= 24, this.landed,
                    this.y - ground);
            final ShipFlightModel.Tick tick = ShipFlightModel.step(this.state, held, air, this.ship, this.pitch);
            // A turn to the left above nought: its heading, east of north, falls.
            this.heading -= tick.turn();
            // On the land its nose stays as the land holds it: pitching would put its tail or nose into the land.
            if (!this.landed) {
                this.pitch += tick.pitch();
            }
            final double h = Math.toRadians(this.heading);
            final double p = Math.toRadians(this.pitch);
            final double ahead = tick.state().speed() * ShipFlightModel.TICK;
            this.x += Math.sin(h) * Math.cos(p) * ahead;
            this.z -= Math.cos(h) * Math.cos(p) * ahead;
            // On the land its nose may point down, the land holding it up as it rolls, as the game's ground does.
            final double along = this.landed ? Math.max(0, Math.sin(p) * ahead) : Math.sin(p) * ahead;
            this.y += along + (tick.state().rise() - tick.state().sink()) * ShipFlightModel.TICK;
            this.state = tick.state();
            this.landed = this.y <= ground;
            if (this.landed) {
                this.y = ground;
                this.state = new ShipFlightModel.State(this.state.speed(), 0, this.state.throttle(),
                        this.state.charge(), Math.max(0, this.state.rise()), this.state.nozzles());
            }
        }

        @Override
        public String toString() {
            return String.format("at %.1f %.1f %.1f, heading %.0f, pitch %.0f, speed %.1f, landed %s", x, y, z,
                    heading, pitch, state.speed(), landed);
        }
    }
}
