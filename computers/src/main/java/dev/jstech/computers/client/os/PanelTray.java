/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * The corner of the panel that reports on the machine: the network, the sound, the memory and the clock.
 *
 * <p>Every desktop has one and every desktop puts it in the same corner, whatever else it does differently,
 * so the drawing of it belongs in one place rather than being repeated once per panel. GNOME is the one that
 * splits it up, keeping its clock in the middle of the top bar and only the status group at the right end,
 * which is why the group can be drawn on its own.
 *
 * <p>Of the three status icons, only the network one says something true about the machine. It is drawn
 * greyed and badged when this computer is on no network, which is the fastest way a player has of telling
 * whether the cable behind the case is doing anything.
 *
 * <p>How wide all this runs matters beyond the corner itself: it is where a panel's task buttons have to
 * stop, so the clock can never be written over by a row of open windows.
 */
final class PanelTray {

    /** The padding at each end of the notification area. */
    static final int PAD = 5;

    private static final int ICON = 9;
    private static final int GAP = 4;

    /** The two status pictures, white so the panel's text colour can be laid over them, and the badge of no link. */
    private static final ResourceLocation NETWORK =
            ResourceLocation.fromNamespaceAndPath("jsc", "textures/gui/tray/network.png");
    private static final ResourceLocation SPEAKER =
            ResourceLocation.fromNamespaceAndPath("jsc", "textures/gui/tray/speaker.png");
    private static final ResourceLocation OFFLINE =
            ResourceLocation.fromNamespaceAndPath("jsc", "textures/gui/tray/offline_badge.png");
    private static final int BADGE = 4;
    private static final int RAM_BAR_W = 26;
    private static final int RAM_BAR_H = 6;

    private final DesktopScreen desktop;

    PanelTray(final DesktopScreen desktop) {
        this.desktop = desktop;
    }

    /** How wide the status group runs: the network icon, the speaker and the memory bar. */
    int statusWidth() {
        return ICON + GAP + ICON + GAP + RAM_BAR_W;
    }

    /** How wide the whole notification area runs: the status group, the clock, and the padding around them. */
    int width() {
        return PAD + statusWidth() + GAP + desktop.textFont().width(desktop.clock()) + PAD;
    }

    /** The left edge of the notification area on a panel {@code sw} wide. */
    int left(final int sw) {
        return sw - width();
    }

    /** Where a panel's task buttons must stop: clear of the notification area at its right end. */
    int taskStripRight(final int sw) {
        return left(sw) - 4;
    }

    /**
     * The whole notification area: the status group and then the clock, right-aligned on the panel.
     *
     * <p>The icons take their tone from the panel's own text, which is the one thing that already knows
     * whether this panel is a dark band or a light one.
     */
    void draw(final GuiGraphics g, final int panelY, final int sw, final int textColor) {
        final int x = left(sw) + PAD;
        drawStatus(g, x, panelY, textColor);
        g.drawString(desktop.textFont(), desktop.clock(),
                x + statusWidth() + GAP, panelY + 8, textColor, false);
    }

    /** The status group alone, for a panel that puts its clock somewhere else of its own. */
    void drawStatus(final GuiGraphics g, final int x, final int panelY, final int textColor) {
        final int iconY = panelY + (DesktopScreen.TASKBAR_H - ICON) / 2;
        drawNetworkIcon(g, x, iconY, desktop.onNetwork(), textColor);
        tinted(g, SPEAKER, x + ICON + GAP, iconY, textColor);
        drawRamBar(g, x + 2 * (ICON + GAP), panelY + (DesktopScreen.TASKBAR_H - RAM_BAR_H) / 2);
    }

    /** The figures behind the tray, shown while the cursor rests on it: the link and the memory. */
    void drawTip(final GuiGraphics g, final int panelY, final int sw) {
        if (!desktop.hoverBeyond(left(sw), panelY)) {
            return;
        }
        final String link = desktop.onNetwork() ? "Network connected" : "No network";
        final String mem = "RAM " + desktop.ramMeter();
        final int w = Math.max(desktop.textFont().width(link), desktop.textFont().width(mem)) + 8;
        final int h = 22;
        final int x = Math.max(2, sw - w - 2);
        final int y = panelY - h - 2;
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF101318);
        g.fill(x, y, x + w, y + h, 0xFFF2F4F8);
        g.drawString(desktop.textFont(), link, x + 4, y + 3, 0xFF202430, false);
        g.drawString(desktop.textFont(), mem, x + 4, y + 12, 0xFF505868, false);
    }

    /**
     * The network icon: two linked machines, faded and badged when this computer is on no network. It is
     * the one status here that says something true about the machine rather than decorating it.
     */
    private static void drawNetworkIcon(final GuiGraphics g, final int x, final int y, final boolean up,
                                        final int textColor) {
        tinted(g, NETWORK, x, y, up ? textColor : faded(textColor));
        if (!up) {
            g.blit(OFFLINE, x + ICON - BADGE, y + ICON - BADGE, 0.0F, 0.0F, BADGE, BADGE, BADGE, BADGE);
        }
    }

    /**
     * A white picture drawn in the panel's own text colour, so it is pale on a dark band and dark on a pale one
     * without a picture for each.
     */
    private static void tinted(final GuiGraphics g, final ResourceLocation mask, final int x, final int y,
                               final int argb) {
        g.setColor((argb >> 16 & 0xFF) / 255.0F, (argb >> 8 & 0xFF) / 255.0F, (argb & 0xFF) / 255.0F, 1.0F);
        g.blit(mask, x, y, 0.0F, 0.0F, ICON, ICON, ICON, ICON);
        g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** The colour halfway to grey, which is how a status that is out reads on either kind of panel. */
    private static int faded(final int argb) {
        final int r = ((argb >> 16 & 0xFF) + 0x80) / 2;
        final int gr = ((argb >> 8 & 0xFF) + 0x80) / 2;
        final int b = ((argb & 0xFF) + 0x80) / 2;
        return 0xFF << 24 | r << 16 | gr << 8 | b;
    }

    /** The memory bar: a dark trough filled green shading to amber, and red once the machine is nearly full. */
    private void drawRamBar(final GuiGraphics g, final int x, final int y) {
        g.fill(x, y, x + RAM_BAR_W, y + RAM_BAR_H, 0xFF2A2F3A);
        g.fill(x + 1, y + 1, x + RAM_BAR_W - 1, y + RAM_BAR_H - 1, 0xFF11151E);
        final int innerW = RAM_BAR_W - 2;
        final int used = desktop.ramUsed();
        final int total = desktop.ramTotal();
        if (total <= 0 || used <= 0) {
            return;
        }
        final int fillW = (int) Math.min(innerW, (long) innerW * used / total);
        final boolean nearlyFull = used * 100L >= total * 95L;
        for (int px = 0; px < fillW; px++) {
            final float t = innerW <= 1 ? 0f : (float) px / (innerW - 1);
            final int color = nearlyFull ? 0xFFEF6A5A : blend(0xFF5FE07A, 0xFFF0B23A, t);
            g.fill(x + 1 + px, y + 1, x + 2 + px, y + RAM_BAR_H - 1, color);
        }
    }

    /** Linear blend of two opaque colours, {@code t} from the first (0) to the second (1). */
    private static int blend(final int from, final int to, final float t) {
        final int r = (int) (((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t);
        final int gr = (int) (((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * t);
        final int b = (int) ((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
        return 0xFF000000 | (r << 16) | (gr << 8) | b;
    }
}
