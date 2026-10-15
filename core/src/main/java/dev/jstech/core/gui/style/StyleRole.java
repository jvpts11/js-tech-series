/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui.style;

import java.util.Locale;
import java.util.Optional;

/**
 * A colour a style gives a name to: a ground, a line, a text, a mark. A style file sets them by these names
 * ({@code "slot_edge": "#FF1C2531"}), or takes them all from a palette whose roles have the same names, and a component
 * paints with the role, never with a colour of its own.
 */
public enum StyleRole {

    /** The line just outside a window. */
    OUTER(null),
    /** A window's ground. */
    SCREEN(null),
    /** A raised strip along a window's side. */
    RAIL(null),
    /** A panel's ground. */
    PANEL(null),
    /** A separator, and the top edge of a panel. */
    LINE(null),
    /** The empty part of a bar. */
    TRACK(null),
    /** The inside of a slot. */
    SLOT_BG(null),
    /** The frame of a slot. */
    SLOT_EDGE(null),
    /** The accent: selections, marks, fills. */
    ACCENT(null),
    /** A second accent. */
    ACCENT2(null),
    /** What is well. */
    GREEN(null),
    /** What needs a look. */
    AMBER(null),
    /** What is wrong. */
    RED(null),
    /** The main text. */
    TEXT(null),
    /** Text that says less: captions, hints. */
    DIM(null),
    /** The ground of a chosen tab. */
    TAB_ON(null),
    /** The text on a chosen tab. */
    TAB_LABEL_ON(null),
    /** The ground of what the pointer rests on. */
    HOVER(null),
    /** The lit edge of a raised surface. */
    BEVEL_LIGHT(null),
    /** The shaded edge of a raised surface. */
    BEVEL_DARK(null),
    /** The faint lines of a screen's scanlines. */
    SCANLINE(null),
    /** The faint halo of an accent. */
    GLOW(null),
    /** The light over the upper half of a glass surface. */
    SHEEN(null),
    /** A window's title. */
    TITLE(TEXT),
    /** The text on a button. */
    BUTTON_TEXT(TEXT),
    /** The text on a button that cannot be pressed. */
    BUTTON_TEXT_DISABLED(DIM),
    /** The text typed in a field. */
    FIELD_TEXT(TEXT),
    /** A tooltip's ground. */
    TOOLTIP_GROUND(PANEL),
    /** A tooltip's frame. */
    TOOLTIP_EDGE(ACCENT),
    /** The ground of chosen text and rows. */
    SELECTION(ACCENT),
    /** A link. */
    LINK(ACCENT2);

    private final StyleRole fallback;

    StyleRole(final StyleRole fallback) {
        this.fallback = fallback;
    }

    /** The name a style file sets it by. */
    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** The role it takes the colour of when a style does not set it, or none when every style must. */
    public Optional<StyleRole> fallback() {
        return Optional.ofNullable(fallback);
    }

    /** The role a style file names {@code key}, or none. */
    public static Optional<StyleRole> byKey(final String key) {
        for (final StyleRole role : values()) {
            if (role.key().equals(key)) {
                return Optional.of(role);
            }
        }
        return Optional.empty();
    }
}
