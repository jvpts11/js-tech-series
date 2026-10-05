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
import dev.jstech.computers.gui.layout.TransitionPanelLayout;
import dev.jstech.computers.os.WorkspaceSet;
import dev.jstech.core.client.gui.component.ContextMenu;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.Texts;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.Nullable;

/**
 * The panels KDE and GNOME had on Transition hardware.
 *
 * <p>KDE 4's Plasma panel runs along the foot, pale and translucent: the round launcher that opens Kickoff, the pager
 * with the four workspaces two by two (each a small picture of the windows on it), the task manager, and the
 * notification area with the clock. Its desktop puts the icons in a Folder View, a dark glass box with a title, and
 * keeps the cashew in the top corner, which offers the desktop's settings and the way out.
 *
 * <p>GNOME 2 has two panels in Clearlooks' warm grey. Along the top its three menus, Applications (by what a program
 * is for), Places and System, two launchers, the notification area, the clock, the account and the way out; along
 * the foot the button that shows the desktop, the window list, the workspaces in a row, and the trash.
 *
 * <p>Everything here is placed by {@link TransitionPanelLayout}, which the clicks read too. The colours are the
 * palettes {@code jsc:panel/kde4} and {@code jsc:panel/gnome2}.
 */
@PaletteHolder
final class TransitionPanels {

    private final DesktopState desktop;

    private static final Palette<Kde4> KDE4 = Palettes.declare(JsComputers.MODID, "panel/kde4", new Kde4(
            0xC8FFFFFF, 0xDCE2EAF2, 0xF2FFFFFF, 0xFF9FD0FF, 0xFF16427E, 0xFF2A6FC9, 0xFFFFFFFF,
            0x99FFFFFF, 0xFF8EC3F5, 0xFF9FB0C4, 0xFF5A7894,
            0x8C8CBEF0, 0x5A5A96DC, 0x995090D2, 0x335A96DC, 0xFF1D2B3A, 0xFF6B7A8A,
            0xBFFFFFFF, 0xFF2A6FC9, 0x8C141E2D, 0x66FFFFFF, 0xFFFFFFFF));

    private static final Palette<Gnome2> GNOME2 = Palettes.declare(JsComputers.MODID, "panel/gnome2", new Gnome2(
            0xFFF4F3F1, 0xFFDCDAD5, 0xFFA7A49E, 0xFF86ABD9, 0xFF6A96CF, 0xFF000000, 0xFFFFFFFF, 0xFF3A3A3A,
            0xFFFBFBFA, 0xFFE4E2DE, 0xFFD8D6D1, 0xFFC5C2BC, 0xFFB5B2AC, 0xFF8F8C86, 0xFFFFFFFF, 0xFF86ABD9,
            0xFF6B6964));

    /** The cashew in KDE 4's top corner, and the launchers along GNOME 2's top: the terminal and the network. */
    private static final int CASHEW = 18;
    private static final List<String> GNOME2_LAUNCHERS = List.of("command_prompt", "network_manager");
    /** GNOME 2's way out at the top right, a small power mark. */
    private static final int POWER = 10;

    /** GNOME 2's Applications menu: what each program is for, by its id path; the rest are system tools. */
    private static final Map<String, TextKey> PURPOSE = purposes();
    /** The programs GNOME 2 keeps elsewhere than its Applications menu: the places and the preferences. */
    private static final Set<String> NOT_APPLICATIONS = Set.of("files", "this_pc", "network", "trash", "settings");

    TransitionPanels(final DesktopState desktop) {
        this.desktop = desktop;
    }

    /** Whether a desktop-local point is on KDE 4's round launcher; GNOME 2 has none on its foot. */
    boolean startHit(final double mx, final double my, final int tbY) {
        if (!desktop.kde4()) {
            return false;
        }
        final int y = tbY + (TransitionPanelLayout.BAR_H - TransitionPanelLayout.ORB) / 2;
        return mx >= TransitionPanelLayout.ORB_X && mx < TransitionPanelLayout.ORB_X + TransitionPanelLayout.ORB
                && my >= y && my < y + TransitionPanelLayout.ORB;
    }

