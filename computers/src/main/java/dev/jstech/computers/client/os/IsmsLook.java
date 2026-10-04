/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.tier.HardwareEra;
import org.jetbrains.annotations.Nullable;

/**
 * How the IQL Server Management Studio looks on each age of the computer it runs on, the way the real studio looked
 * in each of its versions: the grey Query Analyzer of 2000 with its Object Browser, the blue studio of 2008, the
 * light one of 2012 and the lavender one of 2022. Every age has the same functions; only the dress and a few names
 * change.
 *
 * <p>Each look's colours are a palette of their own, {@code jsc:app/isms/<look>}, so a resource pack can repaint
 * one age without touching the others. A colour with a second shade ({@code menuTo}, {@code toolbarTo} and the
 * like) is drawn as a gradient from the first to the second; the flat looks give both the same value. Pure: the
 * colours are plain data, so their contrast is tested without the game.
 */
@PaletteHolder
enum IsmsLook {

    /** The Legacy age's IQL Query Analyzer, of the Midsoft IQL Server 2000. */
    QUERY_ANALYZER(Palettes.declare(JsComputers.MODID, "app/isms/query_analyzer", new Colours(
            0xFFD4D0C8, 0xFFFFFFFF, 0xFFD4D0C8, 0xFFD4D0C8, 0xFFD4D0C8, 0xFFD4D0C8, 0xFF808080,
            0xFFD4D0C8, 0xFF404040, 0xFFFFFFFF, 0xFF808080,
            0xFF000000, 0xFF808080, 0xFF0A246A, 0xFFCCE8FF, 0xFF9BC9F0,
            0xFFD4D0C8, 0xFFD4D0C8, 0xFF000000,
            0xFF0A246A, 0xFF3A6EA5, 0xFFDDDDEE, 0, 0xFFFFFFFF, 0,
            0xFFD4D0C8, 0xFF808080,
            0xFFD4D0C8, 0xFFD4D0C8, 0xFF000000, 0xFF808080, 0xFF2E8B2E,
            0xFF1E7A1E, 0xFFC02020, 0xFFB07000, 0xFFFFFFFF, 0xFFFFFFFF, 0xFF2FAA46, 0xFF0A246A, 0xFFE4EEF8))),
    /** The Transition age's studio, of the Midsoft IQL Server 2008. */
    STUDIO_2008(Palettes.declare(JsComputers.MODID, "app/isms/studio_2008", new Colours(
            0xFFE9EEF6, 0xFFFFFFFF, 0xFFF4F7FB, 0xFFDFE8F3, 0xFFF7FAFF, 0xFFD6E2F2, 0xFF9FB3CF,
            0xFFEEF3FA, 0xFF9FB3CF, 0xFFFFFFFF, 0xFF9FB3CF,
            0xFF1E1E1E, 0xFF6B7A90, 0xFF1E3287, 0xFFCCE8FF, 0xFF9BC9F0,
            0xFFC9DBF2, 0xFFA9C2E6, 0xFF1E3287,
            0xFFD6E2F2, 0xFFBFD2EC, 0xFF3A4A60, 0xFFFFFFFF, 0xFF1E1E1E, 0xFFFFC83C,
            0xFFE9EEF6, 0xFF9FB3CF,
            0xFFD6E2F2, 0xFFB8CBE6, 0xFF1E3287, 0xFF9FB3CF, 0xFF3C9C3C,
            0xFF1E7A1E, 0xFFC02020, 0xFFB07000, 0xFFF6F6F6, 0xFF9AA0A6, 0xFF2FAA46, 0xFF2F6A9C, 0xFFE5F0FB))),
    /** The Standard age's studio, of the Midsoft IQL Server 2012. */
    STUDIO_2012(Palettes.declare(JsComputers.MODID, "app/isms/studio_2012", new Colours(
            0xFFECECEC, 0xFFFFFFFF, 0xFFF5F5F5, 0xFFF5F5F5, 0xFFEEF0F2, 0xFFEEF0F2, 0xFFC4C4C4,
            0xFFF8F8F8, 0xFFC4C4C4, 0xFFF8F8F8, 0xFFC4C4C4,
            0xFF1E1E1E, 0xFF8A8A8A, 0xFF0A6CBA, 0xFFCCE8FF, 0xFF9BC9F0,
            0xFFF0F0F0, 0xFFF0F0F0, 0xFF444444,
            0xFFE9EEF4, 0xFFE9EEF4, 0xFF555555, 0xFFFFFFFF, 0xFF1E1E1E, 0xFFF0A64E,
            0xFFE4E4E4, 0xFFCFCFCF,
            0xFF007ACC, 0xFF007ACC, 0xFFFFFFFF, 0xFF005A9E, 0xFF9FE0A0,
            0xFF1E7A1E, 0xFFC02020, 0xFFB07000, 0xFFF6F6F6, 0xFF9AA0A6, 0xFF2FAA46, 0xFF0A6CBA, 0xFFE5F3FF))),
    /** The Advanced age's studio, of the Midsoft IQL Server 2022. */
    STUDIO_2022(Palettes.declare(JsComputers.MODID, "app/isms/studio_2022", new Colours(
            0xFFCFD6E5, 0xFFFFFFFF, 0xFFCFD6E5, 0xFFCFD6E5, 0xFFCFD6E5, 0xFFCFD6E5, 0xFFCFD6E5,
            0xFFE6E9F0, 0xFF8E9BBC, 0xFFE6E9F0, 0xFF8E9BBC,
            0xFF1E1E1E, 0xFF6D7280, 0xFF0A6CBA, 0xFFCCE8FF, 0xFF9BC9F0,
            0xFFCFD6E5, 0xFFCFD6E5, 0xFF1E1E1E,
            0xFFCFD6E5, 0xFFCFD6E5, 0xFF3A3A3A, 0xFFFFF29D, 0xFF1E1E1E, 0xFFFFF29D,
            0xFFCFD6E5, 0xFFCFD6E5,
            0xFF007ACC, 0xFF007ACC, 0xFFFFFFFF, 0xFF007ACC, 0xFF9FE0A0,
            0xFF1E7A1E, 0xFFC02020, 0xFFB07000, 0xFFFFFFFF, 0xFF2B91AF, 0xFF2FAA46, 0xFF0A6CBA, 0xFFE5F3FF)));

