/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.core.client.gui.component.Draw;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * The bars of Quince's desktops (computers/docs/design/quince.md): the menu bar across the top of every one of them,
 * the fruit's menu at its left and the running program's menus after it, and from Quince OS X on the Dock along the
 * foot, with the system's programs, what is open, and the Trash at its end.
 *
 * <p>Drawn here and nowhere else, so the desktop and the installer, which runs on the same desktop, draw the same bar.
 */
public final class QuinceBars {

    /** How tall the menu bar is, its line under it included. */
    public static final int BAR_H = 12;
    /** How tall the Dock is, from the foot of the screen. */
    public static final int DOCK_H = 21;
    /** How far apart the Dock's icons are. */
    public static final int DOCK_PITCH = 20;
    /** How wide the fruit's menu title is, from the bar's left. */
    private static final int FRUIT_W = 20;
    private static final int TEXT_Y = 2;

    private QuinceBars() {
    }

    /**
     * Draws the menu bar from {@code (x, y)}, {@code w} wide, and answers where each of its titles is: the fruit first,
     * then the program's name on the faces that write it (Quince OS X and after), then its menus, each as
     * {@code {left, right}}.
     *
     * @param app      the running program's name, written in bold after the fruit from Quince OS X on, and at the
     *                 right as the Application menu on Quince OS 9
     * @param appIcon  the running program's icon for Quince OS 9's Application menu, or null for none
     * @param iconSet  the set the icon is drawn from
     * @param clock    the time at the right, empty on the Quince System, which had no clock there
     * @param open     which title is open, its index in the answer, or -1 for none
     */
    public static List<int[]> menuBar(final GuiGraphics g, final Font font, final QuinceLook look, final int x,
                                      final int y, final int w, final String app,
                                      @Nullable final ResourceLocation appIcon, final String iconSet,
                                      final List<String> menus, final String clock, final int open) {
        final QuinceLook.Colours c = look.colours();
        final List<int[]> titles = new ArrayList<>();
        switch (look) {
            case ONE_BIT -> {
                g.fill(x, y, x + w, y + BAR_H - 1, c.barTop());
                g.fill(x, y + BAR_H - 1, x + w, y + BAR_H, c.barLine());
            }
            case PLATINUM -> {
                g.fill(x, y, x + w, y + BAR_H, c.barTop());
                g.fill(x, y, x + w, y + 1, c.light());
                g.fill(x, y + BAR_H - 1, x + w, y + BAR_H, c.barLine());
            }
            default -> {
                ChromeShapes.vGradient(g, x, y, x + w, y + BAR_H - 1, c.barTop(), c.barBottom());
                g.fill(x, y + BAR_H - 1, x + w, y + BAR_H, c.barLine());
            }
        }
        // The fruit's menu.
        final boolean fruitOpen = open == 0;
        if (fruitOpen) {
            g.fill(x + 3, y, x + FRUIT_W - 1, y + BAR_H - 1, c.hot());
        }
        QuinceArt.fruit(g, look, x + (look.lights() ? 8 : 7), y + 1, fruitOpen && look.classic());
        titles.add(new int[] {x + 3, x + FRUIT_W - 1});
        int at = x + (look == QuinceLook.ONE_BIT ? 22 : look == QuinceLook.PLATINUM ? 26 : 24);
        final int gap = look == QuinceLook.ONE_BIT ? 10 : 11;
        if (look.lights() && !app.isEmpty()) {
            final Component bold = Component.literal(app).withStyle(ChatFormatting.BOLD);
            final int aw = font.width(bold);
            final boolean hot = open == titles.size();
            if (hot) {
                g.fill(at - 5, y, at + aw + 5, y + BAR_H - 1, c.hot());
            }
            Draw.text(g, font, bold, at, y + TEXT_Y, hot ? c.hotInk() : c.barInk());
            titles.add(new int[] {at - 5, at + aw + 5});
            at += aw + 12;
        }
        // The menus stop short of the clock and what stands at the right, as many as fit before them.
        final int limit = rightStart(font, look, x, w, app, appIcon, clock) - gap;
        for (final String menu : menus) {
            final int mw = font.width(menu);
            if (at + mw > limit) {
                break;
            }
            final boolean hot = open == titles.size();
            final int pad = look == QuinceLook.ONE_BIT ? 4 : 5;
            if (hot) {
                g.fill(at - pad, y, at + mw + pad, y + BAR_H - 1, c.hot());
            }
            Draw.text(g, font, menu, at, y + TEXT_Y, hot ? c.hotInk() : c.barInk());
            titles.add(new int[] {at - pad, at + mw + pad});
            at += mw + gap;
        }
        right(g, font, look, x, y, w, app, appIcon, iconSet, clock);
        return titles;
    }

    /**
     * Where the Dock's icons are for {@code count} of them on a glass {@code w} wide and {@code h} tall from
     * {@code (x, y)}: the left of the first, and the top they stand on.
     */
    public static int[] dockOrigin(final QuinceLook look, final int x, final int y, final int w, final int h,
                                   final int count) {
        final int shelf = count * DOCK_PITCH + 8;
        final int left = x + (w - shelf) / 2;
        final int top = y + h - (look == QuinceLook.QUINCE_OS ? 22 : 19);
        return new int[] {left + 6, top, left, shelf};
    }

