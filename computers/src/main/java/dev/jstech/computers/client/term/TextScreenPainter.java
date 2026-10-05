/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.term;

import dev.jstech.core.gui.TextScreen;
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
            start = 0;
            final String line = screen.text(row);
            while (start < screen.columns()) {
                final int ink = screen.ink(start, row);
                final int ground = screen.ground(start, row);
                int end = start + 1;
                while (end < screen.columns() && screen.ink(end, row) == ink && screen.ground(end, row) == ground) {
                    end++;
                }
                final String run = line.substring(start, end);
                if (!run.isBlank()) {
                    TermText.draw(face, g, font, run, x + start * cell, ry, ink, ground);
                }
                start = end;
            }
        }
    }
}
