/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.core.gui.ColorContrast;
import dev.jstech.core.tier.HardwareEra;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The IQL Server Management Studio's four looks stay readable: every text it draws stands out from the ground it is
 * drawn on, in every age. The studio this replaced shipped unreadable text twice, which is why each pairing is
 * checked here before a look reaches the game.
 */
class IsmsLookTest {

    private static final double BODY = 4.5;
    private static final double SECONDARY = 3.0;
    private static final List<IsmsLook> LOOKS = List.of(IsmsLook.QUERY_ANALYZER, IsmsLook.STUDIO_2008,
            IsmsLook.STUDIO_2012, IsmsLook.STUDIO_2022);

    @Test
    void of_givesEachAgeItsStudio() {
        assertEquals(IsmsLook.QUERY_ANALYZER, IsmsLook.of(HardwareEra.LEGACY));
        assertEquals(IsmsLook.STUDIO_2008, IsmsLook.of(HardwareEra.TRANSITION));
        assertEquals(IsmsLook.STUDIO_2012, IsmsLook.of(HardwareEra.STANDARD));
        assertEquals(IsmsLook.STUDIO_2022, IsmsLook.of(HardwareEra.ADVANCED));
        assertEquals(IsmsLook.STUDIO_2012, IsmsLook.of(null));
    }

    @Test
    void colours_keepTheEditorAndTheGridReadable() {
        for (final IsmsLook look : LOOKS) {
            final IsmsLook.Colours c = look.colours();
            assertReadable(look, c.text(), c.panel(), BODY);
            assertReadable(look, c.text(), c.select(), BODY);
            assertReadable(look, c.dim(), c.panel(), SECONDARY);
            assertReadable(look, c.accent(), c.panel(), SECONDARY);
            assertReadable(look, c.bad(), c.panel(), SECONDARY);
            assertReadable(look, c.good(), c.panel(), SECONDARY);
        }
    }

    @Test
    void colours_keepTheBarsReadable() {
        for (final IsmsLook look : LOOKS) {
            final IsmsLook.Colours c = look.colours();
            assertReadable(look, c.statusText(), c.status(), SECONDARY);
            assertReadable(look, c.statusText(), c.statusTo(), SECONDARY);
            assertReadable(look, c.headText(), c.head(), BODY);
            assertReadable(look, c.headText(), c.headTo(), SECONDARY);
            assertReadable(look, c.text(), c.button(), BODY);
            assertReadable(look, c.text(), c.menu(), BODY);
        }
    }

    @Test
    void colours_keepTheTabsReadable() {
        for (final IsmsLook look : LOOKS) {
            final IsmsLook.Colours c = look.colours();
            final int activeGround = c.tabActive() != 0 ? c.tabActive() : c.tabs();
            assertReadable(look, c.tabActiveText(), activeGround, BODY);
            assertReadable(look, c.tabText(), c.tabs(), SECONDARY);
            assertReadable(look, c.tabText(), c.tabsTo(), SECONDARY);
        }
    }

    @Test
    void colours_keepAMenuItemReadableUnderTheMouse() {
        for (final IsmsLook look : LOOKS) {
            assertReadable(look, 0xFFFFFFFF, look.colours().menuLit(), BODY);
        }
    }

    private static void assertReadable(final IsmsLook look, final int fg, final int bg, final double min) {
        final double ratio = ColorContrast.ratio(fg, bg);
        assertTrue(ratio >= min, String.format("%s: contrast %.2f below %.1f for fg=%06X bg=%06X", look, ratio, min,
                fg & 0xFFFFFF, bg & 0xFFFFFF));
    }
}
