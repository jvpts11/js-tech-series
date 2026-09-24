/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.component;

import dev.jstech.core.gui.TextShadow;
import dev.jstech.core.gui.layout.WindowGeometry;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Matrix4f;

/**
 * The few strokes a component draws itself, outside the skin: they carry no design of their own.
 */
public final class Draw {

    /**
     * Clips drawing to a rectangle given in the current pose's coordinates. The graphics' own scissor ignores
     * the pose, so a window drawn under a translation would otherwise clip in the wrong place.
     */
    public static void pushScissor(final GuiGraphics g, final int x1, final int y1, final int x2, final int y2) {
        final Matrix4f m = g.pose().last().pose();
        // The pose's translation and its scale both move the clip, since the clip is in screen units.
        final WindowGeometry.Rect r = WindowGeometry.scissor(m.m30(), m.m31(), m.m00(), m.m11(), x1, y1, x2, y2);
        g.enableScissor(r.x(), r.y(), r.x() + r.w(), r.y() + r.h());
    }

    /** Ends the clip {@link #pushScissor} began. */
    public static void popScissor(final GuiGraphics g) {
        g.disableScissor();
    }

    private Draw() {
    }

    /** A 1px outline of one colour. */
    public static void outline(final GuiGraphics g, final int x, final int y, final int w, final int h, final int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }

    /**
     * Text with a shadow under it that suits what it is written on.
     *
     * <p>The game's own shadow is a dark copy of the letter, which is a shadow on a dark ground and a smear on a
     * light one. This one is a unit down and to the right in the colour {@link TextShadow} works out from the
     * letter and the ground, so it is there on a black glass and on a cream panel alike.
     *
     * @param ground the colour of what the text is written on
     */
    public static void text(final GuiGraphics g, final Font font, final String text, final int x, final int y,
                            final int color, final int ground) {
        g.drawString(font, text, x + 1, y + 1, TextShadow.of(color, ground), false);
        g.drawString(font, text, x, y, color, false);
    }

    /** Fades a rectangle out, the way a disabled control is shown. */
    public static void disabled(final GuiGraphics g, final int x, final int y, final int w, final int h) {
        g.fill(x, y, x + w, y + h, ComponentPalette.get().disabled());
    }
}
