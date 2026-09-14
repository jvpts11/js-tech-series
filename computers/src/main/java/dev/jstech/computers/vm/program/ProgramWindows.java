/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import java.util.ArrayList;
import java.util.List;

/**
 * The windows a process has open on its machine's desktop, and the numbers its windows and widgets are known by.
 *
 * <p>A window is the program's own object, so what it shows is written down and brought back with the program; this
 * is the list of the ones that are open, which is what the machine draws, in the order the program opened them.
 */
final class ProgramWindows {

    /** The most windows one program may have open at a time. */
    static final int MOST_WINDOWS = 8;

    private final List<Values.Obj> open = new ArrayList<>();
    /** The numbers the next window and the next widget get, so an event can name what it happened to. */
    private long nextWindow = 1;
    private long nextWidget = 1;
    /** Set when a person shut the last window: the program ends once it has heard about it. */
    private boolean endWithWindows;

    /** Puts a window on the desktop under the next number; one already open stays as it is. */
    void open(final Values.Obj window, final int line) {
        if (Boolean.TRUE.equals(window.get(UiWidgets.OPEN))) {
            return;
        }
        if (this.open.size() >= MOST_WINDOWS) {
            throw new Halt(Halt.Reason.OUT_OF_RANGE, line,
                    "a program may have " + MOST_WINDOWS + " windows open at once");
        }
        window.set(UiWidgets.ID, this.nextWindow++);
        window.set(UiWidgets.OPEN, Boolean.TRUE);
        this.open.add(window);
    }

    /** Takes a window off the desktop; closing one that is not open is nothing at all. */
    void close(final Values.Obj window) {
        window.set(UiWidgets.OPEN, Boolean.FALSE);
        this.open.remove(window);
    }

    /** The windows open, in the order they were opened. */
    List<Values.Obj> all() {
        return new ArrayList<>(this.open);
    }

    /** The open window of that number, or null when there is none. */
    Values.Obj of(final long id) {
        for (final Values.Obj window : this.open) {
            if (Numbers.toLong(window.get(UiWidgets.ID)) == id) {
                return window;
            }
        }
        return null;
    }

    /** Whether no window is open. */
    boolean isEmpty() {
        return this.open.isEmpty();
    }

    /** The number the next widget the program makes is known by. */
    long nextWidgetId() {
        return this.nextWidget++;
    }

    /** A person shut a window: when it was the last one, the program ends once it has heard about it. */
    void closedByPerson() {
        this.endWithWindows = this.open.isEmpty();
    }

    /**
     * Whether the program ends now, because a person shut its last window and nothing is open again; saying yes is
     * the end of it.
     */
    boolean endsNow() {
        if (this.endWithWindows && this.open.isEmpty()) {
            this.endWithWindows = false;
            return true;
        }
        return false;
    }

    /** The open windows, for the save. */
    List<Object> held() {
        return new ArrayList<>(this.open);
    }

    /** The number the next window opened gets. */
    long nextWindow() {
        return this.nextWindow;
    }

    /** The number the next widget made gets. */
    long nextWidget() {
        return this.nextWidget;
    }

    /** Puts back a window that was open when the program was saved. */
    void restoreOpen(final Values.Obj window) {
        this.open.add(window);
    }

    /** Carries on numbering from what a save wrote down; no number is ever below one. */
    void startFrom(final long nextWindow, final long nextWidget) {
        this.nextWindow = Math.max(1, nextWindow);
        this.nextWidget = Math.max(1, nextWidget);
    }
}
