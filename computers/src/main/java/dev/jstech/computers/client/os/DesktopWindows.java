/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.os.WorkspaceSet;
import dev.jstech.core.motion.Motion;
import dev.jstech.core.motion.MotionKinds;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

/**
 * The desktop's window manager: the windows, back to front, and the workspace that is up. A window belongs to the
 * program whose key it goes by, and a dialog to the window that asked it, so the two come forward, go away and close
 * together. Only the windows on the workspace that is up are drawn and answer the pointer; the rest stay exactly as
 * they were left.
 */
final class DesktopWindows {

    private final DesktopState desktop;
    /** The windows, back to front. */
    private final List<DesktopWindow> windows = new ArrayList<>();
    /** The windows closed and still being drawn going away, oldest first. */
    private final List<DesktopWindow> closing = new ArrayList<>();
    /** Which workspace is up, counted from nought; always the first on a desktop that has only one. */
    private int shown;
    /** How many windows each program has opened since the desktop came up, which is what "Most used" ranks by. */
    private final Map<String, Integer> opened = new HashMap<>();
    /**
     * Whether the player clicked the bare desktop since a window last came forward: the windows keep their order, but
     * none of them has the focus, so none takes the keyboard or wears the focused look.
     */
    private boolean onDesktop;

    DesktopWindows(final DesktopState desktop) {
        this.desktop = desktop;
    }

    /** The windows, back to front. */
    List<DesktopWindow> all() {
        return windows;
    }

    /** Which workspace is up, counted from nought. */
    int workspace() {
        return shown;
    }

    /** Puts another workspace up; only what belongs there is drawn and answers the pointer from then on. */
    void setWorkspace(final int workspace) {
        shown = workspace;
    }

    /** Whether a window is out of sight: put away, or on a workspace that is not up. */
    boolean away(final DesktopWindow w) {
        return w.minimized() || !w.on(shown);
    }

    /** The topmost window that is on show, which receives the keyboard and the wheel; or null. */
    @Nullable
    DesktopWindow front() {
        for (int i = windows.size() - 1; i >= 0; i--) {
            if (!away(windows.get(i))) {
                return windows.get(i);
            }
        }
        return null;
    }

    /** The window with the focus, which takes the keyboard: the one in front, or none after a click on the desktop. */
    @Nullable
    DesktopWindow focused() {
        return onDesktop ? null : front();
    }

    /** Takes the focus away from every window, as a click on the bare desktop does. */
    void focusDesktop() {
        onDesktop = true;
    }

    /** Whether the front window's program has a modal dialog open, which disables everything behind it. */
    boolean focusModal() {
        final DesktopWindow f = front();
        return f != null && f.app().modalActive();
    }

    /** The windows put away on the workspace that is up, in the order they were opened, dialogs aside. */
    List<DesktopWindow> putAwayHere() {
        final List<DesktopWindow> out = new ArrayList<>();
        for (final DesktopWindow w : windows) {
            if (w.minimized() && !w.dialog() && w.owner() == null && w.on(shown)) {
                out.add(w);
            }
        }
        /*
         * By when each was opened and not by how they are stacked, since bringing one back restacks the list and the
         * icons beside it must not jump about when that happens.
         */
        out.sort(Comparator.comparingInt(DesktopWindow::serial));
        return out;
    }

    /** Every window listed under {@code key}, back to front, a program's own and its dialogs alike. */
    List<DesktopWindow> group(final String key) {
        final List<DesktopWindow> out = new ArrayList<>();
        for (final DesktopWindow w : windows) {
            if (w.groupKey().equals(key)) {
                out.add(w);
            }
        }
        return out;
    }

    /** The dialog up over {@code w}, or null: while there is one, {@code w} takes nothing itself. */
    @Nullable
    DesktopWindow dialogOf(final DesktopWindow w) {
        for (final DesktopWindow other : windows) {
            if (other.owner() == w) {
                return other;
            }
        }
        return null;
    }

    /** How many programs are open, on every workspace; a dialog is a question a program asks, not a program. */
    int openPrograms() {
        int open = 0;
        for (final DesktopWindow w : windows) {
            if (!w.dialog()) {
                open++;
            }
        }
        return open;
    }

