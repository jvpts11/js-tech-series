/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TrashItemTest {

    @Test
    void of_writesTheOldPlaceTheFramesWay() {
        final TrashItem item = TrashItem.of("Dc1.txt", "Users/Public/Desktop/notes.txt", false, 2L, true);
        assertEquals("notes.txt", item.name());
        assertEquals("C:\\Users\\Public\\Desktop", item.place());
        assertEquals("Dc1.txt", item.stored());
    }

    @Test
    void of_writesTheOldPlaceFromTheRootOnUnix() {
        final TrashItem item = TrashItem.of("old", "home/player/Desktop/old", true, 4L, false);
        assertEquals("old", item.name());
        assertEquals("/home/player/Desktop", item.place());
    }

    @Test
    void of_placesAThingFromTheRootAtTheRoot() {
        assertEquals("C:\\", TrashItem.of("Dc1.txt", "readme.txt", false, 1L, true).place());
        assertEquals("/", TrashItem.of("readme.txt", "readme.txt", false, 1L, false).place());
    }

    @Test
    void summary_countsTheItemsAndAddsUpTheirSize() {
        final List<TrashItem> items = List.of(TrashItem.of("a", "x/a.txt", false, 2L, true),
                TrashItem.of("b", "x/b.txt", false, 12L, true), TrashItem.of("c", "x/c", true, 4L, true));
        assertEquals("3 items, 18 mB", TrashItem.summary(items));
        assertEquals("1 item, 2 mB", TrashItem.summary(items.subList(0, 1)));
        assertEquals("0 items, 0 mB", TrashItem.summary(List.of()));
        assertEquals("3 objects", TrashItem.objects(items));
        assertEquals("1 object", TrashItem.objects(items.subList(0, 1)));
    }

    @Test
    void extension_isEmptyForAFolderAndANameWithoutOne() {
        assertEquals("txt", TrashItem.of("a", "x/notes.txt", false, 1L, false).extension());
        assertEquals("", TrashItem.of("a", "x/notes.d", true, 1L, false).extension());
        assertEquals("", TrashItem.of("a", "x/.profile", false, 1L, false).extension());
    }
}
