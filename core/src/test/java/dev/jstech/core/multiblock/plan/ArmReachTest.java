/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock.plan;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ArmReachTest {

    private static final double CLOSE = 1.0e-6;
    private static final double UPPER = 5.0;
    private static final double FORE = 4.6;

    @Test
    void solve_putsTheWristOnAPointItReaches() {
        final double[] shoulder = {-8.5, 4.75, 2.0};
        final double[] wrist = {-0.5, 3.15, 5.0};
        final ArmReach.Pose pose = ArmReach.solve(shoulder, wrist, UPPER, FORE);
        assertArrayEquals(wrist, pose.wrist(), CLOSE);
        assertArrayEquals(wrist, forward(shoulder, pose), CLOSE, "its turns put its wrist there");
        assertTrue(pose.elbow()[1] > shoulder[1], "its elbow stands up");
    }

    @Test
    void solve_hangsTheToolStraightDown() {
        final ArmReach.Pose pose = ArmReach.solve(new double[] {0, 0, 0}, new double[] {3, -2, 4}, UPPER, FORE);
        assertEquals(Math.PI, pose.upper() + pose.fore() + pose.tool(), CLOSE);
    }

    @Test
    void solve_reachesAsFarAsItGoesTowardAPointBeyondIt() {
        final double[] shoulder = {0, 0, 0};
        final ArmReach.Pose pose = ArmReach.solve(shoulder, new double[] {0, 0, 20}, UPPER, FORE);
        assertEquals(UPPER + FORE, pose.wrist()[2], 1.0e-2, "on the line toward it, straight out");
        assertArrayEquals(pose.wrist(), forward(shoulder, pose), CLOSE);
    }

    @Test
    void reaches_isTrueOnlyWithinBothLinks() {
        final double[] shoulder = {0, 0, 0};
        assertTrue(ArmReach.reaches(shoulder, new double[] {6, 0, 6}, UPPER, FORE));
        assertFalse(ArmReach.reaches(shoulder, new double[] {7, 0, 7}, UPPER, FORE));
    }

    /* Where the pose's turns put the wrist, link by link from the shoulder. */
    private static double[] forward(final double[] shoulder, final ArmReach.Pose pose) {
        final double[] elbow = step(shoulder, pose.yaw(), pose.upper(), UPPER);
        return step(elbow, pose.yaw(), pose.upper() + pose.fore(), FORE);
    }

    private static double[] step(final double[] from, final double yaw, final double lean, final double length) {
        return new double[] {from[0] + Math.sin(yaw) * Math.sin(lean) * length,
            from[1] + Math.cos(lean) * length, from[2] + Math.cos(yaw) * Math.sin(lean) * length};
    }
}
