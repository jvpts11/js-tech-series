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
import java.util.List;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

/**
 * A size the terminals write in: the font it is drawn from, and its cell.
 *
 * <p>The terminals write in the game's own font, laid on a grid of cells six of its pixels wide and ten tall
 * ({@link TermGrid}), so they read like the rest of the game and their columns still line up. A terminal window on a
 * desktop draws it at the desktop's own scale, and a monitor's whole glass at the machine's display scale, the same
 * one, as near it as crisp letters and its columns allow.
 */
public final class TermFace {

    private final @Nullable CellFont font;
    private final TermGrid.Cell cell;
    private final GridPainter<Integer> lines;

    /** The size a terminal window draws in: the game's font on cells six wide and ten tall. */
    public static final TermFace SMALL = new TermFace(null, TermGrid.SIZES.get(0));
    private static final List<TermFace> SIZES = List.of(SMALL);

    private TermFace(final @Nullable CellFont font, final TermGrid.Cell cell) {
        this.font = font;
        this.cell = cell;
        this.lines = new GridPainter<>(font);
    }

    /** The size at that place among the sizes, smallest first. */
    public static TermFace of(final int size) {
        return SIZES.get(Math.max(0, Math.min(SIZES.size() - 1, size)));
    }

    /**
     * The size, and the scale against its own pixels, that a glass that many GUI pixels across draws that many columns
     * at on this screen, for a machine at that display scale.
     *
     * @param scalePercent the machine's display scale in percent, 0 for the default ({@link TermGrid#scaleOf})
     */
    public static Fitted forGlass(final int glassWidth, final int columns, final int scalePercent) {
        final TermGrid.Fit fit = TermGrid.fit(glassWidth, Minecraft.getInstance().getWindow().getGuiScale(), columns,
                TermGrid.scaleOf(scalePercent));
        return new Fitted(of(fit.size()), fit.scale());
    }

    /** The font of cells this size is drawn in, or null for the game's own. */
    public @Nullable CellFont font() {
        return font;
    }

    /** How wide a cell is, in the font's own pixels. */
    public int width() {
        return cell.width();
    }

    /** How tall a row is, in the font's own pixels, so box lines and blocks meet row to row. */
    public int height() {
        return cell.height();
    }

    /** The painter single lines in this size are drawn with. */
    GridPainter<Integer> lines() {
        return lines;
    }

    /** A size of the font and the scale it is drawn at. */
    public record Fitted(TermFace face, float scale) {
    }
}
