/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.client.FramesEmblem;
import dev.jstech.computers.gui.TaskbarGroups;
import dev.jstech.computers.os.PanelStyle;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * The bars the Frames systems put their open windows on.
 *
 * <p>There are two, and they are a generation apart. The classic one runs along the bottom with a Start
 * button at the left corner, a named button per open program beside it, and the notification area sunken
 * into the right end; Frames 95 draws it in bevelled grey and Frames XP in the blue gradient with the green
 * Start pill and the quick launch strip. The modern one is a flat dark band whose Start button and program
 * icons sit centered together, each icon carrying a mark underneath that says what its program is doing,
 * with no window titles at all.
 *
 * <p>Both take their measurements from the same task strip the clicks are tested against, so a button a
 * player sees and the button they hit are the same rectangle. What differs is only how the state behind it
 * is shown: the classic bar says it with a pushed-in button, the modern one with a pill or a dot.
 */
final class FramesPanels {

    private final DesktopScreen desktop;

    FramesPanels(final DesktopScreen desktop) {
        this.desktop = desktop;
    }

    /**
     * The Frames 95 and XP taskbar, and the plain one a Legacy-era Frames desktop wears: background, Start
     * button, the row of named window buttons, and the notification area, each in the edition's own dress.
     */
    void renderClassic(final GuiGraphics g, final int tbY, final int sw, final int sh,
                       final int lmx, final int lmy) {
        final boolean xp = desktop.isPanel(PanelStyle.FRAMES_XP);
        drawBand(g, tbY, sw, sh, xp);
        drawStart(g, tbY, sh, xp);

        final DesktopScreen.TaskStrip strip = desktop.taskButtons(sw);
        if (xp) {
            drawQuickLaunch(g, strip, tbY, sh, lmx, lmy);
        }
        drawTasks(g, strip, tbY, sh, xp);
        drawNotificationArea(g, tbY, sw, sh, xp);
    }

    /**
     * The Frames 11 taskbar: a dark bar with the Start logo and the open windows' icons centered, each app
     * carrying an indicator under it (a wide pill for the focused window, a short dot otherwise), and the
     * clock pinned to the right.
     */
    void renderModern(final GuiGraphics g, final int tbY, final int sw, final int lmx, final int lmy) {
        final int bottom = tbY + DesktopScreen.TASKBAR_H;
        g.fill(0, tbY, sw, bottom, 0xF01E1F23);          // dark, slightly translucent bar
        g.fill(0, tbY, sw, tbY + 1, 0x18FFFFFF);          // faint top hairline

        final int iconY = tbY + (DesktopScreen.TASKBAR_H - DesktopScreen.WIN11_ICON) / 2;

        /*
         * Start: follows the taskbar alignment (centered as the leftmost of the centered group, or left
         * corner), with a hover highlight, the four-pane blue logo, no text.
         */
        final int startX = desktop.modernStartLeft(sw);
        if (lmx >= startX && lmx < startX + DesktopScreen.WIN11_SLOT && lmy >= tbY) {
            g.fill(startX, tbY + 2, startX + DesktopScreen.WIN11_SLOT, bottom - 2, 0x18FFFFFF);
        }
        drawModernStart(g, startX + (DesktopScreen.WIN11_SLOT - 11) / 2, iconY + 2);

        /*
         * One icon per program, CENTERED: the pinned ones first, then the open ones, each with a hover or
         * active background and an indicator underneath that says how it stands. A pinned program with
         * nothing open is the icon alone; an open one carries a dot; the one in front a wide pill; a
         * program with several windows has its mark split in two; and a program whose windows are all
         * put away sits dimmed.
         */
        final DesktopScreen.TaskStrip strip = desktop.taskButtons(sw);
        for (int i = 0; i < strip.entries().size(); i++) {
            final TaskbarGroups.Entry entry = strip.entries().get(i);
            final int ix = strip.x()[i];
            final boolean hover = lmx >= ix && lmx < ix + DesktopScreen.WIN11_SLOT && lmy >= tbY;
            final boolean active = entry.state() == TaskbarGroups.State.ACTIVE;
            final boolean shown = entry.key().equals(desktop.openTaskPopup());
            if (hover || active || shown) {
                g.fill(ix + 1, tbY + 2, ix + DesktopScreen.WIN11_SLOT - 1, bottom - 2,
                        active ? 0x26FFFFFF : 0x18FFFFFF);
            }
            final int iconX = ix + (DesktopScreen.WIN11_SLOT - DesktopScreen.WIN11_ICON) / 2;
            ProgramIcons.draw(g, iconX, iconY, DesktopScreen.WIN11_ICON, DesktopScreen.WIN11_ICON - 2,
                    desktop.programIdFor(entry.key()), "frames_11");
            if (entry.state() == TaskbarGroups.State.MINIMIZED) {
                g.fill(iconX, iconY, iconX + DesktopScreen.WIN11_ICON,
                        iconY + DesktopScreen.WIN11_ICON - 2, 0x901E1F23);
            }
            drawModernIndicator(g, ix + DesktopScreen.WIN11_SLOT / 2, bottom, entry);
        }

        desktop.tray().draw(g, tbY, sw, 0xFFE6E8EC);
    }

