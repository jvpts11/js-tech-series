/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.mechanical;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ShaftNetworkTest {

    /* A crank of 20 N·m and 75 W, free at 60 rpm. */
    private static final TorqueCurve CRANK = TorqueCurve.of(20.0, 75.0, 60.0);

    @Test
    void inertia_isEachPartsTimesItsRatioSquared() {
        final ShaftNetwork network = new ShaftNetwork(List.of(ShaftNetwork.Member.turning(0, 1.0, 2.0, 0.0),
                ShaftNetwork.Member.turning(1, -2.0, 3.0, 0.0)), 0.0, false);
        assertEquals(2.0 + 3.0 * 4.0, network.inertia(), 1e-12);
    }

    @Test
    void source_alone_runsUpToItsFreeSpeed() {
        final ShaftNetwork network = new ShaftNetwork(List.of(new ShaftNetwork.Member(0, 1.0, 0.01, 0.0,
                CRANK::torque)), 0.0, false);
        for (int i = 0; i < 200; i++) {
            network.step(0.05);
        }
        assertEquals(60.0, network.rpm(), 0.5);
    }

    @Test
    void load_slowsTheNetworkWhereTheTorqueMeets() {
        // The crank's 20 N·m fall evenly to none at 60 rpm: they meet 15 N·m at 15 rpm.
        final ShaftNetwork network = new ShaftNetwork(List.of(
                new ShaftNetwork.Member(0, 1.0, 0.01, 0.0, CRANK::torque),
                new ShaftNetwork.Member(1, 1.0, 0.01, 0.0, ITorque.load(15.0))), 0.0, false);
        for (int i = 0; i < 200; i++) {
            network.step(0.05);
        }
        assertEquals(15.0, network.rpm(), 0.1);
    }

    @Test
    void load_pastThePower_meetsWhereThePowerRunsOut() {
        // A source of 1,000 N·m and only 75 W against 30 N·m: the power runs out first, at 2.5 rad/s.
        final TorqueCurve strong = TorqueCurve.of(1_000.0, 75.0, 600.0);
        final ShaftNetwork network = new ShaftNetwork(List.of(
                new ShaftNetwork.Member(0, 1.0, 0.01, 0.0, strong::torque),
                new ShaftNetwork.Member(1, 1.0, 0.01, 0.0, ITorque.load(30.0))), 0.0, false);
        for (int i = 0; i < 200; i++) {
            network.step(0.05);
        }
        assertEquals(RotationMath.rpm(2.5), network.rpm(), 0.1);
    }

    @Test
    void gears_tradeSpeedForTorque() {
        // The crank drives a gear of 16 teeth meshing one of 32: the load turns half as fast, the other way.
        final ShaftNetwork network = new ShaftNetwork(List.of(
                new ShaftNetwork.Member(0, 1.0, 0.01, 0.0, CRANK::torque),
                new ShaftNetwork.Member(1, RotationMath.mesh(16, 32), 0.01, 0.0, ITorque.load(30.0))),
                0.0, false);
        for (int i = 0; i < 200; i++) {
            network.step(0.05);
        }
        // 30 N·m at the slow gear is 15 at the crank: the same balance as a 15 N·m load straight on it.
        assertEquals(15.0, network.rpm(), 0.1);
        assertTrue(network.rpm(network.members().get(1)) < 0.0, "the driven gear turns the other way");
    }

    @Test
    void friction_holdsBackWhatTheSourceCannotPass() {
        final ShaftNetwork network = new ShaftNetwork(List.of(new ShaftNetwork.Member(0, 1.0, 0.01, 25.0,
                CRANK::torque)), 0.0, false);
        for (int i = 0; i < 100; i++) {
            network.step(0.05);
        }
        assertEquals(0.0, network.rpm(), 0.01);
    }

    @Test
    void jammed_neverTurns() {
        final ShaftNetwork network = new ShaftNetwork(List.of(new ShaftNetwork.Member(0, 1.0, 0.01, 0.0,
                CRANK::torque)), 30.0, true);
        network.step(1.0);
        assertEquals(0.0, network.rpm(), 1e-12);
    }

    @Test
    void power_isWhatTheSourcesGive() {
        final ShaftNetwork network = new ShaftNetwork(List.of(
                new ShaftNetwork.Member(0, 1.0, 0.01, 0.0, CRANK::torque),
                new ShaftNetwork.Member(1, 1.0, 0.01, 0.0, ITorque.load(15.0))), 0.0, false);
        for (int i = 0; i < 200; i++) {
            network.step(0.05);
        }
        // 15 N·m at 15 rpm.
        assertEquals(15.0 * RotationMath.radians(15.0), network.power(), 0.1);
    }
}
