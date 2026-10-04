/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.core.gui.layout.GuiLayout;
import java.util.List;
import org.junit.jupiter.api.Test;

class CraftingInterfaceLayoutTest {

    /* How many patterns an interface of each era holds, from Vintage to Advanced. */
    private static final int[] CAPACITIES = {3, 6, 8, 9, 12};

    @Test
    void frame_isCleanForEveryEra() {
        for (final int capacity : CAPACITIES) {
            final GuiLayout l = CraftingInterfaceLayout.frame(CraftingInterfaceLayout.Shape.most(capacity));
            assertTrue(l.isClean(), capacity + ": " + l.overlaps() + " " + l.outOfBounds());
        }
    }

    @Test
    void content_isCleanWithEverythingAtItsMost() {
        for (final int capacity : CAPACITIES) {
            final GuiLayout l = CraftingInterfaceLayout.content(CraftingInterfaceLayout.Shape.most(capacity));
            assertTrue(l.isClean(), capacity + ": " + l.overlaps() + " " + l.outOfBounds());
        }
    }

    @Test
    void heights_neverPassWhatAScreenHolds() {
        for (final int capacity : CAPACITIES) {
            assertTrue(CraftingInterfaceLayout.configureHeight(CraftingInterfaceLayout.Shape.most(capacity))
                    <= CraftingInterfaceLayout.MAX_HEIGHT, String.valueOf(capacity));
        }
        assertTrue(CraftingInterfaceLayout.ACTIVITY_HEIGHT <= CraftingInterfaceLayout.MAX_HEIGHT);
        assertTrue(CraftingInterfaceLayout.softwareHeight(10_000) <= CraftingInterfaceLayout.MAX_HEIGHT);
    }

    @Test
    void rows_stackWithNoGapsOrOverlaps() {
        final List<CraftingInterfaceLayout.Row> rows = CraftingInterfaceLayout.rows(
                CraftingInterfaceLayout.Shape.most(12));
        for (int i = 1; i < rows.size(); i++) {
            assertEquals(rows.get(i - 1).y() + rows.get(i - 1).height(), rows.get(i).y());
        }
        assertEquals(rows.get(rows.size() - 1).y() + rows.get(rows.size() - 1).height(),
                CraftingInterfaceLayout.contentHeight(CraftingInterfaceLayout.Shape.most(12)));
    }

    @Test
    void rows_showTheInputsOnlyForAPatternFedThroughRouters() {
        final CraftingInterfaceLayout.Shape direct = new CraftingInterfaceLayout.Shape(9, 1, 0, 1, 1, List.of(), 1);
        final CraftingInterfaceLayout.Shape routed = new CraftingInterfaceLayout.Shape(9, 1, 2, 1, 1, List.of(3), 1);

        assertFalse(kinds(direct).contains(CraftingInterfaceLayout.Kind.MAPPING));
        assertFalse(kinds(direct).contains(CraftingInterfaceLayout.Kind.WARNING));
        assertEquals(2, CraftingInterfaceLayout.rows(routed).stream()
                .filter(r -> r.kind() == CraftingInterfaceLayout.Kind.MAPPING).count());
        assertTrue(kinds(routed).contains(CraftingInterfaceLayout.Kind.WARNING));
        assertEquals(CraftingInterfaceLayout.Kind.PATTERNS, CraftingInterfaceLayout.rows(routed).get(0).kind());
        assertEquals(CraftingInterfaceLayout.Kind.NOW, CraftingInterfaceLayout.rows(routed)
                .get(CraftingInterfaceLayout.rows(routed).size() - 1).kind());
    }

    @Test
    void cellRows_wrapThePatternsAtTheRowsEnd() {
        assertEquals(1, CraftingInterfaceLayout.cellRows(1));
        assertEquals(1, CraftingInterfaceLayout.cellRows(CraftingInterfaceLayout.CELLS_PER_ROW));
        assertEquals(2, CraftingInterfaceLayout.cellRows(CraftingInterfaceLayout.CELLS_PER_ROW + 1));
        assertEquals(1, CraftingInterfaceLayout.cellRows(0));
    }

    @Test
    void tabs_sitSideBySideWithoutTouching() {
        for (int tab = 1; tab < CraftingInterfaceLayout.TABS; tab++) {
            assertTrue(CraftingInterfaceLayout.tabX(tab - 1) + CraftingInterfaceLayout.tabW(tab - 1)
                    <= CraftingInterfaceLayout.tabX(tab), "tab " + tab);
        }
    }

    @Test
    void options_leaveTheGapBetweenThem() {
        final int[] at = CraftingInterfaceLayout.options(40, 20, 30);

        assertEquals(40, at[0]);
        assertEquals(40 + 20 + CraftingInterfaceLayout.OPTION_GAP, at[1]);
    }

    private static List<CraftingInterfaceLayout.Kind> kinds(final CraftingInterfaceLayout.Shape shape) {
        return CraftingInterfaceLayout.rows(shape).stream().map(CraftingInterfaceLayout.Row::kind).distinct()
                .toList();
    }
}