    /** KDE 4's panel along the foot. */
    void renderKde4(final GuiGraphics g, final int tbY, final int sw, final int sh, final int lmx, final int lmy) {
        final Kde4 c = KDE4.get();
        g.fillGradient(0, tbY, sw, sh, c.panelTop(), c.panelBottom());
        g.fill(0, tbY, sw, tbY + 1, c.panelLine());
        // The launcher: a round blue orb with the K, lit while Kickoff is up or under the pointer.
        final int orbY = tbY + (TransitionPanelLayout.BAR_H - TransitionPanelLayout.ORB) / 2;
        final boolean lit = desktop.start().isOpen() || startHit(lmx, lmy, tbY);
        ChromeShapes.disc(g, TransitionPanelLayout.ORB_X, orbY, TransitionPanelLayout.ORB,
                lit ? ChromeShapes.lighten(c.orbTop()) : c.orbTop(), c.orbBottom(), c.orbRim());
        Draw.text(g, desktop.textFont(), "K", TransitionPanelLayout.ORB_X + 7, orbY + 6, c.orbGlyph());
        // The pager: the four workspaces, the one up lit, each with the windows on it drawn small.
        for (int i = 0; i < WorkspaceSet.COUNT; i++) {
            final int px = TransitionPanelLayout.pagerCellX(i);
            final int py = tbY + TransitionPanelLayout.pagerCellY(i);
            g.fill(px, py, px + TransitionPanelLayout.PAGER_CELL_W, py + TransitionPanelLayout.PAGER_CELL_H,
                    i == desktop.workspace() ? c.pagerOn() : c.pagerFill());
            drawWindowsIn(g, i, px, py, TransitionPanelLayout.PAGER_CELL_W, TransitionPanelLayout.PAGER_CELL_H,
                    c.pagerWindow());
            ChromeShapes.outline(g, px, py, TransitionPanelLayout.PAGER_CELL_W, TransitionPanelLayout.PAGER_CELL_H,
                    c.pagerRim());
        }
        drawKde4Tasks(g, tbY, sw, lmx, lmy, c);
        desktop.tray().draw(g, tbY, sw, c.ink());
    }

    /**
     * KDE 4's desktop over its wallpaper: the Folder View the icons stand in, a dark glass box with its title, and
     * the cashew in the top corner.
     */
    void renderKde4Desktop(final GuiGraphics g, final int sw, final int lmx, final int lmy) {
        final Kde4 c = KDE4.get();
        final int[] box = desktop.iconGrid().folderView();
        ChromeShapes.roundedRect(g, box[0], box[1], box[2], box[3], c.folderFill(), 3, 3);
        ChromeShapes.roundedOutline(g, box[0], box[1], box[2], box[3], c.folderRule(), 2);
        final String title = GameText.resolve(TransitionTexts.DESKTOP_FOLDER);
        final int tw = desktop.textFont().width(title);
        Draw.text(g, desktop.textFont(), title, box[0] + (box[2] - tw) / 2, box[1] + 3, c.folderInk());
        g.fill(box[0] + 4, box[1] + DesktopIcons.FOLDER_TITLE, box[0] + box[2] - 4, box[1] + DesktopIcons.FOLDER_TITLE
                + 1, c.folderRule());
        final int[] cashew = cashewBox(sw);
        final boolean hot = lmx >= cashew[0] && lmy >= cashew[1] && lmx < cashew[0] + CASHEW
                && lmy < cashew[1] + CASHEW;
        ChromeShapes.roundedRect(g, cashew[0], cashew[1], CASHEW, CASHEW, hot ? c.panelLine() : c.cashew(), 0, 6);
        // The cashew's mark: a small diamond of the accent.
        final int cx = cashew[0] + CASHEW / 2;
        final int cy = cashew[1] + CASHEW / 2;
        for (int i = 0; i < 4; i++) {
            g.fill(cx - i, cy - 3 + i, cx + i + 1, cy - 2 + i, c.cashewInk());
            g.fill(cx - i, cy + 3 - i, cx + i + 1, cy + 4 - i, c.cashewInk());
        }
    }

    /** GNOME 2's two panels: its menus along the top, its windows along the foot. */
    void renderGnome2(final GuiGraphics g, final int tbY, final int sw, final int sh, final int lmx, final int lmy) {
        renderGnome2Top(g, sw, lmx, lmy);
        renderGnome2Foot(g, tbY, sw, sh, lmx, lmy);
    }

