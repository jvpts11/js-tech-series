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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.gui.layout.CdeFrontPanelLayout.Control;
import dev.jstech.computers.gui.layout.CdeFrontPanelLayout.Rect;
import dev.jstech.core.gui.layout.GuiLayout;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CdeFrontPanelLayoutTest {

    /** The desktop as wide as the glass itself, and as it is when it is drawn at three quarters. */
    private static final int[][] DESKTOPS = {{384, 256}, {512, 341}, {640, 427}};

    @Test
    void layout_isCleanOnEveryDesktopSize() {
        for (final int[] size : DESKTOPS) {
            final GuiLayout l = CdeFrontPanelLayout.layout(size[0], size[1]);
            assertTrue(l.overlaps().isEmpty(), size[0] + " wide: " + l.overlaps());
            assertTrue(l.outOfBounds().isEmpty(), size[0] + " wide: " + l.outOfBounds());
        }
    }

    @Test
    void panel_standsCentredInsideItsBand() {
        for (final int[] size : DESKTOPS) {
            final Rect panel = CdeFrontPanelLayout.panel(size[0], size[1]);
            assertEquals(size[0] - (panel.x() + panel.w()), panel.x(), 1, "centred on a desktop " + size[0] + " wide");
            assertTrue(panel.y() >= size[1] - CdeFrontPanelLayout.BAND_H, "below the top of its band");
            assertTrue(panel.y() + panel.h() <= size[1], "and above the foot of the desktop");
        }
    }

    @Test
    void everyControlAndTheSwitch_sitInsideThePanel() {
        final Rect panel = CdeFrontPanelLayout.panel(512, 341);
        for (final Control control : Control.values()) {
            assertInside(panel, CdeFrontPanelLayout.control(control, 512, 341), control.name());
        }
        assertInside(panel, CdeFrontPanelLayout.switchWell(512, 341), "the switch");
    }

    @Test
    void theSwitch_standsBetweenTheFourthControlAndTheFifth() {
        final Rect well = CdeFrontPanelLayout.switchWell(512, 341);
        final Rect editor = CdeFrontPanelLayout.control(Control.EDITOR, 512, 341);
        final Rect style = CdeFrontPanelLayout.control(Control.STYLE, 512, 341);
        assertTrue(editor.x() + editor.w() <= well.x(), "the Text Editor ends before the switch begins");
        assertTrue(well.x() + well.w() <= style.x(), "and the Style Manager begins after it ends");
    }

    @Test
    void workspaces_areTwoOverTwoWithOneAndTwoAbove() {
        final Rect one = CdeFrontPanelLayout.workspace(0, 512, 341);
        final Rect two = CdeFrontPanelLayout.workspace(1, 512, 341);
        final Rect three = CdeFrontPanelLayout.workspace(2, 512, 341);
        final Rect four = CdeFrontPanelLayout.workspace(3, 512, 341);
        assertEquals(one.y(), two.y());
        assertEquals(three.y(), four.y());
        assertEquals(one.x(), three.x());
        assertTrue(two.x() > one.x() && three.y() > one.y());
    }

    @Test
    void exit_isAsTallAsTheTwoRowsBesideIt() {
        final Rect exit = CdeFrontPanelLayout.exit(512, 341);
        final Rect one = CdeFrontPanelLayout.workspace(0, 512, 341);
        final Rect four = CdeFrontPanelLayout.workspace(3, 512, 341);
        assertEquals(one.y(), exit.y());
        assertTrue(exit.y() + exit.h() >= four.y() + four.h());
        assertTrue(exit.x() >= four.x() + four.w());
    }

    @Test
    void everyControl_hasANameOfItsOwn() {
        final Set<String> tips = new HashSet<>();
        for (final Control control : Control.values()) {
            assertFalse(control.tip().english().isBlank(), control.name());
            assertTrue(tips.add(control.tip().english()), control + " shares its name");
        }
    }

    @Test
    void controlAt_findsEachControlAndNoneOffThem() {
        for (final Control control : Control.values()) {
            final Rect r = CdeFrontPanelLayout.control(control, 512, 341);
            assertEquals(control, CdeFrontPanelLayout.controlAt(r.x() + 1, r.y() + 1, 512, 341));
        }
        final Rect well = CdeFrontPanelLayout.switchWell(512, 341);
        assertNull(CdeFrontPanelLayout.controlAt(well.x() + 2, well.y() + 2, 512, 341));
    }

    @Test
    void tip_standsAboveThePanelAndOnTheDesktopAtEitherEnd() {
        for (final int[] size : DESKTOPS) {
            final Rect panel = CdeFrontPanelLayout.panel(size[0], size[1]);
            for (final Control control : Control.values()) {
                final Rect tip = CdeFrontPanelLayout.tip(control, 90, 12, size[0], size[1]);
                assertTrue(tip.y() + tip.h() <= panel.y(), control + " tip covers the panel");
                assertTrue(tip.x() >= 0 && tip.x() + tip.w() <= size[0], control + " tip leaves the desktop");
            }
        }
    }

    @Test
    void holds_takesTheLeftAndTopEdgeAndLeavesTheRightAndBottomOne() {
        final Rect r = new Rect(10, 20, 30, 40);
        assertTrue(r.holds(10, 20));
        assertTrue(r.holds(39.5, 59.5));
        assertFalse(r.holds(40, 20));
        assertFalse(r.holds(10, 60));
    }

    private static void assertInside(final Rect outer, final Rect inner, final String what) {
        assertTrue(inner.x() >= outer.x() && inner.y() >= outer.y()
                        && inner.x() + inner.w() <= outer.x() + outer.w()
                        && inner.y() + inner.h() <= outer.y() + outer.h(),
                what + " runs outside the panel: " + inner + " in " + outer);
    }
}
