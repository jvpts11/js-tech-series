/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.gui.TaskbarGroups;
import dev.jstech.computers.operation.payload.SetSettingPayload;
import dev.jstech.computers.os.PanelStyle;
import dev.jstech.core.client.gui.component.ContextMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * The programs on the panel: one entry per program, grouped from the windows on the workspace that is up and the
 * programs pinned there, and where each entry's button sits. A click on an entry brings its window up or puts it
 * down, lists its windows when it has several, or opens the program's own menu with the right button, so a program
 * can be closed or pinned without first going to it.
 */
final class TaskbarModel {

    private final DesktopState desktop;
    /** The programs pinned to the panel, by program id path, in the order the machine keeps them. */
    private final List<String> pinned = new ArrayList<>();
    /** A program's own menu, from its panel entry: the same component every menu on this desktop is. */
    private final ContextMenu menu;

    /** Where the strip of buttons starts, the gap between them, and the widths one may run between. */
    private static final int TASK_X = 64;
    private static final int TASK_GAP = 4;
    private static final int TASK_MIN_W = 44;
    private static final int TASK_MAX_W = 84;
    /** A pinned program with no window, on the panels that keep it in place as an icon (KDE, Cinnamon). */
    private static final int LAUNCHER_W = 22;
    private static final int MENU_W = 118;
    /** The patch at the middle of the screen's foot a window with no button of its own goes down to. */
    private static final int FOOT_W = 24;
    private static final int FOOT_H = 8;

    TaskbarModel(final DesktopState desktop) {
        this.desktop = desktop;
        this.menu = new ContextMenu(MENU_W, DeskMenu.ITEM_H);
    }

    /** How many characters of a title fit on a task button of {@code w} pixels, after its icon. */
    static int titleRoom(final int w) {
        return Math.max(3, (w - 24) / 6);
    }

    /** A small downward caret: a program with several windows says so at the end of its button. */
    static void drawCaret(final GuiGraphics g, final int x, final int y, final int color) {
        g.fill(x, y, x + 5, y + 1, color);
        g.fill(x + 1, y + 1, x + 4, y + 2, color);
        g.fill(x + 2, y + 2, x + 3, y + 3, color);
    }

    /** A program's own menu, open over its entry or not. */
    ContextMenu menu() {
        return menu;
    }

    /** Takes the pinned programs a listing names, which the machine keeps. */
    void takePinned(final List<String> programs) {
        pinned.clear();
        pinned.addAll(programs);
    }

    /** Whether the program with that id path is pinned to the panel. */
    boolean isPinned(final String programPath) {
        return pinned.contains(programPath);
    }

    /**
     * Whether this panel keeps pinned programs in view. The Frames 95 taskbar and the period panels never had a place
     * for them, and the GNOME top bar lists no programs at all.
     */
    boolean pinsOnPanel() {
        // KDE 4's task manager and GNOME 2's window list show what is open, nothing else.
        if (desktop.periodPanel() || desktop.transitionPanel()) {
            return false;
        }
        final PanelStyle style = desktop.panelStyle();
        return style == PanelStyle.FRAMES_11 || style == PanelStyle.FRAMES_XP || style == PanelStyle.FRAMES_7
                || style == PanelStyle.FRAMES_10 || style == PanelStyle.KDE || style == PanelStyle.CINNAMON;
    }

    /** Whether the panel's popup shows the windows' live pictures (a modern panel) rather than their titles. */
    boolean thumbnails() {
        final PanelStyle style = desktop.panelStyle();
        return !desktop.periodPanel()
                && (style == PanelStyle.FRAMES_11 || style == PanelStyle.FRAMES_7 || style == PanelStyle.FRAMES_10
                || style == PanelStyle.KDE || style == PanelStyle.CINNAMON);
    }

