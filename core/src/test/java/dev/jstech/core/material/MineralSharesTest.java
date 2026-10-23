/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.material;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class MineralSharesTest {

    @Test
    void problem_acceptsAMainElementAndSmallerByproducts() {
        assertTrue(MineralShares.problem(0.6, 0.2, 0.05).isEmpty());
        assertTrue(MineralShares.problem(1.0).isEmpty());
        assertTrue(MineralShares.problem(0.7, 0.2, 0.1).isEmpty());
    }

    @Test
    void problem_refusesNothingOrAShareOutsideTheWhole() {
        assertEquals(Optional.of(MineralShares.Problem.NOTHING), MineralShares.problem());
        assertEquals(Optional.of(MineralShares.Problem.OUTSIDE_THE_WHOLE), MineralShares.problem(0.5, 0));
        assertEquals(Optional.of(MineralShares.Problem.OUTSIDE_THE_WHOLE), MineralShares.problem(-0.1));
        assertEquals(Optional.of(MineralShares.Problem.OUTSIDE_THE_WHOLE), MineralShares.problem(1.2));
    }

    @Test
    void problem_refusesSharesAddingUpPastTheWhole() {
        assertEquals(Optional.of(MineralShares.Problem.MORE_THAN_THE_WHOLE), MineralShares.problem(0.6, 0.5));
    }

    @Test
    void problem_acceptsAByproductHeavierThanTheMainElement() {
        // Chalcopyrite's sulfur weighs a little more than its copper, which it is still mined for.
        assertTrue(MineralShares.problem(0.346, 0.349).isEmpty());
    }
}
