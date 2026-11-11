/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * How a slot that keeps a part out says why, over the slot: its lines in the game's tooltip, each broken where it runs
 * past a refusal's width, so a long reason reads as a short paragraph rather than a band across the screen.
 */
public final class RefusalTooltip {

    /*
     * The widest a line runs, in GUI pixels: "a Standard disk needs a Standard" (176) fits and the word after it does
     * not, nor does "or" after "a Standard dock takes disks of its era" (198).
     */
    private static final int WIDTH = 200;

    private RefusalTooltip() {
    }

    /** Draws {@code lines} at the mouse, each broken at the refusal's width. */
    public static void render(final GuiGraphics g, final Font font, final List<Component> lines, final int mouseX,
                              final int mouseY) {
        final List<FormattedCharSequence> broken = new ArrayList<>();
        for (final Component line : lines) {
            broken.addAll(font.split(line, WIDTH));
        }
        g.renderTooltip(font, broken, mouseX, mouseY);
    }
}
