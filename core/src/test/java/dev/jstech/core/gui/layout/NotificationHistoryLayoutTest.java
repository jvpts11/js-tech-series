/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class NotificationHistoryLayoutTest {

    /** The smallest screen the game lays its menus out on, and a large one. */
    private static final int SMALL_WIDTH = 320;
    private static final int SMALL_HEIGHT = 240;
    private static final int LARGE_WIDTH = 480;
    private static final int LARGE_HEIGHT = 270;
    /** The widest labels: the filters, "Mods: J's Computers", "Place: Bottom right" and "Do not disturb". */
    private static final List<Integer> FILTERS = List.of(18, 36, 48, 66);
    private static final int MODS = 114;
    private static final int PLACE = 108;
    private static final int CHECKBOX = 78;

    @Test
    void layout_hasNoOverlapAndStaysInsideOnTheSmallestScreen() {
        final NotificationHistoryLayout.Plan plan = NotificationHistoryLayout.plan(SMALL_WIDTH, SMALL_HEIGHT, FILTERS,
                MODS, PLACE, CHECKBOX);
        final GuiLayout layout = NotificationHistoryLayout.layout(plan, SMALL_WIDTH, SMALL_HEIGHT);

        assertEquals(List.of(), layout.overlaps());
        assertEquals(List.of(), layout.outOfBounds());
    }

    @Test
    void layout_hasNoOverlapAndStaysInsideOnALargeScreen() {
        final NotificationHistoryLayout.Plan plan = NotificationHistoryLayout.plan(LARGE_WIDTH, LARGE_HEIGHT, FILTERS,
                MODS, PLACE, CHECKBOX);
        final GuiLayout layout = NotificationHistoryLayout.layout(plan, LARGE_WIDTH, LARGE_HEIGHT);

        assertEquals(List.of(), layout.overlaps());
        assertEquals(List.of(), layout.outOfBounds());
    }

    @Test
    void plan_putsTheChoicesUnderTheFiltersOnANarrowScreen() {
        final NotificationHistoryLayout.Plan narrow = NotificationHistoryLayout.plan(SMALL_WIDTH, SMALL_HEIGHT,
                FILTERS, MODS, PLACE, CHECKBOX);
        final NotificationHistoryLayout.Plan wide = NotificationHistoryLayout.plan(LARGE_WIDTH * 2, LARGE_HEIGHT,
                FILTERS, MODS, PLACE, CHECKBOX);

        assertTrue(narrow.mods().y() > narrow.filters().get(0).y());
        assertEquals(wide.filters().get(0).y(), wide.mods().y());
    }

    @Test
    void list_showsSeveralRowsOnTheSmallestScreen() {
        final NotificationHistoryLayout.Plan plan = NotificationHistoryLayout.plan(SMALL_WIDTH, SMALL_HEIGHT, FILTERS,
                MODS, PLACE, CHECKBOX);

        assertTrue(plan.list().h() / NotificationHistoryLayout.ROW >= 4);
    }

    @Test
    void row_leavesRoomForWhatANotificationSays() {
        final NotificationHistoryLayout.Row row = NotificationHistoryLayout.row(NotificationHistoryLayout.PAD,
                SMALL_WIDTH - 2 * NotificationHistoryLayout.PAD);

        assertTrue(row.textRoom() >= 80);
        assertTrue(row.modX() > row.textX() + row.textRoom());
        assertTrue(row.crossX() > row.timeRight());
    }
}
