/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.term;

import dev.jstech.computers.gui.term.TermGrid;
import dev.jstech.core.client.font.GridPainter;
import dev.jstech.core.font.CellFont;
import dev.jstech.core.font.CoreFonts;
import java.util.List;
import net.minecraft.client.Minecraft;

/**
 * A size of the terminal font: the Core's font it is drawn from, and its cell.
 *
 * <p>The terminals draw in Misc Fixed, in whichever of its three sizes fits them best on the screen they are on
 * ({@link TermGrid#fit}): a terminal window on a desktop in the small one, one GUI pixel to each of its own, and a
 * monitor's whole glass in the size and at the scale that draws the largest crisp letters its columns leave room for.
 */
public final class TermFace {

    private final CellFont font;
    private final GridPainter<Integer> lines;

    /** The small size, 6x10, which a terminal window draws in. */
    public static final TermFace SMALL = new TermFace(CoreFonts.FIXED_6X10);
    private static final List<TermFace> SIZES =
            List.of(SMALL, new TermFace(CoreFonts.FIXED_9X15), new TermFace(CoreFonts.FIXED_10X20));

    private TermFace(final CellFont font) {
        this.font = font;
        this.lines = new GridPainter<>(font);
    }

    /** The size at that place among the font's sizes, smallest first. */
    public static TermFace of(final int size) {
        return SIZES.get(Math.max(0, Math.min(SIZES.size() - 1, size)));
    }

    /**
     * The size, and the scale against its own pixels, that a glass that many GUI pixels across draws that many columns
     * at on this screen.
     */
    public static Fitted forGlass(final int glassWidth, final int columns) {
        final TermGrid.Fit fit = TermGrid.fit(glassWidth, Minecraft.getInstance().getWindow().getGuiScale(), columns);
        return new Fitted(of(fit.size()), fit.scale());
    }

    public CellFont font() {
        return font;
    }

    /** How wide a cell is, in the font's own pixels. */
    public int width() {
        return font.cellWidth();
    }

    /** How tall a row is, in the font's own pixels: the font's height, so box lines and blocks meet row to row. */
    public int height() {
        return font.cellHeight();
    }

    /** The painter single lines in this size are drawn with. */
    GridPainter<Integer> lines() {
        return lines;
    }

    /** A size of the font and the scale it is drawn at. */
    public record Fitted(TermFace face, float scale) {
    }
}