    /** Whether a desktop-local point is on the bottom panel's Start button. */
    boolean startButtonHit(final double mx, final double my, final int tbY) {
        if (desktop.isPanel(PanelStyle.FRAMES_XP)) {
            return my >= tbY && mx >= 0 && mx <= DesktopScreen.XP_START_W;
        }
        return my >= tbY + 3 && mx >= 4 && mx <= 58;
    }

    /** The bar itself: Frames 95 in flat grey with a light top edge, Frames XP in the blue gradient. */
    private void drawBand(final GuiGraphics g, final int tbY, final int sw, final int sh, final boolean xp) {
        if (xp) {
            g.fillGradient(0, tbY, sw, sh, 0xFF4A86D4, 0xFF1C4D9C);
            g.fill(0, tbY, sw, tbY + 1, 0xFF8FBCEC);
        } else {
            g.fill(0, tbY, sw, sh, desktop.themeColours().taskbar());
            g.fill(0, tbY, sw, tbY + 1, 0xFFFFFFFF);
        }
    }

    /** The Start button, which each Frames version draws its own way and with its own mark. */
    private void drawStart(final GuiGraphics g, final int tbY, final int sh, final boolean xp) {
        if (xp) {
            drawXpStart(g, tbY, sh);
            return;
        }
        final int sbW = 54;
        g.fill(4, tbY + 3, 4 + sbW, sh - 3, desktop.themeColours().startButton());
        bevel(g, 4, tbY + 3, sbW, DesktopScreen.TASKBAR_H - 6, 0xFFFFFFFF, 0xFF808080);
        // The edition's own mark, the same one its setup and its boot screen wear.
        FramesEmblem.draw(g, 8, tbY + 7, desktop.panelStyle());
        g.drawString(desktop.textFont(), "Start", 8 + FramesEmblem.SIZE + 3, tbY + 8, 0xFF000000, false);
    }

    /**
     * The row of window buttons. The program in front reads as a pushed-in button, the way a taskbar has
     * always said which program you are actually looking at; one whose windows are all put away sits raised
     * and paler, so it reads as "on the panel only".
     */
    private void drawTasks(final GuiGraphics g, final DesktopScreen.TaskStrip strip, final int tbY,
                           final int sh, final boolean xp) {
        for (int i = 0; i < strip.entries().size(); i++) {
            final TaskbarGroups.Entry entry = strip.entries().get(i);
            final int bx = strip.x()[i];
            final int btnW = strip.w()[i];
            if (btnW == 0) {
                continue; // on the quick launch only, or not shown at all
            }
            if (bx + btnW > strip.right()) {
                break;
            }
            final boolean active = entry.state() == TaskbarGroups.State.ACTIVE;
            final boolean minimized = entry.state() == TaskbarGroups.State.MINIMIZED;
            taskButton(g, bx, tbY + 3, btnW, DesktopScreen.TASKBAR_H - 6, active);
            if (minimized) {
                g.fill(bx + 1, tbY + 4, bx + btnW - 1, sh - 4, xp ? 0x38FFFFFF : 0x30FFFFFF);
            }
            ProgramIcons.draw(g, bx + 4, tbY + 6, 12, 12, desktop.programIdFor(entry.key()), desktop.icons());
            /*
             * No shadow: the taskbar button name sits on a solid button, where a shadow only muddies it
             * (a dark blob behind the dark 95 text, a halo behind the light XP text).
             */
            final boolean several = entry.windows() > 1;
            final int textColor = minimized
                    ? (xp ? 0xFFD0DCF0 : 0xFF606060)
                    : desktop.themeColours().startText();
            g.drawString(desktop.textFont(),
                    desktop.shorten(desktop.taskLabel(entry), desktop.taskTitleRoom(btnW - (several ? 8 : 0))),
                    bx + 20, tbY + 8, textColor, false);
            if (several) {
                desktop.drawStackCaret(g, bx + btnW - 8, tbY + 10, textColor);
            }
        }
    }

