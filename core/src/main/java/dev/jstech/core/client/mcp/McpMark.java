/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.mcp;

import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.text.GameText;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.Nullable;

/**
 * The MCP server's mark, over the game and every screen while a client is let in: a light and how many agents at the
 * top left corner, green while they read, yellow while one acts; and while one acts, at the top, its name, what it
 * does, and how to take the controls back.
 */
public final class McpMark {

    /** How small the mark's words are, beside the font's own size. */
    private static final float SMALL = 0.75F;
    private static final int LIGHT = 5;
    private static final int DEPTH = 600;

    private McpMark() {
    }

    /**
     * Draws the mark for {@code agents} let in, on a screen {@code width} wide, its corner left out where
     * {@code corner} is false, on a screen that says as much itself; {@code acting}, when one acts, says who and what,
     * and {@code cutKey} names the key that cuts them all off.
     */
    public static void draw(final GuiGraphics g, final Font font, final int width, final int agents,
                            final boolean corner, @Nullable final String acting, final String cutKey) {
        if (agents <= 0) {
            return;
        }
        final McpPalette.Colours colours = McpPalette.get();
        g.pose().pushPose();
        g.pose().translate(0.0F, 0.0F, DEPTH);
        if (corner) {
            final String label = agents == 1 ? GameText.resolve(McpTexts.MARK_ONE)
                    : GameText.resolve(McpTexts.MARK.with(agents));
            final int wide = Math.round(font.width(label) * SMALL);
            g.fill(4, 4, 4 + 17 + wide, 18, colours.ground());
            final int light = acting == null ? colours.connected() : colours.acting();
            g.fill(8, 8, 8 + LIGHT, 8 + LIGHT, light);
            Draw.textScaled(g, font, label, 17, 8, colours.title(), SMALL);
        }
        if (acting != null) {
            final String hint = GameText.resolve(McpTexts.ACTING_HINT.with(cutKey));
            final int box = Math.max(font.width(acting), Math.round(font.width(hint) * SMALL)) + 16;
            final int left = (width - box) / 2;
            g.fill(left, 30, left + box, 54, colours.ground());
            Draw.textCentered(g, font, acting, width / 2, 33, colours.acting());
            final int hintWide = Math.round(font.width(hint) * SMALL);
            Draw.textScaled(g, font, hint, (width - hintWide) / 2, 45, colours.dim(), SMALL);
        }
        g.pose().popPose();
    }
}