    /**
     * Pins a program to the panel or takes it off, and tells the machine, which keeps the list. The panel changes at
     * once rather than waiting for the machine's reply.
     */
    void togglePin(final String key) {
        final Launcher launcher = pinnable(key);
        if (launcher == null) {
            return;
        }
        final String id = launcher.programId().getPath();
        final boolean was = pinned.contains(id);
        if (was) {
            pinned.remove(id);
        } else {
            pinned.add(id);
        }
        PacketDistributor.sendToServer(new SetSettingPayload(desktop.hostPos(), was ? "unpin" : "pin", id));
    }

    /** The pinned programs by the key their windows go by; only those this desktop can start. */
    List<String> pinnedKeys() {
        final List<String> out = new ArrayList<>();
        if (!pinsOnPanel()) {
            return out;
        }
        for (final String id : pinned) {
            final Launcher launcher =
                    desktop.catalogue().find(l -> l.factory() != null && l.programId().getPath().equals(id));
            if (launcher != null) {
                out.add(launcher.key());
            }
        }
        return out;
    }

    /** The programs on the panel, in its order, grouped from the windows and the pins. */
    List<TaskbarGroups.Entry> entries() {
        final List<TaskbarGroups.Window> list = new ArrayList<>();
        for (final DesktopWindow w : desktop.wm().all()) {
            // A panel lists what is on the workspace that is up; the rest are met by going to theirs.
            if (w.on(desktop.workspace())) {
                list.add(new TaskbarGroups.Window(w.groupKey(), w.minimized(), w.serial()));
            }
        }
        final DesktopWindow front = desktop.wm().front();
        return TaskbarGroups.group(list, pinnedKeys(), front == null ? null : front.groupKey());
    }

    /** What a task button says: the window's title, or the count and the program when there are several. */
    String label(final TaskbarGroups.Entry entry) {
        if (entry.windows() > 1) {
            return entry.windows() + " " + desktop.nameOf(entry.key());
        }
        final List<DesktopWindow> mine = desktop.wm().group(entry.key());
        return mine.isEmpty() ? desktop.nameOf(entry.key()) : desktop.titleOf(mine.get(mine.size() - 1));
    }

    /**
     * The Frames 11 Start button's left edge. Centred, it is the leftmost of the centred group of Start and the open
     * programs, so the whole group is centred; otherwise it is pinned to the corner. That is why the taskbar
     * alignment setting moves the Start button.
     */
    int modernStartLeft(final int sw) {
        if (!desktop.prefs().taskbarCentered()) {
            return 4;
        }
        final int group = (entries().size() + 1) * DesktopScreen.WIN11_SLOT;
        return Math.max(4, (sw - group) / 2);
    }

