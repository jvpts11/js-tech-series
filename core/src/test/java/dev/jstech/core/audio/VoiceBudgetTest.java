/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.jstech.core.audio.VoiceBudget.Candidate;
import java.util.List;
import org.junit.jupiter.api.Test;

class VoiceBudgetTest {

    @Test
    void choose_keepsTheMostImportantThenTheNearestWithinTheBudget() {
        final VoiceBudget budget = new VoiceBudget(2, 1);
        final List<Candidate> chosen = budget.choose(List.of(
                new Candidate("far fan", 50, 20, false),
                new Candidate("near fan", 50, 2, false),
                new Candidate("alarm", 90, 30, false),
                new Candidate("middle fan", 50, 10, false)));
        assertEquals(List.of("alarm", "near fan"), chosen.stream().map(Candidate::id).toList());
    }

    @Test
    void choose_countsTheStreamingChannelsApart() {
        final VoiceBudget budget = new VoiceBudget(5, 1);
        final List<Candidate> chosen = budget.choose(List.of(
                new Candidate("room tone", 40, 5, true),
                new Candidate("music", 60, 0, true),
                new Candidate("fan", 50, 3, false)));
        assertEquals(List.of("music", "fan"), chosen.stream().map(Candidate::id).toList());
    }

    @Test
    void choose_isTheSameWhateverOrderTheSoundsCameIn() {
        final VoiceBudget budget = new VoiceBudget(1, 0);
        final Candidate a = new Candidate("a", 50, 4, false);
        final Candidate b = new Candidate("b", 50, 4, false);
        assertEquals(budget.choose(List.of(a, b)), budget.choose(List.of(b, a)));
    }

    @Test
    void constructor_refusesABudgetBelowNothing() {
        assertThrows(IllegalArgumentException.class, () -> new VoiceBudget(-1, 0));
    }
}