    /**
     * A click on these panels or on KDE 4's desktop parts, and true when it was theirs: the pager, the cashew,
     * GNOME 2's menus, launchers, notification area and way out, the show-desktop button, the switcher and the trash.
     * The task buttons and KDE 4's notification area are the common panel's to answer.
     */
    boolean click(final double mx, final double my, final int button, final int tbY) {
        final int sw = desktop.view().width();
        if (desktop.kde4()) {
            return clickKde4(mx, my, button, tbY, sw);
        }
        if (desktop.gnome2()) {
            if (my < TransitionPanelLayout.BAR_H) {
                clickGnome2Top(mx, button, sw);
                return true;
            }
            return my >= tbY && clickGnome2Foot(mx, button, sw);
        }
        return false;
    }

    /** Whether a desktop-local point is on KDE 4's cashew, which must take its click before the windows do. */
    boolean onCashew(final double mx, final double my) {
        if (!desktop.kde4()) {
            return false;
        }
        final int[] box = cashewBox(desktop.view().width());
        return mx >= box[0] && my >= box[1] && mx < box[0] + CASHEW && my < box[1] + CASHEW;
    }

    /** The desktop-local middle of KDE 4's pager cell or GNOME 2's switcher cell {@code index}. */
    int[] workspacePoint(final int index) {
        final int sw = desktop.view().width();
        final int tbY = desktop.view().height() - TransitionPanelLayout.BAR_H;
        if (desktop.kde4()) {
            return new int[] {TransitionPanelLayout.pagerCellX(index) + TransitionPanelLayout.PAGER_CELL_W / 2,
                    tbY + TransitionPanelLayout.pagerCellY(index) + TransitionPanelLayout.PAGER_CELL_H / 2};
        }
        return new int[] {TransitionPanelLayout.switcherCellX(sw, index) + TransitionPanelLayout.SWITCHER_CELL_W / 2,
                tbY + TransitionPanelLayout.BAR_H / 2};
    }

    /** The desktop-local middle of GNOME 2's menu {@code index}: Applications, Places, System. */
    int[] menuPoint(final int index) {
        final int[] menu = gnome2Menus()[index];
        return new int[] {menu[0] + menu[1] / 2, TransitionPanelLayout.BAR_H / 2};
    }

    /** The desktop-local middle of KDE 4's cashew. */
    int[] cashewPoint() {
        final int[] box = cashewBox(desktop.view().width());
        return new int[] {box[0] + CASHEW / 2, box[1] + CASHEW / 2};
    }

    /** The desktop-local middle of GNOME 2's show-desktop button. */
    int[] showDesktopPoint() {
        return new int[] {TransitionPanelLayout.SHOW_DESKTOP_X + TransitionPanelLayout.SHOW_DESKTOP_W / 2,
                desktop.view().height() - TransitionPanelLayout.BAR_H / 2};
    }

    /** The left edge and the right end of the task buttons on these panels. */
    int tasksLeft() {
        return desktop.kde4() ? TransitionPanelLayout.KDE4_TASKS_X : TransitionPanelLayout.GNOME2_TASKS_X;
    }

    int tasksRight(final int sw) {
        return desktop.kde4() ? desktop.tray().taskStripRight(sw) : TransitionPanelLayout.gnome2TasksRight(sw);
    }

    /** Opens GNOME 2's Applications menu, as the keyboard's own key for the programs does. */
    void openApplications() {
        openMenu(applicationsMenu(), gnome2Menus()[0][0]);
    }

    /** Where GNOME 2's speaker stands on its top panel, which the volume control opens from. */
    int gnome2SpeakerX(final int sw) {
        return gnome2StatusX(sw) + PanelTray.speakerOffset();
    }

    private boolean clickKde4(final double mx, final double my, final int button, final int tbY, final int sw) {
        if (onCashew(mx, my) && button == 0) {
            final int[] box = cashewBox(sw);
            desktop.taskbar().menu().open(List.of(
                    item(TransitionTexts.DESKTOP_SETTINGS,
                            () -> desktop.opener().openSettingsPage(SettingsApp.PAGE_PERSONALIZE)),
                    item(TransitionTexts.LEAVE, () -> desktop.power().open())),
                    box[0] - 80, box[1] + CASHEW, 0, 0, sw, desktop.view().height());
            return true;
        }
        if (my < tbY) {
            return false;
        }
        for (int i = 0; i < WorkspaceSet.COUNT; i++) {
            final int px = TransitionPanelLayout.pagerCellX(i);
            final int py = tbY + TransitionPanelLayout.pagerCellY(i);
            if (mx >= px && mx < px + TransitionPanelLayout.PAGER_CELL_W && my >= py
                    && my < py + TransitionPanelLayout.PAGER_CELL_H) {
                if (button == 0) {
                    desktop.switchWorkspace(i);
                }
                return true;
            }
        }
        return false;
    }

