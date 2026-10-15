/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.cable;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class RunPathTest {

    @Test
    void between_goesAlongTheLongestAxisFirst() {
        final List<RunPath.Step> path = RunPath.between(new RunPath.Step(0, 0, 0), new RunPath.Step(3, 0, 1), false);

        assertEquals(List.of(new RunPath.Step(0, 0, 0), new RunPath.Step(1, 0, 0), new RunPath.Step(2, 0, 0),
                new RunPath.Step(3, 0, 0), new RunPath.Step(3, 0, 1)), path);
    }

    @Test
    void between_eachStepIsOneBlockFromTheLast() {
        final List<RunPath.Step> path = RunPath.between(new RunPath.Step(5, 64, -2), new RunPath.Step(1, 60, 4),
                false);

        for (int k = 1; k < path.size(); k++) {
            final RunPath.Step a = path.get(k - 1);
            final RunPath.Step b = path.get(k);
            assertEquals(1, Math.abs(a.x() - b.x()) + Math.abs(a.y() - b.y()) + Math.abs(a.z() - b.z()),
                    "step " + k + " moves one block");
        }
        assertEquals(new RunPath.Step(1, 60, 4), path.get(path.size() - 1), "and the run ends where it was dragged");
        assertEquals(4 + 4 + 6 + 1, path.size());
    }

    @Test
    void between_runsOnlyStraightForACableThatDoes() {
        final List<RunPath.Step> path = RunPath.between(new RunPath.Step(0, 0, 0), new RunPath.Step(0, 2, 5), true);

        assertEquals(6, path.size());
        assertEquals(new RunPath.Step(0, 0, 5), path.get(path.size() - 1), "level with the end on its one axis");
    }

    @Test
    void between_laysNoMoreThanTheMost() {
        final List<RunPath.Step> path = RunPath.between(new RunPath.Step(0, 0, 0), new RunPath.Step(500, 0, 0),
                false);

        assertEquals(RunPath.MOST, path.size());
    }

    @Test
    void between_isTheOneBlockWhenItEndsWhereItStarts() {
        assertEquals(List.of(new RunPath.Step(2, 3, 4)), RunPath.between(new RunPath.Step(2, 3, 4),
                new RunPath.Step(2, 3, 4), false));
    }
}
