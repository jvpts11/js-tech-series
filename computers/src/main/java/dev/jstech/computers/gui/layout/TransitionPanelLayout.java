/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

import dev.jstech.computers.os.WorkspaceSet;
import dev.jstech.core.gui.layout.GuiLayout;

/**
 * Where everything stands on the panels KDE and GNOME had on Transition hardware, measured from the panel's own left
 * edge and top, so the drawing and the clicks read the same numbers and a test proves nothing overlaps.
 *
 * <p>KDE 4 has one panel along the foot: the round launcher, the pager of the workspaces two by two, the task manager,
 * and the notification area with the clock at the right. GNOME 2 has two: along the top its three menus, Applications,
 * Places and System, its launchers and its notification area, the clock and the way out; along the foot the button
 * that shows the desktop, the window list, the switcher of the workspaces in a row and the trash.
 */
public final class TransitionPanelLayout {

    /** How tall every one of these panels is. */
    public static final int BAR_H = 24;

    /** KDE 4's round launcher, the pager's cells, two across and two down, and where the task manager starts. */
    public static final int ORB_X = 3;
    public static final int ORB = 20;
    public static final int PAGER_X = ORB_X + ORB + 4;
    public static final int PAGER_CELL_W = 14;
    public static final int PAGER_CELL_H = 9;
    public static final int PAGER_COLUMNS = 2;
    public static final int KDE4_TASKS_X = PAGER_X + PAGER_COLUMNS * (PAGER_CELL_W + 1) + 5;

    /** GNOME 2's menus along the top: the room either side of a menu's words, and the foot before Applications. */
    public static final int MENU_PAD = 5;
    public static final int FOOT = 12;
    /** A launcher on GNOME 2's top panel. */
    public static final int LAUNCHER = 20;

    /** GNOME 2's foot: the show-desktop button, the window list after it, the workspaces in a row, the trash. */
    public static final int SHOW_DESKTOP_X = 2;
    public static final int SHOW_DESKTOP_W = 18;
    public static final int GNOME2_TASKS_X = SHOW_DESKTOP_X + SHOW_DESKTOP_W + 4;
    public static final int SWITCHER_CELL_W = 16;
    public static final int TRASH_W = 18;

    private TransitionPanelLayout() {
    }

    /** The left edge of the pager's cell {@code index}, from nought, read across and then down. */
    public static int pagerCellX(final int index) {
        return PAGER_X + (index % PAGER_COLUMNS) * (PAGER_CELL_W + 1);
    }

    /** The top of the pager's cell {@code index}, from the panel's top. */
    public static int pagerCellY(final int index) {
        final int rows = (WorkspaceSet.COUNT + PAGER_COLUMNS - 1) / PAGER_COLUMNS;
        final int top = (BAR_H - rows * PAGER_CELL_H - (rows - 1)) / 2;
        return top + (index / PAGER_COLUMNS) * (PAGER_CELL_H + 1);
    }

    /**
     * Where GNOME 2's three menus stand along the top, each its left edge and its width, from the words each one
     * carries at {@code applicationsW}, {@code placesW} and {@code systemW} pixels: Applications with its foot first.
     */
    public static int[][] menus(final int applicationsW, final int placesW, final int systemW) {
        final int appsX = 2;
        final int appsW = MENU_PAD + FOOT + 4 + applicationsW + MENU_PAD;
        final int placesX = appsX + appsW;
        final int placesW2 = MENU_PAD + placesW + MENU_PAD;
        final int systemX = placesX + placesW2;
        final int systemW2 = MENU_PAD + systemW + MENU_PAD;
        return new int[][] {{appsX, appsW}, {placesX, placesW2}, {systemX, systemW2}};
    }

    /** The left edge of GNOME 2's launcher {@code index} on the top panel, after its menus end at {@code menusEnd}. */
    public static int launcherX(final int menusEnd, final int index) {
        return menusEnd + 6 + index * LAUNCHER;
    }

    /** The left edge of the switcher's cell {@code index} on GNOME 2's foot, {@code width} wide. */
    public static int switcherCellX(final int width, final int index) {
        return switcherX(width) + index * (SWITCHER_CELL_W + 1);
    }

    /** Where the switcher of the workspaces starts on GNOME 2's foot. */
    public static int switcherX(final int width) {
        return trashX(width) - 6 - WorkspaceSet.COUNT * (SWITCHER_CELL_W + 1);
    }

    /** Where the trash stands at the right end of GNOME 2's foot. */
    public static int trashX(final int width) {
        return width - 2 - TRASH_W;
    }

    /** The right end of GNOME 2's window list, short of the switcher. */
    public static int gnome2TasksRight(final int width) {
        return switcherX(width) - 6;
    }

    /** KDE 4's panel as solid boxes, the task manager running up to {@code tasksRight}. */
    public static GuiLayout kde4(final int width, final int tasksRight) {
        final GuiLayout l = new GuiLayout(width, BAR_H);
        l.box("orb", ORB_X, (BAR_H - ORB) / 2, ORB, ORB);
        for (int i = 0; i < WorkspaceSet.COUNT; i++) {
            l.box("pager" + i, pagerCellX(i), pagerCellY(i), PAGER_CELL_W, PAGER_CELL_H);
        }
        l.box("tasks", KDE4_TASKS_X, 2, Math.max(1, tasksRight - KDE4_TASKS_X), BAR_H - 4);
        return l;
    }

    /** GNOME 2's foot as solid boxes. */
    public static GuiLayout gnome2Foot(final int width) {
        final GuiLayout l = new GuiLayout(width, BAR_H);
        l.box("show-desktop", SHOW_DESKTOP_X, 3, SHOW_DESKTOP_W, BAR_H - 6);
        l.box("tasks", GNOME2_TASKS_X, 2, Math.max(1, gnome2TasksRight(width) - GNOME2_TASKS_X), BAR_H - 4);
        for (int i = 0; i < WorkspaceSet.COUNT; i++) {
            l.box("workspace" + i, switcherCellX(width, i), 4, SWITCHER_CELL_W, BAR_H - 8);
        }
        l.box("trash", trashX(width), 3, TRASH_W, BAR_H - 6);
        return l;
    }

    /** GNOME 2's top as solid boxes, its menus at the widths of their words and {@code launchers} launchers. */
    public static GuiLayout gnome2Top(final int width, final int applicationsW, final int placesW, final int systemW,
                                      final int launchers, final int statusW) {
        final GuiLayout l = new GuiLayout(width, BAR_H);
        final int[][] menus = menus(applicationsW, placesW, systemW);
        final String[] names = {"applications", "places", "system"};
        for (int i = 0; i < menus.length; i++) {
            l.box(names[i], menus[i][0], 2, menus[i][1], BAR_H - 4);
        }
        final int end = menus[2][0] + menus[2][1];
        for (int i = 0; i < launchers; i++) {
            l.box("launcher" + i, launcherX(end, i), 2, LAUNCHER, BAR_H - 4);
        }
        l.box("status", width - statusW - 2, 2, statusW, BAR_H - 4);
        return l;
    }
}
