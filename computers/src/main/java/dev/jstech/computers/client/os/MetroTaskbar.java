/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.gui.TaskbarGroups;
import dev.jstech.computers.os.OsMotions;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Frames 10's taskbar: a near-black band with the white mark of Start in its corner, the search box that finds the
 * machine's programs and files, the button of Task View, then one square slot per program, pinned and open in one
 * row, an open program marked with a line of light blue under its icon and the one in front lit; the notification area
 * at the right ends with the clock over the date and the button of the Action Center.
 *
 * <p>With "Transparency effects" off the band is drawn solid. Its colours are {@code jsc:panel/frames_10}.
 */
@PaletteHolder
final class MetroTaskbar {

    private final DesktopState desktop;

    /** Start's slot, the search box, Task View's button, and each program's slot after them. */
    static final int START_W = 24;
    static final int SEARCH_W = 92;
    static final int VIEW_W = 18;
    static final int SLOT = 24;
    private static final int ICON = 16;
    private static final int GLYPH = 12;
    private static final ResourceLocation START_GLYPH =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "textures/gui/emblem/frames_10_start.png");

    private static final Palette<Colours> PALETTE = Palettes.declare(JsComputers.MODID, "panel/frames_10",
            new Colours(0xE6101010, 0xFF101010, 0xFF1F1F1F, 0xFF2E2E2E, 0xFFF3F3F3, 0xFF6B6B6B, 0xFFFFFFFF,
                    0xFF76B9ED, 0xFFA6D2F4, 0xFF4A9EE8));

    MetroTaskbar(final DesktopState desktop) {
        this.desktop = desktop;
    }

    /** The left edge of the search box. */
    static int searchLeft() {
        return START_W;
    }

    /** The left edge of Task View's button. */
    static int viewLeft() {
        return START_W + SEARCH_W + 2;
    }

    /** The left edge of the first program's slot. */
    static int appsLeft() {
        return viewLeft() + VIEW_W;
    }

    /** Draws the band, Start, the search box, Task View, the programs and the notification area. */
    void render(final GuiGraphics g, final int tbY, final int sw, final int lmx, final int lmy) {
        final Colours c = PALETTE.get();
        final int bottom = tbY + DesktopScreen.TASKBAR_H;
        final boolean solid = desktop.prefs().effects().isOff(OsMotions.TRANSPARENCY);
        g.fill(0, tbY, sw, bottom, solid ? c.barSolid() : c.bar());

        final boolean onBar = lmy >= tbY;
        // Start: the mark in white, in the accent while the cursor is on it or the menu is up.
        final boolean startLit = desktop.start().isOpen() || onBar && lmx >= 0 && lmx < START_W;
        if (startLit) {
            g.fill(0, tbY, START_W, bottom, c.hover());
        }
        final int tint = startLit ? c.startLit() : c.ink();
        g.setColor((tint >> 16 & 0xFF) / 255.0F, (tint >> 8 & 0xFF) / 255.0F, (tint & 0xFF) / 255.0F, 1.0F);
        Draw.blended(() -> g.blit(START_GLYPH, (START_W - GLYPH) / 2, tbY + (DesktopScreen.TASKBAR_H - GLYPH) / 2,
                0.0F, 0.0F, GLYPH, GLYPH, GLYPH, GLYPH));
        g.setColor(1.0F, 1.0F, 1.0F, 1.0F);

        // The search box, white, with its magnifier and the line it waits with.
        final int sx = searchLeft();
        g.fill(sx, tbY + 3, sx + SEARCH_W, bottom - 3, c.search());
        final int my = tbY + DesktopScreen.TASKBAR_H / 2;
        Draw.outline(g, sx + 4, my - 4, 6, 6, c.searchInk());
        g.fill(sx + 9, my + 1, sx + 11, my + 3, c.searchInk());
        final String hint = GameText.resolve(DesktopTexts.SEARCH_HINT);
        Draw.text(g, desktop.textFont(), desktop.textFont().plainSubstrByWidth(hint, SEARCH_W - 18), sx + 15,
                my - 3, c.searchInk());

        // Task View: two windows, one behind the other.
        final int vx = viewLeft();
        if (onBar && lmx >= vx && lmx < vx + VIEW_W || desktop.taskView().isOpen()) {
            g.fill(vx, tbY, vx + VIEW_W, bottom, c.hover());
        }
        Draw.outline(g, vx + 4, my - 4, 8, 6, c.ink());
        g.fill(vx + 6, my + 3, vx + 14, my + 4, c.ink());
        g.fill(vx + 13, my - 2, vx + 14, my + 4, c.ink());

        final TaskStrip strip = desktop.taskbar().strip(sw);
        for (int i = 0; i < strip.entries().size(); i++) {
            final TaskbarGroups.Entry entry = strip.entries().get(i);
            final int ix = strip.x()[i];
            if (ix + SLOT > strip.right()) {
                break;
            }
            final boolean hover = onBar && lmx >= ix && lmx < ix + SLOT;
            final boolean front = entry.state() == TaskbarGroups.State.ACTIVE;
            if (front) {
                g.fill(ix, tbY, ix + SLOT, bottom, c.active());
            } else if (hover || entry.key().equals(desktop.openTaskPopup())) {
                g.fill(ix, tbY, ix + SLOT, bottom, c.hover());
            }
            ProgramIcons.draw(g, ix + (SLOT - ICON) / 2, tbY + (DesktopScreen.TASKBAR_H - ICON) / 2, ICON, ICON,
                    desktop.programIdFor(entry.key()), desktop.icons());
            if (entry.open()) {
                // The line under an open program: across the slot for the one in front, short for the rest.
                final int inset = front ? 0 : 3;
                g.fill(ix + inset, bottom - 2, ix + SLOT - inset, bottom, front ? c.activeLine() : c.openLine());
            }
        }
        desktop.tray().draw(g, tbY, sw, c.ink());
    }

    /** Whether a desktop-local point is on Start. */
    boolean startHit(final double mx, final double my, final int tbY) {
        return my >= tbY && mx >= 0 && mx < START_W;
    }

    /** Whether a desktop-local point is on the search box, which opens Start to be typed into. */
    boolean searchHit(final double mx, final double my, final int tbY) {
        return my >= tbY && mx >= searchLeft() && mx < searchLeft() + SEARCH_W;
    }

    /** Whether a desktop-local point is on Task View's button. */
    boolean viewHit(final double mx, final double my, final int tbY) {
        return my >= tbY && mx >= viewLeft() && mx < viewLeft() + VIEW_W;
    }

    /**
     * The taskbar's colours: the band with transparency and without, a slot under the cursor and the program in
     * front, the search box and its ink, the bar's ink, the line under an open program and under the one in front, and
     * the mark of Start lit.
     */
    private record Colours(int bar, int barSolid, int hover, int active, int search, int searchInk, int ink,
                           int openLine, int activeLine, int startLit) {
    }
}
