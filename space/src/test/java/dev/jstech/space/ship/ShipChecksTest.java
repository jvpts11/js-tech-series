/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.ship;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.space.api.ShipClass;
import dev.jstech.space.api.ShipPartKind;
import dev.jstech.space.api.ShipPlace;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ShipChecksTest {

    private static final ShipChecks.Piece COCKPIT = piece(ShipPartKind.COCKPIT, ShipPlace.NOSE, 0, false, false);
    private static final ShipChecks.Piece AIRLOCK = piece(ShipPartKind.HULL, ShipPlace.SPINE, 0, true, false);
    private static final ShipChecks.Piece CORRIDOR = piece(ShipPartKind.HULL, ShipPlace.SPINE, 0, false, false);
    private static final ShipChecks.Piece BAY = piece(ShipPartKind.HOLD, ShipPlace.SPINE, 3, false, false);
    private static final ShipChecks.Piece RAMP = piece(ShipPartKind.HOLD, ShipPlace.TAIL, 3, false, false);
    private static final ShipChecks.Piece WORKSHOP = piece(ShipPartKind.MODULE, ShipPlace.SPINE, 1, false, false);
    private static final ShipChecks.Piece REACTOR = piece(ShipPartKind.REACTOR, ShipPlace.SPINE, 0, false, false);
    private static final ShipChecks.Piece TAIL_ENGINE = piece(ShipPartKind.MAIN_ENGINE, ShipPlace.SPINE_END, 0, false,
            false);
    private static final ShipChecks.Piece NACELLE = piece(ShipPartKind.MAIN_ENGINE, ShipPlace.ON_WINGS, 0, false,
            false);
    private static final ShipChecks.Piece INTAKES = piece(ShipPartKind.AIR_ENGINE, ShipPlace.SIDES, 0, false, false);
    private static final ShipChecks.Piece DELTA = piece(ShipPartKind.WINGS, ShipPlace.WING_ROOT, 0, false, false);
    private static final ShipChecks.Piece STUBS = piece(ShipPartKind.WINGS, ShipPlace.WING_ROOT, 0, false, true);
    private static final ShipChecks.Piece FINS = piece(ShipPartKind.FINS, ShipPlace.SPINE_END, 0, false, false);
    private static final ShipChecks.Piece QUADS = piece(ShipPartKind.THRUSTERS, ShipPlace.AROUND, 0, false, false);
    private static final ShipChecks.Piece WHEELS = piece(ShipPartKind.GEAR, ShipPlace.UNDER, 0, false, false);

    @Test
    void problems_noneForAWholeShipThatFlies() {
        assertEquals(Set.of(), ShipChecks.problems(List.of(COCKPIT, AIRLOCK, BAY, REACTOR),
                List.of(TAIL_ENGINE, INTAKES, DELTA, FINS, QUADS, WHEELS)));
    }

    @Test
    void problems_flightCheckNamesWhatIsMissing() {
        assertEquals(Set.of(ShipChecks.Problem.NO_AIRLOCK, ShipChecks.Problem.NO_REACTOR,
                        ShipChecks.Problem.NO_MAIN_ENGINE, ShipChecks.Problem.NO_GEAR),
                ShipChecks.problems(List.of(COCKPIT, CORRIDOR), List.of(DELTA)));
        assertTrue(ShipChecks.problems(List.of(AIRLOCK, REACTOR), List.of(TAIL_ENGINE, WHEELS))
                .contains(ShipChecks.Problem.NO_COCKPIT));
        assertTrue(ShipChecks.problems(List.of(COCKPIT), List.of(TAIL_ENGINE, WHEELS))
                .contains(ShipChecks.Problem.BROKEN_SPINE));
    }

    @Test
    void problems_aRampOnlyAtTheTailAndNoTailEngineBehindIt() {
        assertTrue(ShipChecks.problems(List.of(COCKPIT, AIRLOCK, RAMP, REACTOR), List.of(TAIL_ENGINE, WHEELS))
                .contains(ShipChecks.Problem.MISPLACED), "a ramp not at the tail");
        assertTrue(ShipChecks.problems(List.of(COCKPIT, AIRLOCK, REACTOR, RAMP), List.of(TAIL_ENGINE, WHEELS))
                .contains(ShipChecks.Problem.MISPLACED), "a tail engine behind the ramp");
        assertEquals(Set.of(), ShipChecks.problems(List.of(COCKPIT, AIRLOCK, REACTOR, RAMP),
                List.of(STUBS, NACELLE, WHEELS)), "the ramp at the tail, the main engines in nacelles");
    }

    @Test
    void problems_nacellesOnlyOnWingsThatCarryThem() {
        assertTrue(ShipChecks.problems(List.of(COCKPIT, AIRLOCK, REACTOR), List.of(DELTA, NACELLE, WHEELS))
                .contains(ShipChecks.Problem.MISPLACED));
    }

    @Test
    void problems_noMoreModulesThanTheHullHasRoomFor() {
        final List<ShipChecks.Piece> three = new ArrayList<>(List.of(COCKPIT, AIRLOCK, REACTOR));
        three.addAll(List.of(WORKSHOP, WORKSHOP, WORKSHOP));
        assertEquals(Set.of(), ShipChecks.problems(three, List.of(TAIL_ENGINE, WHEELS)), "three small modules fit");
        assertTrue(ShipChecks.problems(List.of(COCKPIT, AIRLOCK, BAY, WORKSHOP, REACTOR),
                List.of(TAIL_ENGINE, WHEELS)).contains(ShipChecks.Problem.TOO_MUCH),
                "a medium module and a small one do not");
    }

    private static ShipChecks.Piece piece(final ShipPartKind kind, final ShipPlace place, final int room,
                                          final boolean airlock, final boolean carries) {
        return new ShipChecks.Piece(kind, place, ShipClass.SMALL, room, airlock, carries);
    }
}
