/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ThemePresetTest {

    @Test
    void byId_readsAnyCaseAndTakesAnUnknownNameForTheSystemsOwn() {
        assertEquals(ThemePreset.OCEAN, ThemePreset.byId(" Ocean "));
        assertEquals(ThemePreset.SYSTEM, ThemePreset.byId("sepia"));
        assertEquals(ThemePreset.SYSTEM, ThemePreset.byId(null));
    }

    @Test
    void applyTo_keepsTheNameAndSetsTheAccent() {
        final ComputerSettings settings = new ComputerSettings();
        ThemePreset.SLATE.applyTo(settings);
        assertEquals("slate", settings.themePreset());
        assertEquals(0xFF7B52C9, settings.accent());
    }

    @Test
    void applyTo_systemClearsTheNameAndTheAccent() {
        final ComputerSettings settings = new ComputerSettings();
        ThemePreset.OCEAN.applyTo(settings);
        ThemePreset.SYSTEM.applyTo(settings);
        assertEquals("", settings.themePreset());
        assertEquals(0, settings.accent());
    }

    @Test
    void wallpaper_isWhatTheDesktopHangsWithIt() {
        assertEquals("winxp", ThemePreset.OCEAN.wallpaper());
        assertEquals("", ThemePreset.SYSTEM.wallpaper());
    }
}
