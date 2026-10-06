/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.guide;

import dev.jstech.computers.registry.ComputingContent;
import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.ItemEntry;
import dev.jstech.core.guide.CoreGuide;
import dev.jstech.core.guide.GuideStyle;
import dev.jstech.core.guide.ManualItem;
import dev.jstech.core.guide.ModGuide;
import java.util.List;
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
    public static final String OPERATIONS_STYLE = "jsc:operations";

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

    private static boolean isOf(final Object thing, final Class<?>... kinds) {
        for (final Class<?> kind : kinds) {
            if (kind.isInstance(thing)) {
                return true;
            }
        }
        return false;
    }
}
