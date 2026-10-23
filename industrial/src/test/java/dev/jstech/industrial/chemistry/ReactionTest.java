/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.chemistry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ReactionTest {

    @Test
    void amount_givesAHundredMillibucketsAFluidsCoefficient() {
        final Reaction ammonia = Reactions.AMMONIA;
        assertEquals(100L, ammonia.amount(ammonia.reactants().get(0), 1));
        assertEquals(300L, ammonia.amount(ammonia.reactants().get(1), 1));
        assertEquals(200L, ammonia.amount(ammonia.products().get(0), 1));
        assertEquals(600L, ammonia.amount(ammonia.reactants().get(1), 2));
    }

    @Test
    void amount_givesADustASolidsCoefficient() {
        final Reaction rust = Reaction.builder().in("jscore:iron", "Fe", 4, Reaction.Phase.SOLID)
                .in("jscore:oxygen", "O2", 3, Reaction.Phase.FLUID).out("jscore:hematite", "Fe2O3", 2,
                        Reaction.Phase.SOLID).build();
        assertEquals(4L, rust.amount(rust.reactants().get(0), 1));
        assertEquals(300L, rust.amount(rust.reactants().get(1), 1));
        assertEquals(2L, rust.amount(rust.products().get(0), 1));
    }

    @Test
    void balanced_countsTheAtomsOnEachSide() {
        assertTrue(Reactions.AMMONIA.balanced());
        assertFalse(new Reaction(List.of(new Reaction.Term("jscore:nitrogen", "N2", 1, Reaction.Phase.FLUID)),
                List.of(new Reaction.Term("jscore:ammonia", "NH3", 2, Reaction.Phase.FLUID))).balanced());
    }

    @Test
    void builder_refusesAReactionThatDoesNotBalance() {
        assertThrows(IllegalArgumentException.class, () -> Reaction.builder()
                .in("jscore:nitrogen", "N2", 1, Reaction.Phase.FLUID).in("jscore:hydrogen", "H2", 2,
                        Reaction.Phase.FLUID).out("jscore:ammonia", "NH3", 2, Reaction.Phase.FLUID).build());
    }

    @Test
    void equation_isWrittenAsChemistsWriteIt() {
        assertEquals("N2 + 3 H2 → 2 NH3", Reactions.AMMONIA.equation());
    }

    @Test
    void amount_refusesLessThanABatch() {
        assertThrows(IllegalArgumentException.class, () -> Reactions.AMMONIA.amount(
                Reactions.AMMONIA.products().getFirst(), 0));
    }
}
