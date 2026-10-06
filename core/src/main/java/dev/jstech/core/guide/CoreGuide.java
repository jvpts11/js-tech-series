/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide;

import dev.jstech.core.content.ContentTab;
import dev.jstech.core.content.ItemEntry;
import dev.jstech.core.font.CoreFonts;
import dev.jstech.core.registry.CoreItems;
import java.util.Map;

/**
 * What the Core brings to the manuals: the binder style, two cream pages side by side in navy vinyl covers with three
 * rings through them, its tables in the terminal font and its pages numbered by chapter, as technical manuals were;
 * and the series' own manual, the Technical Reference, which holds every chapter of every mod there is, handed to
 * each player once as they first join a world and kept in the Core's creative tab.
 */
public final class CoreGuide {

    /** The binder style's id, which a manual names to be drawn in it. */
    public static final String BINDER = "jscore:binder";
    /** The series' manual. */
    public static final String TECHNICAL_REFERENCE = "jscore:technical_reference";

    /** A binder's page, in the screen's pixels: two of them and the covers fit a screen of 640 by 360. */
    public static final int PAGE_WIDTH = 166;
    public static final int PAGE_HEIGHT = 201;
    public static final int MARGIN = 10;
    /** A drawing's sheet: as wide as two binder pages and their gutter, its text inside the frame. */
    public static final int SHEET_WIDTH = 340;
    public static final int SHEET_MARGIN = 15;

    /** The Core's creative tab: the series' manual, and whatever else of the Core a player takes in hand. */
    public static final ContentTab TAB = CoreItems.CONTENT.tab("core", "J's Core", () -> CoreGuide.MANUAL);
    private static final ContentTab.Section MANUALS = TAB.section();

    public static final ItemEntry<ManualItem> MANUAL = CoreItems.CONTENT.item("technical_reference",
            properties -> new ManualItem(properties, TECHNICAL_REFERENCE)).named("Technical Reference")
            .tab(MANUALS).register();

    static {
        CoreItems.CONTENT.guide().style("binder", binder("jscore:guide/binder", false));
        CoreItems.CONTENT.guide().manual("technical_reference").titled("Technical Reference")
                .cover("J's Tech Series", "Reference Library").edition("First Edition (September 2026)")
                .partNumber("JTS-0001").style(BINDER).chapters(GuideManual.EVERY_CHAPTER).priority(100)
                .about("This binder holds the reference for every mod of the series you have. Each has its chapter"
                                + " and its tab at the edge.",
                        "Every entry says, in this order, what the thing is, what it is for, how to get it, how to"
                                + " use it step by step, and what can go wrong. A figure (Figure 3-9) shows what a"
                                + " part looks like, a table (Table 3-7) gives its numbers, and a number after See"
                                + " (See 3.4.3) leads to another entry.",
                        "The magnifier on the top edge finds any part by name in the index. Hold the manual key"
                                + " over an item to open its page.")
                .register();
        GuideGifts.giveOnFirstJoin(TECHNICAL_REFERENCE, MANUAL);
        SeriesChapter.declare();
        CoreChapter.declare();
    }

    private CoreGuide() {
    }

    /** Declares the Core's style, its manual and its chapters, before the data generation writes the Core's files. */
    public static void declare() {
        // Loading the class declares it.
    }

    /**
     * A binder style in a palette of the {@code GuidePalettes.Binder} kind: two pages, three rings, tables in the
     * terminal font, pages numbered by chapter. A mod's own binder names its palette and whether a band of colour
     * crosses its cover; its words then stand on the cover itself rather than on a label.
     */
    public static GuideStyle binder(final String palette, final boolean band) {
        return new GuideStyle(palette, Map.of(), new GuideStyle.Pages(true, PAGE_WIDTH, PAGE_HEIGHT, MARGIN, 1),
                GuideStyle.Decor.binder(true), "", CoreFonts.FIXED_6X10.id().toString(),
                GuideStyle.Folios.CHAPTER_PAGE, "", new GuideStyle.Cover(GuideStyle.CoverKind.BINDER, !band, band),
                GuideStyle.HoldBar.PLAIN);
    }

    /**
     * A set of drawings in a folder: one wide sheet at a time in two columns, with the grid, the frame and its title
     * block, the notes lettered small, the blocks traced, and every entry a drawing numbered after {@code prefix}
     * (JI-102). The folder has a label and an elastic band round it.
     */
    public static GuideStyle drawings(final String palette, final String prefix) {
        return new GuideStyle(palette, Map.of(), new GuideStyle.Pages(false, SHEET_WIDTH, PAGE_HEIGHT,
                SHEET_MARGIN, 2), GuideStyle.Decor.drawing(), "", CoreFonts.FIXED_6X10.id().toString(),
                GuideStyle.Folios.DRAWING, prefix, new GuideStyle.Cover(GuideStyle.CoverKind.FOLDER, true, true),
                GuideStyle.HoldBar.PLAIN);
    }
}
