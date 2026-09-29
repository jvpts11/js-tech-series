/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.theme;

import dev.jstech.core.gui.ColorContrast;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.tier.HardwareEra;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure-logic checks for the era-to-theme selection and the frozen STANDARD palette. No Minecraft types are touched:
 * the draw helpers (which take GuiGraphics) are never linked here, only the palette/selection are exercised.
 */
class EraThemeTest {

    // The STANDARD palette is frozen; these are the exact values the original flat-dark theme shipped with.
    private static final int STD_OUTER = 0xFF05070A;
    private static final int STD_SCREEN = 0xFF0B0E13;
    private static final int STD_PANEL = 0xFF11161D;
    private static final int STD_LINE = 0xFF1D2530;
    private static final int STD_ACCENT = 0xFF39D6C4;
    private static final int STD_GREEN = 0xFF5FE07A;
    private static final int STD_AMBER = 0xFFF0B23A;
    private static final int STD_RED = 0xFFEF6A5A;
    private static final int STD_TEXT = 0xFFCDD6E2;
    private static final int STD_DIM = 0xFF7D8A9C;

    /** The contrast body text needs to read, and the lower one labels, accents and status words need. */
    private static final double BODY = 4.5;
    private static final double SECONDARY = 3.0;

    @Test
    void of_standard_returnsTheStandardSingleton() {
        assertSame(EraThemes.STANDARD, EraThemes.of(HardwareEra.STANDARD));
    }

    @Test
    void of_standard_keyColorsEqualTheFrozenStandardValues() {
        final EraTheme t = EraThemes.of(HardwareEra.STANDARD);
        assertEquals(STD_OUTER, t.outer());
        assertEquals(STD_SCREEN, t.screen());
        assertEquals(STD_PANEL, t.panel());
        assertEquals(STD_LINE, t.line());
        assertEquals(STD_ACCENT, t.accent());
        assertEquals(STD_GREEN, t.green());
        assertEquals(STD_AMBER, t.amber());
        assertEquals(STD_RED, t.red());
        assertEquals(STD_TEXT, t.text());
        assertEquals(STD_DIM, t.dim());
    }

    @Test
    void of_standard_smallFontScaleIsThreeQuarters() {
        assertEquals(0.75f, EraThemes.of(HardwareEra.STANDARD).small());
    }

    @Test
    void of_standard_hasNoOverlays() {
        final EraTheme t = EraThemes.of(HardwareEra.STANDARD);
        final EraStyle s = t.style();
        assertEquals(false, s.scanlines());
        assertEquals(false, s.doubleBevel());
        assertEquals(false, s.glowAccent());
        assertEquals(0, t.palette().scanline());
        assertEquals(0, t.palette().bevelLight());
        assertEquals(0, t.palette().bevelDark());
        assertEquals(0, t.palette().glow());
    }

    @Test
    void of_vintage_differsFromStandard() {
        final EraTheme vintage = EraThemes.of(HardwareEra.VINTAGE);
        assertNotEquals(EraThemes.STANDARD, vintage);
        // A green-phosphor CRT inverts the accent/text away from the cyan flat-dark values.
        assertNotEquals(STD_ACCENT, vintage.accent());
        assertNotEquals(STD_TEXT, vintage.text());
        assertNotEquals(STD_SCREEN, vintage.screen());
    }

    @Test
    void of_vintage_enablesScanlinesWithAColor() {
        final EraTheme vintage = EraThemes.of(HardwareEra.VINTAGE);
        assertEquals(true, vintage.style().scanlines());
        assertNotEquals(0, vintage.palette().scanline());
    }

    @Test
    void everySkin_isColouredByADeclaredPalette() {
        // Reading a skin loads the class that declares the palettes, before the declared ones are asked for.
        assertSame(EraThemes.STANDARD_COLOURS.get(), EraThemes.STANDARD.palette());
        final Set<String> eras = Palettes.of("jscore").stream()
                .map(Palette::id)
                .filter(id -> id.startsWith("jscore:era/"))
                .collect(Collectors.toSet());
        assertEquals(Set.of("jscore:era/standard", "jscore:era/vintage", "jscore:era/legacy", "jscore:era/transition",
                "jscore:era/advanced"), eras);
    }

    @Test
    void of_transition_isNavyGlassInTwoBands() {
        final EraTheme transition = EraThemes.of(HardwareEra.TRANSITION);
        assertSame(EraThemes.TRANSITION, transition);
        assertEquals(true, transition.style().glassBands());
        assertEquals(true, transition.style().accentRule());
        assertEquals(false, transition.style().doubleBevel());
        assertNotEquals(0, transition.palette().sheen());
        // The sheen is a light laid over the band, never a solid colour that would hide what is under it.
        assertTrue((transition.palette().sheen() >>> 24) < 0x80);
    }

