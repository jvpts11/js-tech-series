/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.client.guide;

import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.guide.GuidePalettes;
import dev.jstech.industrial.guide.PlantDrawing;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * What the Plant Drawings' own drawings are drawn with: one-pixel lines, dashed or whole, circles, frames and the
 * lettering of a drawing, in the colours of the sheet they stand on, from the drawing's top left corner.
 */
final class DrawingPen {

    private final GuiGraphics graphics;
    private final Font font;
    private final int originX;
    private final int originY;
    private final GuidePalettes.Binder colours;

    /** How large the small lettering of a drawing is, as the manual letters its notes. */
    private static final float SMALL = 0.75F;
    /** How long a dash of a dashed line is, and the gap after it. */
    private static final int DASH = 2;
    /** How far apart the lines of lettering run on under one another. */
    static final int LINE = 9;

    DrawingPen(final GuiGraphics graphics, final Font font, final int x, final int y) {
        this.graphics = graphics;
        this.font = font;
        this.originX = x;
        this.originY = y;
        this.colours = DrawingsGuidePalette.DRAWINGS.get();
    }

    /**
     * The words a drawing's data holds under {@code field}: a tag as it is, or a note in the player's language when
     * its key is written beside it, its English when the language has none.
     */
    static String words(final CompoundTag data, final String field) {
        final String english = data.getString(field);
        final String key = data.getString(field + PlantDrawing.KEY);
        return key.isEmpty() ? english : Language.getInstance().getOrDefault(key, english);
    }

    /** The sheet's ink: its lines and its lettering. */
    int ink() {
        return this.colours.ink();
    }

    /** The sheet's faint colour, for tags and notes. */
    int faint() {
        return this.colours.faint();
    }

    /** The sheet's accent, the pencil yellow of what is pointed at. */
    int accent() {
        return this.colours.accent();
    }

    /** The sheet itself, to draw over what is under a symbol. */
    int paper() {
        return this.colours.paper();
    }

    void fill(final int x0, final int y0, final int x1, final int y1, final int colour) {
        this.graphics.fill(this.originX + x0, this.originY + y0, this.originX + x1, this.originY + y1, colour);
    }

    void dot(final int x, final int y, final int colour) {
        this.fill(x, y, x + 1, y + 1, colour);
    }

    /** A frame one pixel wide, from {@code x0}, {@code y0} to its far corner {@code x1}, {@code y1}, which it holds. */
    void frame(final int x0, final int y0, final int x1, final int y1, final int colour) {
        this.fill(x0, y0, x1 + 1, y0 + 1, colour);
        this.fill(x0, y1, x1 + 1, y1 + 1, colour);
        this.fill(x0, y0, x0 + 1, y1 + 1, colour);
        this.fill(x1, y0, x1 + 1, y1 + 1, colour);
    }

    /** A straight line from one place to another, both held, whole or dashed. */
    void line(final int x0, final int y0, final int x1, final int y1, final boolean dashed, final int colour) {
        final int steps = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
        for (int i = 0; i <= steps; i++) {
            if (dashed && i / DASH % 2 == 1) {
                continue;
            }
            final double t = steps == 0 ? 0.0 : (double) i / steps;
            this.dot((int) Math.round(x0 + (x1 - x0) * t), (int) Math.round(y0 + (y1 - y0) * t), colour);
        }
    }

    /** An arrow's head at {@code x1}, {@code y1}, pointing the way from {@code x0}, {@code y0}. */
    void arrowHead(final int x0, final int y0, final int x1, final int y1, final int colour) {
        final double angle = Math.atan2(y1 - y0, x1 - x0);
        for (final double side : new double[] {Math.PI * 5 / 6, -Math.PI * 5 / 6}) {
            this.line(x1, y1, (int) Math.round(x1 + 4 * Math.cos(angle + side)),
                    (int) Math.round(y1 + 4 * Math.sin(angle + side)), false, colour);
        }
    }

    /** A circle of radius {@code r} about {@code cx}, {@code cy}, filled with the sheet inside it when asked. */
    void circle(final int cx, final int cy, final int r, final int colour, final boolean filled) {
        for (int y = cy - r - 1; y <= cy + r + 1; y++) {
            for (int x = cx - r - 1; x <= cx + r + 1; x++) {
                final double d = Math.hypot(x - cx, y - cy);
                if (filled && d < r - 0.5) {
                    this.dot(x, y, this.paper());
                }
                if (Math.abs(d - r) < 0.5) {
                    this.dot(x, y, colour);
                }
            }
        }
    }

    /** Lettering at its full size, its top left at {@code x}, {@code y}, on the sheet. */
    void text(final String text, final int x, final int y, final int colour) {
        Draw.text(this.graphics, this.font, text, this.originX + x, this.originY + y, colour, this.paper());
    }

    /**
     * Lettering at its full size, run on under itself where it would pass {@code room} pixels; how many lines it took.
     */
    int wrapped(final String text, final int x, final int y, final int room, final int colour) {
        int lines = 0;
        for (final FormattedText line : this.font.getSplitter().splitLines(text, room, Style.EMPTY)) {
            this.text(line.getString(), x, y + lines * LINE, colour);
            lines++;
        }
        return Math.max(1, lines);
    }

    /** Small lettering, as a drawing writes its tags and notes. */
    void small(final String text, final int x, final int y, final int colour) {
        Draw.text(this.graphics, this.font, text, this.originX + x, this.originY + y, colour, this.paper(), SMALL);
    }

    /** Small lettering centred on {@code x}. */
    void smallCentred(final String text, final int x, final int y, final int colour) {
        this.small(text, Math.round(x - this.smallWidth(text) / 2.0F), y, colour);
    }

    /** How wide small lettering is. */
    float smallWidth(final String text) {
        return this.font.width(text) * SMALL;
    }

    /**
     * Keeps what is drawn next inside the drawing's place from {@code x0}, {@code y0} to {@code x1}, {@code y1}, until
     * {@link #unclip()}. The screen may have moved where drawing lands, and the game's clipping does not follow that,
     * so the place is moved the same way first.
     */
    void clip(final int x0, final int y0, final int x1, final int y1) {
        final Matrix4f pose = this.graphics.pose().last().pose();
        final Vector4f from = pose.transform(new Vector4f(this.originX + x0, this.originY + y0, 0.0F, 1.0F));
        final Vector4f to = pose.transform(new Vector4f(this.originX + x1, this.originY + y1, 0.0F, 1.0F));
        this.graphics.enableScissor((int) Math.floor(from.x()), (int) Math.floor(from.y()), (int) Math.ceil(to.x()),
                (int) Math.ceil(to.y()));
    }

    void unclip() {
        this.graphics.disableScissor();
    }

    /** Where a drawing stands on the screen, for what it must draw itself, such as a block's own picture. */
    int originX() {
        return this.originX;
    }

    int originY() {
        return this.originY;
    }

    GuiGraphics graphics() {
        return this.graphics;
    }
}
