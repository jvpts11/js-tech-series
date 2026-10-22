/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide;

import dev.jstech.core.content.ItemEntry;
import dev.jstech.core.font.CoreFonts;
import dev.jstech.core.material.CatalogueItems;
import dev.jstech.core.material.MaterialLiquids;
import dev.jstech.core.material.MaterialUses;
import dev.jstech.core.material.RegisteredThing;
import dev.jstech.core.registry.CoreItems;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.ItemLike;

/**
 * What the Core brings to the manuals: the binder style, two cream pages side by side in navy vinyl covers with three
 * rings through them, its code and figures in the small terminal font and its pages numbered by chapter, as technical
 * manuals were;
 * and the series' own manual, the Technical Reference, which holds every chapter of every mod there is, handed to
 * each player once as they first join a world and kept in the J's Tech creative tab.
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

    /** The series' manual, shown in the J's Tech creative tab with the rest of what the series' mods share. */
    public static final ItemEntry<ManualItem> MANUAL = CoreItems.CONTENT.item("technical_reference",
            properties -> new ManualItem(properties, TECHNICAL_REFERENCE)).named("Technical Reference").register();

    /** The family of every form of every material some installed mod uses, which the materials' entry covers. */
    public static final String MATERIALS = "jscore:materials";

    static {
        CoreItems.CONTENT.guide().style("binder", binder("jscore:guide/binder", false));
        // The manual itself, the series' chapter and the Core's are written in Markdown, under
        // assets/jscore/jstech/manuals/technical_reference/ and assets/jstech/jstech/manuals/technical_reference/.
        GuideGifts.giveOnFirstJoin(TECHNICAL_REFERENCE, MANUAL);
        GuideFamilies.register(MATERIALS, CoreGuide::materialItems);
    }

    private CoreGuide() {
    }

    /** Declares the Core's style and what its manual reads from code, before the data generation writes the files. */
    public static void declare() {
        // Loading the class declares it.
    }

    /**
     * A binder style in a palette of the {@code GuidePalettes.Binder} kind: two pages, three rings, code and figures in
     * the small terminal font, pages numbered by chapter. A mod's own binder names its palette and whether a band of
     * colour crosses its cover; its words then stand on the cover itself rather than on a label.
     */
    public static GuideStyle binder(final String palette, final boolean band) {
        return new GuideStyle(palette, Map.of(), new GuideStyle.Pages(true, PAGE_WIDTH, PAGE_HEIGHT, MARGIN, 1),
                GuideStyle.Decor.binder(true), "", CoreFonts.FIXED_5X8.id().toString(),
                GuideStyle.Folios.CHAPTER_PAGE, "", new GuideStyle.Cover(GuideStyle.CoverKind.BINDER, !band, band),
                GuideStyle.HoldBar.PLAIN);
    }

    /**
     * A bound book in a palette of the {@code GuidePalettes.Binder} kind, its pages laid out as a binder's are, made as
     * {@code make} says (its binding, its boards, its paper and how its chapters are marked), and its cover drawn from
     * {@code parts}.
     */
    public static GuideStyle book(final String palette, final GuideStyle.Make make,
                                  final List<GuideStyle.CoverPart> parts) {
        return new GuideStyle(palette, Map.of(), new GuideStyle.Pages(true, PAGE_WIDTH, PAGE_HEIGHT, MARGIN, 1),
                GuideStyle.Decor.binder(false), "", CoreFonts.FIXED_5X8.id().toString(),
                GuideStyle.Folios.CHAPTER_PAGE, "", GuideStyle.Cover.book(parts), GuideStyle.HoldBar.PLAIN, make);
    }

    /**
     * A set of drawings in a folder: one wide sheet at a time in two columns, with the grid, the frame and its title
     * block, the notes lettered small, the blocks traced, and every entry a drawing numbered after {@code prefix}
     * (JI-102). The folder has a label and an elastic band round it.
     */
    public static GuideStyle drawings(final String palette, final String prefix) {
        return new GuideStyle(palette, Map.of(), new GuideStyle.Pages(false, SHEET_WIDTH, PAGE_HEIGHT,
                SHEET_MARGIN, 2), GuideStyle.Decor.drawing(), "", CoreFonts.FIXED_5X8.id().toString(),
                GuideStyle.Folios.DRAWING, prefix, new GuideStyle.Cover(GuideStyle.CoverKind.FOLDER, true, true),
                GuideStyle.HoldBar.PLAIN);
    }

    /*
     * Every thing the Core registered for the materials and the shared catalogue the installed mods use, in the order
     * of the materials: their forms, their liquids' buckets, the components.
     */
    private static List<String> materialItems() {
        final List<String> items = new ArrayList<>();
        for (final RegisteredThing thing : MaterialUses.registered()) {
            items.add(thing.id().toString());
        }
        for (final ItemLike bucket : MaterialLiquids.buckets()) {
            items.add(BuiltInRegistries.ITEM.getKey(bucket.asItem()).toString());
        }
        for (final ItemLike component : CatalogueItems.items()) {
            items.add(BuiltInRegistries.ITEM.getKey(component.asItem()).toString());
        }
        return items;
    }
}