    /** The notification area, dressed in each version's own frame: XP inset in blue, 95 sunken in grey. */
    private void drawNotificationArea(final GuiGraphics g, final int tbY, final int sw, final int sh,
                                      final boolean xp) {
        final int trayX = desktop.tray().left(sw);
        if (xp) {
            g.fillGradient(trayX, tbY + 2, sw, sh - 2, 0xFF1A53C4, 0xFF0D3590);
            g.fill(trayX, tbY + 2, trayX + 1, sh - 2, 0xFF4A83E6); // the lit left edge
            g.fill(trayX + 1, tbY + 2, trayX + 2, sh - 2, 0xFF0A2C7A); // and its inset shadow
            desktop.tray().draw(g, tbY, sw, 0xFFFFFFFF);
            return;
        }
        if (desktop.isPanel(PanelStyle.FRAMES_95)) {
            g.fill(trayX, tbY + 3, sw - 2, sh - 3, desktop.themeColours().taskbar());
            bevel(g, trayX, tbY + 3, sw - 2 - trayX, DesktopScreen.TASKBAR_H - 6, 0xFF808080, 0xFFFFFFFF);
        }
        desktop.tray().draw(g, tbY, sw, desktop.themeColours().startText());
    }

    /**
     * The Frames XP quick launch: the pinned programs as small icons right after Start, with a rule
     * between them and the task buttons, the way that desktop kept them.
     */
    private void drawQuickLaunch(final GuiGraphics g, final DesktopScreen.TaskStrip strip, final int tbY,
                                 final int sh, final int lmx, final int lmy) {
        if (strip.quickCount() == 0) {
            return;
        }
        int j = 0;
        for (final TaskbarGroups.Entry entry : strip.entries()) {
            if (!entry.pinned()) {
                continue;
            }
            final int qx = strip.quickX() + j * DesktopScreen.QL_W;
            if (lmx >= qx && lmx < qx + DesktopScreen.QL_W && lmy >= tbY) {
                g.fill(qx, tbY + 3, qx + DesktopScreen.QL_W, sh - 3, 0x30FFFFFF);
            }
            ProgramIcons.draw(g, qx + 2, tbY + 6, 12, 12, desktop.programIdFor(entry.key()), desktop.icons());
            j++;
        }
        final int rule = strip.quickX() + strip.quickCount() * DesktopScreen.QL_W + 2;
        g.fill(rule, tbY + 5, rule + 1, sh - 5, 0xFF2C5FA8);
        g.fill(rule + 1, tbY + 5, rule + 2, sh - 5, 0xFF6FA3EF);
    }

    /** Draws a taskbar window button in the system's style (95 bevelled, XP gradient, 11 flat). */
    private void taskButton(final GuiGraphics g, final int x, final int y, final int w, final int h,
                            final boolean active) {
        switch (desktop.panelStyle()) {
            case FRAMES_XP -> {
                if (active) {
                    // Pushed in: the gradient runs the other way, with a shadow along the top edge.
                    g.fillGradient(x, y, x + w, y + h, 0xFF1E4FBC, 0xFF3670DC);
                    g.fill(x, y, x + w, y + 1, 0x40000000);
                } else {
                    g.fillGradient(x, y, x + w, y + h, 0xFF5B95DD, 0xFF2C5FA8);
                    g.fill(x, y, x + w, y + 1, 0x33FFFFFF);
                }
                desktop.drawOutline(g, x, y, w, h, 0xFF1A4CBF);
            }
            case FRAMES_11 -> g.fill(x, y, x + w, y + h, 0xFFE3E5EE);
            default -> {
                g.fill(x, y, x + w, y + h, desktop.themeColours().taskButton());
                // The classic bevel inverts when the button is pressed: dark on top, light underneath.
                bevel(g, x, y, w, h, active ? 0xFF808080 : 0xFFFFFFFF, active ? 0xFFFFFFFF : 0xFF808080);
            }
        }
    }

