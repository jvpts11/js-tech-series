/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.operation.payload.FirmwareStatePayload;
import dev.jstech.computers.os.FirmwareKind;
import dev.jstech.core.text.GameText;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

/**
 * The one-time boot menu, drawn three ways: one per firmware.
 *
 * <p>What is picked in one of these is for that boot and no other; the order saved in the setup is not
 * touched, which is the whole point of having a menu apart from the setup.
 *
 * <p>Three drawings and not one, because these were three different things. The earliest boards ruled a box
 * out of the same characters they printed everything else with, the ones after them opened their setup's blue
 * box over the self-test, and the modern machines put up a dialog with a mark beside each entry. Drawing one
 * blue box for all three meant a green-phosphor monitor showing a colour it could not display.
 */
public final class FirmwareBootMenus {

    /**
     * How many entries a menu shows at once.
     *
     * <p>More than any machine a player builds offers, and few enough that the tallest of the three boxes
     * still fits inside the glass. A machine with more than this walks its list through the window rather
     * than growing a box off the bottom of the monitor, so every entry can still be reached and booted.
     */
    public static final int ROWS = 8;

    private FirmwareBootMenus() {
    }

    /**
     * One thing a menu can boot: what is on it, and the disk slot it sits in, or -1 for a medium in a drive.
     */
    public record Choice(FirmwareStatePayload.Entry entry, int slot) {

        /** What a menu row calls the place this boots from: the disk by slot, or the drive by its kind. */
        public String where() {
            return this.slot >= 0 ? "Disk " + this.slot : GameText.resolve(this.entry.device());
        }

        /** How a modern dialog names it: the disk by slot and model, or the drive on its own. */
        public String device() {
            final String device = GameText.resolve(this.entry.device());
            return this.slot < 0 ? device : "Disk " + this.slot + " " + device;
        }
    }

    /** Draws the menu this firmware would have drawn, over whatever the self-test left on the glass. */
    public static void draw(final GuiGraphics g, final Font font, final FirmwareKind kind,
                            final List<Choice> choices, final int at, final int x, final int y,
                            final int w, final int h, final int text, final int dim, final int accent) {
        switch (kind) {
            case CLI_BIOS -> tube(g, font, choices, at, x, y, w, h, text, accent);
            case BLUE_BIOS -> blue(g, font, choices, at, x, y, w, h);
            case UEFI -> dialog(g, font, choices, at, x, y, w, h, text, dim, accent);
        }
    }

    /** The earliest machines: a double-ruled box on the tube, its entries numbered as that firmware did. */
    private static void tube(final GuiGraphics g, final Font font, final List<Choice> choices, final int at,
                             final int x, final int y, final int w, final int h, final int text,
                             final int accent) {
        final int rows = shownRows(choices.size());
        final int from = firstShown(choices.size(), at);
        final int boxW = 210;
        final int boxH = 28 + rows * TextWall.ROW + 12;
        final int bx = x + (w - boxW) / 2;
        final int by = y + (h - boxH) / 2;
        g.fill(bx, by, bx + boxW, by + boxH, 0xFF000000);
        rule(g, bx, by, boxW, boxH, accent);
        rule(g, bx + 2, by + 2, boxW - 4, boxH - 4, accent);
        TextWall.draw(g, font, "Boot Menu  (this boot only)", bx + 8, by + 8, accent);
        int ly = by + 24;
        for (int i = from; i < from + rows; i++) {
            final Choice choice = choices.get(i);
            final boolean on = i == at;
            TextWall.draw(g, font, TextWall.clip(font,
                            (on ? "> " : "  ") + (i + 1) + ". " + choice.where(), 78),
                    bx + 8, ly, on ? accent : text);
            TextWall.draw(g, font, TextWall.clip(font, choice.entry().label(), boxW - 100), bx + 92, ly,
                    on ? accent : text);
            ly += TextWall.ROW;
        }
        TextWall.draw(g, font, "Up/Down  select     Enter  boot", bx + 8, by + boxH - 12, text);
    }

