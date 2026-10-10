/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.term;

import dev.jstech.core.font.GridSpan;
import dev.jstech.core.gui.TextScreen;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Paints a {@link TextScreen} in the terminal font, a cell at a time: each row's grounds laid down as runs of one
 * colour, then its characters written over them as runs of one ink, so a screen of sixteen colours costs a few
 * strokes a row rather than one a cell. The box lines and blocks fill their cells, so frames drawn in characters join.
 */
public final class TextScreenPainter {

    private TextScreenPainter() {
    }

    /**
     * Paints {@code screen} with the top left of its first cell at ({@code x}, {@code y}), its cells the face's width
     * apart and its rows {@code pitch} apart, in the units of the pose.
     */
    public static void paint(final GuiGraphics g, final Font font, final TermFace face, final TextScreen screen,
                             final int x, final int y, final int pitch) {
        final int cell = face.width();
        for (int row = 0; row < screen.rows(); row++) {
            final int ry = y + row * pitch;
            int start = 0;
            while (start < screen.columns()) {
                final int ground = screen.ground(start, row);
                int end = start + 1;
                while (end < screen.columns() && screen.ground(end, row) == ground) {
                    end++;
                }
                g.fill(x + start * cell, ry, x + end * cell, ry + pitch, ground);
                start = end;
            }
            // The whole row goes to the grid as one line of coloured runs, so it is laid out and sent once.
            final String line = screen.text(row);
            final List<GridSpan<Integer>> runs = new ArrayList<>();
            int shownRuns = 0;
            start = 0;
            while (start < screen.columns()) {
                final int ink = screen.ink(start, row);
                int end = start + 1;
                while (end < screen.columns() && screen.ink(end, row) == ink) {
                    end++;
                }
                final String run = line.substring(start, end);
                runs.add(new GridSpan<>(run, ink));
                if (!run.isBlank()) {
                    shownRuns = runs.size();
                }
                start = end;
            }
            // Blanks after the last character draw nothing, so they are left off.
            if (shownRuns > 0) {
                TermText.drawRuns(face, g, font, runs.subList(0, shownRuns), x, ry);
            }
        }
    }
}