    private void clickGnome2Top(final double mx, final int button, final int sw) {
        if (button != 0) {
            return;
        }
        final int[][] menus = gnome2Menus();
        if (hit(mx, menus[0])) {
            openMenu(applicationsMenu(), menus[0][0]);
            return;
        }
        if (hit(mx, menus[1])) {
            openMenu(placesMenu(), menus[1][0]);
            return;
        }
        if (hit(mx, menus[2])) {
            openMenu(systemMenu(), menus[2][0]);
            return;
        }
        final int end = menus[2][0] + menus[2][1];
        final List<Launcher> launchers = gnome2Launchers();
        for (int i = 0; i < launchers.size(); i++) {
            final int lx = TransitionPanelLayout.launcherX(end, i);
            if (mx >= lx && mx < lx + TransitionPanelLayout.LAUNCHER) {
                desktop.opener().run(launchers.get(i));
                return;
            }
        }
        final int speaker = gnome2SpeakerX(sw);
        if (mx >= speaker - 2 && mx < speaker + PanelTray.speakerWidth() + 2) {
            desktop.volume().toggle();
            return;
        }
        if (mx >= sw - 4 - POWER - 2) {
            desktop.start().close();
            desktop.power().open();
        }
    }

    private boolean clickGnome2Foot(final double mx, final int button, final int sw) {
        if (mx >= TransitionPanelLayout.SHOW_DESKTOP_X
                && mx < TransitionPanelLayout.SHOW_DESKTOP_X + TransitionPanelLayout.SHOW_DESKTOP_W) {
            if (button == 0) {
                desktop.wm().showDesktop();
            }
            return true;
        }
        for (int i = 0; i < WorkspaceSet.COUNT; i++) {
            final int cx = TransitionPanelLayout.switcherCellX(sw, i);
            if (mx >= cx && mx < cx + TransitionPanelLayout.SWITCHER_CELL_W) {
                if (button == 0) {
                    desktop.switchWorkspace(i);
                }
                return true;
            }
        }
        final int trash = TransitionPanelLayout.trashX(sw);
        if (mx >= trash && mx < trash + TransitionPanelLayout.TRASH_W) {
            if (button == 0) {
                desktop.openTrash();
            }
            return true;
        }
        return false;
    }

    private void renderGnome2Top(final GuiGraphics g, final int sw, final int lmx, final int lmy) {
        final Gnome2 c = GNOME2.get();
        final int h = TransitionPanelLayout.BAR_H;
        g.fillGradient(0, 0, sw, h, c.barTop(), c.barBottom());
        g.fill(0, h - 1, sw, h, c.rule());
        final int[][] menus = gnome2Menus();
        final TextKey[] names = {DesktopTexts.APPLICATIONS, TransitionTexts.PLACES, TransitionTexts.SYSTEM};
        for (int i = 0; i < menus.length; i++) {
            final boolean lit = lmy < h && lmx >= menus[i][0] && lmx < menus[i][0] + menus[i][1];
            if (lit) {
                g.fillGradient(menus[i][0], 2, menus[i][0] + menus[i][1], h - 2, c.menuTop(), c.menuBottom());
            }
            int tx = menus[i][0] + TransitionPanelLayout.MENU_PAD;
            if (i == 0) {
                foot(g, tx, 6, lit ? c.menuInk() : c.foot());
                tx += TransitionPanelLayout.FOOT + 4;
            }
            Draw.text(g, desktop.textFont(), GameText.resolve(names[i]), tx, 8, lit ? c.menuInk() : c.ink());
        }
        final int end = menus[2][0] + menus[2][1];
        final List<Launcher> launchers = gnome2Launchers();
        for (int i = 0; i < launchers.size(); i++) {
            ProgramIcons.draw(g, TransitionPanelLayout.launcherX(end, i) + 2, 4, 16, 16,
                    launchers.get(i).programId(), desktop.icons());
        }
        // The right end: the notification area, the clock, the account and the way out.
        final int statusX = gnome2StatusX(sw);
        desktop.tray().drawStatus(g, statusX, 0, c.ink());
        final String clock = gnome2Clock();
        final int clockX = statusX + desktop.tray().statusWidth() + 8;
        Draw.text(g, desktop.textFont(), clock, clockX, 8, c.ink());
        final String account = desktop.accountLabel();
        Draw.text(g, desktop.textFont(), account, clockX + desktop.textFont().width(clock) + 10, 8, c.ink());
        power(g, sw - 4 - POWER, 7, c.ink());
    }

