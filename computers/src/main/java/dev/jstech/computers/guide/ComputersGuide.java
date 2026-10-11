/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.guide;

import dev.jstech.computers.block.part.CablePartItem;
import dev.jstech.computers.os.edit.CodeRuns;
import dev.jstech.computers.program.iql.IqlColouring;
import dev.jstech.computers.registry.ComputingContent;
import dev.jstech.core.cable.CableEntry;
import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.ItemEntry;
import dev.jstech.core.grid.GridKind;
import dev.jstech.core.guide.CoreGuide;
import dev.jstech.core.guide.GuideCode;
import dev.jstech.core.guide.GuideHighlighters;
import dev.jstech.core.guide.GuideStyle;
import dev.jstech.core.guide.ManualItem;
import dev.jstech.core.guide.ModGuide;
import dev.jstech.core.multipart.PartType;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.world.level.ItemLike;

/**
 * J's Computers in the manuals: its chapter, written once and shown both in the series' Technical Reference and in
 * the mod's own Guide to Operations, the beige binder of the computer manuals of the early 1980s. Every item of the
 * mod is the page of one entry, a family of parts sharing the entry that explains them.
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

    /** Declares the chapter, the manual and its style, before the data generation writes the mod's files. */
    public static void declare() {
        final ModGuide guide = ComputingContent.CONTENT.guide();
        // Held over an item, the manual key lights blue blocks one after another, as Frames XP did while it loaded.
        guide.style("operations", CoreGuide.binder("jsc:guide/operations", true)
                .withHoldBar(GuideStyle.HoldBar.BLOCKS));
        guide.manual("guide_to_operations").titled("Guide to Operations").cover("J's Computers")
                .edition("First Edition (September 2026)").style(OPERATIONS_STYLE).chapters("jsc").priority(50)
                .icon("jsc:gui/guide/cover_mark")
                .about("This binder is the operator's guide to J's Computers: what a computer is here, how to build"
                                + " one, and everything the network does for you.",
                        "New to it? Start at the beginning, with Welcome to J's Computers. The entries explain the"
                                + " ideas first and the parts after, and a number in brackets leads to another"
                                + " entry.",
                        "The magnifier on the top edge finds anything by name, and the items on the plate under a"
                                + " title lead to their own pages. Hold the manual key over an item to open its"
                                + " page.")
                .register();
        guide.chapter().titled("J's Computers").order(20).tab("#FF39D6C4")
                .about("Computers from the early 1990s to today: build them part by part, join them in a network,"
                        + " and let the network keep your items, move them and craft for you.")
                .register();
        ComputersEntries.declare(guide);
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

    /** Every item the mod registers that is of one of those kinds, read when the files are written. */
    static Supplier<List<ItemLike>> itemsOf(final Class<?>... kinds) {
        return () -> ComputingContent.CONTENT.declaredItems().stream().map(entry -> (ItemLike) entry.get())
                .filter(item -> isOf(item, kinds)).toList();
    }

    /** The items of every block the mod registers that is of one of those kinds. */
    static Supplier<List<ItemLike>> blocksOf(final Class<?>... kinds) {
        return () -> ComputingContent.CONTENT.declaredBlocks().stream().filter(BlockEntry::hasItem)
                .filter(entry -> isOf(entry.get(), kinds)).map(entry -> (ItemLike) entry.get()).toList();
    }

    /** The cable items of every cable the mod registers on {@code grid}, but those named, read when written. */
    static Supplier<List<ItemLike>> cablesOn(final GridKind grid, final CableEntry... except) {
        final List<CableEntry> left = List.of(except);
        return () -> ComputingContent.CONTENT.declaredCables().stream()
                .filter(entry -> entry.get().grid() == grid && !left.contains(entry))
                .map(entry -> (ItemLike) entry).toList();
    }

    /** The items of every cable part the mod registers whose kind passes {@code kind}. */
    static Supplier<List<ItemLike>> partsOf(final Predicate<PartType<?>> kind) {
        return () -> ComputingContent.CONTENT.declaredItems().stream()
                .filter(entry -> entry.get() instanceof CablePartItem part && kind.test(part.partType()))
                .map(entry -> (ItemLike) entry.get()).toList();
    }

    private static boolean isOf(final Object thing, final Class<?>... kinds) {
        for (final Class<?> kind : kinds) {
            if (kind.isInstance(thing)) {
                return true;
            }
        }
        return false;
    }
}
