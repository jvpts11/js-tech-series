/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.guide;

import dev.jstech.core.content.ItemEntry;
import dev.jstech.core.font.CoreFonts;
import dev.jstech.core.guide.CoreGuide;
import dev.jstech.core.guide.GuideStyle;
import dev.jstech.core.guide.GuideStyle.CoverPart;
import dev.jstech.core.guide.GuideStyle.PartKind;
import dev.jstech.core.guide.ManualItem;
import dev.jstech.space.SpaceContent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * J's Space's own manual, the Mission Handbook, dressed as a crew's flight documents were: an off-white ring binder
 * with the agency's emblem and typed lines on its board, cream pages typed in capitals, a plastic tab for each
 * section in that section's colour and lettered with its code, steps checked off in boxes, cautions and warnings
 * under their headed bars, and the pages numbered by section (LO 2-3). The handbook and its chapter are written in
 * Markdown, under {@code assets/jsspace/jstech/manuals/mission_handbook/}; this class gives it the item that opens it
 * and the style it is drawn in.
 */
public final class MissionHandbook {

    /** The handbook, and the style it is drawn in. */
    public static final String MANUAL = "jsspace:mission_handbook";
    public static final String STYLE = "jsspace:flight_data_file";

    /** The handbook's item, the first thing in the mod's tab. */
    public static final ItemEntry<ManualItem> ITEM = SpaceContent.CONTENT.item("mission_handbook",
            properties -> new ManualItem(properties, MANUAL)).named("Mission Handbook").tab(SpaceContent.MANUALS)
            .register();

    /* The closed binder's board, in the screen's pixels, and how far apart the grain of its vinyl runs. */
    private static final int BOARD_WIDTH = 180;
    private static final int BOARD_HEIGHT = 218;
    private static final int GRAIN_PITCH = 3;
    /* The rings down the spine, where their tops stand on the board. */
    private static final int[] RINGS = {36, 104, 172};
    /* The title block, the typed box in the middle of the board. */
    private static final int BLOCK_X = 30;
    private static final int BLOCK_Y = 70;
    private static final int BLOCK_WIDTH = 134;
    private static final int BLOCK_HEIGHT = 66;
    private static final float TITLE_SCALE = 1.5F;
    /* The agencies' blue and red, the band across the board's foot. */
    private static final String AGENCY_BLUE = "#FF133A8E";
    private static final String AGENCY_RED = "#FFD52B23";

    private MissionHandbook() {
    }

    /** Declares the handbook's style, before the data generation writes the mod's files. */
    public static void declare() {
        SpaceContent.CONTENT.guide().style("flight_data_file", style());
    }

    /*
     * Typed pages in a binder, numbered by section, each section's tab in its colour; a caution's and a warning's
     * grounds, the card's bar and a changed step's mark in the colours the crews' books printed them in.
     */
    private static GuideStyle style() {
        final Map<String, String> colours = Map.of(GuideStyle.CAUTION, "#FF8A5A00", GuideStyle.CAUTION_PAPER,
                "#FFF7E8C4", GuideStyle.WARNING_PAPER, "#FFF5DDD8", GuideStyle.CARD, AGENCY_BLUE, GuideStyle.MARK,
                AGENCY_RED);
        return new GuideStyle("jsspace:guide/flight_data_file", colours,
                new GuideStyle.Pages(true, CoreGuide.PAGE_WIDTH, CoreGuide.PAGE_HEIGHT, CoreGuide.MARGIN, 1),
                GuideStyle.Decor.missionDocument(), "", CoreFonts.FIXED_5X8.id().toString(),
                GuideStyle.Folios.SECTION_PAGE, "", GuideStyle.Cover.book(cover()), GuideStyle.HoldBar.PLAIN,
                new GuideStyle.Make(GuideStyle.Binding.RINGS, GuideStyle.Board.PLAIN, GuideStyle.Paper.PLAIN,
                        GuideStyle.Marks.SECTION_TABS, List.of(), false));
    }

