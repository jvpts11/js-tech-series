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

public final class ThisPcLayoutTest {

    @Test
    public void layout_isCleanAtEverySize() {
        for (final int[] size : new int[][]{{ThisPcLayout.MIN_W, ThisPcLayout.MIN_H},
                {ThisPcLayout.DEFAULT_W, ThisPcLayout.DEFAULT_H}, {420, 300}}) {
            final GuiLayout l = ThisPcLayout.layout(size[0], size[1]);
            assertTrue(l.isClean(), size[0] + "x" + size[1] + ": " + l.overlaps() + " " + l.outOfBounds());
        }
    }

    @Test
    public void driveRow_textNeverRunsUnderTheButtons() {
        for (final int width : new int[]{ThisPcLayout.MIN_W, ThisPcLayout.DEFAULT_W, 420}) {
            for (int buttons = 0; buttons <= 3; buttons++) {
                final GuiLayout l = ThisPcLayout.driveRow(width, buttons);
                assertTrue(l.isClean(), width + " with " + buttons + ": " + l.overlaps() + " " + l.outOfBounds());
            }
        }
    }

    @Test
    public void driveRow_keepsRoomForTheTextAtTheMinimumWidthWithThreeButtons() {
        // Three buttons on the narrowest window still leave a name's worth of room.
        assertTrue(ThisPcLayout.driveTextMaxW(ThisPcLayout.MIN_W, 3) >= 100);
    }

    @Test
    public void buttons_areRightAlignedInOrder() {
        final int width = ThisPcLayout.DEFAULT_W;
        assertTrue(ThisPcLayout.buttonX(width, 0, 3) < ThisPcLayout.buttonX(width, 1, 3));
        assertTrue(ThisPcLayout.buttonX(width, 1, 3) < ThisPcLayout.buttonX(width, 2, 3));
        assertEquals(width - 3, ThisPcLayout.buttonX(width, 2, 3) + ThisPcLayout.BTN_W);
    }

    @Test
    public void programGrid_tilesWithoutTouching() {
        for (final int width : new int[]{ThisPcLayout.MIN_W, ThisPcLayout.DEFAULT_W, 420}) {
            final GuiLayout l = ThisPcLayout.programGrid(width, 7);
            assertTrue(l.isClean(), width + ": " + l.overlaps() + " " + l.outOfBounds());
            assertTrue(ThisPcLayout.programColumns(width) >= 4, "at least four columns at " + width);
        }
    }

    @Test
    public void kdeAbout_isClean() {
        final GuiLayout l = ThisPcLayout.KdeAbout.layout();
        assertTrue(l.isClean(), l.overlaps() + " " + l.outOfBounds());
    }

    @Test
    public void kdeAbout_longestLabelNeverRunsIntoTheValueColumn() {
        // The label column has to clear the longest label in either language before the value starts.
        final GuiLayout l = ThisPcLayout.KdeAbout.layout();
        final GuiLayout.Box label = l.boxAt("software-label0");
        assertTrue(label.x() + label.width() <= ThisPcLayout.KdeAbout.VALUE_X,
                "the label reaches " + (label.x() + label.width()) + ", the value starts at "
                        + ThisPcLayout.KdeAbout.VALUE_X);
    }

    @Test
    public void kdeAbout_valueColumnHoldsTheLongestProcessorLine() {
        final String longestProcessorLine = "Integra Vertex III-S 1400, 1400 MHz";
        final int neededWidth = Math.round(longestProcessorLine.length() * GuiLayout.GLYPH_WIDTH);
        assertTrue(ThisPcLayout.KdeAbout.VALUE_W >= neededWidth,
                "the value column is " + ThisPcLayout.KdeAbout.VALUE_W + " px, needs " + neededWidth);
    }

    @Test
    public void kdeAbout_rowsFallInsideTheirSection() {
        // The last row of Software (four rows) must still land above where Hardware's header starts.
        final int lastRow = ThisPcLayout.KdeAbout.rowY(ThisPcLayout.KdeAbout.SOFTWARE_Y, 3);
        assertTrue(lastRow + ThisPcLayout.ABOUT_ROW_H <= ThisPcLayout.KdeAbout.HARDWARE_Y);
    }

    @Test
    public void gnomeAbout_isClean() {
        final GuiLayout l = ThisPcLayout.GnomeAbout.layout();
        assertTrue(l.isClean(), l.overlaps() + " " + l.outOfBounds());
    }

    @Test
    public void gnomeAbout_longestLabelNeverRunsIntoTheValueColumn() {
        final GuiLayout l = ThisPcLayout.GnomeAbout.layout();
        final GuiLayout.Box label = l.boxAt("list1-label0");
        final int valueX = 4 + ThisPcLayout.GnomeAbout.LABEL_W;
        assertTrue(label.x() + label.width() <= valueX,
                "the label reaches " + (label.x() + label.width()) + ", the value starts at " + valueX);
    }

