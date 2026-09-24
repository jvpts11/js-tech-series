/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.gui.CdePalette;
import dev.jstech.computers.gui.layout.CdeFrontPanelLayout.Rect;
import dev.jstech.computers.gui.layout.CdeStyleLayout;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * What the two pages of CDE's Style Manager draw alike: a sunken list with the line picked lit in the palette's
 * active colour, and the two buttons along the foot, the default one first.
 */
final class CdeStylePages {

    private CdeStylePages() {
    }

    /**
     * The list of a page, its content's top left at {@code (left, top)}.
     *
     * @param colors whether it is the Color page's list, which is narrower than the Backdrop page's
     */
    static void list(final GuiGraphics g, final Font font, final OsSkin skin, final CdePalette p, final int left,
                     final int top, final boolean colors, final List<String> labels, final int picked) {
        final Rect well = CdeStyleLayout.list(colors, labels.size());
        skin.panel(g, left + well.x(), top + well.y(), well.w(), well.h());
        for (int i = 0; i < labels.size(); i++) {
            final Rect r = CdeStyleLayout.row(colors, i);
            if (i == picked) {
                g.fill(left + r.x(), top + r.y(), left + r.x() + r.w(), top + r.y() + r.h(), p.active());
            }
            g.drawString(font, labels.get(i), left + r.x() + 4, top + r.y() + 2,
                    i == picked ? p.activeInk() : skin.text(), false);
        }
    }

    /** The two buttons of a page along its foot, with their words, the first being the default one. */
    static void buttons(final GuiGraphics g, final Font font, final OsSkin skin, final int left, final int top,
                        final boolean colors, final TextKey first, final TextKey second, final int mx,
                        final int my) {
        for (int i = 0; i < 2; i++) {
            final Rect r = CdeStyleLayout.button(colors, i);
            final boolean over = r.holds(mx - left, my - top);
            skin.button(g, font, left + r.x(), top + r.y(), r.w(), r.h(), GameText.resolve(i == 0 ? first : second),
                    over, false, i == 0);
        }
    }

    /** The middle of a rectangle of a page, in desktop pixels. */
    static int[] centre(final Rect r, final int left, final int top) {
        return new int[] {left + r.x() + r.w() / 2, top + r.y() + r.h() / 2};
    }
}
