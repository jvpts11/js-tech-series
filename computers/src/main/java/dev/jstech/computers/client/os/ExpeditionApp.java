/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Expedition, Quince's browser (computers/docs/design/quince.md), which waits for the web of the world: its address
 * bar and its page, saying that there is no web for it to reach yet.
 */
@TextHolder
final class ExpeditionApp implements IDesktopApp {

    private static final String HOME = "http://www.quince.mc/start";
    private static final TextKey CANNOT = TextKey.of("jsc.quince.expedition.cannot", "You are not connected.");
    private static final TextKey WAITS = TextKey.of("jsc.quince.expedition.waits",
            "Expedition can't open the page because there is no web in this world yet. Your network reaches its own"
                    + " machines, not a world wide web.");
    private static final int BAR_H = 16;

    ExpeditionApp() {
    }

    @Override
    public String title() {
        return QuinceAboutApp.programName("expedition");
    }

    @Override
    public int defaultWidth() {
        return 260;
    }

    @Override
    public int defaultHeight() {
        return 160;
    }

    @Override
    public void renderContent(final GuiGraphics g, final Font font, final int x, final int y, final int width,
                              final int height, final int mouseX, final int mouseY, final float partialTick) {
        final QuinceLook.Colours c = QuinceLook.FLAT.colours();
        g.fill(x, y, x + width, y + BAR_H, c.face());
        g.fill(x, y + BAR_H, x + width, y + BAR_H + 1, c.shade());
        QuinceArt.roundRect(g, x + 6, y + 2, x + width - 6, y + BAR_H - 2, 4, c.light());
        Draw.text(g, font, HOME, x + 12, y + 4, c.dim());
        g.fill(x, y + BAR_H + 1, x + width, y + height, c.light());
        Draw.text(g, font, Component.literal(GameText.resolve(CANNOT)).withStyle(ChatFormatting.BOLD), x + 12,
                y + BAR_H + 14, c.ink());
        int ty = y + BAR_H + 30;
        String line = "";
        for (final String word : GameText.resolve(WAITS).split(" ")) {
            final String probe = line.isEmpty() ? word : line + " " + word;
            if (font.width(probe) <= width - 24 || line.isEmpty()) {
                line = probe;
            } else {
                Draw.text(g, font, line, x + 12, ty, c.ink());
                ty += 11;
                line = word;
            }
        }
        Draw.text(g, font, line, x + 12, ty, c.ink());
    }
}
