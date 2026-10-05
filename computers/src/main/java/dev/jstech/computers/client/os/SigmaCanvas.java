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
                case "DrawLine" -> line(g, rect.x() + 1 + stroke.x(), rect.y() + 1 + stroke.y(),
                        rect.x() + 1 + stroke.x2(), rect.y() + 1 + stroke.y2(), colour);
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
}