    /**
     * Draws the Dock along the foot: the shelf of the face, Pulp's glass, the flat grey, or quinceOS's floating one,
     * and on it each icon, a gap where an icon is null (the divider before the Trash), and the mark under each one
     * that is running.
     */
    public static void dock(final GuiGraphics g, final QuinceLook look, final int x, final int y, final int w,
                            final int h, final List<ResourceLocation> icons, final List<Boolean> running,
                            final String iconSet, final int hovered) {
        final QuinceLook.Colours c = look.colours();
        final int[] origin = dockOrigin(look, x, y, w, h, icons.size());
        final int left = origin[2];
        final int shelf = origin[3];
        final int bottom = y + h;
        switch (look) {
            case PULP -> {
                g.fill(left, bottom - 20, left + shelf, bottom, c.dock());
                ChromeShapes.outline(g, left, bottom - 20, shelf, 21, c.dockLine());
            }
            case FLAT -> {
                g.fill(left, bottom - DOCK_H, left + shelf, bottom, c.dock());
                g.fill(left, bottom - DOCK_H, left + shelf, bottom - DOCK_H + 1, c.dockLine());
            }
            default -> QuinceArt.roundRect(g, left, bottom - 25, left + shelf, bottom - 3, 6, c.dock());
        }
        for (int k = 0; k < icons.size(); k++) {
            final int ix = origin[0] + k * DOCK_PITCH;
            final ResourceLocation icon = icons.get(k);
            if (icon == null) {
                g.fill(ix + 8, bottom - 18, ix + 9, bottom - 3, c.dockLine());
                continue;
            }
            final int lift = k == hovered ? 2 : 0;
            ProgramIcons.draw(g, ix, origin[1] - lift, 16, 16, icon, iconSet);
            if (k < running.size() && running.get(k)) {
                if (look == QuinceLook.PULP) {
                    for (int j = 0; j < 3; j++) {
                        g.fill(ix + 8 - j, bottom - 2 - j, ix + 9 + j, bottom - 1 - j, c.running());
                    }
                } else {
                    final int my = look == QuinceLook.FLAT ? bottom - 2 : bottom - 4;
                    g.fill(ix + 7, my, ix + 9, my + 1, c.running());
                }
            }
        }
    }

    /** The time of the world the bar is in, in the twelve-hour clock every Quince system showed. */
    public static String clock() {
        final Minecraft minecraft = Minecraft.getInstance();
        final long day = minecraft.level == null ? 6_000L : minecraft.level.getDayTime() % 24_000L;
        // The world's day starts at six in the morning.
        final int minutes = (int) ((day * 60L / 1_000L + 6L * 60L) % (24L * 60L));
        final int hour = minutes / 60;
        final int twelve = hour % 12 == 0 ? 12 : hour % 12;
        return twelve + ":" + String.format(Locale.ROOT, "%02d", minutes % 60) + (hour < 12 ? " AM" : " PM");
    }

    /** The index of the Dock icon under {@code (mx, my)}, or -1. */
    public static int dockAt(final QuinceLook look, final int x, final int y, final int w, final int h,
                             final int count, final int mx, final int my) {
        final int[] origin = dockOrigin(look, x, y, w, h, count);
        if (my < origin[1] - 2 || my > y + h) {
            return -1;
        }
        final int k = Math.floorDiv(mx - origin[0] + 2, DOCK_PITCH);
        return k >= 0 && k < count ? k : -1;
    }

    /* Where the right end of the bar begins: the bar's end on the Quince System, which has nothing there. */
    private static int rightStart(final Font font, final QuinceLook look, final int x, final int w, final String app,
                                  @Nullable final ResourceLocation appIcon, final String clock) {
        if (look == QuinceLook.ONE_BIT) {
            return x + w;
        }
        if (look == QuinceLook.PLATINUM) {
            final int iconW = appIcon == null ? 0 : 11;
            return x + w - 6 - iconW - 3 - font.width(app) - 12 - font.width(clock);
        }
        return x + w - 22 - font.width(clock);
    }

    /*
     * The right end of the bar: the clock and the running program's menu on Quince OS 9, the clock and the search
     * glass from Quince OS X on, and nothing on the Quince System.
     */
    private static void right(final GuiGraphics g, final Font font, final QuinceLook look, final int x, final int y,
                              final int w, final String app, @Nullable final ResourceLocation appIcon,
                              final String iconSet, final String clock) {
        final QuinceLook.Colours c = look.colours();
        if (look == QuinceLook.ONE_BIT) {
            return;
        }
        if (look == QuinceLook.PLATINUM) {
            final int iconW = appIcon == null ? 0 : 11;
            final int ax = x + w - 6 - iconW - 3 - font.width(app);
            if (appIcon != null) {
                ProgramIcons.draw(g, ax, y + 1, 10, 10, appIcon, iconSet);
            }
            Draw.text(g, font, app, ax + iconW + 3, y + TEXT_Y, c.barInk());
            g.fill(ax - 6, y + 1, ax - 5, y + BAR_H - 1, c.shade());
            Draw.text(g, font, clock, ax - 12 - font.width(clock), y + TEXT_Y, c.barInk());
            return;
        }
        // The search glass at the very edge, then the clock.
        final int gx = x + w - 15;
        ChromeShapes.outline(g, gx + 1, y + 2, 5, 5, c.dim());
        g.fill(gx + 5, y + 7, gx + 6, y + 8, c.dim());
        g.fill(gx + 6, y + 8, gx + 7, y + 9, c.dim());
        Draw.text(g, font, clock, x + w - 22 - font.width(clock), y + TEXT_Y, c.barInk());
    }
}
