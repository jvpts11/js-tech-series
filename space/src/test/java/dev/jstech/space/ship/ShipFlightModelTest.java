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

class ShipFlightModelTest {

    private static final double CLOSE = 1e-9;
    /* A ship of 40 tonnes: 420 kN of air engines, 420 kN of bells, delta wings holding 400 kN, 4 MW to spare. */
    private static final ShipFlightModel.Performance SHIP = new ShipFlightModel.Performance(40_000, 420_000,
            420_000, 400_000, 4.0e6);
    private static final ShipFlightModel.Air SEA = new ShipFlightModel.Air(1.0, 1.0, true);
    private static final ShipFlightModel.Air SPACE = new ShipFlightModel.Air(0.0, 0.0, true);
    private static final ShipFlightModel.Held FORWARD = new ShipFlightModel.Held(1, 0, 0, 0, 0, 0, false, true);
    private static final ShipFlightModel.Held CRUISING = new ShipFlightModel.Held(0, 0, 0, 0, 0, 0, false, true);
    private static final ShipFlightModel.Held BOOSTING = new ShipFlightModel.Held(0, 0, 0, 0, 0, 0, true, true);
    private static final int SECONDS = 20;

    @Test
    void step_openThrottleGathersSpeedByThrustOverMass() {
        final ShipFlightModel.Tick one = ShipFlightModel.step(ShipFlightModel.State.RESTING, FORWARD, SEA, SHIP, 0);
        assertEquals(0.025, one.state().throttle(), CLOSE);
        assertEquals(420_000.0 / 40_000.0 * ShipFlightModel.TICK, one.state().speed(), CLOSE);
    }

    @Test
    void step_aHeavierShipGathersSpeedMoreSlowly() {
        final ShipFlightModel.Performance heavy = new ShipFlightModel.Performance(80_000, 420_000, 420_000, 400_000,
                0);
        final double light = fly(SHIP, FORWARD, SEA, 2 * SECONDS).speed();
        final double laden = fly(heavy, FORWARD, SEA, 2 * SECONDS).speed();
        assertTrue(laden < light, "twice the mass, slower after two seconds: " + laden + " against " + light);
    }

    @Test
    void step_inTheAirTheAirEnginesPushAndInSpaceTheBells() {
        final ShipFlightModel.Performance airOnly = new ShipFlightModel.Performance(40_000, 420_000, 0, 0, 0);
        final ShipFlightModel.Performance bellsOnly = new ShipFlightModel.Performance(40_000, 0, 420_000, 0, 0);
        assertEquals(420_000, ShipFlightModel.push(airOnly, 1.0), CLOSE);
        assertEquals(0, ShipFlightModel.push(airOnly, 0.0), CLOSE);
        assertEquals(0, ShipFlightModel.push(bellsOnly, 1.0), CLOSE);
        assertEquals(420_000, ShipFlightModel.push(bellsOnly, 0.0), CLOSE);
        assertEquals(0.0, ShipFlightModel.step(ShipFlightModel.State.RESTING, FORWARD, SEA, SHIP, 0).bells(), CLOSE);
        assertEquals(1.0, ShipFlightModel.step(ShipFlightModel.State.RESTING, FORWARD, SPACE, SHIP, 0).bells(),
                CLOSE);
    }

    @Test
    void step_tooSlowInTheAirItSinksAndFastEnoughItsWingsHoldIt() {
        final ShipFlightModel.State slow = new ShipFlightModel.State(5, 0, 0.125, 1);
        final ShipFlightModel.Tick sinking = ShipFlightModel.step(slow, CRUISING, SEA, SHIP, 0);
        assertTrue(sinking.stalled() && sinking.state().sink() > 0, "at 5 m/s it sinks: " + sinking);
        final ShipFlightModel.State fast = new ShipFlightModel.State(30, 6, 0.75, 1);
        final ShipFlightModel.Tick held = ShipFlightModel.step(fast, CRUISING, SEA, SHIP, 0);
        assertFalse(held.stalled(), "at 30 m/s its wings hold it");
        assertTrue(held.state().sink() < 6, "and it stops sinking: " + held.state().sink());
    }