    /**
     * Opens a program's window at its default size, clamped to the work area but never below the program's minimum
     * while there is room for it, so it opens laid out rather than collapsed on a small monitor. Each window opens a
     * little further down and across than the last, on the workspace that is up, which is where whoever started it
     * is looking.
     */
    void open(final String key, final IDesktopApp app) {
        final DesktopViewport view = desktop.view();
        final int top = view.workAreaTop();
        final int workH = view.workAreaBottom() - top;
        final int availW = view.width() - 16;
        final int availH = workH - 16;
        final int w = availW >= app.minWidth() ? Math.min(app.defaultWidth(), availW) : availW;
        final int h = availH >= app.minHeight() ? Math.min(app.defaultHeight(), availH) : availH;
        // A window that draws its own frame keeps its own size, which may be more than was clamped.
        final int shownW = app.drawsOwnFrame() ? app.defaultWidth() : w;
        final int shownH = app.drawsOwnFrame() ? app.defaultHeight() : h;
        final int x = Math.max(0,
                Math.min(Math.max(48, (view.width() - w) / 2 + windows.size() * 12), view.width() - shownW));
        final int y = Math.max(top, Math.min(Math.max(top + 6, top + (workH - h) / 2 + windows.size() * 12),
                view.workAreaBottom() - shownH));
        final DesktopWindow opened = new DesktopWindow(app, key, x, y, w, h);
        opened.setWorkspaces(WorkspaceSet.only(shown));
        opened.move(desktop.motion().start(MotionKinds.WINDOW_OPEN));
        windows.add(opened);
        onDesktop = false;
        this.opened.merge(key, 1, Integer::sum);
        // A copy's window is the system telling of a copy, not a program being loaded.
        if (!CopyWindows.KEY.equals(key)) {
            desktop.programStarting(key);
        }
    }

    /** How many windows the program under {@code key} has opened since the desktop came up. */
    int openedCount(final String key) {
        return opened.getOrDefault(key, 0);
    }

    /**
     * Opens {@code dialog} as a window over the one running {@code ownerApp}, centred across it and hung just under
     * its title bar, so the owner's name and edges stay in view around the question it is asking. The owner comes
     * forward first, so the pair reads as one program that just asked something.
     */
    void openDialog(final IDesktopApp ownerApp, final IDesktopApp dialog) {
        DesktopWindow ownerWin = null;
        for (final DesktopWindow w : windows) {
            if (w.app() == ownerApp) {
                ownerWin = w;
            } else if (w.app() == dialog) {
                focus(w);
                return;
            }
        }
        if (ownerWin == null) {
            return;
        }
        final DesktopViewport view = desktop.view();
        final int top = view.workAreaTop();
        final int w = Math.max(dialog.minWidth(), Math.min(dialog.defaultWidth(), view.width() - 8));
        final int h = Math.max(dialog.minHeight(), Math.min(dialog.defaultHeight(), view.workAreaBottom() - top - 8));
        final int x = Math.max(0, Math.min(ownerWin.x() + (ownerWin.width() - w) / 2, view.width() - w));
        final int y = Math.max(top, Math.min(ownerWin.y() + DesktopWindow.TITLE_H + 6, view.workAreaBottom() - h));
        dialog.applySkin(desktop.prefs().skin());
        final DesktopWindow made = new DesktopWindow(dialog, ownerWin.appKey(), x, y, w, h);
        made.setOwner(ownerWin);
        made.setWorkspaces(ownerWin.workspaces());
        made.move(desktop.motion().start(MotionKinds.DIALOG_OPEN));
        ownerWin.setMinimized(false);
        bringToFront(ownerWin);
        windows.add(made);
    }

    /** Puts {@code w} in front, and its dialogs in front of it, in the order they were opened. */
    void bringToFront(final DesktopWindow w) {
        if (!windows.remove(w)) {
            return;
        }
        windows.add(w);
        onDesktop = false;
        final List<DesktopWindow> dialogs = new ArrayList<>();
        for (final DesktopWindow other : windows) {
            if (other.owner() == w) {
                dialogs.add(other);
            }
        }
        for (final DesktopWindow dialog : dialogs) {
            bringToFront(dialog);
        }
    }

    /** Puts the window at {@code index} in the stack in front, when there is one there. */
    void bringToFront(final int index) {
        if (index >= 0 && index < windows.size()) {
            bringToFront(windows.get(index));
        }
    }

    /** Brings {@code w} up and forward: a dialog comes with the window it belongs to. */
    void focus(final DesktopWindow w) {
        final DesktopWindow root = w.owner() != null ? w.owner() : w;
        // A window asked for by name from another workspace takes the desktop there, as CDE did.
        if (!root.on(shown)) {
            shown = WorkspaceSet.first(root.workspaces());
        }
        for (final DesktopWindow other : group(root.groupKey())) {
            if (other == root || other.owner() == root) {
                other.setMinimized(false);
            }
        }
        bringToFront(root);
        if (w != root) {
            bringToFront(w);
        }
    }