    /**
     * The strip shared out between the programs, never wider than is comfortable nor narrower than a name can be
     * read in. A taskbar that keeps one fixed width simply runs out of room, which is what hid the open programs
     * behind the notification area. Frames 11 gives every program one slot, right after the Start button; Frames XP
     * keeps the pinned programs on a quick launch beside Start; KDE and Cinnamon keep a pinned program in place as an
     * icon until it opens; the older panels list only what is open.
     */
    TaskStrip strip(final int sw) {
        final List<TaskbarGroups.Entry> entries = entries();
        final int n = entries.size();
        final int[] x = new int[n];
        final int[] w = new int[n];
        // KDE 4 runs its task manager up to its notification area, GNOME 2 its window list up to its switcher.
        final boolean transition = desktop.transitionPanel();
        final int right = transition ? desktop.transitionPanels().tasksRight(sw) : desktop.tray().taskStripRight(sw);
        final PanelStyle style = desktop.panelStyle();
        if (style == PanelStyle.FRAMES_11) {
            final int appsX = modernStartLeft(sw) + DesktopScreen.WIN11_SLOT;
            for (int i = 0; i < n; i++) {
                x[i] = appsX + i * DesktopScreen.WIN11_SLOT;
                w[i] = DesktopScreen.WIN11_SLOT;
            }
            return new TaskStrip(entries, x, w, 0, 0, right);
        }
        // Frames 7 and 10 give every program one button of icon alone, pinned and open in one row after Start.
        if (style == PanelStyle.FRAMES_7 || style == PanelStyle.FRAMES_10) {
            final boolean seven = style == PanelStyle.FRAMES_7;
            final int appsX = seven ? AeroSuperbar.appsLeft() : MetroTaskbar.appsLeft();
            final int slot = seven ? AeroSuperbar.SLOT : MetroTaskbar.SLOT;
            for (int i = 0; i < n; i++) {
                x[i] = appsX + i * slot;
                w[i] = slot;
            }
            return new TaskStrip(entries, x, w, 0, 0, right);
        }
        int left = transition ? desktop.transitionPanels().tasksLeft() : TASK_X;
        int quickX = 0;
        int quickCount = 0;
        if (style == PanelStyle.FRAMES_XP) {
            quickX = DesktopScreen.XP_START_W + 4;
            for (final TaskbarGroups.Entry entry : entries) {
                if (entry.pinned()) {
                    quickCount++;
                }
            }
            left = quickCount > 0 ? quickX + quickCount * TaskStrip.QL_W + 6 : TASK_X;
        }
        final boolean launcherCells = desktop.linuxDesktop() && !desktop.periodPanel() && !transition;
        int openCount = 0;
        int launcherCount = 0;
        for (final TaskbarGroups.Entry entry : entries) {
            if (entry.open()) {
                openCount++;
            } else if (launcherCells) {
                launcherCount++;
            }
        }
        final int room = Math.max(0, right - left - launcherCount * LAUNCHER_W);
        final int btnW = openCount == 0 ? 0 : Math.max(TASK_MIN_W, Math.min(TASK_MAX_W, room / openCount - TASK_GAP));
        int cx = left;
        for (int i = 0; i < n; i++) {
            final TaskbarGroups.Entry entry = entries.get(i);
            if (entry.open()) {
                x[i] = cx;
                w[i] = btnW;
                cx += btnW + TASK_GAP;
            } else if (launcherCells) {
                x[i] = cx;
                w[i] = LAUNCHER_W;
                cx += LAUNCHER_W;
            }
        }
        return new TaskStrip(entries, x, w, quickX, quickCount, right);
    }

    /**
     * Where the panel shows the windows grouped under {@code groupKey}, desktop-local, as x, y, width and height: its
     * button, or its quick launch icon on Frames XP. A panel that lists no button for it (GNOME's top bar, CDE's
     * Front Panel) gives the middle of the screen's foot, which is where a window with no button of its own went.
     */
    int[] entryRect(final String groupKey) {
        final DesktopViewport view = desktop.view();
        final TaskStrip strip = strip(view.width());
        final int index = TaskbarGroups.indexOf(strip.entries(), groupKey);
        final int top = view.panelOnTop() ? 0 : view.height() - view.panelBand();
        final int high = Math.max(1, view.panelBand());
        if (index >= 0 && strip.w()[index] > 0) {
            return new int[] {strip.x()[index], top, strip.w()[index], high};
        }
        final int quick = index < 0 ? -1 : strip.quickIndexOf(index);
        if (quick >= 0) {
            return new int[] {strip.quickX() + quick * TaskStrip.QL_W, top, TaskStrip.QL_W, high};
        }
        return new int[] {view.width() / 2 - FOOT_W / 2, view.height() - FOOT_H, FOOT_W, FOOT_H};
    }

    /** A desktop-local x on the panel clear of Start and of the task buttons: its empty stretch, at its right end. */
    int emptyX(final int sw) {
        return Math.max(TASK_X, desktop.tray().taskStripRight(sw) - 8);
    }

