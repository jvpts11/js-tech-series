/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.theme;

import dev.jstech.core.JsCore;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.tier.HardwareEra;
import org.jetbrains.annotations.Nullable;

/**
 * The registry of concrete per-era GUI skins, plus the {@link HardwareEra} to {@link EraTheme} mapping a screen uses
 * to pick its skin. STANDARD is frozen: its palette is the exact set of values the original flat-dark theme shipped
 * with, and its style is flat, so a STANDARD-era (or no-board / unknown-era) screen renders pixel-for-pixel as before.
 *
 * <p>Each skin's colours are a declared palette, {@code jscore:era/<era>}, which a resource pack can recolour; the
 * style (which overlays an era draws) is the skin's own.
 *
 * <p>Square corners only, no rounded geometry, in every era; the eras differ by colour first and by a restrained
 * square overlay (scanlines, bevel, glass bands, an accent line) only where it reads as period-correct. The palettes
 * are starting points, tunable in playtesting.
 */
@PaletteHolder
public final class EraThemes {

    private static final float SMALL = 0.75f;

    /** Frozen copy of the original flat-dark constants; STANDARD must stay byte-identical to today. */
    public static final Palette<EraPalette> STANDARD_COLOURS = Palettes.declare(JsCore.MODID, "era/standard",
            new EraPalette(
                    0xFF05070A, 0xFF0B0E13, 0xFF0E131A, 0xFF11161D, 0xFF1D2530, 0xFF0A0E14,
                    0xFF0A0D12, 0xFF1C2531,
                    0xFF39D6C4, 0xFF2AA7E0,
                    0xFF5FE07A, 0xFFF0B23A, 0xFFEF6A5A,
                    0xFFCDD6E2, 0xFF7D8A9C,
                    0xFF15212A, 0xFF39D6C4, 0xFF1A2937,
                    0, 0, 0, 0, 0));

    /** Green-phosphor CRT: near-black screen, phosphor green text/accents, amber cautions, square scanlines. */
    public static final Palette<EraPalette> VINTAGE_COLOURS = Palettes.declare(JsCore.MODID, "era/vintage",
            new EraPalette(
                    0xFF000000, 0xFF020A02, 0xFF031004, 0xFF051405, 0xFF1E5A1E, 0xFF030D03,
                    0xFF020A02, 0xFF1E5A1E,
                    0xFF33FF66, 0xFF66FF99,
                    0xFF33FF66, 0xFFFFB000, 0xFFFF6655,
                    0xFF66FF66, 0xFF2E8B2E,
                    0xFF0A2A0A, 0xFF33FF66, 0xFF103810,
                    0, 0, 0x2200FF00, 0, 0));

    /**
     * Early-PC beige/blue chrome: classic system-blue accents, dark text on warm-cream backgrounds, cream-white bevel
     * highlights. The palette targets Windows 3.1 / early-90s PC BIOS aesthetics.
     */
    public static final Palette<EraPalette> LEGACY_COLOURS = Palettes.declare(JsCore.MODID, "era/legacy",
            new EraPalette(
                    0xFF808070, 0xFFC8C4B0, 0xFFB8B4A0, 0xFFD6D2C0, 0xFF6E6A58, 0xFF969280,
                    0xFFE4E0D0, 0xFF8A8676,
                    0xFF1A3C8C, 0xFF2E5AB8,
                    0xFF1E7A2E, 0xFFB8860B, 0xFFA01818,
                    0xFF1A1A14, 0xFF5A5648,
                    0xFF1A3C8C, 0xFFFFFFF0, 0xFFD4D0C0,
                    0xFFFFFFF0, 0xFF6E6A58, 0, 0, 0));

    /**
     * The glass desktops of the late 2000s, square as every window of the series: a deep navy screen, sky-blue
     * accents, and headers, buttons and the selected tab in two bands, a sheen of the accent over the upper half.
     */
    public static final Palette<EraPalette> TRANSITION_COLOURS = Palettes.declare(JsCore.MODID, "era/transition",
            new EraPalette(
                    0xFF07111F, 0xFF0D1B2E, 0xFF10223A, 0xFF132A47, 0xFF2C5584, 0xFF0A1628,
                    0xFF0A1628, 0xFF335D8C,
                    0xFF5AB4FF, 0xFF8FD0FF,
                    0xFF6FE08A, 0xFFF0C24A, 0xFFFF6A5A,
                    0xFFE6F0FA, 0xFF8AA4C2,
                    0xFF1F4A7A, 0xFFE6F0FA, 0xFF1D3C62,
                    0, 0, 0, 0, 0x335AB4FF));

    /**
     * The light flat look of the Advanced era: a light grey screen, white panels, dark text, a deep blue accent, and
     * a thin line of that accent under each header.
     */
    public static final Palette<EraPalette> ADVANCED_COLOURS = Palettes.declare(JsCore.MODID, "era/advanced",
            new EraPalette(
                    0xFFC9CED4, 0xFFF3F3F3, 0xFFEBEBEB, 0xFFFFFFFF, 0xFFD6D9DD, 0xFFF7F7F7,
                    0xFFE9ECEF, 0xFFC3C8CE,
                    0xFF0067C0, 0xFF3A8EE0,
                    0xFF0F7B0F, 0xFF9D5D00, 0xFFC42B1C,
                    0xFF1B1B1B, 0xFF5F6670,
                    0xFFE5F0FB, 0xFF0067C0, 0xFFEEF4FB,
                    0, 0, 0, 0, 0));

    /** The original flat-dark skin. */
    public static final EraTheme STANDARD = new EraTheme(STANDARD_COLOURS, EraStyle.flat(SMALL));

    /** Green-phosphor CRT, finished with square scanlines. */
    public static final EraTheme VINTAGE = new EraTheme(VINTAGE_COLOURS, EraStyle.flat(SMALL).withScanlines());

    /** Early-PC chrome: raised buttons, sunken display panels. */
    public static final EraTheme LEGACY = new EraTheme(LEGACY_COLOURS, EraStyle.flat(SMALL).withDoubleBevel());

    /** Navy glass: two-band headers, buttons and tabs over a line of the accent. */
    public static final EraTheme TRANSITION = new EraTheme(TRANSITION_COLOURS,
            EraStyle.flat(SMALL).withGlassBands().withAccentRule());

    /** Light and flat, with a thin accent line under each header. */
    public static final EraTheme ADVANCED = new EraTheme(ADVANCED_COLOURS, EraStyle.flat(SMALL).withAccentRule());

    private EraThemes() {
    }

    /**
     * The skin for a given hardware era. The enum is closed, so the switch is exhaustive. The Exa and Singularity
     * eras have no content yet, so they wear the skin of the Advanced era, the nearest to them, until they do.
     */
    public static EraTheme of(final HardwareEra era) {
        return switch (era) {
            case VINTAGE -> VINTAGE;
            case LEGACY -> LEGACY;
            case TRANSITION -> TRANSITION;
            case STANDARD -> STANDARD;
            case ADVANCED, EXA, SINGULARITY -> ADVANCED;
        };
    }

    /**
     * The skin for a possibly-absent era. A screen with no valid build (no board installed) or no era available falls
     * back to STANDARD, so the GUI always has a defined skin and the default look never changes.
     */
    public static EraTheme ofNullable(@Nullable final HardwareEra era) {
        return era == null ? STANDARD : of(era);
    }
}