    /** Sends a window behind every other, its dialogs with it and still in front of it. */
    void lower(final DesktopWindow w) {
        final List<DesktopWindow> sent = new ArrayList<>();
        for (final DesktopWindow other : windows) {
            if (other == w || other.owner() == w) {
                sent.add(other);
            }
        }
        windows.removeAll(sent);
        sent.remove(w);
        windows.addAll(0, sent);
        windows.add(0, w);
    }

    /**
     * Says which workspaces a window is on, its dialogs with it. One taken off the workspace that is up simply leaves
     * it, as it did on CDE, and is found again on any workspace it is still on.
     */
    void occupy(final DesktopWindow w, final int workspaces) {
        for (final DesktopWindow other : windows) {
            if (other == w || other.owner() == w) {
                other.setWorkspaces(workspaces);
            }
        }
    }

    void bringGroupToFront(final String key) {
        for (final DesktopWindow w : group(key)) {
            if (!w.dialog()) {
                bringToFront(w);
            }
        }
    }

    void restoreGroup(final String key) {
        for (final DesktopWindow w : group(key)) {
            w.setMinimized(false);
        }
        bringGroupToFront(key);
    }

    void minimizeGroup(final String key) {
        for (final DesktopWindow w : group(key)) {
            w.setMinimized(true);
        }
    }

    void minimizeOthers(final String key) {
        for (final DesktopWindow w : windows) {
            w.setMinimized(!w.groupKey().equals(key));
        }
        bringGroupToFront(key);
    }

    /** Closes every window of a program, front first. */
    void closeGroup(final String key) {
        final List<DesktopWindow> mine = group(key);
        for (int i = mine.size() - 1; i >= 0; i--) {
            if (windows.contains(mine.get(i))) {
                close(mine.get(i));
            }
        }
    }

    /**
     * Ends {@code w}: its dialogs go first, since a window put away takes its questions with it. On a desktop where
     * closing moves, the window leaves the stack at once, so nothing reaches it any more, and is drawn going away
     * for as long as that takes; its program is told it closed when it is gone from the glass.
     */
    void close(final DesktopWindow w) {
        for (final DesktopWindow other : new ArrayList<>(windows)) {
            if (other.owner() == w) {
                close(other);
            }
        }
        if (!windows.remove(w)) {
            return;
        }
        // A window that has not been drawn yet has nothing on the glass to watch go.
        final Motion going = w.drawn() && !away(w) ? desktop.motion().start(MotionKinds.WINDOW_CLOSE)
                : Motion.FINISHED;
        if (going.done(DesktopMotion.now())) {
            w.app().onClosed();
            return;
        }
        w.move(going);
        closing.add(w);
    }

    /** The windows still going away, oldest first: gone from the stack, still on the glass. */
    List<DesktopWindow> closing() {
        return closing;
    }

    /** Tells each window that has finished going away by {@code nowMs} that it closed, and lets it go. */
    void settleClosing(final double nowMs) {
        if (closing.isEmpty()) {
            return;
        }
        for (final DesktopWindow w : new ArrayList<>(closing)) {
            if (w.motion().done(nowMs)) {
                closing.remove(w);
                w.app().onClosed();
            }
        }
    }

    /** Ends the newest window of the program {@code key}, dialogs aside, when it has one. */
    void closeNewest(final String key) {
        for (int i = windows.size() - 1; i >= 0; i--) {
            final DesktopWindow w = windows.get(i);
            if (!w.dialog() && w.appKey().equals(key)) {
                close(w);
                return;
            }
        }
    }

    /** Ends the window running {@code app}, when there is one. */
    void closeOf(final IDesktopApp app) {
        for (final DesktopWindow w : new ArrayList<>(windows)) {
            if (w.app() == app) {
                close(w);
            }
        }
    }

    /** Puts every window away, which is what Show desktop on the panel's menu does. */
    void showDesktop() {
        for (final DesktopWindow w : windows) {
            w.setMinimized(true);
        }
    }

    /** Steps the open windows down and to the right from the work area's corner, the way a cascade does. */
    void cascade() {
        final DesktopViewport view = desktop.view();
        int step = 0;
        for (final DesktopWindow w : windows) {
            if (away(w)) {
                continue;
            }
            w.setMaximized(false);
            w.moveTo(16 + step * 12, view.workAreaTop() + 10 + step * 12, view.workAreaTop(), view.width(),
                    view.workAreaBottom());
            step++;
        }
    }
}
