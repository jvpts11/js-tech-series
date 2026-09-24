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
import dev.jstech.computers.os.PanelStyle;
import dev.jstech.core.client.gui.component.Texts;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import net.minecraft.client.gui.GuiGraphics;

/**
 * The bars the Linux desktops put their open windows on.
 *
 * <p>KDE and Cinnamon share one: a dark band along the bottom with the launcher button at the left, a button
 * per open program, and the notification corner at the right. GNOME does not have one at all in the usual
 * sense; it has a thin top bar with Activities at one end and the clock in the middle, because the shell it
 * copies never had a task list. And a Legacy-era desktop has neither: it has a panel built out of raised
 * studs and sunken wells, drawn from the skin's own relief rather than from flat colours, the way panels
 * looked before anybody flattened them.
 *
 * <p>What a task button shows is the state of the program behind it: an open one has a line under it, the
 * one in front fills with the accent, one whose windows are all put away goes faint, and one with several
 * windows stacks and carries their number. That is worth the detail, because on a desktop with six things
 * open the panel is the only way of telling what is where.
 *
 * <p>The buttons are laid out by the same measurement the click handling tests against, so the button a
 * player sees and the button they hit are the same rectangle.
 *
 * <p>The colours they add to their theme's are the palette {@code jsc:panel/linux}.
 */
@PaletteHolder
final class LinuxPanels {

    private final DesktopScreen desktop;

    private static final Palette<Colours> PALETTE = Palettes.declare(JsComputers.MODID, "panel/linux",
            new Colours(0x22FFFFFF, 0xFFFFFFFF, 0x30FFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, 0xFF7C838A, 0xFFFFFFFF));

    /* How strongly a put-away program's line and icon are faded, as alphas over their own colour. */
    private static final int FADED_LINE_ALPHA = 0x60;
    private static final int FADED_ICON_ALPHA = 0x90;

    LinuxPanels(final DesktopScreen desktop) {
        this.desktop = desktop;
    }

    /**
     * The KDE Plasma and Cinnamon bottom panel: a dark bar with the launcher on the left, the task buttons
     * along it, and the clock and status at the right.
     */
    void renderModern(final GuiGraphics g, final int tbY, final int sw, final int sh,
                      final int lmx, final int lmy) {
        final boolean kde = desktop.isPanel(PanelStyle.KDE);
        final DesktopTheme theme = desktop.themeColours();
        g.fill(0, tbY, sw, sh, theme.taskbar());
        g.fill(0, tbY, sw, tbY + 1, theme.taskbarEdge());
        drawModernLauncher(g, tbY, sh, lmx, lmy, kde, theme);
        drawModernTasks(g, tbY, sw, sh, lmx, lmy, theme);
        desktop.tray().draw(g, tbY, sw, theme.startText());
    }

    /**
     * The panel of a Legacy-era Unix desktop, at the bottom for both KDE and GNOME. It is drawn entirely out
     * of the skin's own primitives (a raised launcher stud, raised task buttons, a sunken clock well) so the
     * panel is made of the same relief the windows are, instead of the flat modern band.
     */
    void renderPeriod(final GuiGraphics g, final int tbY, final int sw, final int sh,
                      final int lmx, final int lmy) {
        final OsSkin skin = desktop.panelSkin();
        final boolean kde = skin.form() == OsSkin.Form.KDE2;
        skin.statusBar(g, 0, tbY, sw, DesktopScreen.TASKBAR_H);

        // Launcher: KDE's K, GNOME's footprint. Both are a raised square stud, not a wide Start slab.
        final boolean startHot = desktop.launcherOpen() || (lmx >= 4 && lmx <= 58 && lmy >= tbY);
        skin.button(g, desktop.textFont(), 4, tbY + 3, 54, DesktopScreen.TASKBAR_H - 6,
                kde ? "K  " + GameText.resolve(PanelTexts.APPS) : "▲  " + GameText.resolve(PanelTexts.MENU),
                startHot, desktop.launcherOpen(), false);

        drawPeriodTasks(g, tbY, sw, lmx, lmy, skin);

        /*
         * A sunken well on the right: the period panels all recessed their status area rather than floating
         * the text on the band.
         */
        final int trayX = desktop.tray().left(sw);
        skin.field(g, trayX, tbY + 4, sw - trayX - 3, DesktopScreen.TASKBAR_H - 8, false);
        desktop.tray().draw(g, tbY, sw, skin.text());
    }