    @Test
    void of_advanced_isLightAndFlatWithAnAccentLine() {
        final EraTheme advanced = EraThemes.of(HardwareEra.ADVANCED);
        assertSame(EraThemes.ADVANCED, advanced);
        assertEquals(true, advanced.style().accentRule());
        assertEquals(false, advanced.style().glassBands());
        assertEquals(0, advanced.palette().sheen());
        // A light era: dark text over a lighter screen.
        assertTrue(ColorContrast.luminance(advanced.screen()) > ColorContrast.luminance(advanced.text()));
    }

    @Test
    void newEraSkins_textIsReadableOnEveryGround() {
        for (final EraTheme skin : new EraTheme[]{EraThemes.TRANSITION, EraThemes.ADVANCED}) {
            final EraPalette p = skin.palette();
            assertReadable(p.text(), p.screen(), BODY);
            assertReadable(p.text(), p.panel(), BODY);
            assertReadable(p.text(), p.hover(), BODY);
            assertReadable(p.tabLabelOn(), p.tabOn(), BODY);
            assertReadable(p.dim(), p.panel(), SECONDARY);
            assertReadable(p.dim(), p.screen(), SECONDARY);
        }
    }

    @Test
    void newEraSkins_statusAndAccentColoursReadOnAPanel() {
        for (final EraTheme skin : new EraTheme[]{EraThemes.TRANSITION, EraThemes.ADVANCED}) {
            final EraPalette p = skin.palette();
            assertReadable(p.accent(), p.panel(), SECONDARY);
            assertReadable(p.accent2(), p.panel(), SECONDARY);
            assertReadable(p.green(), p.panel(), SECONDARY);
            assertReadable(p.amber(), p.panel(), SECONDARY);
            assertReadable(p.red(), p.panel(), SECONDARY);
        }
    }

    @Test
    void transition_textStaysReadableOnTheLighterBand() {
        // The upper band is the selected tab's colour with the sheen over it, the lightest ground the skin draws.
        final EraPalette p = EraThemes.TRANSITION.palette();
        final int band = over(p.tabOn(), p.sheen());
        assertReadable(p.tabLabelOn(), band, BODY);
        assertReadable(p.text(), over(p.panel(), p.sheen()), BODY);
        assertReadable(p.text(), over(p.hover(), p.sheen()), BODY);
    }

    @Test
    void of_legacy_usesDarkTextOnLight() {
        // Legacy is the contrast-inverted era: near-black text over a light beige screen.
        final EraTheme legacy = EraThemes.of(HardwareEra.LEGACY);
        assertEquals(true, legacy.style().doubleBevel());
        assertNotEquals(STD_TEXT, legacy.text());
    }

    @Test
    void everyEra_tabLabelOnDiffersFromTabOn() {
        /*
         * tabLabelOn must contrast with tabOn: if they were the same, labels drawn on a selected tab
         * would be invisible. Verified as a structural invariant: tabLabelOn != tabOn for every era.
         */
        for (final HardwareEra era : HardwareEra.values()) {
            final EraTheme t = EraThemes.of(era);
            assertNotEquals(t.tabOn(), t.tabLabelOn(),
                    "Era " + era + ": tabLabelOn must differ from tabOn");
        }
    }

    @Test
    void everyEra_resolvesToANonNullTheme() {
        for (final HardwareEra era : HardwareEra.values()) {
            assertNotEquals(null, EraThemes.of(era));
        }
    }

    @Test
    void of_erasWithNoContentYet_wearTheAdvancedSkin() {
        // Exa and Singularity ship no content yet, so they wear the skin of the era nearest to them.
        assertSame(EraThemes.ADVANCED, EraThemes.of(HardwareEra.EXA));
        assertSame(EraThemes.ADVANCED, EraThemes.of(HardwareEra.SINGULARITY));
    }

    @Test
    void ofNullable_null_fallsBackToStandard() {
        assertSame(EraThemes.STANDARD, EraThemes.ofNullable(null));
    }

    @Test
    void ofNullable_presentEra_matchesOf() {
        for (final HardwareEra era : HardwareEra.values()) {
            assertSame(EraThemes.of(era), EraThemes.ofNullable(era));
        }
    }

    private static void assertReadable(final int fg, final int bg, final double min) {
        final double ratio = ColorContrast.ratio(fg, bg);
        assertTrue(ratio >= min, String.format("contrast %.2f below %.1f for fg=%06X bg=%06X",
                ratio, min, fg & 0xFFFFFF, bg & 0xFFFFFF));
    }

    /** An opaque colour with a translucent one painted over it, the way the screen blends a fill. */
    private static int over(final int base, final int layer) {
        final double alpha = (layer >>> 24) / 255.0;
        int out = 0xFF000000;
        for (int shift = 0; shift <= 16; shift += 8) {
            final int under = base >> shift & 0xFF;
            final int top = layer >> shift & 0xFF;
            out |= (int) Math.round(under + (top - under) * alpha) << shift;
        }
        return out;
    }
}
