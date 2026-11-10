/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma;

import java.util.List;
import java.util.Map;

/**
 * What a desktop a player wrote says it is (computers/docs/design/player-systems.md): its name, version and author, and
 * its look, which the mod's own desktops are drawn with too: the word on its menu button, where its panel is, the
 * style of its window frames, its colours, its icons and wallpaper from image files on the disk, and its programs.
 *
 * <p>Read by the compiler from the desktop's properties, which are written with an arrow and values the compiler
 * knows. This record is pure and carries no Minecraft dependency.
 *
 * @param name      the package's name: "lantern-desktop"
 * @param version   its version, as written
 * @param author    who wrote it
 * @param menu      the word on its menu button
 * @param panelAt   where its panel runs: "top" or "bottom"
 * @param frames    the style of its window frames, one of {@link #FRAMES}
 * @param colours   its colours, each by its role in {@link #COLOURS}, as "#RRGGBB"
 * @param icons     the folder of its programs' icons, a picture for each named after the program
 * @param wallpaper the picture its desktop is papered with
 * @param programs  its programs, by name, in their order on its menu
 */
public record DesktopFacts(String name, String version, String author, String menu, String panelAt, String frames,
                           Map<String, String> colours, String icons, String wallpaper, List<String> programs) {

    /** Where a desktop's panel may run. */
    public static final List<String> PANEL_PLACES = List.of("top", "bottom");
    /** The styles of window frame a desktop may wear, the mod's own desktops' styles. */
    public static final List<String> FRAMES = List.of("flat", "bevel", "luna", "aero", "metro", "kde2", "gnome1",
            "oxygen", "clearlooks", "motif", "platinum");
    /** The colours a desktop names, by the property each is written in. */
    public static final List<String> COLOURS = List.of("DesktopColour", "PanelColour", "TextColour", "AccentColour",
            "WindowColour", "TitleColour");

    public DesktopFacts {
        colours = Map.copyOf(colours);
        programs = List.copyOf(programs);
    }

    /** The desktop as one line: "lantern-desktop 1.2". */
    public String label() {
        return this.name + " " + this.version;
    }

    /** Whether {@code written} is a colour as a desktop writes one: "#RRGGBB". */
    public static boolean isColour(final String written) {
        return written.matches("#[0-9A-Fa-f]{6}");
    }
}
