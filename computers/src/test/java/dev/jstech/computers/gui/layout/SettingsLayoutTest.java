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

public final class SettingsLayoutTest {

    /** The Settings app's own minimum content width, the personalize page's narrowest case. */
    private static final int MIN_CONTENT_W = 135;
    /** Every wallpaper offered today: the desktop's own plus the nine a player may pick. */
    private static final int MOST_WALLPAPERS = 10;

    @Test
    public void of_isCleanAtTheWorstCase() {
        // The flat skin shows every row this page ever draws, at the narrowest window with every wallpaper offered.
        final GuiLayout l = SettingsLayout.layout(MIN_CONTENT_W, MOST_WALLPAPERS, true, true);
        assertTrue(l.isClean(), l.overlaps() + " " + l.outOfBounds());
    }

    @Test
    public void of_isCleanOnTheBevelSkinWithNoWallpaperOffered() {
        // The bevel skin hides accent and theme; a system offering none of the extra wallpapers is the other edge.
        final GuiLayout l = SettingsLayout.layout(MIN_CONTENT_W, 1, false, false);
        assertTrue(l.isClean(), l.overlaps() + " " + l.outOfBounds());
    }

    @Test
    public void of_wrapsMoreRowsAtANarrowerWidth() {
        final SettingsLayout.Offsets narrow = SettingsLayout.of(MIN_CONTENT_W, MOST_WALLPAPERS, true, true);
        final SettingsLayout.Offsets wide = SettingsLayout.of(300, MOST_WALLPAPERS, true, true);
        assertTrue(narrow.gridRows() >= wide.gridRows(),
                "a narrower window fits no more columns, so it never takes fewer rows");
    }

    @Test
    public void of_hidesAccentAndThemeOnlyOnTheBevelSkin() {
        final SettingsLayout.Offsets bevel = SettingsLayout.of(MIN_CONTENT_W, MOST_WALLPAPERS, false, false);
        assertTrue(!bevel.richSkin() && !bevel.flatSkin(), "the bevel skin shows neither section");
        final SettingsLayout.Offsets rich = SettingsLayout.of(MIN_CONTENT_W, MOST_WALLPAPERS, true, false);
        assertTrue(rich.richSkin() && !rich.flatSkin(), "a richer, non-flat skin shows accent and theme alone");
    }

    @Test
    public void of_stacksEverySectionInOrderWithNoGapOrOverlap() {
        // Every row band, in the order the page draws them, each one starting where the one before it ends.
        final SettingsLayout.Offsets o = SettingsLayout.of(MIN_CONTENT_W, MOST_WALLPAPERS, true, true);
        assertTrue(o.wallpaperCaptionY() < o.gridY());
        assertTrue(o.gridY() < o.accentCaptionY());
        assertTrue(o.accentCaptionY() < o.accentY());
        assertTrue(o.accentY() < o.themeCaptionY());
        assertTrue(o.themeCaptionY() < o.themeY());
        assertTrue(o.themeY() < o.clockCaptionY());
        assertTrue(o.clockCaptionY() < o.clockY());
        assertTrue(o.clockY() < o.taskbarCaptionY());
        assertTrue(o.taskbarCaptionY() < o.taskbarY());
        assertTrue(o.taskbarY() < o.appearanceCaptionY());
        assertTrue(o.appearanceCaptionY() < o.appearanceY());
        assertTrue(o.appearanceY() < o.contentHeight());
    }

    @Test
    public void swatchColumns_neverGoesBelowOne() {
        assertEquals(1, SettingsLayout.swatchColumns(0));
        assertTrue(SettingsLayout.swatchColumns(MIN_CONTENT_W) >= 1);
    }

    @Test
    public void swatchColumns_leaveRoomForTheScrollThumb() {
        // Every width the page can take, so a width where one more column would just touch the thumb is met.
        for (int w = MIN_CONTENT_W; w <= MIN_CONTENT_W + 4 * (SettingsLayout.SWATCH_W + SettingsLayout.SWATCH_GAP);
             w++) {
            final int columns = SettingsLayout.swatchColumns(w);
            final int gridRight = columns * (SettingsLayout.SWATCH_W + SettingsLayout.SWATCH_GAP)
                    - SettingsLayout.SWATCH_GAP;
            assertTrue(gridRight <= w - SettingsLayout.SCROLL_THUMB_SPACE,
                    "at " + w + " px the last column reaches " + gridRight + ", the thumb starts at "
                            + (w - SettingsLayout.SCROLL_THUMB_SPACE));
        }
    }

    @Test
    public void swatchX_tilesAcrossTheColumnsWithoutTouching() {
        final int columns = SettingsLayout.swatchColumns(MIN_CONTENT_W);
        for (int i = 0; i < columns; i++) {
            assertEquals(i * (SettingsLayout.SWATCH_W + SettingsLayout.SWATCH_GAP),
                    SettingsLayout.swatchX(i, columns));
        }
        // The column wraps back to the left edge once it is full.
        assertEquals(0, SettingsLayout.swatchX(columns, columns));
    }
}
