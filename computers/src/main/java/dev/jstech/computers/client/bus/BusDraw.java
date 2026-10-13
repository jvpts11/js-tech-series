/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.bus;

import dev.jstech.computers.gui.layout.BusLayout;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.Grounds;
import dev.jstech.core.client.gui.theme.JsTechTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Locale;

/**
 * The pieces a bus window is drawn from, in the skin bound for the frame: the small words, the toggles, the
 * steppers' parts, the cells, the bars, the scrollbar and the mark of what a program set. Each fill says what ground it
 * is, so the words drawn over it get the shadow that reads on it.
 */
final class BusDraw {

    /** The gear's pixels, a row each, that marks what a program set. */
    private static final String[] GEAR = {"#.#.#", ".###.", "##.##", ".###.", "#.#.#"};
    private static final int GEAR_SIZE = 5;
    /** How much of the cell's own ground lies over an item it only lists, out of 255: the item shows through. */
    private static final int GHOST_ALPHA = 102;
    private static final String ELLIPSIS = "...";
    /** The smallest a row's word is drawn, as a part of the game's letters. */
    private static final float SMALLEST = 0.5f;
    /** How much brighter the lamp's lens corner is than its light, per channel. */
    private static final int LENS_LIFT = 70;
    private static final long DAY = 24_000L;
    private static final long HOUR = 1_000L;
    private static final int HOURS_BEFORE_DAWN = 6;
    private static final int MINUTES = 60;

    private BusDraw() {
    }

    /** A line of the small letters with its top left at {@code (x, y)}. */
    static void small(final GuiGraphics g, final Font font, final String text, final int x, final int y,
                      final int color) {
        Draw.textScaled(g, font, text, x, y, color, JsTechTheme.small());
    }

    /**
     * A word in the small letters that fits {@code room}: smaller still when it is longer, as a row's word in a
     * language with longer words is, down to half the game's letters, and cut past that.
     */
    static void fitted(final GuiGraphics g, final Font font, final String text, final int x, final int y,
                       final int color, final int room) {
        if (width(font, text) <= room) {
            small(g, font, text, x, y, color);
            return;
        }
        final float scale = Math.max(SMALLEST, room / (float) font.width(text));
        final String shown = scale > SMALLEST ? text : clipAt(font, text, room, SMALLEST);
        Draw.textScaled(g, font, shown, x, y + 1, color, scale);
    }

    /** A line of the small letters centred on {@code cx}. */
    static void smallCentered(final GuiGraphics g, final Font font, final String text, final int cx, final int y,
                              final int color) {
        small(g, font, text, cx - width(font, text) / 2, y, color);
    }

    /** A line of the small letters ending at {@code right}. */
    static void smallRight(final GuiGraphics g, final Font font, final String text, final int right, final int y,
                           final int color) {
        small(g, font, text, right - width(font, text), y, color);
    }

