/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.guide;

import dev.jstech.computers.os.edit.CodeRuns;
import dev.jstech.computers.program.iql.IqlColouring;
import dev.jstech.computers.registry.ComputingContent;
import dev.jstech.core.content.ItemEntry;
import dev.jstech.core.guide.CoreGuide;
import dev.jstech.core.guide.GuideCode;
import dev.jstech.core.guide.GuideHighlighters;
import dev.jstech.core.guide.GuideStyle;
import dev.jstech.core.guide.ManualItem;
import dev.jstech.core.guide.ModGuide;

/**
 * J's Computers in the manuals: its chapter, shown both in the series' Technical Reference and in the mod's own Guide
 * to Operations, the beige binder of the computer manuals of the early 1980s, and the books of the systems and the
 * languages beside it. Every word of them is written in Markdown, under {@code assets/jsc/jstech/manuals/}, a folder
 * for each manual and language (core/docs/MANUALS.md); what is declared here is what only code can give: the items a
 * player holds, the books' looks, the tables read from the mod's things, the families of items the entries are the
 * pages of, and the colours of the languages' listings.
 */
public final class ComputersGuide {

    /** The mod's own manual, and the style it is drawn in. */
    public static final String GUIDE_TO_OPERATIONS = "jsc:guide_to_operations";
    /** The books of the systems and the languages, beside the Guide, each the first a system's help opens. */
    public static final String FRAMES_RESOURCE_KIT = "jsc:frames_resource_kit";
    public static final String LINUX_DESK_REFERENCE = "jsc:linux_desk_reference";
    public static final String LINUX_BY_HAND = "jsc:linux_by_hand";
    public static final String DAEMON_HANDBOOK = "jsc:daemon_handbook";
    public static final String LEARNING_IQL = "jsc:learning_iql";
    public static final String BOOK_OF_SIGMA = "jsc:book_of_sigma";
    public static final String OPERATIONS_STYLE = "jsc:operations";
    /** What a listing of the network's query language names its language, as its files' extension does. */
    public static final String IQL = "iql";

    public static final ItemEntry<ManualItem> MANUAL = ComputingContent.CONTENT.item("guide_to_operations",
            properties -> new ManualItem(properties, GUIDE_TO_OPERATIONS)).named("Guide to Operations")
            .tab(ComputingContent.shelf(ComputingContent.Shelf.PROGRAMS, null)).register();

    private ComputersGuide() {
    }

    /** Declares what the manuals read from the mod's code, before anything reads them. */
    public static void declare() {
        // The tables the pages read from the things themselves: the cables, the servers and the storage cards.
        CableFacts.declare();
        RackFacts.declare();
        SigmaReferenceFacts.declare();
        // The families of parts and blocks the manuals' entries are the pages of, asked for as the manuals are read.
        ComputersFamilies.register();
        // A listing in one of the machines' languages is coloured as the game's editors colour it.
        LanguageHighlighting.declare();
        final ModGuide guide = ComputingContent.CONTENT.guide();
        // Held over an item, the manual key lights blue blocks one after another, as Frames XP did while it loaded.
        guide.style("operations", CoreGuide.binder("jsc:guide/operations", true)
                .withHoldBar(GuideStyle.HoldBar.BLOCKS));
        ComputersBooks.declare(guide);
        // Sigma and Sigma# colour themselves as languages the game compiles; the network's queries are coloured here.
        GuideHighlighters.register(IQL, lines -> GuideCode.runs(lines, IqlColouring.spans(lines).stream()
                .map(span -> new GuideCode.Span(span.line(), span.column(), span.length(), inkOf(span.ink())))
                .toList()));
    }

    /** What the manuals colour a kind of the editors' code as: the same kinds, by their own names. */
    private static GuideCode.Ink inkOf(final CodeRuns.Ink ink) {
        return switch (ink) {
            case PLAIN -> GuideCode.Ink.PLAIN;
            case KEYWORD -> GuideCode.Ink.KEYWORD;
            case NAME -> GuideCode.Ink.NAME;
            case TEXT -> GuideCode.Ink.TEXT;
            case NUMBER -> GuideCode.Ink.NUMBER;
            case COMMENT -> GuideCode.Ink.COMMENT;
            case SYMBOL -> GuideCode.Ink.SYMBOL;
        };
    }
}
