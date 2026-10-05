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
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Frames 7's taskbar, the superbar: a band of glass along the foot of the screen with the round Start orb at its left,
 * then one wide button per program, pinned and open in the same place and in the same row, each the program's icon
 * alone with no title; an open program's button is a pane of lighter glass, the one in front lighter still, and a
 * program with several windows shows the edge of a second pane behind its own. The notification area at the right
 * carries the action-center flag, the clock over the date, and the narrow corner that shows the desktop.
 *
 * <p>The orb sits wholly inside the band. With "Enable transparent glass" off the band is drawn opaque, as Frames 7
 * Basic drew it. Its colours are {@code jsc:panel/frames_7}.
 */
@PaletteHolder
final class AeroSuperbar {

    private final DesktopState desktop;

    /** The orb's slot at the left of the band, and each program's button after it. */
    static final int START_W = 24;
    static final int SLOT = 30;
    private static final int ICON = 16;
    private static final int ORB = 20;
    private static final ResourceLocation ORB_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "textures/gui/emblem/frames_7_orb.png");

    private static final Palette<Colours> PALETTE = Palettes.declare(JsComputers.MODID, "panel/frames_7",
            new Colours(0xB83C5A82, 0xDB12203A, 0xFF33496A, 0xFF101C30, 0x8CA0C8F0,
                    0x59FFFFFF, 0x14FFFFFF, 0x80FFFFFF, 0x80FFFFFF, 0x30FFFFFF, 0x26FFFFFF,
                    0xFFFFFFFF, 0x60FFFFFF));

    AeroSuperbar(final DesktopState desktop) {
        this.desktop = desktop;
    }

    /** The left edge of the first program's button. */
    static int appsLeft() {
        return START_W + 2;
    }

    /** Draws the band, the orb, the programs' buttons and the notification area. */
    void render(final GuiGraphics g, final int tbY, final int sw, final int lmx, final int lmy) {
        final Colours c = PALETTE.get();
        final int bottom = tbY + DesktopScreen.TASKBAR_H;
        final boolean glass = !desktop.prefs().effects().isOff(OsMotions.GLASS);
        g.fillGradient(0, tbY, sw, bottom, glass ? c.bandTop() : c.basicTop(), glass ? c.bandBottom()
                : c.basicBottom());
        g.fill(0, tbY, sw, tbY + 1, c.edge());

        // The orb, lit while the cursor is on it or the menu it opens is up.
        final boolean orbLit = desktop.start().isOpen() || lmx >= 0 && lmx < START_W && lmy >= tbY;
        final int ox = (START_W - ORB) / 2;
        final int oy = tbY + (DesktopScreen.TASKBAR_H - ORB) / 2;
        g.blit(ORB_TEXTURE, ox, oy, 0.0F, 0.0F, ORB, ORB, ORB, ORB);
        if (orbLit) {
            disc(g, ox + ORB / 2, oy + ORB / 2, ORB / 2 - 1, c.orbGlow());
        }

        final TaskStrip strip = desktop.taskbar().strip(sw);
        for (int i = 0; i < strip.entries().size(); i++) {
            final TaskbarGroups.Entry entry = strip.entries().get(i);
            final int ix = strip.x()[i];
            if (ix + SLOT > strip.right()) {
                break;
            }
            final boolean hover = lmx >= ix && lmx < ix + SLOT && lmy >= tbY;
            pane(g, c, ix, tbY + 2, SLOT, DesktopScreen.TASKBAR_H - 4, entry, hover
                    || entry.key().equals(desktop.openTaskPopup()));
            ProgramIcons.draw(g, ix + (SLOT - ICON) / 2, tbY + (DesktopScreen.TASKBAR_H - ICON) / 2, ICON, ICON,
                    desktop.programIdFor(entry.key()), desktop.icons());
        }
        desktop.tray().draw(g, tbY, sw, c.trayInk());
    }

    /** Whether a desktop-local point is on the orb. */
    boolean startHit(final double mx, final double my, final int tbY) {
        return my >= tbY && mx >= 0 && mx < START_W;
    }

    /**
     * One program's button: nothing for a pinned program with no window but a faint pane under the cursor; a pane
     * of lighter glass for an open one, lighter for the one in front, with a second pane's edge behind it when the
     * program has several windows.
     */
    private static void pane(final GuiGraphics g, final Colours c, final int x, final int y, final int w, final int h,
                             final TaskbarGroups.Entry entry, final boolean hover) {
        if (!entry.open()) {
            if (hover) {
                g.fill(x + 1, y, x + w - 1, y + h, c.hover());
            }
            return;
        }
        if (entry.windows() > 1) {
            g.fill(x + w - 2, y + 2, x + w - 1, y + h - 1, c.rim());
            g.fill(x + 3, y + h - 1, x + w - 1, y + h, c.rim());
        }
        final boolean front = entry.state() == TaskbarGroups.State.ACTIVE;
        final int top = front || hover ? c.frontTop() : c.openTop();
        g.fillGradient(x + 1, y, x + w - 3, y + h - 1, top, c.openBottom());
        outline(g, x + 1, y, w - 4, h - 1, c.rim());
        if (front) {
            g.fill(x + 2, y + 1, x + w - 4, y + h / 2, c.frontGloss());
        }
    }

    /** A soft round light over the orb. */
    private static void disc(final GuiGraphics g, final int cx, final int cy, final int r, final int colour) {
        for (int dy = -r; dy <= r; dy++) {
            final int half = (int) Math.round(Math.sqrt(Math.max(0, r * r - dy * dy)));
            g.fill(cx - half, cy + dy, cx + half, cy + dy + 1, colour);
        }
    }

    private static void outline(final GuiGraphics g, final int x, final int y, final int w, final int h,
                                final int colour) {
        g.fill(x, y, x + w, y + 1, colour);
        g.fill(x, y + h - 1, x + w, y + h, colour);
        g.fill(x, y + 1, x + 1, y + h - 1, colour);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, colour);
    }

    /**
     * The superbar's colours: the glass band's top and foot, the same opaque for Frames 7 Basic, the light along its
     * top; an open button's pane top and foot, the one in front, its rim, the gloss on the one in front and a hovered
     * slot; the tray's ink and the orb's glow.
     */
    private record Colours(int bandTop, int bandBottom, int basicTop, int basicBottom, int edge,
                           int openTop, int openBottom, int frontTop, int rim, int frontGloss, int hover,
                           int trayInk, int orbGlow) {
    }
}
