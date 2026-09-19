/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os;

/**
 * The chrome family a desktop environment draws with: where its panel sits, what its launcher menu looks
 * like, and how its windows behave. The desktop screen renders one of these; an add-on desktop environment
 * picks the closest style and brings its own skin, names and icons.
 */
public enum PanelStyle {

    /** Bottom grey bevelled taskbar, classic side-band Start list (Frames 95). */
    FRAMES_95(false),

    /** Bottom Luna taskbar, two-column Start menu (Frames XP). */
    FRAMES_XP(false),

    /** Bottom dark centred taskbar, floating Start with search and a pinned grid (Frames 11). */
    FRAMES_11(false),

    /** Bottom dark panel with a Kickoff-style two-pane launcher (KDE Plasma). */
    KDE(true),

    /** Top bar with an Activities overview: search, workspaces, app grid and a dash (GNOME). */
    GNOME(true),

    /** Bottom panel with a favourites-rail / categories / apps menu (Cinnamon). */
    CINNAMON(true),

    /**
     * A Front Panel at the bottom centre with the four workspaces in its middle, no list of open windows at
     * all (a minimised window is an icon on its workspace), and the applications found through a manager that
     * opens like a folder (CDE).
     */
    CDE(true);

    /**
     * Whether the desktop stands on a system of a Unix family. Every style says so for itself, so that a
     * program which has to know (a terminal, a file dialog) asks here instead of keeping a list of its own
     * that the next desktop is missing from.
     */
    private final boolean unixLike;

    PanelStyle(final boolean unixLike) {
        this.unixLike = unixLike;
    }

    /** Whether what runs on this desktop speaks a Unix shell and sees one tree from {@code /}. */
    public boolean unixLike() {
        return this.unixLike;
    }
}