    /**
     * The GNOME top bar: Activities on the left, lit while the overview is open, the clock centered, and the
     * status group on the right. It occupies the top of the screen rather than the bottom.
     */
    void renderGnomeTopBar(final GuiGraphics g, final int sw, final int lmx, final int lmy) {
        final DesktopTheme theme = desktop.themeColours();
        g.fill(0, 0, sw, DesktopScreen.TASKBAR_H, theme.taskbar());
        g.fill(0, DesktopScreen.TASKBAR_H - 1, sw, DesktopScreen.TASKBAR_H, theme.taskbarEdge());
        final boolean hot = desktop.launcherOpen() || (lmx < 64 && lmy < DesktopScreen.TASKBAR_H);
        if (hot) {
            g.fill(4, 3, 62, DesktopScreen.TASKBAR_H - 3, PALETTE.get().hover());
        }
        g.drawString(desktop.textFont(), GameText.resolve(PanelTexts.ACTIVITIES), 8, 8, theme.startText(), false);
        final String clock = desktop.clock();
        g.drawString(desktop.textFont(), clock,
                (sw - desktop.textFont().width(clock)) / 2, 8, theme.startText(), false);
        // GNOME keeps its clock in the middle, so only the status group sits at the right end.
        desktop.tray().drawStatus(g, sw - PanelTray.PAD - desktop.tray().statusWidth(), 0, theme.startText());
    }

    private void drawModernLauncher(final GuiGraphics g, final int tbY, final int sh, final int lmx,
                                    final int lmy, final boolean kde, final DesktopTheme theme) {
        final Colours c = PALETTE.get();
        final boolean startHot = desktop.launcherOpen() || (lmx >= 4 && lmx <= 58 && lmy >= tbY);
        if (startHot) {
            g.fill(4, tbY + 2, 58, sh - 2, c.hover());
        }
        if (kde) {
            g.fill(8, tbY + 5, 22, tbY + 19, theme.startButton());
            g.drawString(desktop.textFont(), "K", 12, tbY + 8, c.glyph(), false);
            g.drawString(desktop.textFont(), GameText.resolve(PanelTexts.APPS), 26, tbY + 8, theme.startText(),
                    false);
        } else {
            g.fill(8, tbY + 5, 22, tbY + 19, theme.startButton());
            g.fill(11, tbY + 8, 19, tbY + 16, c.glyph());
            g.fill(13, tbY + 10, 17, tbY + 14, theme.startButton());
            g.drawString(desktop.textFont(), GameText.resolve(PanelTexts.MENU), 26, tbY + 8, theme.startText(),
                    false);
        }
    }

    /**
     * The task manager on a modern panel: a pinned program with nothing open is its icon alone; an open
     * program is a button with a line under it; the one in front fills with the accent; one whose windows are
     * all put away goes faint; and one with several windows stacks and carries their number.
     */
    private void drawModernTasks(final GuiGraphics g, final int tbY, final int sw, final int sh,
                                 final int lmx, final int lmy, final DesktopTheme theme) {
        final DesktopScreen.TaskStrip strip = desktop.taskButtons(sw);
        final int accent = theme.startButton();
        for (int i = 0; i < strip.entries().size(); i++) {
            final TaskbarGroups.Entry entry = strip.entries().get(i);
            final int bx = strip.x()[i];
            final int bw = strip.w()[i];
            if (bw == 0) {
                continue;
            }
            if (bx + bw > strip.right()) {
                break;
            }
            final boolean hot = lmx >= bx && lmx < bx + bw && lmy >= tbY;
            final boolean shown = entry.key().equals(desktop.openTaskPopup());
            if (!entry.open()) {
                if (hot || shown) {
                    g.fill(bx, tbY + 3, bx + bw, sh - 3, PALETTE.get().hover());
                }
                ProgramIcons.draw(g, bx + 3, tbY + 4, 16, 16,
                        desktop.programIdFor(entry.key()), desktop.icons());
                continue;
            }
            drawOpenTask(g, tbY, sh, entry, bx, bw, hot || shown, accent, theme);
        }
    }

