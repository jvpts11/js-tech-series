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
import net.minecraft.network.chat.Component;
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
     * Text written on a ground of that colour, plain, with no shadow under it.
     *
     * <p>The series draws its letters plain: a shadow, even one worked out from the letter and the ground as
     * {@link TextShadow} does, made small text harder to read on many of its screens. The ground is still said in
     * every call, and the grounds are still declared ({@link Grounds}), so a shadow can come back to the places it
     * suits without touching the callers.
     *
     * @param ground the colour of what the text is written on
     */
    public static void text(final GuiGraphics g, final Font font, final String text, final int x, final int y,
                            final int color, final int ground) {
        g.drawString(font, text, x, y, color, false);
    }

    /** The same text drawn at {@code scale} and anchored at its top-left corner: a hero title larger than the rest. */
    public static void text(final GuiGraphics g, final Font font, final String text, final int x, final int y,
                            final int color, final int ground, final float scale) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1.0f);
        g.drawString(font, text, 0, 0, color, false);
        g.pose().popPose();
    }

    /** Text on whatever ground was declared under it ({@link Grounds}). */
    public static void text(final GuiGraphics g, final Font font, final String text, final int x, final int y,
                            final int color) {
        g.drawString(font, text, x, y, color, false);
    }

    /** Text centred on {@code cx}, on whatever ground was declared under it. */
    public static void textCentered(final GuiGraphics g, final Font font, final String text, final int cx,
                                    final int y, final int color) {
        text(g, font, text, cx - font.width(text) / 2, y, color);
    }

    /** A line of game text, styles and all, written on a ground of that colour. */
    public static void text(final GuiGraphics g, final Font font, final Component text, final int x, final int y,
                            final int color, final int ground) {
        g.drawString(font, text, x, y, color, false);
    }

    /** A line of game text, styles and all, centred on {@code cx}, on the ground declared under it. */
    public static void textCentered(final GuiGraphics g, final Font font, final Component text, final int cx,
                                    final int y, final int color) {
        g.drawString(font, text, cx - font.width(text) / 2, y, color, false);
    }

    /** Text at {@code scale}, anchored at its top-left corner, on whatever ground was declared under it. */
    public static void textScaled(final GuiGraphics g, final Font font, final String text, final int x, final int y,
                                  final int color, final float scale) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1.0f);
        g.drawString(font, text, 0, 0, color, false);
        g.pose().popPose();
    }

    /** Fades a rectangle out, the way a disabled control is shown. */
    public static void disabled(final GuiGraphics g, final int x, final int y, final int w, final int h) {
        g.fill(x, y, x + w, y + h, ComponentPalette.get().disabled());
    }
}
