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
            420_000, 400_000, 4.0e6, ShipFlightModel.Gear.WHEELS);
    /* The same with bells alone, nothing to hover on. */
    private static final ShipFlightModel.Performance BELLS = new ShipFlightModel.Performance(40_000, 0, 420_000,
            400_000, 0, ShipFlightModel.Gear.WHEELS);
    private static final ShipFlightModel.Air SEA = new ShipFlightModel.Air(1.0, 1.0, true, false, 100);
    private static final ShipFlightModel.Air SPACE = new ShipFlightModel.Air(0.0, 0.0, true, false, 1000);
    private static final ShipFlightModel.Air LANDED = new ShipFlightModel.Air(1.0, 1.0, false, true, 0);
    private static final ShipFlightModel.Held FORWARD = new ShipFlightModel.Held(1, 0, 0, 0, 0, 0, false, true);
    private static final ShipFlightModel.Held CRUISING = new ShipFlightModel.Held(0, 0, 0, 0, 0, 0, false, true);
    private static final ShipFlightModel.Held BOOSTING = new ShipFlightModel.Held(0, 0, 0, 0, 0, 0, true, true);
    private static final ShipFlightModel.Held UP = new ShipFlightModel.Held(0, 0, 0, 0, 1, 0, false, true);
    private static final ShipFlightModel.Held DOWN = new ShipFlightModel.Held(0, 0, 0, 0, 0, 1, false, true);
    /* Mars's air at its ground, as a share of the overworld's, and its pull. */
    private static final ShipFlightModel.Air THIN = new ShipFlightModel.Air(0.006, 0.38, true, false, 1000);
    /* How high the air of a climb thins to none, in metres, and how far its pilot holds its nose up. */
    private static final double TOP = 2000;
    private static final double CLIMBING = 60;
    private static final int SECONDS = 20;

    @Test
    void step_openThrottleGathersSpeedByThrustOverMass() {
        final ShipFlightModel.Tick one = ShipFlightModel.step(ShipFlightModel.State.RESTING, FORWARD, SPACE, SHIP,
                0);
        assertEquals(0.025, one.state().throttle(), CLOSE);
        assertEquals(420_000.0 / 40_000.0 * ShipFlightModel.TICK, one.state().speed(), CLOSE);
    }

    @Test
    void step_aHeavierShipGathersSpeedMoreSlowly() {
        final ShipFlightModel.Performance heavy = new ShipFlightModel.Performance(80_000, 420_000, 420_000, 400_000,
                0, ShipFlightModel.Gear.WHEELS);
        final double light = fly(SHIP, FORWARD, SPACE, 2 * SECONDS).speed();
        final double laden = fly(heavy, FORWARD, SPACE, 2 * SECONDS).speed();
        assertTrue(laden < light, "twice the mass, slower after two seconds: " + laden + " against " + light);
    }

    @Test
    void step_inTheAirTheAirEnginesPushAndInSpaceTheBells() {
        final ShipFlightModel.Performance airOnly = new ShipFlightModel.Performance(40_000, 420_000, 0, 0, 0,
                ShipFlightModel.Gear.NONE);
        assertEquals(420_000, ShipFlightModel.push(airOnly, 1.0), CLOSE);
        assertEquals(0, ShipFlightModel.push(airOnly, 0.0), CLOSE);
        assertEquals(0, ShipFlightModel.push(BELLS, 1.0), CLOSE);
        assertEquals(420_000, ShipFlightModel.push(BELLS, 0.0), CLOSE);
        final ShipFlightModel.State fast = new ShipFlightModel.State(40, 0, 1, 1, 0, 0);
        assertEquals(0.0, ShipFlightModel.step(fast, CRUISING, SEA, SHIP, 0).bells(), CLOSE);
        assertEquals(1.0, ShipFlightModel.step(fast, CRUISING, SPACE, SHIP, 0).bells(), CLOSE);
    }

    @Test
    void step_tooSlowInTheAirWithNothingToHoverOnItSinksAndFastEnoughItsWingsHoldIt() {
        final ShipFlightModel.State slow = new ShipFlightModel.State(5, 0, 0.125, 1, 0, 0);
        final ShipFlightModel.Tick sinking = ShipFlightModel.step(slow, CRUISING, SEA, BELLS, 0);
        assertTrue(sinking.stalled() && sinking.state().sink() > 0, "at 5 m/s it sinks: " + sinking);
        final ShipFlightModel.State fast = new ShipFlightModel.State(30, 6, 0.75, 1, 0, 0);
        final ShipFlightModel.Tick held = ShipFlightModel.step(fast, CRUISING, SEA, BELLS, 0);
        assertFalse(held.stalled(), "at 30 m/s its wings hold it");
        assertTrue(held.state().sink() < 6, "and it stops sinking: " + held.state().sink());
    }

    @Test
    void step_slowItsNozzlesTurnDownJustFarEnoughToHoldWhatItsWingsDoNot() {
        final ShipFlightModel.State hovered = fly(SHIP, CRUISING, SEA, 3 * SECONDS);
        final double hold = Math.toDegrees(Math.asin(40_000 * ShipFlightModel.GRAVITY / 420_000.0));
        assertEquals(hold, hovered.nozzles(), 0.5, "at rest in the air its nozzles hold its weight");
        assertEquals(0, hovered.sink(), 1.0e-6, "and it stops sinking");
        final ShipFlightModel.Tick on = ShipFlightModel.step(hovered, CRUISING, SEA, SHIP, 0);
        assertTrue(on.lifting() && on.gearDown(), "it blows down, its gear down: " + on);
        final ShipFlightModel.State faster = new ShipFlightModel.State(15, 0, 0.5, 1, 0, hovered.nozzles());
        assertTrue(fly(SHIP, CRUISING, SEA, SECONDS, faster).nozzles() < hovered.nozzles(),
                "faster, its wings holding more, its nozzles turn back");
    }

    @Test
    void step_hoveringItRisesAndComesDownStraightAsItsPilotHoldsItsNoseKeptLevel() {
        final ShipFlightModel.State hovered = fly(SHIP, CRUISING, SEA, 3 * SECONDS);
        final ShipFlightModel.State rising = fly(SHIP, UP, SEA, SECONDS, hovered);
        assertTrue(rising.rise() > 0, "held up, it rises: " + rising);
        assertTrue(fly(SHIP, DOWN, SEA, SECONDS, hovered).rise() < 0, "held down, it comes down");
        assertEquals(-1.5, ShipFlightModel.step(hovered, UP, SEA, SHIP, 10).pitch(), CLOSE,
                "its nose comes back level");
    }

    @Test
    void step_fastInThinAirItsNoseStaysWhereItsPilotPointsIt() {
        final ShipFlightModel.Tick fast = ShipFlightModel.step(new ShipFlightModel.State(100, 0, 1, 1, 0, 0), UP,
                THIN, SHIP, 20);
        assertEquals(30.0 * ShipFlightModel.TICK, fast.pitch(), CLOSE, "held up, its nose goes up: " + fast);
        final ShipFlightModel.Tick slow = ShipFlightModel.step(new ShipFlightModel.State(5, 0, 0, 1, 0, 0), UP, THIN,
                SHIP, 20);
        assertTrue(slow.pitch() < 0, "slow, it hovers, its nose coming back level: " + slow);
    }

    @Test
    void step_pitchedUpItClimbsOutOfThinningAirOnItsBellsWithoutStopping() {
        ShipFlightModel.State now = new ShipFlightModel.State(40, 0, 1, 1, 0, 0);
        double pitch = CLIMBING;
        double height = 0;
        for (int tick = 0; tick < 200 * SECONDS && height < TOP; tick++) {
            final ShipFlightModel.Air air = new ShipFlightModel.Air(Math.max(0, 1 - height / TOP), 1.0, true, false,
                    height);
            final ShipFlightModel.Tick next = ShipFlightModel.step(now, CRUISING, air, SHIP, pitch);
            pitch += next.pitch();
            now = next.state();
            final double climb = now.speed() * Math.sin(Math.toRadians(pitch)) + now.rise() - now.sink();
            assertTrue(climb > 0, "it climbs all the way: " + climb + " m/s at " + height + " m, " + now);
            height += climb * ShipFlightModel.TICK;
        }
        assertTrue(height >= TOP, "out of the air: " + height + " m");
        assertEquals(CLIMBING, pitch, CLOSE, "its nose where its pilot held it");
    }

    @Test
    void step_tooHeavyForItsNozzlesItSinksTheirsTurnedRightDown() {
        final ShipFlightModel.Performance heavy = new ShipFlightModel.Performance(80_000, 420_000, 0, 400_000, 0,
                ShipFlightModel.Gear.LEGS);
        final ShipFlightModel.State tried = fly(heavy, UP, SEA, 2 * SECONDS);
        assertEquals(ShipFlightModel.DOWN, tried.nozzles(), CLOSE);
        assertTrue(tried.sink() > 0 && tried.rise() <= 0, "it cannot hold its weight: " + tried);
    }

    @Test
    void step_withNobodyAtTheHelmItComesDownGently() {
        final ShipFlightModel.State hovered = fly(SHIP, CRUISING, SEA, 3 * SECONDS);
        final ShipFlightModel.State settling = fly(SHIP, ShipFlightModel.Held.NOBODY, SEA, 2 * SECONDS, hovered);
        assertEquals(-2.0, settling.rise(), CLOSE);
        assertEquals(0, settling.sink(), 1.0e-6);
    }

    @Test
    void step_onTheLandWheelsRollItAndFeetHoldIt() {
        final ShipFlightModel.Performance legs = new ShipFlightModel.Performance(40_000, 420_000, 420_000, 400_000,
                0, ShipFlightModel.Gear.LEGS);
        assertTrue(fly(SHIP, FORWARD, LANDED, SECONDS).speed() > 0, "on wheels it rolls");
        assertEquals(0, fly(legs, FORWARD, LANDED, SECONDS).speed(), CLOSE, "on feet it stands");
        final ShipFlightModel.Tick parked = ShipFlightModel.step(ShipFlightModel.State.RESTING, CRUISING, LANDED,
                legs, 0);
        assertTrue(parked.gearDown() && !parked.lifting() && parked.state().nozzles() == 0,
                "resting on the land, its gear is down and its nozzles idle: " + parked);
    }

    @Test
    void step_itsGearComesDownNearTheLandAndGoesUpHigh() {
        final ShipFlightModel.State fast = new ShipFlightModel.State(40, 0, 1, 1, 0, 0);
        assertFalse(ShipFlightModel.step(fast, CRUISING, SEA, SHIP, 0).gearDown(), "high and fast, gear up");
        final ShipFlightModel.Air low = new ShipFlightModel.Air(1.0, 1.0, false, false, 4);
        assertTrue(ShipFlightModel.step(fast, CRUISING, low, SHIP, 0).gearDown(), "near the land, gear down");
        final ShipFlightModel.Performance none = new ShipFlightModel.Performance(40_000, 420_000, 420_000, 400_000,
                0, ShipFlightModel.Gear.NONE);
        assertFalse(ShipFlightModel.step(fast, CRUISING, low, none, 0).gearDown(), "no gear, none to lower");
    }

    @Test
    void stall_isFasterInThinnerAirForAHeavierShipAndEndlessWithNoWings() {
        final double sea = ShipFlightModel.stall(SHIP, 1.0, ShipFlightModel.GRAVITY);
        assertEquals(20.0 * Math.sqrt(40_000 * ShipFlightModel.GRAVITY / 400_000.0), sea, CLOSE);
        assertTrue(ShipFlightModel.stall(SHIP, 0.25, ShipFlightModel.GRAVITY) > sea, "thinner air");
        assertTrue(ShipFlightModel.stall(new ShipFlightModel.Performance(80_000, 0, 0, 400_000, 0,
                ShipFlightModel.Gear.NONE), 1.0, ShipFlightModel.GRAVITY) > sea, "a heavier ship");
        assertEquals(Double.POSITIVE_INFINITY, ShipFlightModel.stall(new ShipFlightModel.Performance(40_000, 0, 0,
                0, 0, ShipFlightModel.Gear.NONE), 1.0, ShipFlightModel.GRAVITY));
        assertEquals(0, ShipFlightModel.stall(SHIP, 0.0, 0.0), CLOSE);
    }

    @Test
    void step_inSpaceItFliesFreeAndNeverSinks() {
        final ShipFlightModel.Tick tick = ShipFlightModel.step(ShipFlightModel.State.RESTING, CRUISING, SPACE, SHIP,
                0);
        assertFalse(tick.stalled());
        assertEquals(0, tick.state().sink(), CLOSE);
        final ShipFlightModel.State flown = fly(SHIP, FORWARD, SPACE, 10 * SECONDS);
        assertTrue(flown.speed() > fly(SHIP, FORWARD, SEA, 10 * SECONDS).speed(), "space lets it go faster");
    }

    @Test
    void step_theBoostTakesItFasterSpendsItsStoreAndTheReactorFillsIt() {
        final ShipFlightModel.State cruising = new ShipFlightModel.State(40, 0, 1, 1, 0, 0);
        final ShipFlightModel.Tick boosted = ShipFlightModel.step(cruising, BOOSTING, SEA, SHIP, 0);
        assertTrue(boosted.boosting() && boosted.state().speed() > 40, "boosted, it goes past its cruise");
        assertEquals(1 - ShipFlightModel.TICK / 8.0, boosted.state().charge(), CLOSE);
        final ShipFlightModel.Air crowded = new ShipFlightModel.Air(1.0, 1.0, false, false, 100);
        assertFalse(ShipFlightModel.step(cruising, BOOSTING, crowded, SHIP, 0).boosting(), "not near the ground");
        final ShipFlightModel.State spent = new ShipFlightModel.State(40, 0, 1, 0, 0, 0);
        assertFalse(ShipFlightModel.step(spent, BOOSTING, SEA, SHIP, 0).boosting(), "not with its store spent");
        assertEquals(4.0e6 * ShipFlightModel.TICK / 160.0e6, ShipFlightModel.step(spent, CRUISING, SEA, SHIP, 0)
                .state().charge(), CLOSE);
    }

    @Test
    void step_withNobodyAtTheHelmItsThrottleClosesAndItSlowsToAStop() {
        final ShipFlightModel.State flown = fly(SHIP, ShipFlightModel.Held.NOBODY, SPACE, 3 * SECONDS,
                new ShipFlightModel.State(60, 0, 0.5, 1, 0, 0));
        assertEquals(0, flown.throttle(), CLOSE);
        assertTrue(flown.speed() < 60, "it slows: " + flown.speed());
    }

    @Test
    void step_itTurnsAndPitchesAsHeldAndItsNoseStopsAtSixtyDegrees() {
        final ShipFlightModel.Held leftUp = new ShipFlightModel.Held(0, 0, 1, 0, 1, 0, false, true);
        final ShipFlightModel.Tick tick = ShipFlightModel.step(ShipFlightModel.State.RESTING, leftUp, SPACE, SHIP,
                0);
        assertEquals(45.0 * ShipFlightModel.TICK, tick.turn(), CLOSE);
        assertEquals(30.0 * ShipFlightModel.TICK, tick.pitch(), CLOSE);
        assertEquals(0.5, ShipFlightModel.step(ShipFlightModel.State.RESTING, leftUp, SPACE, SHIP, 59.5).pitch(),
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
