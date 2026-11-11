/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os;

import java.util.Map;
import org.jetbrains.annotations.Nullable;

/**
 * Where a program is filed: Linux Mint's categories in the Software Manager, with Network and Desktops beside them, and
 * the category Gentoo's portage writes before a package's name ({@code media-gfx/paint}), so every system that lists
 * the Mirror's programs by kind files each one in the same place.
 */
public enum ProgramShelf {
    ACCESSORIES("app-misc"),
    GRAPHICS("media-gfx"),
    INTERNET("net-im"),
    OFFICE("app-office"),
    PROGRAMMING("dev-util"),
    SOUND_AND_VIDEO("media-sound"),
    SYSTEM_TOOLS("sys-apps"),
    NETWORK("net-misc"),
    GAMES("games-misc"),
    DESKTOPS("x11-wm");

    private final String portage;

    /* Where each program is filed, by its id's path; a desktop is with the desktops, a service with the network. */
    private static final Map<String, ProgramShelf> FILED = Map.ofEntries(
            Map.entry("ark", ACCESSORIES), Map.entry("screenfetch", ACCESSORIES), Map.entry("calculator", ACCESSORIES),
            Map.entry("paint", GRAPHICS), Map.entry("exposure", GRAPHICS),
            Map.entry("messenger", INTERNET), Map.entry("remote_control", INTERNET),
            Map.entry("exceed", OFFICE),
            Map.entry("virtual_studio_code", PROGRAMMING), Map.entry("sigma", PROGRAMMING),
            Map.entry("knot", PROGRAMMING), Map.entry("vim", PROGRAMMING), Map.entry("emacs", PROGRAMMING),
            Map.entry("soundfoundry", SOUND_AND_VIDEO),
            Map.entry("storage_insights", SYSTEM_TOOLS), Map.entry("automation_manager", SYSTEM_TOOLS),
            Map.entry("craft_planner", NETWORK),
            Map.entry("crypto_wallet", INTERNET), Map.entry("mining_panel", NETWORK), Map.entry("miner", NETWORK),
            Map.entry("minesweeper", GAMES), Map.entry("solitaire", GAMES), Map.entry("snake", GAMES));

    ProgramShelf(final String portage) {
        this.portage = portage;
    }

    /** The shelf a program is filed on: its own, else the desktops for a desktop and the network for a service. */
    public static ProgramShelf of(@Nullable final ProgramSpec spec) {
        if (spec == null) {
            return ACCESSORIES;
        }
        final ProgramShelf filed = FILED.get(spec.id().getPath());
        if (filed != null) {
            return filed;
        }
        if (spec.kind() == ProgramKind.DESKTOP_ENVIRONMENT) {
            return DESKTOPS;
        }
        return spec.kind().runsInBackground() ? NETWORK : ACCESSORIES;
    }

    /** The category Gentoo writes before a package's name on this shelf. */
    public String portage() {
        return this.portage;
    }
}
