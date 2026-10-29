/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.rocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.space.orbit.OrbitScale;
import java.util.List;
import org.junit.jupiter.api.Test;

class StageMathTest {

    private static final double G0 = OrbitScale.STANDARD_GRAVITY;
    private static final double CLOSE = 1e-6;
    /* A methalox engine's thrust in a vacuum, in newtons, and its specific impulses, in seconds. */
    private static final double THRUST = 1_000_000;
    private static final double SEA_LEVEL = 330;
    private static final double VACUUM = 350;

    @Test
    void burns_ofOneStageAreTsiolkovskys() {
        final List<StageMath.Burn> burns = StageMath.burns(List.of(engine(1_000), StageMath.Piece.inert(500, 9_000,
                false)));
        assertEquals(1, burns.size());
        final StageMath.Burn burn = burns.getFirst();
        assertEquals(10_500, burn.startMass(), CLOSE);
        assertEquals(1_500, burn.endMass(), CLOSE);
        assertEquals(VACUUM * G0 * Math.log(10_500.0 / 1_500), burn.vacuumDeltaV(), CLOSE);
        assertEquals(SEA_LEVEL * G0 * Math.log(10_500.0 / 1_500), burn.seaLevelDeltaV(), CLOSE);
    }

    @Test
    void burns_splitAtWhatEndsAStageAndCarryWhatIsAbove() {
        final List<StageMath.Burn> burns = StageMath.burns(List.of(engine(1_000),
                StageMath.Piece.inert(500, 9_000, false), StageMath.Piece.inert(200, 0, true), engine(800),
                StageMath.Piece.inert(300, 2_000, false), StageMath.Piece.inert(1_000, 0, false)));
        assertEquals(2, burns.size());
        final double upper = 800 + 300 + 2_000 + 1_000;
        assertEquals(1_000 + 500 + 9_000 + 200 + upper, burns.get(0).startMass(), CLOSE);
        assertEquals(1_000 + 500 + 200 + upper, burns.get(0).endMass(), CLOSE);
        assertEquals(upper, burns.get(1).startMass(), CLOSE);
        assertEquals(upper - 2_000, burns.get(1).endMass(), CLOSE);
        assertEquals(VACUUM * G0 * Math.log(upper / (upper - 2_000)), burns.get(1).vacuumDeltaV(), CLOSE);
    }

    @Test
    void burns_ofAStageWithNoEngineGiveNothing() {
        final List<StageMath.Burn> burns = StageMath.burns(List.of(StageMath.Piece.inert(3_000, 0, false)));
        assertEquals(0, burns.getFirst().vacuumDeltaV(), CLOSE);
        assertEquals(0, StageMath.deltaV(burns, 0), CLOSE);
    }

    @Test
    void burns_withBoostersComeInTwoTheBoostersDroppingFirst() {
        final StageMath.Piece booster = new StageMath.Piece(500, 4_000, THRUST, 250, 270, false, List.of());
        final StageMath.Piece core = new StageMath.Piece(1_000, 0, THRUST, SEA_LEVEL, VACUUM, false,
                List.of(booster, booster));
        final List<StageMath.Burn> burns = StageMath.burns(List.of(core, StageMath.Piece.inert(500, 9_000, false)));
        assertEquals(2, burns.size());
        assertTrue(burns.get(0).boosters() && !burns.get(1).boosters());
        final double start = 1_000 + 500 + 9_000 + 2 * (500 + 4_000);
        assertEquals(start, burns.get(0).startMass(), CLOSE);
        // The boosters burn out in their propellant over their flow; the core burns its flow for as long.
        final double boosterTime = 8_000 / (2 * THRUST / (270 * G0));
        final double coreBurned = THRUST / (VACUUM * G0) * boosterTime;
        assertEquals(start - 8_000 - coreBurned, burns.get(0).endMass(), CLOSE);
        assertEquals(start - 8_000 - coreBurned - 1_000, burns.get(1).startMass(), CLOSE);
        assertEquals(1_500, burns.get(1).endMass(), CLOSE);
        assertEquals(3 * THRUST, burns.get(0).thrust(), CLOSE);
    }

    @Test
    void thrustToWeight_isTheSeaLevelThrustInTheAirAndLessOnTheMoon() {
        final List<StageMath.Burn> burns = StageMath.burns(List.of(engine(1_000), StageMath.Piece.inert(500, 9_000,
                false)));
        final StageMath.Burn burn = burns.getFirst();
        final double weight = burn.startMass() * G0;
        assertEquals(THRUST * SEA_LEVEL / VACUUM / weight, StageMath.thrustToWeight(burn, 1, 1), CLOSE);
        assertEquals(THRUST / (weight * 0.165), StageMath.thrustToWeight(burn, 0.165, 0), CLOSE);
    }

    @Test
    void deltaV_sumsTheBurns() {
        final List<StageMath.Burn> burns = StageMath.burns(List.of(engine(1_000),
                StageMath.Piece.inert(500, 9_000, true), engine(800), StageMath.Piece.inert(300, 2_000, false)));
        assertEquals(burns.get(0).vacuumDeltaV() + burns.get(1).vacuumDeltaV(), StageMath.deltaV(burns, 0), CLOSE);
        assertTrue(StageMath.deltaV(burns, 1) < StageMath.deltaV(burns, 0));
    }

    @Test
    void deltaV_fallsInAStraightLineWithTheAirsPressure() {
        final StageMath.Burn burn = StageMath.burns(List.of(engine(1_000), StageMath.Piece.inert(500, 9_000,
                false))).getFirst();
        final double half = (burn.vacuumDeltaV() + burn.seaLevelDeltaV()) / 2;
        assertEquals(half, StageMath.deltaV(burn, StageMath.air(StageMath.SEA_LEVEL_KILOPASCALS / 2)), CLOSE);
        assertEquals((VACUUM + SEA_LEVEL) / 2, StageMath.isp(SEA_LEVEL, VACUUM, 0.5), CLOSE);
        assertEquals(0, StageMath.isp(SEA_LEVEL, VACUUM, 100), CLOSE);
        assertEquals(0, StageMath.deltaV(burn, 1_000), CLOSE);
    }

    private static StageMath.Piece engine(final double mass) {
        return new StageMath.Piece(mass, 0, THRUST, SEA_LEVEL, VACUUM, false, List.of());
    }
}