    private void renderGnome2Foot(final GuiGraphics g, final int tbY, final int sw, final int sh, final int lmx,
                                  final int lmy) {
        final Gnome2 c = GNOME2.get();
        g.fillGradient(0, tbY, sw, sh, c.barTop(), c.barBottom());
        g.fill(0, tbY, sw, tbY + 1, c.rule());
        // The button that shows the desktop: a small screen.
        final int sx = TransitionPanelLayout.SHOW_DESKTOP_X;
        g.fillGradient(sx, tbY + 3, sx + TransitionPanelLayout.SHOW_DESKTOP_W, sh - 3, c.listTop(), c.listBottom());
        ChromeShapes.roundedOutline(g, sx, tbY + 3, TransitionPanelLayout.SHOW_DESKTOP_W, TransitionPanelLayout.BAR_H
                - 6, c.listRim(), 1);
        g.fill(sx + 4, tbY + 7, sx + 14, tbY + 15, c.menuBottom());
        g.fill(sx + 8, tbY + 15, sx + 10, tbY + 17, c.foot());
        drawGnome2Tasks(g, tbY, sw, lmx, lmy, c);
        // The switcher: the four workspaces in a row, the one up in the theme's blue.
        for (int i = 0; i < WorkspaceSet.COUNT; i++) {
            final int cx = TransitionPanelLayout.switcherCellX(sw, i);
            final int cy = tbY + 4;
            final int ch = TransitionPanelLayout.BAR_H - 8;
            g.fill(cx, cy, cx + TransitionPanelLayout.SWITCHER_CELL_W, cy + ch,
                    i == desktop.workspace() ? c.switcherOn() : c.switcherCell());
            drawWindowsIn(g, i, cx, cy, TransitionPanelLayout.SWITCHER_CELL_W, ch, c.listRim());
            ChromeShapes.outline(g, cx, cy, TransitionPanelLayout.SWITCHER_CELL_W, ch, c.switcherRim());
        }
        ProgramIcons.draw(g, TransitionPanelLayout.trashX(sw) + 1, tbY + 4, 16, 16, desktop.trash().icon(),
                desktop.icons());
    }

    /** KDE 4's task manager: a soft lit pill under the program in front, the others plain until the pointer is over. */
    private void drawKde4Tasks(final GuiGraphics g, final int tbY, final int sw, final int lmx, final int lmy,
                               final Kde4 c) {
        final TaskStrip strip = desktop.taskbar().strip(sw);
        for (int i = 0; i < strip.entries().size(); i++) {
            final TaskbarGroups.Entry entry = strip.entries().get(i);
            final int bx = strip.x()[i];
            final int bw = strip.w()[i];
            if (bw == 0 || bx + bw > strip.right()) {
                continue;
            }
            final boolean active = entry.state() == TaskbarGroups.State.ACTIVE;
            final boolean hot = lmx >= bx && lmx < bx + bw && lmy >= tbY;
            if (active) {
                g.fillGradient(bx, tbY + 3, bx + bw, tbY + TransitionPanelLayout.BAR_H - 3, c.taskOnTop(),
                        c.taskOnBottom());
                ChromeShapes.roundedOutline(g, bx, tbY + 3, bw, TransitionPanelLayout.BAR_H - 6, c.taskRim(), 2);
            } else if (hot) {
                g.fill(bx, tbY + 3, bx + bw, tbY + TransitionPanelLayout.BAR_H - 3, c.taskHover());
            }
            ProgramIcons.draw(g, bx + 4, tbY + 4, 16, 16, desktop.programIdFor(entry.key()), desktop.icons());
            final boolean away = entry.state() == TaskbarGroups.State.MINIMIZED;
            Draw.text(g, desktop.textFont(), desktop.shorten(desktop.taskbar().label(entry),
                    TaskbarModel.titleRoom(bw - 4)), bx + 23, tbY + 8, away ? c.dimInk() : c.ink());
        }
    }

