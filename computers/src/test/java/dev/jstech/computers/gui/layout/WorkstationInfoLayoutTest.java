/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.gui.layout.CdeFrontPanelLayout.Rect;
import dev.jstech.core.gui.layout.GuiLayout;
import org.junit.jupiter.api.Test;

class WorkstationInfoLayoutTest {

    /** A network's id as the window writes it, in the small text: thirty-six letters six units wide at 0.85. */
    private static final int NETWORK_ID_W = 184;

    /** The longest label, Operating System, as the window writes it: 84 units wide at 0.85. */
    private static final int LONGEST_LABEL_W = 72;

    @Test
    void layout_isClean() {
        final GuiLayout l = WorkstationInfoLayout.layout();
        assertTrue(l.overlaps().isEmpty(), l.overlaps().toString());
        assertTrue(l.outOfBounds().isEmpty(), l.outOfBounds().toString());
    }

    @Test
    void window_fitsTheWorkAreaOfTheGlass() {
        assertTrue(WorkstationInfoLayout.H + WorkstationInfoLayout.FRAME_H <= 256 - CdeFrontPanelLayout.BAND_H);
        assertTrue(WorkstationInfoLayout.W + WorkstationInfoLayout.FRAME_W <= 384);
    }

    @Test
    void valueColumn_holdsANetworksIdOnOneLine() {
        assertTrue(WorkstationInfoLayout.valueWidth() >= NETWORK_ID_W,
                "value column " + WorkstationInfoLayout.valueWidth());
    }

    @Test
    void labelColumn_holdsTheLongestLabelInsideTheWell() {
        assertTrue(WorkstationInfoLayout.LABEL_RIGHT - LONGEST_LABEL_W >= WorkstationInfoLayout.innerX());
        assertTrue(WorkstationInfoLayout.LABEL_RIGHT < WorkstationInfoLayout.VALUE_X);
    }

    @Test
    void everyFact_standsInsideItsWell() {
        for (int g = 0; g < WorkstationInfoLayout.GROUPS.size(); g++) {
            final Rect well = WorkstationInfoLayout.group(g);
            final int last = WorkstationInfoLayout.rowY(g, WorkstationInfoLayout.GROUPS.get(g) - 1);
            assertTrue(WorkstationInfoLayout.headY(g) >= well.y(), "heading of group " + g);
            assertTrue(last + WorkstationInfoLayout.ROW_H <= well.y() + well.h(), "last fact of group " + g);
        }
    }

    @Test
    void groups_standOneUnderTheOtherAndAboveTheButton() {
        final Rect first = WorkstationInfoLayout.group(0);
        final Rect second = WorkstationInfoLayout.group(1);
        final Rect third = WorkstationInfoLayout.group(2);
        assertEquals(first.x(), third.x());
        assertTrue(first.y() + first.h() < second.y() && second.y() + second.h() < third.y());
        assertTrue(third.y() + third.h() < WorkstationInfoLayout.close().y() - 2);
    }

    @Test
    void meter_standsInsideTheHardwareWell() {
        final Rect well = WorkstationInfoLayout.group(2);
        final Rect meter = WorkstationInfoLayout.meter(2, 2);
        assertTrue(meter.x() + meter.w() <= well.x() + well.w());
        assertTrue(meter.y() >= well.y() && meter.y() + meter.h() <= well.y() + well.h());
    }
}