    private final Palette<Colours> palette;

    IsmsLook(final Palette<Colours> palette) {
        this.palette = palette;
    }

    /** The look of the studio on a computer of {@code era}: the Query Analyzer before Transition, then by age. */
    static IsmsLook of(@Nullable final HardwareEra era) {
        if (era == null) {
            return STUDIO_2012;
        }
        return switch (era) {
            case VINTAGE, LEGACY -> QUERY_ANALYZER;
            case TRANSITION -> STUDIO_2008;
            case STANDARD -> STUDIO_2012;
            default -> STUDIO_2022;
        };
    }

    /** The look's colours, as a resource pack may have repainted them. */
    Colours colours() {
        return palette.get();
    }

    /** Whether this is the Query Analyzer, whose window, explorer and maintenance have names of their own. */
    boolean analyzer() {
        return this == QUERY_ANALYZER;
    }

    /** Whether the controls are bevelled, raised by a light edge and a shadow, as the 2000 age drew them. */
    boolean bevelled() {
        return this == QUERY_ANALYZER;
    }

    /** Whether the editor numbers its lines, which the Query Analyzer did not. */
    boolean lineNumbers() {
        return this != QUERY_ANALYZER;
    }

    /**
     * The colours of one look.
     *
     * @param window       the window's ground
     * @param panel        the explorer's, the editor's and the grid's ground
     * @param menu         the menu bar, and the ground of a menu that drops from it
     * @param menuTo       the menu bar's second shade
     * @param toolbar      the toolbar
     * @param toolbarTo    the toolbar's second shade
     * @param edge         the lines between the window's parts
     * @param button       a toolbar button's face
     * @param buttonEdge   its border, or its dark outer edge on a bevelled look
     * @param buttonLight  the light edge of a bevelled control
     * @param buttonShadow the shadow edge of a bevelled control
     * @param text         the text
     * @param dim          dimmed text: counts, hints, a disabled button
     * @param accent       the network's name in the toolbar, numbers in the grid, the active result tab's mark
     * @param select       the ground of the node or row picked
     * @param selectEdge   its outline
     * @param head         the explorer's caption and the grid's header
     * @param headTo       the caption's second shade
     * @param headText     the caption's text
     * @param tabs         the strip of document tabs
     * @param tabsTo       the strip's second shade
     * @param tabText      an inactive tab's label
     * @param tabActive    the active tab's ground, or 0 to leave the strip under it
     * @param tabActiveText the active tab's label
     * @param tabMark      the line along the active tab's top, or 0 for none
     * @param split        a splitter's ground
     * @param splitEdge    a splitter's lines, and a scroll bar's thumb
     * @param status       the status bar
     * @param statusTo     the status bar's second shade
     * @param statusText   the status bar's text
     * @param statusEdge   the status bar's top line, and the wells of a bevelled one
     * @param led          the lamp at the status bar's left
     * @param good         a message that went
     * @param bad          a message that failed
     * @param warn         a message worth a look
     * @param gutter       the editor's line-number gutter
     * @param gutterText   its numbers
     * @param run          the Execute button's triangle
     * @param menuLit      the ground of the menu item under the mouse, under white text
     * @param hover        the ground of a row the mouse is over
     */
    record Colours(int window, int panel, int menu, int menuTo, int toolbar, int toolbarTo, int edge,
                   int button, int buttonEdge, int buttonLight, int buttonShadow,
                   int text, int dim, int accent, int select, int selectEdge,
                   int head, int headTo, int headText,
                   int tabs, int tabsTo, int tabText, int tabActive, int tabActiveText, int tabMark,
                   int split, int splitEdge,
                   int status, int statusTo, int statusText, int statusEdge, int led,
                   int good, int bad, int warn, int gutter, int gutterText, int run, int menuLit, int hover) {
    }
}
