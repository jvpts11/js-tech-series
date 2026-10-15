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

class NotificationPlaceLayoutTest {

    private static final int SMALL_WIDTH = 320;
    private static final int SMALL_HEIGHT = 240;

    @Test
    void layout_hasNoOverlapAndStaysInsideOnTheSmallestScreen() {
        final GuiLayout layout = NotificationPlaceLayout.layout(SMALL_WIDTH, SMALL_HEIGHT);

        assertEquals(List.of(), layout.overlaps());
        assertEquals(List.of(), layout.outOfBounds());
    }

    @Test
    void map_takesMostOfTheScreen() {
        final NotificationHistoryLayout.Rect map = NotificationPlaceLayout.map(SMALL_WIDTH, SMALL_HEIGHT);

        assertTrue(map.w() >= 300 && map.h() >= 150);
    }

    @Test
    void where_leavesRoomForCoordinatesAndADimension() {
        assertTrue(NotificationPlaceLayout.where(SMALL_WIDTH, SMALL_HEIGHT).w() >= 200);
    }
}