    /**
     * The Frames XP Start button: a glossy green pill flush with the left edge and rounded at its right end,
     * carrying the four-pane flag and the word in italics. It is the one control of that desktop everybody
     * pictures, and a plain green rectangle never read as it.
     */
    private void drawXpStart(final GuiGraphics g, final int tbY, final int sh) {
        final int top = tbY + 1;
        final int bottom = sh - 1;
        final int h = bottom - top;
        final int round = 6;
        xpStartBand(g, 0, top, DesktopScreen.XP_START_W - round, h);
        for (int i = 0; i < round; i++) {
            final double d = i + 1;
            final int inset = (int) Math.round(round - Math.sqrt(Math.max(0.0, round * round - d * d)));
            xpStartBand(g, DesktopScreen.XP_START_W - round + i, top + inset, 1, h - inset * 2);
        }
        // The gloss along the top.
        g.fill(2, top + 1, DesktopScreen.XP_START_W - round, top + 1 + h / 3, 0x3AFFFFFF);
        // The edition's own mark, the same one its setup and its boot screen wear.
        final int fx = 7;
        final int fy = tbY + 7;
        FramesEmblem.draw(g, fx, fy, PanelStyle.FRAMES_XP);
        g.drawString(desktop.textFont(), Component.literal("start")
                        .withStyle(ChatFormatting.BOLD, ChatFormatting.ITALIC),
                fx + 13, tbY + 8, 0xFFFFFFFF, true);
    }

    /** One vertical slice of the Start pill: light crown, body, and a darker foot, as the Luna button had. */
    private static void xpStartBand(final GuiGraphics g, final int x, final int y, final int w, final int h) {
        if (w <= 0 || h <= 0) {
            return;
        }
        final int q = Math.max(1, h / 4);
        g.fillGradient(x, y, x + w, y + q, 0xFF8FDD72, 0xFF57C04B);
        g.fillGradient(x, y + q, x + w, y + h - q, 0xFF4CB745, 0xFF2E9A33);
        g.fillGradient(x, y + h - q, x + w, y + h, 0xFF2E9A33, 0xFF24802A);
    }

    /** The Frames 11 Start glyph, the edition's mark centred in the eleven pixels its slot keeps for it. */
    private static void drawModernStart(final GuiGraphics g, final int x, final int y) {
        FramesEmblem.draw(g, x + 1, y + 1, PanelStyle.FRAMES_11);
    }

    /** The mark under a Frames 11 icon: what the program is doing, in the bar's own language. */
    private static void drawModernIndicator(final GuiGraphics g, final int cx, final int bottom,
                                            final TaskbarGroups.Entry entry) {
        final boolean several = entry.windows() > 1;
        switch (entry.state()) {
            case ACTIVE -> {
                if (several) {
                    g.fill(cx - 6, bottom - 2, cx - 1, bottom - 1, 0xFF4C84F0);
                    g.fill(cx + 1, bottom - 2, cx + 6, bottom - 1, 0xFF4C84F0);
                } else {
                    g.fill(cx - 6, bottom - 2, cx + 6, bottom - 1, 0xFF4C84F0);
                }
            }
            case OPEN, MINIMIZED -> {
                final int color = entry.state() == TaskbarGroups.State.OPEN
                        ? 0xFF8A93A4 : 0xFF5E6570;
                if (several) {
                    g.fill(cx - 4, bottom - 2, cx - 1, bottom - 1, color);
                    g.fill(cx + 1, bottom - 2, cx + 4, bottom - 1, color);
                } else {
                    g.fill(cx - 2, bottom - 2, cx + 2, bottom - 1, color);
                }
            }
            default -> {
            }
        }
    }

    /** A 1px 3D bevel: light top/left, dark bottom/right (the classic raised look). */
    private static void bevel(final GuiGraphics g, final int x, final int y, final int w, final int h,
                              final int light, final int dark) {
        g.fill(x, y, x + w, y + 1, light);
        g.fill(x, y, x + 1, y + h, light);
        g.fill(x, y + h - 1, x + w, y + h, dark);
        g.fill(x + w - 1, y, x + w, y + h, dark);
    }
}