    /** A click on a panel entry whose button starts at {@code atX}, on a panel whose top is {@code tbY}. */
    void click(final TaskbarGroups.Entry entry, final int atX, final int button, final int tbY) {
        if (button == 1) {
            openMenu(entry, atX, tbY);
            return;
        }
        if (button != 0) {
            return;
        }
        if (!entry.open()) {
            desktop.opener().runKeyed(entry.key());
            return;
        }
        if (entry.windows() == 1) {
            final List<DesktopWindow> mine = desktop.wm().group(entry.key());
            if (entry.state() == TaskbarGroups.State.ACTIVE) {
                desktop.wm().minimizeGroup(entry.key());
            } else if (mine.get(0).minimized()) {
                desktop.wm().restoreGroup(entry.key());
            } else {
                desktop.wm().bringGroupToFront(entry.key());
            }
            return;
        }
        // Several windows: the popup lists them, and stays until a click puts it away.
        desktop.taskPopup().openFor(entry.key());
    }

    /** Opens a program's menu over its panel entry: what can be done with its windows and its pin. */
    private void openMenu(final TaskbarGroups.Entry entry, final int atX, final int tbY) {
        final String key = entry.key();
        final List<ContextMenu.Item> items = new ArrayList<>();
        final boolean canPin = pinsOnPanel() && pinnable(key) != null;
        if (entry.open()) {
            final boolean several = entry.windows() > 1;
            final boolean minimized = entry.state() == TaskbarGroups.State.MINIMIZED;
            items.add(DeskMenu.item(several ? DesktopTexts.RESTORE_ALL
                    : minimized ? DesktopTexts.RESTORE : DesktopTexts.BRING_TO_FRONT, true,
                    () -> desktop.wm().restoreGroup(key)));
            items.add(DeskMenu.item(several ? DesktopTexts.MINIMIZE_ALL : DesktopTexts.MINIMIZE, true,
                    () -> desktop.wm().minimizeGroup(key)));
            if (!several) {
                items.add(DeskMenu.item(DesktopTexts.MAXIMIZE, true, () -> {
                    desktop.wm().restoreGroup(key);
                    final List<DesktopWindow> mine = desktop.wm().group(key);
                    if (!mine.isEmpty()) {
                        mine.get(0).setMaximized(!mine.get(0).maximized());
                    }
                }));
            }
            items.add(DeskMenu.item(DesktopTexts.MINIMIZE_OTHERS, true, () -> desktop.wm().minimizeOthers(key)));
            if (canPin) {
                items.add(ContextMenu.Item.separator());
                items.add(DeskMenu.item(entry.pinned() ? DesktopTexts.UNPIN : DesktopTexts.PIN, true,
                        () -> togglePin(key)));
            }
            items.add(ContextMenu.Item.separator());
            items.add(DeskMenu.item(several ? DesktopTexts.CLOSE_ALL_WINDOWS : DesktopTexts.CLOSE, true,
                    () -> desktop.wm().closeGroup(key)));
        } else {
            items.add(DeskMenu.item(DesktopTexts.OPEN, true, () -> desktop.opener().runKeyed(key)));
            if (canPin) {
                items.add(ContextMenu.Item.separator());
                items.add(DeskMenu.item(DesktopTexts.UNPIN, true, () -> togglePin(key)));
            }
        }
        desktop.taskPopup().dismiss();
        final DesktopViewport view = desktop.view();
        final int h = items.size() * DeskMenu.ITEM_H + 2;
        // Above a bottom panel, below a top one: the menu never covers the entry it came from.
        final int y = view.panelOnTop() ? DesktopScreen.TASKBAR_H + 2 : tbY - h - 2;
        menu.open(items, atX, y, 0, 0, view.width(), view.height());
    }

    /** The launcher a panel entry stands for, or null for a program with no launcher (the Task Manager). */
    @Nullable
    private Launcher pinnable(final String key) {
        return desktop.catalogue().find(l -> l.factory() != null && l.key().equals(key));
    }
}