    /** GNOME 2's window list: a raised button per program, pressed in for the one in front. */
    private void drawGnome2Tasks(final GuiGraphics g, final int tbY, final int sw, final int lmx, final int lmy,
                                 final Gnome2 c) {
        final TaskStrip strip = desktop.taskbar().strip(sw);
        for (int i = 0; i < strip.entries().size(); i++) {
            final TaskbarGroups.Entry entry = strip.entries().get(i);
            final int bx = strip.x()[i];
            final int bw = strip.w()[i];
            if (bw == 0 || bx + bw > strip.right()) {
                continue;
            }
            final boolean active = entry.state() == TaskbarGroups.State.ACTIVE;
            final boolean hot = lmx >= bx && lmx < bx + bw && lmy >= tbY;
            // Pressed in for the program in front; lit through under the pointer, a plain raised button otherwise.
            final int top = active ? c.pressedTop() : c.listTop();
            final int bottom = active ? c.pressedBottom() : hot ? c.listTop() : c.listBottom();
            g.fillGradient(bx, tbY + 3, bx + bw, tbY + TransitionPanelLayout.BAR_H - 3, top, bottom);
            ChromeShapes.roundedOutline(g, bx, tbY + 3, bw, TransitionPanelLayout.BAR_H - 6, c.listRim(), 1);
            ProgramIcons.draw(g, bx + 4, tbY + 5, 14, 14, desktop.programIdFor(entry.key()), desktop.icons());
            final boolean away = entry.state() == TaskbarGroups.State.MINIMIZED;
            Draw.text(g, desktop.textFont(), desktop.shorten(desktop.taskbar().label(entry),
                    TaskbarModel.titleRoom(bw - 4)), bx + 21, tbY + 8 + (active ? 1 : 0), away ? c.dim() : c.ink());
        }
    }

    /** The windows on workspace {@code index} drawn small in a cell of the pager or the switcher. */
    private void drawWindowsIn(final GuiGraphics g, final int index, final int x, final int y, final int w,
                               final int h, final int colour) {
        final int vw = Math.max(1, desktop.view().width());
        final int vh = Math.max(1, desktop.view().height());
        for (final DesktopWindow window : desktop.wm().all()) {
            if (window.dialog() || window.minimized() || !window.on(index)) {
                continue;
            }
            final int wx = x + 1 + window.x() * (w - 2) / vw;
            final int wy = y + 1 + window.y() * (h - 2) / vh;
            final int ww = Math.max(2, window.width() * (w - 2) / vw);
            final int wh = Math.max(2, window.height() * (h - 2) / vh);
            ChromeShapes.outline(g, wx, wy, Math.min(ww, x + w - 1 - wx), Math.min(wh, y + h - 1 - wy), colour);
        }
    }

    /** GNOME 2's footprint, the mark on its Applications menu: a sole and four toes. */
    private static void foot(final GuiGraphics g, final int x, final int y, final int colour) {
        g.fill(x + 3, y + 5, x + 9, y + 11, colour);
        g.fill(x + 4, y + 11, x + 8, y + 12, colour);
        g.fill(x + 1, y + 2, x + 3, y + 4, colour);
        g.fill(x + 4, y, x + 6, y + 2, colour);
        g.fill(x + 7, y, x + 9, y + 2, colour);
        g.fill(x + 10, y + 2, x + 12, y + 4, colour);
    }

    /** A power mark: a ring open at the top, with a bar standing in its gap. */
    private static void power(final GuiGraphics g, final int x, final int y, final int colour) {
        final int top = y + 2;
        final int bottom = y + POWER;
        g.fill(x, top + 1, x + 1, bottom - 1, colour);
        g.fill(x + POWER - 1, top + 1, x + POWER, bottom - 1, colour);
        g.fill(x + 1, bottom - 1, x + POWER - 1, bottom, colour);
        g.fill(x + 1, top, x + 3, top + 1, colour);
        g.fill(x + POWER - 3, top, x + POWER - 1, top + 1, colour);
        g.fill(x + POWER / 2 - 1, y, x + POWER / 2 + 1, y + 5, colour);
    }