    @Test
    public void gnomeAbout_valueColumnHoldsTheLongestProcessorLine() {
        final String longestProcessorLine = "Integra Vertex III-S 1400, 1400 MHz";
        final int neededWidth = Math.round(longestProcessorLine.length() * GuiLayout.GLYPH_WIDTH);
        final int valueW = ThisPcLayout.GnomeAbout.W - ThisPcLayout.GnomeAbout.LABEL_W - 8;
        assertTrue(valueW >= neededWidth, "the value column is " + valueW + " px, needs " + neededWidth);
    }

    @Test
    public void gnomeAbout_secondListFallsBelowTheFirst() {
        final int list1Bottom = ThisPcLayout.GnomeAbout.LIST1_Y + ThisPcLayout.GnomeAbout.LIST1_H;
        assertTrue(list1Bottom <= ThisPcLayout.GnomeAbout.LIST2_Y);
    }

    @Test
    public void cinnamonAbout_isClean() {
        final GuiLayout l = ThisPcLayout.CinnamonAbout.layout();
        assertTrue(l.isClean(), l.overlaps() + " " + l.outOfBounds());
    }

    @Test
    public void cinnamonAbout_longestLabelNeverRunsIntoTheValueColumn() {
        final GuiLayout l = ThisPcLayout.CinnamonAbout.layout();
        final GuiLayout.Box label = l.boxAt("list-label0");
        assertTrue(label.x() + label.width() <= ThisPcLayout.CinnamonAbout.VALUE_X,
                "the label reaches " + (label.x() + label.width()) + ", the value starts at "
                        + ThisPcLayout.CinnamonAbout.VALUE_X);
    }

    @Test
    public void cinnamonAbout_valueColumnHoldsTheKernelAndDiskLinesWhole() {
        // Values draw at the labels' own size; only the longest processor names are ever trimmed to the column,
        // so the lines every machine writes must fit whole at the audit's six pixels a letter.
        for (final String line : new String[]{"14.1-RELEASE GENERIC", "2 TB, 1.8 TB used", "3072 MB VRAM"}) {
            final int neededWidth = Math.round(line.length() * GuiLayout.GLYPH_WIDTH);
            assertTrue(ThisPcLayout.CinnamonAbout.VALUE_W >= neededWidth,
                    "\"" + line + "\" needs " + neededWidth + " px, the column is "
                            + ThisPcLayout.CinnamonAbout.VALUE_W);
        }
    }

    @Test
    public void aboutHeroes_subtitleClearsTheScaledTitleAndItsShadow() {
        // A glyph is eight pixels tall and its shadow one more, both drawn at the hero's own scale.
        final int kdeNeeds = (int) Math.ceil(9 * ThisPcLayout.KdeAbout.HERO_SCALE);
        assertTrue(ThisPcLayout.KdeAbout.SUBTITLE_Y - ThisPcLayout.KdeAbout.TITLE_Y >= kdeNeeds,
                "KDE's subtitle sits on the title's shadow");
        final int gnomeNeeds = (int) Math.ceil(9 * ThisPcLayout.GnomeAbout.HERO_SCALE);
        assertTrue(ThisPcLayout.GnomeAbout.SUBTITLE_Y - ThisPcLayout.GnomeAbout.TITLE_Y >= gnomeNeeds,
                "GNOME's subtitle sits on the title's shadow");
    }

    @Test
    public void barWidth_endsBeforeTheWordsSayingWhatIsFree() {
        // "8128 of 8192 it free" at the audit's six pixels a letter, beside none to three buttons in every width.
        final int usageW = Math.round("8128 of 8192 it free".length() * GuiLayout.GLYPH_WIDTH);
        for (final int width : new int[]{ThisPcLayout.MIN_W, ThisPcLayout.DEFAULT_W, 420}) {
            for (int buttons = 0; buttons <= 3; buttons++) {
                final int maxW = ThisPcLayout.driveTextMaxW(width, buttons);
                final int barW = ThisPcLayout.barWidth(maxW, usageW);
                assertTrue(barW >= ThisPcLayout.BAR_MIN_W, "the bar keeps a length of its own");
                if (maxW - usageW - ThisPcLayout.BAR_GAP >= ThisPcLayout.BAR_MIN_W) {
                    assertTrue(barW + ThisPcLayout.BAR_GAP <= maxW - usageW,
                            "at " + width + " with " + buttons + " buttons the bar runs into the words");
                }
            }
        }
    }

    @Test
    public void cinnamonAbout_sevenRowsFitInsideTheWindow() {
        final int lastRow = ThisPcLayout.CinnamonAbout.rowY(ThisPcLayout.CinnamonAbout.LIST_ROWS - 1);
        assertTrue(lastRow + ThisPcLayout.ABOUT_ROW_H <= ThisPcLayout.CinnamonAbout.H);
    }
}
