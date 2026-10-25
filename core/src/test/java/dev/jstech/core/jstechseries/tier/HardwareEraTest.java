/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.tier;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HardwareEraTest {

    @Test
    void series_hasTheSevenEarliestFirst() {
        assertEquals(List.of(HardwareEra.VINTAGE, HardwareEra.LEGACY, HardwareEra.TRANSITION, HardwareEra.STANDARD,
                HardwareEra.ADVANCED, HardwareEra.EXA, HardwareEra.SINGULARITY), HardwareEra.SERIES);
    }

    @Test
    void series_areTheRegistryOrderWhenNoAddOnDeclaresAny() {
        assertEquals(HardwareEra.SERIES, Eras.ordered());
    }

    @Test
    void ids_areTheCoresNamespaceAndTheEraName() {
        assertEquals("jscore:vintage", HardwareEra.VINTAGE.id());
        assertEquals("legacy", HardwareEra.LEGACY.path());
        assertEquals("jscore", HardwareEra.SINGULARITY.namespace());
    }

    @Test
    void level_countsFromZeroWithoutGaps() {
        for (int i = 0; i < HardwareEra.SERIES.size(); i++) {
            assertEquals(i, HardwareEra.SERIES.get(i).level());
        }
    }

    @Test
    void next_advancesByOneAndStopsAtTheLast() {
        assertSame(HardwareEra.LEGACY, HardwareEra.VINTAGE.next());
        assertSame(HardwareEra.EXA, HardwareEra.ADVANCED.next());
        assertSame(HardwareEra.SINGULARITY, HardwareEra.SINGULARITY.next());
    }

    @Test
    void prev_goesBackByOneAndStopsAtTheFirst() {
        assertSame(HardwareEra.STANDARD, HardwareEra.ADVANCED.prev());
        assertSame(HardwareEra.VINTAGE, HardwareEra.VINTAGE.prev());
    }

    @Test
    void isAtLeast_acceptsEqualOrLater() {
        assertTrue(HardwareEra.STANDARD.isAtLeast(HardwareEra.STANDARD));
        assertTrue(HardwareEra.ADVANCED.isAtLeast(HardwareEra.LEGACY));
        assertFalse(HardwareEra.LEGACY.isAtLeast(HardwareEra.TRANSITION));
    }

    @Test
    void isAtMost_acceptsEqualOrEarlier() {
        assertTrue(HardwareEra.LEGACY.isAtMost(HardwareEra.LEGACY));
        assertTrue(HardwareEra.VINTAGE.isAtMost(HardwareEra.EXA));
        assertFalse(HardwareEra.SINGULARITY.isAtMost(HardwareEra.ADVANCED));
    }

    @Test
    void ofSeriesIndex_isTheInverseOfSeriesIndex() {
        for (final Era era : HardwareEra.SERIES) {
            assertSame(era, HardwareEra.ofSeriesIndex(HardwareEra.seriesIndex(era)));
        }
    }

    @Test
    void ofSeriesIndex_refusesAnIndexNoneOfTheSevenHas() {
        assertThrows(IllegalArgumentException.class, () -> HardwareEra.ofSeriesIndex(-1));
        assertThrows(IllegalArgumentException.class, () -> HardwareEra.ofSeriesIndex(7));
        assertNull(HardwareEra.seriesAt(7));
    }

    @Test
    void seriesNamed_findsEachOfTheSevenByItsBareName() {
        for (final Era era : HardwareEra.SERIES) {
            assertSame(era, HardwareEra.seriesNamed(era.path()));
        }
        assertNull(HardwareEra.seriesNamed("quantum"));
    }

    @Test
    void withParts_dressesTheExaAndTheSingularityAsTheAdvanced() {
        assertSame(HardwareEra.TRANSITION, HardwareEra.withParts(HardwareEra.TRANSITION));
        assertSame(HardwareEra.ADVANCED, HardwareEra.withParts(HardwareEra.EXA));
        assertSame(HardwareEra.ADVANCED, HardwareEra.withParts(HardwareEra.SINGULARITY));
    }

    @Test
    void pick_givesEachEraWithPartsItsOwnValueAndTheLaterOnesTheAdvanceds() {
        assertEquals(10, HardwareEra.pick(HardwareEra.VINTAGE, 10, 20, 30, 40, 50));
        assertEquals(40, HardwareEra.pick(HardwareEra.STANDARD, 10, 20, 30, 40, 50));
        assertEquals(50, HardwareEra.pick(HardwareEra.SINGULARITY, 10, 20, 30, 40, 50));
    }

    @Test
    void colour_givesEveryEraItsOwnColour() {
        final Set<Integer> colours = new HashSet<>();
        for (final Era era : HardwareEra.SERIES) {
            assertTrue(colours.add(era.colour()), era.id() + " shares its colour");
        }
    }

    @Test
    void year_climbsWithEveryEra() {
        for (int i = 1; i < HardwareEra.SERIES.size(); i++) {
            assertTrue(HardwareEra.SERIES.get(i).year() > HardwareEra.SERIES.get(i - 1).year());
        }
    }

    @Test
    void byName_readsAnIdAndTheBareNameOfOneOfTheSeven() {
        assertSame(HardwareEra.LEGACY, Eras.byName("legacy").orElseThrow());
        assertSame(HardwareEra.LEGACY, Eras.byName("jscore:legacy").orElseThrow());
        assertTrue(Eras.byName("jscore:quantum").isEmpty());
    }

    @Test
    void answersTo_takesTheIdAndTheBareName() {
        assertTrue(HardwareEra.STANDARD.answersTo("jscore:standard"));
        assertTrue(HardwareEra.STANDARD.answersTo("standard"));
        assertFalse(HardwareEra.STANDARD.answersTo("advanced"));
    }

    @Test
    void requireId_refusesAnIdThatIsNotNamespaceAndPath() {
        assertThrows(IllegalArgumentException.class, () -> Eras.requireId("legacy"));
        assertThrows(IllegalArgumentException.class, () -> Eras.requireId("Mod:Era"));
        assertThrows(IllegalArgumentException.class, () -> Eras.requireId("a:b:c"));
    }
}