    private int[][] gnome2Menus() {
        return TransitionPanelLayout.menus(desktop.textFont().width(GameText.resolve(DesktopTexts.APPLICATIONS)),
                desktop.textFont().width(GameText.resolve(TransitionTexts.PLACES)),
                desktop.textFont().width(GameText.resolve(TransitionTexts.SYSTEM)));
    }

    /** Where GNOME 2's notification area starts at the top right, before its clock, the account and the way out. */
    private int gnome2StatusX(final int sw) {
        final int account = desktop.textFont().width(desktop.accountLabel());
        final int clock = desktop.textFont().width(gnome2Clock());
        return sw - 4 - POWER - 10 - account - 10 - clock - 8 - desktop.tray().statusWidth();
    }

    /** GNOME 2's clock: the day and the time, as its applet wrote them. */
    private String gnome2Clock() {
        return GameText.resolve(PanelTexts.DAY.with(desktop.prefs().dayOfWorld())) + ", "
                + desktop.prefs().clockText();
    }

    /** The launchers GNOME 2 keeps beside its menus, those of them this machine has. */
    private List<Launcher> gnome2Launchers() {
        final List<Launcher> out = new ArrayList<>();
        for (final String id : GNOME2_LAUNCHERS) {
            final Launcher l = launcher(id);
            if (l != null) {
                out.add(l);
            }
        }
        return out;
    }

    @Nullable
    private Launcher launcher(final String programPath) {
        for (final Launcher l : desktop.launcherList()) {
            if (l.programId().getPath().equals(programPath)) {
                return l;
            }
        }
        return null;
    }

    private void openMenu(final List<ContextMenu.Item> items, final int x) {
        desktop.start().close();
        desktop.taskbar().menu().open(items, x, TransitionPanelLayout.BAR_H, 0, 0, desktop.view().width(),
                desktop.view().height());
    }

    /** Applications: a submenu for each purpose this machine has a program for, in the order GNOME listed them. */
    private List<ContextMenu.Item> applicationsMenu() {
        final Map<TextKey, List<ContextMenu.Item>> by = new LinkedHashMap<>();
        for (final TextKey purpose : List.of(TransitionTexts.ACCESSORIES, TransitionTexts.GAMES,
                TransitionTexts.GRAPHICS, TransitionTexts.INTERNET, TransitionTexts.PROGRAMMING,
                TransitionTexts.SOUND_AND_VIDEO, TransitionTexts.SYSTEM_TOOLS)) {
            by.put(purpose, new ArrayList<>());
        }
        for (final Launcher l : desktop.launcherList()) {
            final String path = l.programId().getPath();
            if (NOT_APPLICATIONS.contains(path)) {
                continue;
            }
            by.get(PURPOSE.getOrDefault(path, TransitionTexts.SYSTEM_TOOLS))
                    .add(new ContextMenu.Item(l.label(), true, () -> desktop.opener().run(l)));
        }
        final List<ContextMenu.Item> out = new ArrayList<>();
        by.forEach((purpose, programs) -> {
            if (!programs.isEmpty()) {
                out.add(ContextMenu.Item.submenu(GameText.resolve(purpose), programs));
            }
        });
        return out;
    }

    /** Places: the home folder, the desktop's folder, the computer and the network. */
    private List<ContextMenu.Item> placesMenu() {
        final List<ContextMenu.Item> out = new ArrayList<>();
        final Launcher files = launcher("files");
        if (files != null) {
            out.add(item(TransitionTexts.HOME_FOLDER, () -> desktop.opener().run(files)));
            out.add(item(TransitionTexts.DESKTOP, () -> desktop.opener().openFolder(
                    FilesApp.desktopDirAt(desktop.hostPos(), false))));
        }
        addLauncher(out, "this_pc");
        addLauncher(out, "network");
        return out;
    }

