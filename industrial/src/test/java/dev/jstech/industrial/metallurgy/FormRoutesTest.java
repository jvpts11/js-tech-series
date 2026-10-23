/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.metallurgy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class FormRoutesTest {

    @Test
    void of_givesEachFormItsMachinesAndTheirYields() {
        final FormRoute wire = FormRoutes.of("wire").orElseThrow();
        assertEquals("ingot", wire.from());
        assertEquals(new Yield(1, 2), wire.by("wire_drawer").orElseThrow().yield());
        assertEquals(1, wire.by("wire_drawer").orElseThrow().tier());
        assertEquals(new Yield(1, 4), FormRoutes.of("foil").orElseThrow().by("rolling_mill").orElseThrow().yield());
        assertTrue(FormRoutes.of("plate").orElseThrow().by("wire_drawer").isEmpty());
    }

    @Test
    void of_givesTheHandToolsYieldLessThanTheMachines() {
        assertEquals(FormRoute.ByHand.FORGE_HAMMER, FormRoutes.of("plate").orElseThrow().hand());
        assertEquals(Optional.of(new Yield(3, 2)), FormRoutes.of("plate").orElseThrow().handYield());
        assertEquals(Optional.of(new Yield(1, 1)), FormRoutes.of("wire").orElseThrow().handYield());
        assertEquals(Optional.of(new Yield(3, 2)), FormRoutes.of("dust").orElseThrow().handYield());
    }

    @Test
    void of_makesNuggetsAndBlocksNineForOne() {
        assertEquals(Optional.of(new Yield(1, FormRoutes.NINE)), FormRoutes.of("nugget").orElseThrow().handYield());
        assertEquals(Optional.of(new Yield(FormRoutes.NINE, 1)), FormRoutes.of("block").orElseThrow().handYield());
    }

    @Test
    void routes_leaveScrapsOnlyRollingAndDrawing() {
        final Set<String> scrapped = new HashSet<>();
        for (final FormRoute route : FormRoutes.all()) {
            if (route.leavesScrap()) {
                scrapped.add(route.form());
            }
        }
        assertEquals(Set.of("sheet", "foil", "wire"), scrapped);
        assertEquals(0.125, FormRoutes.SCRAP_SHARE);
    }

    @Test
    void routes_nameEachFormOnceAndAreMadeFromAnotherForm() {
        final Set<String> forms = new HashSet<>();
        for (final FormRoute route : FormRoutes.all()) {
            assertTrue(forms.add(route.form()), route.form() + " twice");
            assertFalse(route.form().equals(route.from()));
        }
        assertTrue(FormRoutes.of("scrap").isEmpty());
    }

    @Test
    void route_refusesAHandWithoutItsYield() {
        assertThrows(IllegalArgumentException.class, () -> new FormRoute("plate", "ingot",
                FormRoute.ByHand.FORGE_HAMMER, Optional.empty(), List.of(), false));
    }
}