    private void drawOpenTask(final GuiGraphics g, final int tbY, final int sh,
                              final TaskbarGroups.Entry entry, final int bx, final int bw,
                              final boolean highlighted, final int accent, final DesktopTheme theme) {
        final boolean active = entry.state() == TaskbarGroups.State.ACTIVE;
        final boolean minimized = entry.state() == TaskbarGroups.State.MINIMIZED;
        final boolean several = entry.windows() > 1;
        if (several) {
            g.fill(bx + 2, tbY + 1, bx + bw + 2, tbY + 3, theme.taskButton());
            g.fill(bx + bw, tbY + 3, bx + bw + 2, sh - 5, theme.taskButton());
        }
        final Colours c = PALETTE.get();
        g.fill(bx, tbY + 3, bx + bw, sh - 3,
                active ? accent : highlighted ? c.taskHover() : theme.taskButton());
        final int line = active ? c.activeLine() : minimized ? faded(accent, FADED_LINE_ALPHA) : accent;
        g.fill(bx, sh - 4, bx + bw, sh - 3, line);
        ProgramIcons.draw(g, bx + 3, tbY + 4, 16, 16, desktop.programIdFor(entry.key()), desktop.icons());
        if (minimized) {
            g.fill(bx + 3, tbY + 4, bx + 19, tbY + 20, faded(theme.taskbar(), FADED_ICON_ALPHA));
        }
        final int textColor = active ? c.activeInk() : minimized ? c.minimizedInk() : theme.startText();
        final int textW = bw - 22 - (several ? 12 : 0);
        g.drawString(desktop.textFont(),
                desktop.shorten(desktop.taskLabel(entry), desktop.taskTitleRoom(textW + 20)),
                bx + 22, tbY + 8, textColor, false);
        if (several) {
            final int badgeX = bx + bw - 12;
            g.fill(badgeX, tbY + 5, badgeX + 10, tbY + 13, active ? c.badge() : accent);
            Texts.small(g, desktop.textFont(), String.valueOf(entry.windows()),
                    badgeX + 3, tbY + 6, active ? accent : c.badge());
        }
    }

    /* That colour with its own alpha replaced, which is how a put-away program fades on the panel. */
    private static int faded(final int colour, final int alpha) {
        return (colour & 0xFFFFFF) | (alpha << 24);
    }

    /**
     * Task buttons on a period panel: pressed when that window is the one in front, exactly as such a panel
     * showed it, and drawn out of the skin's own button rather than a filled rectangle.
     */
    private void drawPeriodTasks(final GuiGraphics g, final int tbY, final int sw, final int lmx,
                                 final int lmy, final OsSkin skin) {
        final DesktopScreen.TaskStrip strip = desktop.taskButtons(sw);
        for (int i = 0; i < strip.entries().size(); i++) {
            final TaskbarGroups.Entry entry = strip.entries().get(i);
            final int bx = strip.x()[i];
            final int btnW = strip.w()[i];
            if (btnW == 0) {
                continue;
            }
            if (bx + btnW > strip.right()) {
                break;
            }
            final boolean active = entry.state() == TaskbarGroups.State.ACTIVE;
            final boolean minimized = entry.state() == TaskbarGroups.State.MINIMIZED;
            final boolean several = entry.windows() > 1;
            final boolean hot = lmx >= bx && lmx <= bx + btnW && lmy >= tbY;
            skin.button(g, desktop.textFont(), bx, tbY + 3, btnW, DesktopScreen.TASKBAR_H - 6, "",
                    hot, active, false);
            ProgramIcons.draw(g, bx + 3, tbY + 5, 12, 12, desktop.programIdFor(entry.key()), desktop.icons());
            final int textColor = minimized ? skin.dim() : skin.text();
            g.drawString(desktop.textFont(),
                    desktop.shorten(desktop.taskLabel(entry), desktop.taskTitleRoom(btnW - (several ? 8 : 0))),
                    bx + 19, tbY + 8 + (active ? 1 : 0), textColor, false);
            if (several) {
                desktop.drawStackCaret(g, bx + btnW - 8, tbY + 10 + (active ? 1 : 0), textColor);
            }
        }
    }

    /**
     * What the Linux panels add to their theme: a hovered launcher or icon, the marks on the launcher, a hovered
     * task button, the line and ink of the program in front, a put-away program's ink, and the window count badge.
     */
    private record Colours(int hover, int glyph, int taskHover, int activeLine, int activeInk, int minimizedInk,
                           int badge) {
    }
}