    /*
     * The closed binder: the vinyl's grain, the spine and its three rings; the emblem at the top left where an agency
     * set its own, the directorate beside it; FLIGHT DATA FILE under a rule; the boxed title; the book's level and
     * revision, who prepared it, and the agencies' band at the foot. The words are the manual's cover lines, in order.
     */
    private static List<CoverPart> cover() {
        final List<CoverPart> parts = new ArrayList<>();
        bevel(parts, 0, 0, BOARD_WIDTH, BOARD_HEIGHT, GuideStyle.COVER, GuideStyle.COVER_EDGE, "#FFBCB6A6");
        for (int x = 2; x < BOARD_WIDTH - 2; x += GRAIN_PITCH) {
            parts.add(CoverPart.fill(x, 2, 1, BOARD_HEIGHT - 4, "#FFE3E0D6"));
        }
        bevel(parts, 0, 0, 14, BOARD_HEIGHT, GuideStyle.SPINE, GuideStyle.COVER, "#FF8F8878");
        for (final int ring : RINGS) {
            bevel(parts, 2, ring, 10, 7, "#FF7D838C", "#FFC9CCD1", "#FF4A4E56");
        }
        parts.add(CoverPart.image(23, 13, 32, "jsspace:gui/guide/emblem"));
        parts.add(CoverPart.words(PartKind.CODE, 62, 16, 110, 1, 1, GuideStyle.INK));
        parts.add(CoverPart.words(PartKind.CODE, 62, 26, 110, 2, 1, GuideStyle.INK));
        parts.add(CoverPart.words(PartKind.CODE, 62, 34, 110, 3, 1, GuideStyle.INK));
        parts.add(CoverPart.fill(22, 52, 150, 1, GuideStyle.INK));
        parts.add(CoverPart.words(PartKind.CENTRED_CODE, 22, 57, 150, 4, 1, GuideStyle.INK));
        parts.add(CoverPart.fill(BLOCK_X, BLOCK_Y, BLOCK_WIDTH, BLOCK_HEIGHT, GuideStyle.LABEL));
        parts.add(CoverPart.frame(BLOCK_X, BLOCK_Y, BLOCK_WIDTH, BLOCK_HEIGHT, GuideStyle.INK));
        parts.add(CoverPart.words(PartKind.CENTRED, BLOCK_X, BLOCK_Y + 9, BLOCK_WIDTH, 5, TITLE_SCALE,
                GuideStyle.INK));
        parts.add(CoverPart.words(PartKind.CENTRED, BLOCK_X, BLOCK_Y + 25, BLOCK_WIDTH, 6, TITLE_SCALE,
                GuideStyle.INK));
        parts.add(CoverPart.fill(BLOCK_X + 10, BLOCK_Y + 45, BLOCK_WIDTH - 20, 1, GuideStyle.RULE));
        parts.add(CoverPart.words(PartKind.CENTRED_CODE, BLOCK_X, BLOCK_Y + 51, BLOCK_WIDTH, 7, 1,
                GuideStyle.FAINT));
        parts.add(CoverPart.words(PartKind.CODE, BLOCK_X, 146, BLOCK_WIDTH, 8, 1, GuideStyle.INK));
        parts.add(CoverPart.words(PartKind.RIGHT_CODE, BLOCK_X, 146, BLOCK_WIDTH, 9, 1, GuideStyle.INK));
        parts.add(CoverPart.fill(BLOCK_X, 156, BLOCK_WIDTH, 1, GuideStyle.RULE));
        parts.add(CoverPart.words(PartKind.CODE, BLOCK_X, 162, BLOCK_WIDTH, 10, 1, GuideStyle.FAINT));
        parts.add(CoverPart.words(PartKind.CODE, BLOCK_X, 171, BLOCK_WIDTH, 11, 1, GuideStyle.INK));
        parts.add(CoverPart.words(PartKind.CODE, BLOCK_X, 180, BLOCK_WIDTH, 12, 1, GuideStyle.INK));
        parts.add(CoverPart.fill(0, 196, BOARD_WIDTH, 4, AGENCY_BLUE));
        parts.add(CoverPart.fill(0, 200, BOARD_WIDTH, 2, AGENCY_RED));
        return parts;
    }

    /* A raised plate: a rectangle of a colour, lit along its top and left edges and shaded along the others. */
    private static void bevel(final List<CoverPart> parts, final int x, final int y, final int width,
                              final int height, final String fill, final String light, final String dark) {
        parts.add(CoverPart.fill(x, y, width, height, fill));
        parts.add(CoverPart.fill(x, y, width, 1, light));
        parts.add(CoverPart.fill(x, y, 1, height, light));
        parts.add(CoverPart.fill(x, y + height - 1, width, 1, dark));
        parts.add(CoverPart.fill(x + width - 1, y, 1, height, dark));
    }
}