    @Test
    void stall_isFasterInThinnerAirForAHeavierShipAndEndlessWithNoWings() {
        final double sea = ShipFlightModel.stall(SHIP, 1.0, ShipFlightModel.GRAVITY);
        assertEquals(20.0 * Math.sqrt(40_000 * ShipFlightModel.GRAVITY / 400_000.0), sea, CLOSE);
        assertTrue(ShipFlightModel.stall(SHIP, 0.25, ShipFlightModel.GRAVITY) > sea, "thinner air");
        assertTrue(ShipFlightModel.stall(new ShipFlightModel.Performance(80_000, 0, 0, 400_000, 0), 1.0,
                ShipFlightModel.GRAVITY) > sea, "a heavier ship");
        assertEquals(Double.POSITIVE_INFINITY, ShipFlightModel.stall(new ShipFlightModel.Performance(40_000, 0, 0,
                0, 0), 1.0, ShipFlightModel.GRAVITY));
        assertEquals(0, ShipFlightModel.stall(SHIP, 0.0, 0.0), CLOSE);
    }

    @Test
    void step_inSpaceItFliesFreeAndNeverSinks() {
        final ShipFlightModel.State still = ShipFlightModel.State.RESTING;
        final ShipFlightModel.Tick tick = ShipFlightModel.step(still, CRUISING, SPACE, SHIP, 0);
        assertFalse(tick.stalled());
        assertEquals(0, tick.state().sink(), CLOSE);
        final ShipFlightModel.State flown = fly(SHIP, FORWARD, SPACE, 10 * SECONDS);
        assertTrue(flown.speed() > fly(SHIP, FORWARD, SEA, 10 * SECONDS).speed(), "space lets it go faster");
    }

    @Test
    void step_theBoostTakesItFasterSpendsItsStoreAndTheReactorFillsIt() {
        final ShipFlightModel.State cruising = new ShipFlightModel.State(40, 0, 1, 1);
        final ShipFlightModel.Tick boosted = ShipFlightModel.step(cruising, BOOSTING, SEA, SHIP, 0);
        assertTrue(boosted.boosting() && boosted.state().speed() > 40, "boosted, it goes past its cruise");
        assertEquals(1 - ShipFlightModel.TICK / 8.0, boosted.state().charge(), CLOSE);
        final ShipFlightModel.Air crowded = new ShipFlightModel.Air(1.0, 1.0, false);
        assertFalse(ShipFlightModel.step(cruising, BOOSTING, crowded, SHIP, 0).boosting(), "not near the ground");
        final ShipFlightModel.State spent = new ShipFlightModel.State(40, 0, 1, 0);
        assertFalse(ShipFlightModel.step(spent, BOOSTING, SEA, SHIP, 0).boosting(), "not with its store spent");
        assertEquals(4.0e6 * ShipFlightModel.TICK / 160.0e6, ShipFlightModel.step(spent, CRUISING, SEA, SHIP, 0)
                .state().charge(), CLOSE);
    }

    @Test
    void step_withNobodyAtTheHelmItsThrottleClosesAndItSlowsToAStop() {
        final ShipFlightModel.State flown = fly(SHIP, ShipFlightModel.Held.NOBODY, SPACE, 3 * SECONDS,
                new ShipFlightModel.State(60, 0, 0.5, 1));
        assertEquals(0, flown.throttle(), CLOSE);
        assertTrue(flown.speed() < 60, "it slows: " + flown.speed());
    }

    @Test
    void step_itTurnsAndPitchesAsHeldAndItsNoseStopsAtSixtyDegrees() {
        final ShipFlightModel.Held leftUp = new ShipFlightModel.Held(0, 0, 1, 0, 1, 0, false, true);
        final ShipFlightModel.Tick tick = ShipFlightModel.step(ShipFlightModel.State.RESTING, leftUp, SEA, SHIP, 0);
        assertEquals(45.0 * ShipFlightModel.TICK, tick.turn(), CLOSE);
        assertEquals(30.0 * ShipFlightModel.TICK, tick.pitch(), CLOSE);
        assertEquals(0.5, ShipFlightModel.step(ShipFlightModel.State.RESTING, leftUp, SEA, SHIP, 59.5).pitch(),
                CLOSE);
    }

    private static ShipFlightModel.State fly(final ShipFlightModel.Performance ship, final ShipFlightModel.Held held,
                                             final ShipFlightModel.Air air, final int ticks) {
        return fly(ship, held, air, ticks, ShipFlightModel.State.RESTING);
    }

    private static ShipFlightModel.State fly(final ShipFlightModel.Performance ship, final ShipFlightModel.Held held,
                                             final ShipFlightModel.Air air, final int ticks,
                                             final ShipFlightModel.State from) {
        ShipFlightModel.State now = from;
        for (int tick = 0; tick < ticks; tick++) {
            now = ShipFlightModel.step(now, held, air, ship, 0).state();
        }
        return now;
    }
}