    /** A line of game text in the small letters, its colours and all, with its top left at {@code (x, y)}. */
    static void small(final GuiGraphics g, final Font font, final Component text, final int x, final int y,
                      final int color) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(JsTechTheme.small(), JsTechTheme.small(), 1.0f);
        Draw.text(g, font, text, 0, 0, color, Grounds.under(g, 1, 4, color));
        g.pose().popPose();
    }

    /** How wide a line of the small letters is. */
    static int width(final Font font, final String text) {
        return JsTechTheme.widthS(font, text);
    }

    /** How wide a line of game text is in the small letters. */
    static int width(final Font font, final Component text) {
        return (int) Math.ceil(font.width(text) * JsTechTheme.small());
    }

    /** {@code text} broken into the lines of the small letters that fit {@code room}. */
    static List<String> lines(final Font font, final String text, final int room) {
        return font.getSplitter().splitLines(text, (int) (room / JsTechTheme.small()), Style.EMPTY).stream()
                .map(FormattedText::getString).toList();
    }

    /** The line cut to {@code room} in the small letters, ending in dots where it was cut. */
    static String clip(final Font font, final String text, final int room) {
        return clipAt(font, text, room, JsTechTheme.small());
    }

    /** How wide an option of that word is, with the room beside it. */
    static int optionWidth(final Font font, final String word) {
        return width(font, word) + BusLayout.OPTION_PAD;
    }

    /** A toggle's option: the chosen one in the accent, the others dim on the panel. */
    static void option(final GuiGraphics g, final Font font, final String word, final int x, final int y, final int w,
                       final boolean on, final boolean hovered) {
        Grounds.fill(g, x, y, x + w, y + BusLayout.CONTROL_H,
                on || hovered ? JsTechTheme.hover() : JsTechTheme.panel());
        Draw.outline(g, x, y, w, BusLayout.CONTROL_H, on ? JsTechTheme.accent() : JsTechTheme.line());
        smallCentered(g, font, word, x + w / 2, y + 2, on ? JsTechTheme.accent() : JsTechTheme.dim());
    }

    /** A button with a word on it, in the accent. */
    static void button(final GuiGraphics g, final Font font, final String word, final int x, final int y, final int w,
                       final int h, final boolean hovered) {
        JsTechTheme.button(g, x, y, w, h, hovered);
        Draw.outline(g, x, y, w, h, JsTechTheme.line());
        smallCentered(g, font, word, x + w / 2, y + (h - 6) / 2, JsTechTheme.accent());
    }

    /** A sunken box a value or a field's text sits in. */
    static void well(final GuiGraphics g, final int x, final int y, final int w, final int h) {
        Grounds.fill(g, x, y, x + w, y + h, JsTechTheme.track());
        Draw.outline(g, x, y, w, h, JsTechTheme.line());
    }

    /** A box of code, on the ground a cell has: dark on the dark skins, the paper of the light ones. */
    static void code(final GuiGraphics g, final int x, final int y, final int w, final int h) {
        Grounds.fill(g, x, y, x + w, y + h, JsTechTheme.slotBg());
        Draw.outline(g, x, y, w, h, JsTechTheme.line());
    }

    /** A bar a condition or an added row sits on. */
    static void bar(final GuiGraphics g, final int x, final int y, final int w, final int h, final boolean hovered) {
        Grounds.fill(g, x, y, x + w, y + h, hovered ? JsTechTheme.hover() : JsTechTheme.panel());
        Draw.outline(g, x, y, w, h, JsTechTheme.line());
    }

    /** A cell of {@link BusLayout#CELL} with {@code stack} in it, drawn faded when it only lists the item. */
    static void cell(final GuiGraphics g, final int x, final int y, final ItemStack stack, final boolean ghost) {
        JsTechTheme.slot(g, x + 1, y + 1);
        if (!stack.isEmpty()) {
            g.renderItem(stack, x + 1, y + 1);
            if (ghost) {
                g.pose().pushPose();
                g.pose().translate(0, 0, 200);
                final int ground = JsTechTheme.slotBg();
                g.fill(x + 1, y + 1, x + 17, y + 17, FastColor.ARGB32.color(GHOST_ALPHA, FastColor.ARGB32.red(ground),
                        FastColor.ARGB32.green(ground), FastColor.ARGB32.blue(ground)));
                g.pose().popPose();
            }
        }
    }

    /** The scrollbar beside rows that scroll: its track, and its thumb as long as the part in view. */
    static void scrollbar(final GuiGraphics g, final int x, final int y, final int h, final int scroll,
                          final int content) {
        if (content <= h) {
            return;
        }
        g.fill(x, y, x + BusLayout.SCROLL_W, y + h, JsTechTheme.track());
        final int thumb = Math.max(8, h * h / content);
        final int top = y + (h - thumb) * scroll / Math.max(1, content - h);
        g.fill(x, top, x + BusLayout.SCROLL_W, top + thumb, JsTechTheme.accent());
    }

    /** The mark of a setting a program set: a small gear and the program's name, in amber; how wide it came out. */
    static int mark(final GuiGraphics g, final Font font, final String program, final int x, final int y,
                    final int room) {
        final int amber = JsTechTheme.amber();
        for (int row = 0; row < GEAR_SIZE; row++) {
            for (int col = 0; col < GEAR_SIZE; col++) {
                if (GEAR[row].charAt(col) == '#') {
                    g.fill(x + col, y + row, x + col + 1, y + row + 1, amber);
                }
            }
        }
        final String name = clip(font, program, Math.max(0, room - GEAR_SIZE - 3));
        small(g, font, name, x + GEAR_SIZE + 3, y, amber);
        return GEAR_SIZE + 3 + width(font, name);
    }

    /* The line cut to {@code room} at {@code scale}, ending in dots where it was cut. */
    private static String clipAt(final Font font, final String text, final int room, final float scale) {
        if (font.width(text) * scale <= room) {
            return text;
        }
        String cut = text;
        while (!cut.isEmpty() && font.width(cut + ELLIPSIS) * scale > room) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut + ELLIPSIS;
    }

    /**
     * The link as a lamp, green when it reaches the network and red when it does not: a lit square in a dark bezel,
     * with a lighter corner so it reads as a lens.
     */
    static void lamp(final GuiGraphics g, final int x, final int y, final int size, final boolean linked) {
        final int colour = linked ? JsTechTheme.green() : JsTechTheme.red();
        g.fill(x, y, x + size, y + size, JsTechTheme.outer());
        g.fill(x + 1, y + 1, x + size - 1, y + size - 1, colour);
        final int r = Math.min(255, ((colour >> 16) & 0xFF) + LENS_LIFT);
        final int gr = Math.min(255, ((colour >> 8) & 0xFF) + LENS_LIFT);
        final int b = Math.min(255, (colour & 0xFF) + LENS_LIFT);
        g.fill(x + 1, y + 1, x + 2, y + 2, (colour >>> 24) << 24 | r << 16 | gr << 8 | b);
    }

    /** The hour of the day {@code time} was, as the clock read: what happened at a game time, read on today's. */
    static String clock(final long time) {
        final Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return "--:--";
        }
        final long day = Math.floorMod(mc.level.getDayTime() - (mc.level.getGameTime() - time), DAY);
        final long hour = (day / HOUR + HOURS_BEFORE_DAWN) % 24L;
        final long minute = day % HOUR * MINUTES / HOUR;
        return String.format(Locale.ROOT, "%02d:%02d", hour, minute);
    }

    /** Whether the point is in the rectangle. */
    static boolean inside(final double mx, final double my, final int x, final int y, final int w, final int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