    /** The boards after them: the setup's own blue, a grey title bar, and the choice filled light. */
    private static void blue(final GuiGraphics g, final Font font, final List<Choice> choices, final int at,
                             final int x, final int y, final int w, final int h) {
        final int ground = 0xFF0000A8;
        final int frame = 0xFFB9B9B9;
        final int chosen = 0xFFD9D9D9;
        final int rows = shownRows(choices.size());
        final int from = firstShown(choices.size(), at);
        final int boxW = 212;
        final int rowH = TextWall.ROW + 3;
        final int boxH = 14 + rows * rowH + 18;
        final int bx = x + (w - boxW) / 2;
        final int by = y + (h - boxH) / 2;
        g.fill(bx, by, bx + boxW, by + boxH, ground);
        rule(g, bx, by, boxW, boxH, frame);
        g.fill(bx + 1, by + 1, bx + boxW - 1, by + 13, frame);
        TextWall.centered(g, font, "Boot Menu", bx + boxW / 2, by + 4, ground);
        int ly = by + 16;
        for (int i = from; i < from + rows; i++) {
            final Choice choice = choices.get(i);
            final boolean on = i == at;
            if (on) {
                g.fill(bx + 2, ly - 1, bx + boxW - 2, ly + rowH - 2, chosen);
            }
            TextWall.draw(g, font, TextWall.clip(font, choice.where(), 70), bx + 8, ly,
                    on ? ground : 0xFFFFFFFF);
            TextWall.draw(g, font, TextWall.clip(font, choice.entry().label(), boxW - 92), bx + 84, ly,
                    on ? ground : 0xFFFFFFFF);
            ly += rowH;
        }
        g.fill(bx + 1, ly + 1, bx + boxW - 1, ly + 2, 0xFF6FB7FF);
        TextWall.draw(g, font, "Up/Down: Select     Enter: Boot", bx + 8, ly + 6, 0xFFFFE14D);
    }

    /** The modern machines: a dialog over the dimmed splash, each entry marked by what it is. */
    private static void dialog(final GuiGraphics g, final Font font, final List<Choice> choices, final int at,
                               final int x, final int y, final int w, final int h, final int text,
                               final int dim, final int accent) {
        final int rows = shownRows(choices.size());
        final int from = firstShown(choices.size(), at);
        final int boxW = 212;
        final int rowH = TextWall.ROW + 6;
        final int boxH = 16 + rows * (rowH + 3) + 16;
        final int bx = x + (w - boxW) / 2;
        final int by = y + (h - boxH) / 2;
        g.fill(bx, by, bx + boxW, by + boxH, 0xFF2A2D3E);
        g.fill(bx, by, bx + boxW, by + 14, 0xFF3A4060);
        g.fill(bx, by, bx + 2, by + 14, accent);
        TextWall.draw(g, font, "Boot Menu", bx + 7, by + 4, text);
        TextWall.right(g, font, "this boot only", bx + boxW - 7, by + 4, dim);
        int ly = by + 17;
        for (int i = from; i < from + rows; i++) {
            final Choice choice = choices.get(i);
            final boolean on = i == at;
            g.fill(bx + 5, ly, bx + boxW - 5, ly + rowH, on ? 0xFF23405A : 0xFF3A4060);
            if (on) {
                g.fill(bx + 5, ly, bx + 7, ly + rowH, accent);
            }
            /*
             * Green for a disk that carries a system, amber for a medium: what a player wants to know at a
             * glance is which of these boots what is already installed and which one installs something new.
             */
            g.fill(bx + 12, ly + 5, bx + 16, ly + 9, choice.slot() >= 0 ? 0xFF5FE07A : 0xFFF0B23A);
            /*
             * The system takes what it needs and the device takes what is left: a player choosing between two
             * systems is reading the names of the systems, and the drive beside each is how they tell two
             * installations of the same one apart.
             */
            final String name = TextWall.clip(font, choice.entry().label(), (boxW - 30) / 2);
            TextWall.draw(g, font, name, bx + 21, ly + 3, text);
            TextWall.right(g, font,
                    TextWall.clip(font, choice.device(), boxW - 30 - TextWall.width(font, name)),
                    bx + boxW - 9, ly + 3, dim);
            ly += rowH + 3;
        }
        TextWall.draw(g, font, "Up/Down Select   Enter Boot", bx + 7, ly + 3, dim);
    }

    /**
     * The first entry a menu draws, so the one under the cursor is always among the ones it draws.
     *
     * <p>A machine with more systems than the box has rows walks its list rather than growing a box past the
     * edge of the glass, and the walking follows the cursor so nothing can be selected off-screen.
     */
    private static int firstShown(final int count, final int at) {
        if (count <= ROWS) {
            return 0;
        }
        return Math.max(0, Math.min(count - ROWS, at - ROWS / 2));
    }

    /** How many rows a menu really draws: the window, or the whole list when it is shorter. */
    private static int shownRows(final int count) {
        return Math.min(count, ROWS);
    }

    /** A one-pixel frame around a box, which is how every one of these firmwares draws one. */
    private static void rule(final GuiGraphics g, final int x, final int y, final int w, final int h,
                             final int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }
}
