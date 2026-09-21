/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CdeStyleTest {

    @Test
    void default_wearsTheDefaultPaletteAndADifferentBackdropOnEachWorkspace() {
        assertEquals(CdePalette.DEFAULT, CdeStyle.DEFAULT.colours());
        assertEquals(4, new HashSet<>(CdeStyle.DEFAULT.backdrops()).size());
    }

    @Test
    void parse_readsBackWhatEncodedWrote() {
        final CdeStyle style = CdeStyle.DEFAULT.withPalette("Desert").withBackdrop(1, CdeBackdrop.DOTS)
                .withBackdrop(3, CdeBackdrop.PLAIN);
        assertEquals(style, CdeStyle.parse(style.encoded()));
        assertEquals("Desert;hatch,dots,tiles,plain", style.encoded());
    }

    @Test
    void parse_takesTheDefaultForAnythingItCannotRead() {
        assertEquals(CdeStyle.DEFAULT, CdeStyle.parse(null));
        assertEquals(CdeStyle.DEFAULT, CdeStyle.parse(""));
        assertEquals(CdeStyle.DEFAULT, CdeStyle.parse("Nobody's;no,such,thing,here"));
    }

    @Test
    void parse_keepsWhatItCanReadAndStartsTheRestAfresh() {
        final CdeStyle style = CdeStyle.parse("Neptune;dots,???");
        assertEquals("Neptune", style.palette());
        assertEquals(CdeBackdrop.DOTS, style.backdrop(0));
        assertEquals(CdeStyle.DEFAULT.backdrop(1), style.backdrop(1));
        assertEquals(CdeStyle.DEFAULT.backdrop(3), style.backdrop(3));
    }

    @Test
    void palette_isTheNameThePaletteIsListedUnderWhateverItWasTypedAs() {
        assertEquals("SeaFoam", CdeStyle.parse("seafoam").palette());
    }

    @Test
    void withBackdrop_changesThatWorkspaceAlone() {
        final CdeStyle changed = CdeStyle.DEFAULT.withBackdrop(2, CdeBackdrop.PLAIN);
        assertEquals(CdeBackdrop.PLAIN, changed.backdrop(2));
        for (final int other : new int[] {0, 1, 3}) {
            assertEquals(CdeStyle.DEFAULT.backdrop(other), changed.backdrop(other));
        }
        assertEquals(CdeStyle.DEFAULT.palette(), changed.palette());
    }

    @Test
    void backdrop_bringsAWorkspaceNoDesktopHasInsideWhatItHas() {
        assertEquals(CdeStyle.DEFAULT.backdrop(3), CdeStyle.DEFAULT.backdrop(9));
        assertEquals(CdeStyle.DEFAULT.backdrop(0), CdeStyle.DEFAULT.backdrop(-2));
    }

    @Test
    void backdrops_cannotBeChangedFromOutside() {
        final List<CdeBackdrop> given = new ArrayList<>(CdeStyle.DEFAULT.backdrops());
        final CdeStyle style = new CdeStyle("Default", given);
        given.set(0, CdeBackdrop.PLAIN);
        assertNotEquals(CdeBackdrop.PLAIN, style.backdrop(0));
    }

    @Test
    void encoded_neverRunsPastWhatIsKeptForIt() {
        for (final CdePalette palette : CdePalette.ALL) {
            for (final CdeBackdrop backdrop : CdeBackdrop.values()) {
                final CdeStyle style = new CdeStyle(palette.name(), List.of(backdrop, backdrop, backdrop, backdrop));
                assertTrue(style.encoded().length() <= CdeStyle.MOST_LETTERS, style.encoded());
            }
        }
    }

    @Test
    void backdrops_haveADifferentPlaceAndKeyEachAndOnlyPlainHasNoMask() {
        final Set<Integer> places = new HashSet<>();
        final Set<String> keys = new HashSet<>();
        for (final CdeBackdrop backdrop : CdeBackdrop.values()) {
            assertTrue(places.add(backdrop.place()), backdrop + " shares its place");
            assertTrue(keys.add(backdrop.key()), backdrop + " shares its key");
            assertEquals(backdrop, CdeBackdrop.keyed(backdrop.key()));
            assertEquals(backdrop == CdeBackdrop.PLAIN, backdrop.mask().isEmpty());
        }
        assertNull(CdeBackdrop.keyed("Hatch"));
    }
}
