/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.builder;

import dev.jstech.core.gui.layout.GuiLayout;
import dev.jstech.core.jstechseries.probe.ProbeLayout;
import dev.jstech.core.text.TextKey;

/**
 * Where the Multiblock Builder's pages stand on the display of its family's body ({@link ProbeLayout}), as the approved
 * design lays them out, in the body's pixels: on the page to build, what is to place and the materials, the choice of a
 * place that takes several, the source and whether it is ready; on the page of choices, a row for each group of places;
 * on the page of sources, a row for each. Pure, so its layout is tested without the game.
 */
public final class BuilderLayout {

    public static final int TO_PLACE_Y = ProbeLayout.DISPLAY_TOP + ProbeLayout.HEADER_HEIGHT + 4;
    public static final int MATERIALS_Y = TO_PLACE_Y + 10;
    public static final int MATERIAL_PITCH = 13;
    /** The materials shown at once; the pad's arrows bring the others in view. */
    public static final int MATERIAL_ROWS = 3;
    /** An item's side, drawn small. */
    public static final int ICON = 12;
    public static final float ICON_SCALE = ICON / 16F;
    public static final int NAME_X = ProbeLayout.TEXT_LEFT + ICON + 3;
    public static final int CHOICE_TOP = MATERIALS_Y + MATERIAL_ROWS * MATERIAL_PITCH;
    public static final int CHOICE_HEIGHT = 24;
    /** The blocks of a choice shown side by side. */
    public static final int CHOICE_OPTIONS = 3;
    public static final int CHOICE_WIDTH = (ProbeLayout.TEXT_RIGHT - ProbeLayout.TEXT_LEFT) / CHOICE_OPTIONS;
    public static final int SOURCE_Y = CHOICE_TOP + CHOICE_HEIGHT + 3;
    public static final int READY_Y = SOURCE_Y + 18;
    public static final int GROUP_PITCH = 22;
    /** The groups of places shown at once on the page of choices. */
    public static final int GROUP_ROWS = 4;
    public static final int SOURCE_PITCH = 11;
    /** The sources shown at once. */
    public static final int SOURCE_ROWS = 6;
    public static final int LINE = 8;
    /** The display's room for small text, in the font's own pixels. */
    public static final int ROOM = (int) ((ProbeLayout.TEXT_RIGHT - ProbeLayout.TEXT_LEFT) / ProbeLayout.SMALL);

    private BuilderLayout() {
    }

    /** The page to build at its fullest, its longest words in, for the test that nothing on it overlaps. */
    public static GuiLayout layout() {
        final GuiLayout layout = new GuiLayout(ProbeLayout.WIDTH, ProbeLayout.HEIGHT);
        final int tabs = ProbeLayout.DISPLAY_BOTTOM - ProbeLayout.TABS_HEIGHT;
        layout.box("tabs", ProbeLayout.DISPLAY_LEFT, tabs, ProbeLayout.DISPLAY_RIGHT - ProbeLayout.DISPLAY_LEFT,
                ProbeLayout.TABS_HEIGHT);
        layout.box("header", ProbeLayout.DISPLAY_LEFT, ProbeLayout.DISPLAY_TOP,
                ProbeLayout.DISPLAY_RIGHT - ProbeLayout.DISPLAY_LEFT, ProbeLayout.HEADER_HEIGHT);
        for (int row = 0; row < MATERIAL_ROWS; row++) {
            layout.box("icon" + row, ProbeLayout.TEXT_LEFT, MATERIALS_Y + row * MATERIAL_PITCH, ICON, ICON);
        }
        layout.box("choice", ProbeLayout.DISPLAY_LEFT + 1, CHOICE_TOP, ProbeLayout.DISPLAY_RIGHT
                - ProbeLayout.DISPLAY_LEFT - 2, CHOICE_HEIGHT);
        layout.text("toPlace", ProbeLayout.TEXT_LEFT, TO_PLACE_Y, chars(BuilderTexts.TO_PLACE), ProbeLayout.SMALL);
        layout.text("source", ProbeLayout.TEXT_LEFT, SOURCE_Y, chars(BuilderTexts.SOURCE), ProbeLayout.SMALL);
        layout.text("sourceName", ProbeLayout.TEXT_LEFT, SOURCE_Y + LINE, chars(BuilderTexts.CREATIVE),
                ProbeLayout.SMALL);
        layout.text("ready", ProbeLayout.TEXT_LEFT, READY_Y, chars(BuilderTexts.LACKING) + 1
                + chars(BuilderTexts.READY_LINE), ProbeLayout.SMALL);
        return layout;
    }

    /** The lowest a line of a page may start at, above the tabs. */
    public static int pageFoot() {
        return ProbeLayout.DISPLAY_BOTTOM - ProbeLayout.TABS_HEIGHT - LINE;
    }

    /* How many letters a key's English holds, a placeholder counted as two. */
    private static int chars(final TextKey key) {
        return key.english().replace("%s", "99").length();
    }
}
