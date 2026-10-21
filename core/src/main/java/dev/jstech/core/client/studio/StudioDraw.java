/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.studio;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.gui.layout.StudioLayout;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

/**
 * How the studio draws, the game's own menus' way: a panel on the ground of the game's lists, its heading, a row of a
 * name and its value, words cut short to their room, small words, the game's separator lines, and a bar of a chart.
 */
final class StudioDraw {

    /** How small the small words are drawn, and how far in from a panel's side its words stand. */
    static final float SMALL = 0.75F;
    static final int INSET = 6;
    /** How tall a panel's heading is, and a row of small words. */
    static final int HEAD = 16;
    static final int SMALL_ROW = 11;

    private static final ResourceLocation LIST = ResourceLocation.withDefaultNamespace(
            "textures/gui/menu_list_background.png");
    private static final ResourceLocation INWORLD_LIST = ResourceLocation.withDefaultNamespace(
            "textures/gui/inworld_menu_list_background.png");
    private static final int TILE = 32;
    private static final int SEPARATOR = 2;
    private static final int SEPARATOR_WIDTH = 32;
    private static final String ELLIPSIS = "...";

    private StudioDraw() {
    }

    /** A panel on the ground the game's lists have, the world's when a world is open behind, with its heading. */
    static void panel(final GuiGraphics g, final Font font, final StudioLayout.Rect area, final String heading) {
        panel(g, font, area, heading, 0);
    }

    /** A panel whose heading keeps {@code clear} pixels at its right free, for a button that stands there. */
    static void panel(final GuiGraphics g, final Font font, final StudioLayout.Rect area, final String heading,
                      final int clear) {
        RenderSystem.enableBlend();
        g.blit(Minecraft.getInstance().level == null ? LIST : INWORLD_LIST, area.x(), area.y(), area.right(),
                area.bottom(), area.width(), area.height(), TILE, TILE);
        RenderSystem.disableBlend();
        if (!heading.isEmpty()) {
            Draw.text(g, font, cut(font, heading, area.width() - 2 * INSET - clear, 1.0F), area.x() + INSET,
                    area.y() + 4, StudioPalette.get().text());
        }
    }

    /** A row of a name and its value, both small, the value at the row's right. */
    static void row(final GuiGraphics g, final Font font, final StudioLayout.Rect area, final int y, final String name,
                    final String value, final int valueColour) {
        final String nameShown = cut(font, name, area.width() / 2 - INSET, SMALL);
        Draw.textScaled(g, font, nameShown, area.x() + INSET, y, StudioPalette.get().dim(), SMALL);
        final String valueShown = cut(font, value, area.width() - 3 * INSET
                - Math.round(font.width(nameShown) * SMALL), SMALL);
        Draw.textScaled(g, font, valueShown, area.right() - INSET - Math.round(font.width(valueShown) * SMALL), y,
                valueColour, SMALL);
    }

    /** Small words at their place. */
    static void small(final GuiGraphics g, final Font font, final String text, final int x, final int y,
                      final int room, final int colour) {
        Draw.textScaled(g, font, cut(font, text, room, SMALL), x, y, colour, SMALL);
    }

    /** Small words wrapped to their room, a line under another; how many lines they took. */
    static int smallWrapped(final GuiGraphics g, final Font font, final String text, final int x, final int y,
                            final int room, final int colour) {
        final List<FormattedCharSequence> lines = font.split(FormattedText.of(text), (int) (room / SMALL));
        for (int i = 0; i < lines.size(); i++) {
            g.pose().pushPose();
            g.pose().translate(x, y + i * (SMALL_ROW - 3), 0);
            g.pose().scale(SMALL, SMALL, 1.0F);
            Draw.text(g, font, lines.get(i), 0, 0, colour);
            g.pose().popPose();
        }
        return lines.size();
    }

    /** Words at their place, cut to their room. */
    static void text(final GuiGraphics g, final Font font, final String text, final int x, final int y,
                     final int room, final int colour) {
        Draw.text(g, font, cut(font, text, room, 1.0F), x, y, colour);
    }

    /** {@code text} cut to {@code width} pixels at {@code scale}, ending in dots when cut. */
    static String cut(final Font font, final String text, final int width, final float scale) {
        final int room = Math.max(0, (int) (width / scale));
        if (font.width(text) <= room) {
            return text;
        }
        return font.plainSubstrByWidth(text, Math.max(0, room - font.width(ELLIPSIS))) + ELLIPSIS;
    }

    /** The game's header line across the screen at {@code y}. */
    static void headLine(final GuiGraphics g, final int y, final int width) {
        line(g, Minecraft.getInstance().level == null ? Screen.HEADER_SEPARATOR : Screen.INWORLD_HEADER_SEPARATOR, y,
                width);
    }

    /** The game's footer line across the screen at {@code y}. */
    static void footLine(final GuiGraphics g, final int y, final int width) {
        line(g, Minecraft.getInstance().level == null ? Screen.FOOTER_SEPARATOR : Screen.INWORLD_FOOTER_SEPARATOR, y,
                width);
    }

    /**
     * Bars of a chart inside {@code area}, one a value, as tall as the value is of the greatest; a line along the
     * foot when every value is none. The bar {@code lit} is drawn in the lit colour.
     */
    static void bars(final GuiGraphics g, final StudioLayout.Rect area, final long[] values, final int colour,
                     final int lit, final int litColour) {
        long top = 0;
        for (final long value : values) {
            top = Math.max(top, value);
        }
        if (top == 0 || values.length == 0) {
            g.fill(area.x(), area.bottom() - 1, area.right(), area.bottom(), colour);
            return;
        }
        final double wide = (double) area.width() / values.length;
        for (int i = 0; i < values.length; i++) {
            final int tall = (int) Math.round(area.height() * (double) values[i] / top);
            final int left = area.x() + (int) Math.round(i * wide);
            final int right = Math.max(left + 1, area.x() + (int) Math.round((i + 1) * wide) - 1);
            g.fill(left, area.bottom() - tall, right, area.bottom(), i == lit ? litColour : colour);
        }
    }

    private static void line(final GuiGraphics g, final ResourceLocation line, final int y, final int width) {
        RenderSystem.enableBlend();
        g.blit(line, 0, y, 0.0F, 0.0F, width, SEPARATOR, SEPARATOR_WIDTH, SEPARATOR);
        RenderSystem.disableBlend();
    }
}
