/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.gui.layout.UiLayout;
import dev.jstech.computers.operation.payload.UiWindowPayload;
import dev.jstech.core.client.gui.component.Draw;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** A program's canvas, drawn stroke by stroke as the program asked, and the pixel line other widgets draw with. */
final class SigmaCanvas {

    /**
     * The sixteen colours a program paints a canvas with, in the order it names them.
     *
     * <p>Sixteen rather than a free choice of colour, because a canvas is drawn by a machine whose
     * screen has this many and no more, and a program that says "green" means the same green on any of
     * them.
     */
    private static final int[] PALETTE = {
        0xF0F0F0, 0xF2B233, 0xE57FD8, 0x99B2F2, 0xDEDE6C, 0x7FCC19, 0xF2B2CC, 0x4C4C4C,
        0x999999, 0x4C99B2, 0xB266E5, 0x3366CC, 0x7F664C, 0x57A64E, 0xCC4C4C, 0x111111
    };

    private SigmaCanvas() {
    }

    /* A canvas is drawn stroke by stroke, in the sixteen colours every screen on these machines has. */
    static void draw(final GuiGraphics g, final Font font, final OsSkin skin, final UiWindowPayload.Widget widget,
                     final UiLayout.Rect rect) {
        skin.panel(g, rect.x(), rect.y(), rect.w(), rect.h());
        for (final UiWindowPayload.Stroke stroke : widget.drawing()) {
            final int colour = paint(stroke.colour());
            switch (stroke.kind()) {
                case "Clear" -> g.fill(rect.x() + 1, rect.y() + 1, rect.x() + rect.w() - 1,
                        rect.y() + rect.h() - 1, colour);
                case "FillRect" -> g.fill(rect.x() + 1 + stroke.x(), rect.y() + 1 + stroke.y(),
                        rect.x() + 1 + stroke.x() + Math.max(0, stroke.x2()),
                        rect.y() + 1 + stroke.y() + Math.max(0, stroke.y2()), colour);
                case "DrawLine" -> inside(g, rect, (long) rect.x() + 1 + stroke.x(), (long) rect.y() + 1 + stroke.y(),
                        (long) rect.x() + 1 + stroke.x2(), (long) rect.y() + 1 + stroke.y2(), colour);
                case "DrawText" -> Draw.text(g, font, stroke.text(), rect.x() + 1 + stroke.x(),
                        rect.y() + 1 + stroke.y(), colour);
                case "SetPixel" -> g.fill(rect.x() + 1 + stroke.x(), rect.y() + 1 + stroke.y(),
                        rect.x() + 2 + stroke.x(), rect.y() + 2 + stroke.y(), colour);
                default -> {
                    // A stroke of a kind this version does not draw is left out rather than guessed at.
                }
            }
        }
    }

    /** One of the sixteen colours, opaque. */
    static int paint(final int colour) {
        return 0xFF << 24 | PALETTE[Math.floorMod(colour, PALETTE.length)];
    }

    /** A line of single pixels, since the screen has no line of its own to draw with. */
    static void line(final GuiGraphics g, final int x1, final int y1, final int x2, final int y2, final int colour) {
        final int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
        if (steps == 0) {
            g.fill(x1, y1, x1 + 1, y1 + 1, colour);
            return;
        }
        for (int i = 0; i <= steps; i++) {
            final int x = x1 + (x2 - x1) * i / steps;
            final int y = y1 + (y2 - y1) * i / steps;
            g.fill(x, y, x + 1, y + 1, colour);
        }
    }

    /*
     * A program's line, cut to the part inside the canvas first. A line is drawn a pixel at a time, and one a program
     * asked to run two billion pixels long held the game's drawing for minutes every frame; the part inside is never
     * longer than the canvas is wide and tall.
     */
    private static void inside(final GuiGraphics g, final UiLayout.Rect rect, final long x1, final long y1,
                               final long x2, final long y2, final int colour) {
        final double left = rect.x() + 1;
        final double top = rect.y() + 1;
        final double right = rect.x() + rect.w() - 2;
        final double bottom = rect.y() + rect.h() - 2;
        final double dx = x2 - x1;
        final double dy = y2 - y1;
        // The part of the line, from 0 at its start to 1 at its end, that lies on the canvas (Liang and Barsky).
        double from = 0.0;
        double to = 1.0;
        final double[] toward = {-dx, dx, -dy, dy};
        final double[] room = {x1 - left, right - x1, y1 - top, bottom - y1};
        for (int side = 0; side < 4; side++) {
            if (toward[side] == 0.0) {
                if (room[side] < 0.0) {
                    return;
                }
                continue;
            }
            final double at = room[side] / toward[side];
            if (toward[side] < 0.0) {
                from = Math.max(from, at);
            } else {
                to = Math.min(to, at);
            }
        }
        if (from > to) {
            return;
        }
        line(g, (int) Math.round(x1 + from * dx), (int) Math.round(y1 + from * dy),
                (int) Math.round(x1 + to * dx), (int) Math.round(y1 + to * dy), colour);
    }
}
