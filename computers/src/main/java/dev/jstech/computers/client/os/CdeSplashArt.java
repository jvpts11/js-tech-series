/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.gui.CdePalette;
import dev.jstech.core.gui.TextShadow;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * What a workstation shows between its console and CDE: the backdrop the desktop is about to stand on, a raised
 * plate with the desktop's name and its maker's, and one line saying which workstation it is starting on.
 *
 * <p>There is no login to show, so this is the whole of the moment, and it moves nothing: the desktop of those
 * years put up a plate and waited. It is drawn with the same relief as the rest of CDE, which is why it lives
 * beside it, so what comes up a moment later is visibly the same thing.
 */
public final class CdeSplashArt {

    private static final String NAME = "CDE";
    private static final String MAKER = "Open Desk Consortium";
    private static final float NAME_SCALE = 2.0F;

    private static final int PLATE_W = 150;
    private static final int PLATE_H = 46;
    private static final int LINE_GAP = 12;
    private static final int LINE_INK = 0xFFFFFFFF;

    private CdeSplashArt() {
    }

    /**
     * Draws the screen over the glass at {@code (x, y)}.
     *
     * @param hostName the name the workstation answers to, or empty when it is not known
     */
    public static void draw(final GuiGraphics g, final Font font, final String hostName, final int x, final int y,
                            final int w, final int h) {
        final CdePalette p = CdePalette.DEFAULT;
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        MotifChrome.backdrop(g, w, h, p);

        final int px = (w - PLATE_W) / 2;
        final int py = h / 2 - PLATE_H / 2 - LINE_GAP;
        MotifChrome.raised(g, px, py, PLATE_W, PLATE_H, p.window(), p);
        MotifChrome.raised(g, px + 3, py + 3, PLATE_W - 6, PLATE_H - 6, p.window(), p);

        g.pose().pushPose();
        g.pose().translate(w / 2.0F, py + 9, 0);
        g.pose().scale(NAME_SCALE, NAME_SCALE, 1.0F);
        g.drawString(font, NAME, -font.width(NAME) / 2, 0, p.ink(), false);
        g.pose().popPose();
        g.drawString(font, MAKER, (w - font.width(MAKER)) / 2, py + PLATE_H - 15, p.ink(), false);

        final String line = hostName.isEmpty() ? "Starting the desktop..."
                : "Starting the desktop on workstation " + hostName + "...";
        final int lx = (w - font.width(line)) / 2;
        final int ly = py + PLATE_H + LINE_GAP;
        // White on a patterned ground stays legible over a shadow worked out from the letter and that ground.
        g.drawString(font, line, lx + 1, ly + 1, TextShadow.of(LINE_INK, p.backdropA()), false);
        g.drawString(font, line, lx, ly, LINE_INK, false);
        g.pose().popPose();
    }
}
