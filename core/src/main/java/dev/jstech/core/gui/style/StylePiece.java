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
 * A piece every screen is cut from: a window, a button in each of its states, a slot, a field. A style draws each with
 * a sprite of the game or of a resource pack, a part of a texture, or flat colours; a piece a style does not name is
 * drawn flat from its colours.
 */
public enum StylePiece {

    /** A whole window, a machine's or a dialog's. */
    WINDOW,
    /** A raised panel inside a window. */
    PANEL,
    /** A sunken well, where a list or a picture sits. */
    WELL,
    /** An item slot, 18 pixels square with its frame. */
    SLOT,
    /** A button. */
    BUTTON,
    /** A button under the pointer. */
    BUTTON_HOVER,
    /** A button that cannot be pressed. */
    BUTTON_DISABLED,
    /** The button of a window's main action. */
    BUTTON_PRIMARY,
    /** A text field. */
    FIELD,
    /** A text field that has the keyboard. */
    FIELD_FOCUSED,
    /** A tab. */
    TAB,
    /** A tab under the pointer. */
    TAB_HOVER,
    /** The chosen tab. */
    TAB_ACTIVE,
    /** A check box, empty. */
    CHECKBOX,
    /** A check box, empty, under the pointer. */
    CHECKBOX_HOVER,
    /** A check box, ticked. */
    CHECKBOX_ON,
    /** A check box, ticked, under the pointer. */
    CHECKBOX_ON_HOVER,
    /** A slider's track. */
    SLIDER,
    /** A slider's track under the pointer. */
    SLIDER_HOVER,
    /** A slider's handle. */
    SLIDER_HANDLE,
    /** A slider's handle under the pointer. */
    SLIDER_HANDLE_HOVER,
    /** A switch turned off. */
    SWITCH_OFF,
    /** A switch turned on. */
    SWITCH_ON,
    /** A scroll bar's track. */
    SCROLL_TRACK,
    /** A scroll bar's thumb. */
    SCROLL_THUMB,
    /** A line between two parts. */
    SEPARATOR,
    /** A window floating over another, a pop-up or a menu. */
    POPUP,
    /** A tooltip. */
    TOOLTIP,
    /** A header strip. */
    HEADER,
    /** A status bar. */
    STATUS_BAR,
    /** A row under the pointer. */
    LIST_HOVER,
    /** A chosen row. */
    LIST_SELECTED,
    /** The empty part of a progress bar. */
    PROGRESS_TRACK,
    /** The filled part of a progress bar. */
    PROGRESS_FILL,
    /** An option button, not chosen. */
    RADIO,
    /** An option button under the pointer. */
    RADIO_HOVER,
    /** The open list of a drop-down. */
    POPUP_LIST,
    /** A chart's ground. */
    CHART,
    /** The ground of a screen drawn over the whole game, a menu's; the game's own blurs what is behind it. */
    MENU_GROUND,
    /** The ground of a list on a menu. */
    LIST_GROUND,
    /** The line over a menu's footer. */
    SEPARATOR_FOOTER;

    /** The name a style file sets it by. */
    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** The piece a style file names {@code key}, or none. */
    public static Optional<StylePiece> byKey(final String key) {
        for (final StylePiece piece : values()) {
            if (piece.key().equals(key)) {
                return Optional.of(piece);
            }
        }
        return Optional.empty();
    }
}
