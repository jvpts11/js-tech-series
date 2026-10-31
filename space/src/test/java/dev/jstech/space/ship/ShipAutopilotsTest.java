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

    /* A ship flown a tick at a time over flat land at a height, as its structure would fly it, its nose turned. */
    private static final class Flight {

        private ShipFlightModel.State state;
        private double x;
        private double y;
        private double z;
        private double heading;
        private double pitch;
        private boolean landed;

        Flight(final ShipFlightModel.State state, final double x, final double y, final double z,
               final double heading, final double pitch, final boolean landed) {
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
            final ShipFlightModel.Tick tick = ShipFlightModel.step(this.state, held, air, SHIP, this.pitch);
            // A turn to the left above nought: its heading, east of north, falls.
            this.heading -= tick.turn();
            this.pitch += tick.pitch();
            final double h = Math.toRadians(this.heading);
            final double p = Math.toRadians(this.pitch);
            final double ahead = tick.state().speed() * ShipFlightModel.TICK;
            this.x += Math.sin(h) * Math.cos(p) * ahead;
            this.z -= Math.cos(h) * Math.cos(p) * ahead;
            this.y += Math.sin(p) * ahead + (tick.state().rise() - tick.state().sink()) * ShipFlightModel.TICK;
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
