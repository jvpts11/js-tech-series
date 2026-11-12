/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

import dev.jstech.core.gui.layout.GuiLayout;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class SeekerLayoutTest {

    private static final int[][] SIZES = {{FilesLayout.MIN_W, FilesLayout.MIN_H},
        {FilesLayout.DEFAULT_W, FilesLayout.DEFAULT_H}, {420, 300}};

    @Test
    public void layout_isCleanInEveryFormAtEverySize() {
        for (final SeekerLayout.Form form : SeekerLayout.Form.values()) {
            for (final int[] size : SIZES) {
                final GuiLayout l = SeekerLayout.layout(form, size[0], size[1]);
                assertTrue(l.isClean(), form + " " + size[0] + "x" + size[1] + ": " + l.overlaps() + " "
                        + l.outOfBounds());
            }
        }
    }

    @Test
    public void listX_isAtTheEdgeOnTheQuinceSystemInFromItOnOs9AndPastTheSidebarOnOsX() {
        assertEquals(0, SeekerLayout.listX(SeekerLayout.Form.CLASSIC));
        assertEquals(SeekerLayout.INSET, SeekerLayout.listX(SeekerLayout.Form.PLATINUM));
        assertEquals(SeekerLayout.Form.X.sidebar() + 1, SeekerLayout.listX(SeekerLayout.Form.X));
    }

    @Test
    public void list_holdsTwoRowsOfTwoIconsAtTheSmallestWindowInEveryForm() {
        for (final SeekerLayout.Form form : SeekerLayout.Form.values()) {
            final int across = SeekerLayout.listW(form, FilesLayout.MIN_W) / form.cellW();
            final int down = SeekerLayout.listH(form, FilesLayout.MIN_H) / form.cellH();
            assertTrue(across >= 2 && down >= 2, form + " holds " + across + " by " + down);
        }
    }

    @Test
    public void footY_isWhereTheListEnds() {
        for (final SeekerLayout.Form form : SeekerLayout.Form.values()) {
            final int listEnd = SeekerLayout.listY(form) + SeekerLayout.listH(form, FilesLayout.DEFAULT_H);
            assertEquals(SeekerLayout.footY(form, FilesLayout.DEFAULT_H) - (form == SeekerLayout.Form.PLATINUM
                    ? SeekerLayout.INSET : 0), listEnd, form.toString());
        }
    }

    @Test
    public void viewX_standsPastForward() {
        assertTrue(SeekerLayout.viewX(0) > SeekerLayout.buttonX(1) + SeekerLayout.BUTTON_W);
        assertEquals(SeekerLayout.viewX(0) + SeekerLayout.VIEW_W, SeekerLayout.viewX(1));
    }
}
