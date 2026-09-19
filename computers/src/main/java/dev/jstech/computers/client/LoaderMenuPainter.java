/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.gui.layout.LoaderMenuLayout;
import dev.jstech.computers.os.boot.BootMenu;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * FreeBSD's boot loader as it is drawn: a ruled box of numbered entries with its heading written over the top
 * rule, the system's lockup beside it, and the count underneath.
 *
 * <p>Apart from the screen that takes the keys because the two have nothing in common: this one only knows where
 * things go and in what colour, and is handed the menu and how long is left on the clock.
 */
public final class LoaderMenuPainter {

    /** The loader's grey, the white it picks its numbers and its keys out in, and the ground under both. */
    private static final int TEXT = 0xFFBDBDBD;
    private static final int BRIGHT = 0xFFFFFFFF;
    private static final int GROUND = 0xFF000000;

    private LoaderMenuPainter() {
    }

    /**
     * Draws the whole loader on a glass whose top left corner is at {@code (x, y)}.
     *
     * <p>No entry is marked as the one a cursor is on, because a loader has no cursor: an entry is chosen by
     * its number, and the one Enter boots says so beside its name.
     *
     * @param remaining ticks left on the count, which is not drawn once it has been stopped
     * @param held      whether a key has already stopped the count
     */
    public static void draw(final GuiGraphics g, final Font font, final int x, final int y, final BootMenu menu,
                            final int remaining, final boolean held) {
        g.fill(x, y, x + LoaderMenuLayout.WIDTH, y + LoaderMenuLayout.HEIGHT, GROUND);
        box(g, font, x, y, menu.title());
        entries(g, font, x, y, menu);
        /*
         * The sphere with its horns and the name are one picture, not a disc of fills and the game's letters: a
         * mark is a drawing with light on it and lettering of its own, and rectangles get neither right.
         */
        SplashLogos.at(g, SplashLogos.FREEBSD, x + LoaderMenuLayout.LOCKUP_X, y + LoaderMenuLayout.LOCKUP_Y,
                LoaderMenuLayout.LOCKUP_W, LoaderMenuLayout.LOCKUP_H);
        foot(g, font, x + LoaderMenuLayout.FOOT_X, y + LoaderMenuLayout.FOOT_Y, menu, remaining, held);
    }

    /** The rule, and the heading that breaks it: written on a strip of the ground so the rule stops either side. */
    private static void box(final GuiGraphics g, final Font font, final int x, final int y, final String title) {
        final int left = x + LoaderMenuLayout.BOX_X;
        final int top = y + LoaderMenuLayout.BOX_Y;
        final int right = left + LoaderMenuLayout.BOX_W;
        final int bottom = top + LoaderMenuLayout.BOX_H;
        g.fill(left, top, right, top + 1, TEXT);
        g.fill(left, bottom - 1, right, bottom, TEXT);
        g.fill(left, top, left + 1, bottom, TEXT);
        g.fill(right - 1, top, right, bottom, TEXT);
        final int tx = x + LoaderMenuLayout.TITLE_X;
        final int ty = y + LoaderMenuLayout.TITLE_Y;
        g.fill(tx - 3, ty - 1, tx + TextWall.width(font, title) + 3, ty + TextWall.ROW, GROUND);
        TextWall.draw(g, font, title, tx, ty, BRIGHT);
    }

    /** The entries by number, the number in white, and the key that boots written after the one it boots. */
    private static void entries(final GuiGraphics g, final Font font, final int x, final int y,
                                final BootMenu menu) {
        final int left = x + LoaderMenuLayout.ITEMS_X;
        int ty = y + LoaderMenuLayout.ITEMS_Y;
        for (int i = 0; i < menu.entries().size(); i++) {
            final String number = (i + 1) + ".";
            final int after = left + TextWall.width(font, number + " ");
            final String name = TextWall.clip(font, menu.entries().get(i).label(),
                    LoaderMenuLayout.itemRoom() - (after - left) - TextWall.width(font, " [Enter]"));
            TextWall.draw(g, font, number, left, ty, BRIGHT);
            TextWall.draw(g, font, name, after, ty, TEXT);
            if (i == menu.defaultIndex()) {
                TextWall.draw(g, font, "[Enter]", after + TextWall.width(font, name + " "), ty, BRIGHT);
            }
            ty += LoaderMenuLayout.ITEM_PITCH;
        }
    }

    /** The count, with the seconds in white, or what to press once somebody has stopped it. */
    private static void foot(final GuiGraphics g, final Font font, final int x, final int y, final BootMenu menu,
                             final int remaining, final boolean held) {
        if (held) {
            TextWall.draw(g, font, LoaderMenuLayout.FOOT_PAUSED, x, y, TEXT);
            return;
        }
        final String before = "Autoboot in ";
        final String seconds = Integer.toString(menu.secondsLeft(remaining));
        TextWall.draw(g, font, before, x, y, TEXT);
        TextWall.draw(g, font, seconds, x + TextWall.width(font, before), y, BRIGHT);
        // Each piece starts where the whole line before it ends, so three roundings never add up to a gap.
        TextWall.draw(g, font, " seconds. [Space] to pause", x + TextWall.width(font, before + seconds), y, TEXT);
    }
}