    /** System: the preferences by page, the administration tools, and the way out. */
    private List<ContextMenu.Item> systemMenu() {
        final List<ContextMenu.Item> preferences = List.of(
                page(SettingsTexts.PERSONALIZE, SettingsApp.PAGE_PERSONALIZE),
                page(SettingsTexts.DISPLAY, SettingsApp.PAGE_DISPLAY),
                page(SettingsTexts.SOUND, SettingsApp.PAGE_SOUND),
                page(SettingsTexts.NETWORK, 2));
        final List<ContextMenu.Item> administration = new ArrayList<>();
        addLauncher(administration, "device_manager");
        addLauncher(administration, "task_manager");
        addLauncher(administration, "disks");
        final List<ContextMenu.Item> out = new ArrayList<>();
        out.add(ContextMenu.Item.submenu(GameText.resolve(TransitionTexts.PREFERENCES), preferences));
        if (!administration.isEmpty()) {
            out.add(ContextMenu.Item.submenu(GameText.resolve(TransitionTexts.ADMINISTRATION), administration));
        }
        out.add(ContextMenu.Item.separator());
        out.add(item(TransitionTexts.SHUT_DOWN_ELLIPSIS, () -> desktop.power().open()));
        return out;
    }

    private ContextMenu.Item page(final TextKey name, final int page) {
        return item(name, () -> desktop.opener().openSettingsPage(page));
    }

    private void addLauncher(final List<ContextMenu.Item> out, final String programPath) {
        final Launcher l = launcher(programPath);
        if (l != null) {
            out.add(new ContextMenu.Item(l.label(), true, () -> desktop.opener().run(l)));
        }
    }

    private static ContextMenu.Item item(final TextKey label, final Runnable action) {
        return new ContextMenu.Item(GameText.resolve(label), true, action);
    }

    private static boolean hit(final double mx, final int[] span) {
        return mx >= span[0] && mx < span[0] + span[1];
    }

    /** The cashew's box in KDE 4's top right corner. */
    private int[] cashewBox(final int sw) {
        return new int[] {sw - CASHEW, desktop.view().workAreaTop()};
    }

    private static Map<String, TextKey> purposes() {
        final Map<String, TextKey> out = new LinkedHashMap<>();
        for (final String id : List.of("calculator", "editor", "command_prompt", "screenfetch", "ark")) {
            out.put(id, TransitionTexts.ACCESSORIES);
        }
        for (final String id : List.of("minesweeper", "solitaire", "snake")) {
            out.put(id, TransitionTexts.GAMES);
        }
        for (final String id : List.of("paint", "exposure")) {
            out.put(id, TransitionTexts.GRAPHICS);
        }
        for (final String id : List.of("network_manager", "messenger", "remote_control", "gateway_manager",
                "knothub", "mirror")) {
            out.put(id, TransitionTexts.INTERNET);
        }
        for (final String id : List.of("virtual_studio", "virtual_studio_code", "sigma", "sigma_program", "emacs",
                "vim", "knot", "workshop", "iqlengine", "isms", "nextgre_studio", "prophet_console")) {
            out.put(id, TransitionTexts.PROGRAMMING);
        }
        out.put("soundfoundry", TransitionTexts.SOUND_AND_VIDEO);
        return Map.copyOf(out);
    }

    /**
     * KDE 4's panel colours: the panel's crown and foot and its lit top line, the launcher orb's crown, foot, rim and
     * mark, the pager's cells, the one up, their rim and the windows drawn in them, the program in front's pill (crown,
     * foot, rim), a hovered button, the ink and a put-away program's ink, the cashew and its mark, and the Folder
     * View's glass, its rule and its title.
     */
    private record Kde4(int panelTop, int panelBottom, int panelLine, int orbTop, int orbBottom, int orbRim,
                        int orbGlyph, int pagerFill, int pagerOn, int pagerRim, int pagerWindow, int taskOnTop,
                        int taskOnBottom, int taskRim, int taskHover, int ink, int dimInk, int cashew, int cashewInk,
                        int folderFill, int folderRule, int folderInk) {
    }

    /**
     * GNOME 2's panel colours: the panels' crown and foot and their rule, a menu lit under the pointer (crown and
     * foot), the ink, a lit menu's ink and the footprint, a window button's crown and foot, pressed in, and its rim,
     * the switcher's rim, cells and the one up, and a put-away program's ink.
     */
    private record Gnome2(int barTop, int barBottom, int rule, int menuTop, int menuBottom, int ink, int menuInk,
                          int foot, int listTop, int listBottom, int pressedTop, int pressedBottom, int listRim,
                          int switcherRim, int switcherCell, int switcherOn, int dim) {
    }
}
