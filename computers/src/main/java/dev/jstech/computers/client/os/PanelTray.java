/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.client.FramesEmblem;
import dev.jstech.computers.os.PanelStyle;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
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
 * <p>The network icon is drawn greyed and badged when this computer is on no network, which is the fastest way a
 * player has of telling whether the cable behind the case is doing anything. The speaker loses its waves to a cross
 * while the system is muted, and opens the system's volume control.
 *
 * <p>How wide all this runs matters beyond the corner itself: it is where a panel's task buttons have to
 * stop, so the clock can never be written over by a row of open windows.
 *
 * <p>Its own colours are the palette {@code jsc:panel/tray}.
 */
@PaletteHolder
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
    /** A program's tray picture, drawn at its own size. */
    private static final int PROGRAM_ICON = 16;
    private static final String FURNACE_CARD = "furnace_card";

    private static final Palette<Colours> PALETTE = Palettes.declare(JsComputers.MODID, "panel/tray",
            new Colours(0xFF101318, 0xFFF2F4F8, 0xFF202430, 0xFF505868, 0xFF2A2F3A, 0xFF11151E, 0xFF5FE07A,
                    0xFFF0B23A, 0xFFEF6A5A, 0xFFEF6A5A, 0x80FFFFFF, 0x1FFFFFFF));
    /** Frames 7's corner that shows the desktop, and Frames 10's button of the Action Center. */
    private static final int SHOW_DESKTOP_W = 7;
    private static final int ACTION_CENTER_W = 16;
    /** How many columns of the speaker picture are its body; the rest are the waves a muted speaker loses. */
    private static final int SPEAKER_BODY = 4;
    private static final int MUTE_MARK = 4;

    private final DesktopState desktop;

    PanelTray(final DesktopState desktop) {
        this.desktop = desktop;
    }

    /**
     * How wide the status group runs: the network icon, the speaker and the memory bar, and after them the Furnace
     * Card's own picture while it is smelting.
     */
    int statusWidth() {
        return ICON + GAP + ICON + GAP + RAM_BAR_W + (desktop.furnaceSmelting() ? GAP + PROGRAM_ICON : 0);
    }

    /** How wide the whole notification area runs: the status group, the clock, and the padding around them. */
    int width() {
        return PAD + lead() + statusWidth() + GAP + clockWidth() + PAD + trail();
    }

    /**
     * What stands before the status group: Frames 7's action-center flag, which is where that system said it had
     * something to tell; nothing on the others.
     */
    int lead() {
        return desktop.panelStyle() == PanelStyle.FRAMES_7 ? FramesEmblem.SIZE + GAP : 0;
    }

    /**
     * What closes the notification area at the panel's end: Frames 7's corner that shows the desktop, Frames 10's
     * button of the Action Center; nothing on the others.
     */
    int trail() {
        return switch (desktop.panelStyle()) {
            case FRAMES_7 -> SHOW_DESKTOP_W;
            case FRAMES_10 -> ACTION_CENTER_W;
            default -> 0;
        };
    }

    /** Whether the clock carries the day under the time, as Frames 7 and 10 wrote it. */
    boolean dated() {
        return desktop.panelStyle() == PanelStyle.FRAMES_7 || desktop.panelStyle() == PanelStyle.FRAMES_10;
    }

    /** Whether a desktop-local point is on Frames 7's corner that shows the desktop. */
    boolean onShowDesktop(final double mx, final double my, final int sw, final int panelY) {
        return desktop.panelStyle() == PanelStyle.FRAMES_7 && my >= panelY && mx >= sw - SHOW_DESKTOP_W;
    }

    /** Whether a desktop-local point is on Frames 10's button of the Action Center. */
    boolean onActionCenter(final double mx, final double my, final int sw, final int panelY) {
        return desktop.panelStyle() == PanelStyle.FRAMES_10 && my >= panelY && mx >= sw - ACTION_CENTER_W;
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
        int x = left(sw) + PAD;
        if (desktop.panelStyle() == PanelStyle.FRAMES_7) {
            FramesEmblem.draw(g, x, panelY + (DesktopScreen.TASKBAR_H - FramesEmblem.SIZE) / 2, PanelStyle.FRAMES_7);
        }
        x += lead();
        drawStatus(g, x, panelY, textColor);
        final int clockX = x + statusWidth() + GAP;
        final String time = desktop.prefs().clockText();
        if (dated()) {
            final String day = dateText();
            final int w = clockWidth();
            Draw.text(g, desktop.textFont(), time, clockX + (w - desktop.textFont().width(time)) / 2, panelY + 3,
                    textColor);
            Draw.text(g, desktop.textFont(), day, clockX + (w - desktop.textFont().width(day)) / 2, panelY + 13,
                    textColor);
        } else {
            Draw.text(g, desktop.textFont(), time, clockX, panelY + 8, textColor);
        }
        drawTrail(g, panelY, sw, textColor);
    }

    /** The clock's width: the time, or the wider of the time and the day under it. */
    private int clockWidth() {
        final int time = desktop.textFont().width(desktop.prefs().clockText());
        return dated() ? Math.max(time, desktop.textFont().width(dateText())) : time;
    }

    /** The day the clock writes under the time. */
    private String dateText() {
        return GameText.resolve(PanelTexts.DAY.with(desktop.prefs().dayOfWorld()));
    }

    /**
     * The end of the area: Frames 7's corner, a pane of its own past a hairline, and Frames 10's button, a speech
     * bubble that opens the Action Center.
     */
    private void drawTrail(final GuiGraphics g, final int panelY, final int sw, final int textColor) {
        final Colours c = PALETTE.get();
        final int bottom = panelY + DesktopScreen.TASKBAR_H;
        if (desktop.panelStyle() == PanelStyle.FRAMES_7) {
            final int x = sw - SHOW_DESKTOP_W;
            g.fill(x, panelY + 1, x + 1, bottom, c.cornerLine());
            g.fill(x + 1, panelY + 1, sw, bottom, c.cornerPane());
        } else if (desktop.panelStyle() == PanelStyle.FRAMES_10) {
            final int x = sw - ACTION_CENTER_W + 3;
            final int y = panelY + (DesktopScreen.TASKBAR_H - 9) / 2;
            Draw.outline(g, x, y, 10, 7, textColor);
            g.fill(x + 2, y + 7, x + 4, y + 8, textColor);
            g.fill(x + 2, y + 8, x + 3, y + 9, textColor);
            if (desktop.notices().unread() > 0) {
                g.fill(x + 3, y + 2, x + 7, y + 3, textColor);
                g.fill(x + 3, y + 4, x + 6, y + 5, textColor);
            }
        }
    }

    /** The status group alone, for a panel that puts its clock somewhere else of its own. */
    void drawStatus(final GuiGraphics g, final int x, final int panelY, final int textColor) {
        final int iconY = panelY + (DesktopScreen.TASKBAR_H - ICON) / 2;
        drawNetworkIcon(g, x, iconY, desktop.onNetwork(), textColor);
        speaker(g, x + ICON + GAP, iconY, textColor, desktop.soundMuted());
        drawRamBar(g, x + 2 * (ICON + GAP), panelY + (DesktopScreen.TASKBAR_H - RAM_BAR_H) / 2);
        if (desktop.furnaceSmelting()) {
            // A program's picture rather than a status mask: the card at work, in its own colours.
            final ResourceLocation furnace = SkinSprites.find("device", FURNACE_CARD, FURNACE_CARD,
                    desktop.iconSet());
            if (SkinSprites.exists(furnace)) {
                SkinSprites.draw(g, furnace, x + 2 * (ICON + GAP) + RAM_BAR_W + GAP,
                        panelY + (DesktopScreen.TASKBAR_H - PROGRAM_ICON) / 2, PROGRAM_ICON, PROGRAM_ICON,
                        PROGRAM_ICON);
            }
        }
    }

    /**
     * The speaker, in the panel's own tone; a muted one keeps its body and loses its waves to a red cross. The
     * volume control draws it too, beside its slider.
     */
    static void speaker(final GuiGraphics g, final int x, final int y, final int argb, final boolean muted) {
        if (!muted) {
            tinted(g, SPEAKER, x, y, argb);
            return;
        }
        g.setColor((argb >> 16 & 0xFF) / 255.0F, (argb >> 8 & 0xFF) / 255.0F, (argb & 0xFF) / 255.0F, 1.0F);
        g.blit(SPEAKER, x, y, 0.0F, 0.0F, SPEAKER_BODY, ICON, ICON, ICON);
        g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        final int mark = PALETTE.get().muted();
        for (int i = 0; i < MUTE_MARK; i++) {
            g.fill(x + 5 + i, y + 3 + i, x + 6 + i, y + 4 + i, mark);
            g.fill(x + 8 - i, y + 3 + i, x + 9 - i, y + 4 + i, mark);
        }
    }

    /** Where the speaker stands on a panel {@code sw} wide: in the notification area, or at the top bar's end. */
    int speakerX(final int sw, final boolean topBar) {
        return (topBar ? sw - PAD - statusWidth() : left(sw) + PAD + lead()) + ICON + GAP;
    }

    /** Whether a desktop-local point is on the speaker of the panel whose band starts at {@code panelY}. */
    boolean onSpeaker(final double mx, final double my, final int sw, final int panelY, final boolean topBar) {
        final int x = speakerX(sw, topBar);
        return mx >= x - 2 && mx < x + ICON + 2 && my >= panelY && my < panelY + DesktopScreen.TASKBAR_H;
    }

    /** The figures behind the tray, shown while the cursor rests on it: the link and the memory. */
    void drawTip(final GuiGraphics g, final int panelY, final int sw) {
        if (!desktop.hoverBeyond(left(sw), panelY)) {
            return;
        }
        // On the speaker itself the tip is the volume, alone.
        if (desktop.hoverIn(speakerX(sw, false) - 2, panelY, ICON + 4, DesktopScreen.TASKBAR_H)) {
            final String volume = desktop.volumeTip();
            final int w = desktop.textFont().width(volume) + 8;
            final int x = Math.max(2, sw - w - 2);
            final int y = panelY - 13 - 2;
            final Colours c = PALETTE.get();
            g.fill(x - 1, y - 1, x + w + 1, y + 14, c.tipBorder());
            g.fill(x, y, x + w, y + 13, c.tip());
            Draw.text(g, desktop.textFont(), volume, x + 4, y + 3, c.tipInk());
            return;
        }
        final String link =
                GameText.resolve(desktop.onNetwork() ? PanelTexts.NETWORK_CONNECTED : PanelTexts.NO_NETWORK);
        final String mem = GameText.resolve(PanelTexts.RAM.with(desktop.memory().meterText()));
        final int w = Math.max(desktop.textFont().width(link), desktop.textFont().width(mem)) + 8;
        final int h = 22;
        final int x = Math.max(2, sw - w - 2);
        final int y = panelY - h - 2;
        final Colours c = PALETTE.get();
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, c.tipBorder());
        g.fill(x, y, x + w, y + h, c.tip());
        Draw.text(g, desktop.textFont(), link, x + 4, y + 3, c.tipInk());
        Draw.text(g, desktop.textFont(), mem, x + 4, y + 12, c.tipDim());
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
        final Colours c = PALETTE.get();
        g.fill(x, y, x + RAM_BAR_W, y + RAM_BAR_H, c.barEdge());
        g.fill(x + 1, y + 1, x + RAM_BAR_W - 1, y + RAM_BAR_H - 1, c.barTrough());
        final int innerW = RAM_BAR_W - 2;
        final int used = desktop.memory().usedMb();
        final int total = desktop.memory().totalMb();
        if (total <= 0 || used <= 0) {
            return;
        }
        final int fillW = (int) Math.min(innerW, (long) innerW * used / total);
        final boolean nearlyFull = used * 100L >= total * 95L;
        for (int px = 0; px < fillW; px++) {
            final float t = innerW <= 1 ? 0f : (float) px / (innerW - 1);
            final int color = nearlyFull ? c.barFull() : blend(c.barLow(), c.barHigh(), t);
            g.fill(x + 1 + px, y + 1, x + 2 + px, y + RAM_BAR_H - 1, color);
        }
    }

    /** Linear blend of two opaque colours, {@code t} from the first (0) to the second (1). */
    private static int blend(final int from, final int to, final float t) {
        final int r = (int) (((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t);
        final int gr = (int) (((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * t);
        final int b = (int) ((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
        return 0xFF << 24 | (r << 16) | (gr << 8) | b;
    }

    /**
     * The tray's own colours: the tip's border, paper and two inks, the memory bar's edge and trough with the
     * green it starts at, the amber it shades to and the red of a machine that is nearly full, the cross of a
     * muted speaker, and the hairline and pane of Frames 7's corner that shows the desktop.
     */
    private record Colours(int tipBorder, int tip, int tipInk, int tipDim, int barEdge, int barTrough, int barLow,
                           int barHigh, int barFull, int muted, int cornerLine, int cornerPane) {
    }
}
