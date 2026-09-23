/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.palette;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PaletteRolesTest {

    @Test
    void roles_areTheComponentsInTheOrderTheRecordDeclaresThem() {
        assertEquals(List.of("text", "accent", "edge"), PaletteRoles.roles(Swatch.class));
    }

    @Test
    void roles_refuseARecordThatHoldsAnythingButColours() {
        assertThrows(IllegalArgumentException.class, () -> PaletteRoles.roles(Labelled.class));
    }

    @Test
    void read_givesEveryRoleItsColour() {
        assertEquals(Map.of("text", 0xFF000000, "accent", 0xFF3A6AE0, "edge", 0x80FFFFFF),
                PaletteRoles.read(new Swatch(0xFF000000, 0xFF3A6AE0, 0x80FFFFFF)));
    }

    @Test
    void with_changesTheRolesNamedAndKeepsTheRest() {
        final Swatch base = new Swatch(0xFF000000, 0xFF3A6AE0, 0x80FFFFFF);
        assertEquals(new Swatch(0xFF000000, 0xFF12A26F, 0x80FFFFFF),
                PaletteRoles.with(base, Map.of("accent", 0xFF12A26F)));
    }

    @Test
    void with_ignoresARoleThePaletteDoesNotHave() {
        final Swatch base = new Swatch(1, 2, 3);
        assertEquals(base, PaletteRoles.with(base, Map.of("glow", 0xFFFFFFFF)));
    }

    @Test
    void format_writesAlphaFirstInCapitals() {
        assertEquals("#FF3A6AE0", PaletteRoles.format(0xFF3A6AE0));
        assertEquals("#0000FF00", PaletteRoles.format(0x0000FF00));
    }

    @Test
    void parse_readsBothLengthsAndTakesSixDigitsAsOpaque() {
        assertEquals(0xFF3A6AE0, PaletteRoles.parse("#3a6ae0"));
        assertEquals(0x803A6AE0, PaletteRoles.parse("#803A6AE0"));
    }

    @Test
    void parse_givesNothingForWhatIsNoColour() {
        assertNull(PaletteRoles.parse("3A6AE0"));
        assertNull(PaletteRoles.parse("#3A6AE"));
        assertNull(PaletteRoles.parse("#GGGGGG"));
        assertNull(PaletteRoles.parse(null));
    }

    @Test
    void formatThenParse_comesBackToTheSameColour() {
        assertEquals(0x14FFFFFF, PaletteRoles.parse(PaletteRoles.format(0x14FFFFFF)));
    }

    /** A palette of three roles. */
    private record Swatch(int text, int accent, int edge) {
    }

    /** Not a palette: it holds a word beside its colour. */
    private record Labelled(int colour, String name) {
    }
}
