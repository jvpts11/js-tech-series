/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GuideStyleTest {

    private static final GuideStyle BINDER = new GuideStyle("", Map.of(), new GuideStyle.Pages(true, 166, 201, 10, 1),
            GuideStyle.Decor.binder(true), "", "", GuideStyle.Folios.CHAPTER_PAGE, "",
            new GuideStyle.Cover(GuideStyle.CoverKind.BINDER, true, false), GuideStyle.HoldBar.PLAIN);

    @Test
    void make_isARingBindersWithTabsWhenAStyleSaysNothingOfIt() {
        assertEquals(new GuideStyle.Make(GuideStyle.Binding.RINGS, GuideStyle.Paper.PLAIN, GuideStyle.Marks.TABS,
                false), BINDER.make());
        assertTrue(BINDER.rings());
        assertEquals(GuideStyle.Binding.GLUED, GuideStyle.Make.of(GuideStyle.Decor.binder(false)).binding());
    }

    @Test
    void withMake_bindsTheSameStyleAnotherWayAndKeepsTheRest() {
        final GuideStyle notebook = BINDER.withMake(new GuideStyle.Make(GuideStyle.Binding.WIRE,
                GuideStyle.Paper.GRAPH, GuideStyle.Marks.FLAGS, false));

        assertFalse(notebook.rings(), "a coil has no rings");
        assertEquals(BINDER.pages(), notebook.pages());
        assertEquals(BINDER.cover(), notebook.cover());
        assertEquals(GuideStyle.HoldBar.BLOCKS, notebook.withHoldBar(GuideStyle.HoldBar.BLOCKS).holdBar());
        assertEquals(notebook.make(), notebook.withHoldBar(GuideStyle.HoldBar.BLOCKS).make(),
                "and keeps how it is made");
    }

    @Test
    void cover_ofABookIsDrawnFromItsPartsInOrder() {
        final List<GuideStyle.CoverPart> parts = List.of(
                GuideStyle.CoverPart.fill(0, 0, 180, 218, "#FFF4F4EF"),
                GuideStyle.CoverPart.words(GuideStyle.PartKind.BOLD, 22, 64, 150, 1, 2.5F, GuideStyle.HEADING),
                GuideStyle.CoverPart.stamp(76, 140, 100, 34, 3, -5, "#FFB3261E"));
        final GuideStyle.Cover cover = GuideStyle.Cover.book(parts);

        assertEquals(GuideStyle.CoverKind.BOOK, cover.kind());
        assertEquals(parts, cover.parts());
        assertEquals(List.of(), new GuideStyle.Cover(GuideStyle.CoverKind.BINDER, true, false).parts());
        assertThrows(IllegalArgumentException.class, () -> GuideStyle.CoverPart.words(GuideStyle.PartKind.BOLD, 0, 0,
                10, 1, 0, GuideStyle.INK), "words have a scale");
    }
}
