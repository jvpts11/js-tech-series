/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.term;

import dev.jstech.computers.gui.term.TermGrid;
import dev.jstech.core.font.GridSpan;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Single lines written the way a terminal writes them: in a size of the terminal font, one character to a cell. The
 * console editors write their title rows, keys and text this way, and a text-mode installer its pages.
 *
 * <p>A line is measured in cells, not in the pixels of a font whose letters differ in width, so a column of names
 * lines up, a bar sized to a word fits it, and a line cut to a column is cut at a character.
 */
public final class TermText {

    private TermText() {
    }

    /**
     * Writes a line with the top left of its first cell at ({@code x}, {@code y}), in the units of the pose.
     *
     * @param ground the colour the line is written on
     */
    public static void draw(final TermFace face, final GuiGraphics g, final Font font, final String line, final int x,
                            final int y, final int colour, final int ground) {
        if (!line.isEmpty()) {
            face.lines().drawOnce(g, font, List.of(new GridSpan<>(line, colour)), x, y, face.height(),
                    Integer::intValue, ground);
        }
    }

    /**
     * Writes a line at that scale, the top left of its first cell at ({@code x}, {@code y}) in the pose's units, on a
     * ground nobody has named, which is what a page of a machine's own words is: it is written on whatever the frame
     * round it painted.
     */
    public static void draw(final TermFace face, final GuiGraphics g, final Font font, final String line, final int x,
                            final int y, final float scale, final int colour) {
        if (line.isEmpty()) {
            return;
        }
        g.pose().pushPose();
        g.pose().translate(x, y, 0.0F);
        g.pose().scale(scale, scale, 1.0F);
        draw(face, g, font, line, 0, 0, colour, 0);
        g.pose().popPose();
    }

    /** The same, centred on {@code cx}. */
    public static void centred(final TermFace face, final GuiGraphics g, final Font font, final String line,
                               final int cx, final int y, final float scale, final int colour) {
        draw(face, g, font, line, cx - width(face, line, scale) / 2, y, scale, colour);
    }

    /** The same, ending at {@code right}. */
    public static void right(final TermFace face, final GuiGraphics g, final Font font, final String line,
                             final int right, final int y, final float scale, final int colour) {
        draw(face, g, font, line, right - width(face, line, scale), y, scale, colour);
    }

    /** How wide a line comes out, in the pose's units. */
    public static int width(final TermFace face, final String line) {
        return TermGrid.cells(line) * face.width();
    }

    /** How wide a line comes out at that scale, rounded to whole units. */
    public static int width(final TermFace face, final String line, final float scale) {
        return Math.round(width(face, line) * scale);
    }

    /** The first characters of a line, as many as that many units across hold. */
    public static String first(final TermFace face, final String line, final int room) {
        return TermGrid.first(line, room / face.width());
    }

    /** A line cut to the room it has at that scale, ending in three dots where it had to be cut. */
    public static String clip(final TermFace face, final String line, final int room, final float scale) {
        return TermGrid.clip(line, (int) (room / (face.width() * scale)));
    }
}
