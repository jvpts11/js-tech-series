/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program;

import java.util.Locale;

/**
 * The themes a machine's desktop can be dressed in at once: each is an accent and a wallpaper, so picking one
 * restyles the whole desktop instead of one knob of it. The order is the order they are offered in.
 */
public enum ThemePreset {

    /** What the system comes with: its own accent and its own wallpaper. */
    SYSTEM("system", 0, ""),

    /** A green accent over the sky and the hill. */
    OCEAN("ocean", 0xFF12A26F, "winxp"),

    /** A violet accent over the deep blue bloom. */
    SLATE("slate", 0xFF7B52C9, "win11");

    private final String id;
    /** The accent it sets, {@code 0} for the skin's own. */
    private final int accent;
    /** The wallpaper it hangs, by the id a machine keeps its wallpaper under; empty for the desktop's own. */
    private final String wallpaper;

    ThemePreset(final String id, final int accent, final String wallpaper) {
        this.id = id;
        this.accent = accent;
        this.wallpaper = wallpaper;
    }

    /** The preset by its id, case aside; anything that names none is the system's own. */
    public static ThemePreset byId(final String id) {
        final String wanted = id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
        for (final ThemePreset preset : values()) {
            if (preset.id.equals(wanted)) {
                return preset;
            }
        }
        return SYSTEM;
    }

    public String id() {
        return this.id;
    }

    public String wallpaper() {
        return this.wallpaper;
    }

    /** Sets what of it the settings store keeps: its name, and its accent. The wallpaper is the desktop's. */
    public void applyTo(final ComputerSettings settings) {
        settings.setThemePreset(this == SYSTEM ? "" : this.id);
        settings.setAccent(this.accent);
    }
}
