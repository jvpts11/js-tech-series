/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui.layout;

/**
 * Where the map of a notification's place puts its parts, on a screen of any size: the notification's title over a
 * line, the map below it, and a footer saying where the place is and how far, with Done. Pure, so a unit test checks
 * that nothing overlaps on the smallest screen.
 */
public final class NotificationPlaceLayout {

    /** The room at the screen's sides. */
    public static final int PAD = NotificationHistoryLayout.PAD;
    /** The title's line, and the line under it. */
    public static final int TITLE_Y = NotificationHistoryLayout.TITLE_Y;
    public static final int HEADER = NotificationHistoryLayout.HEADER;
    /** The footer, from its line to the screen's foot. */
    public static final int FOOTER = NotificationHistoryLayout.FOOTER;
    /** Done's size. */
    public static final int DONE_WIDTH = NotificationHistoryLayout.DONE_WIDTH;
    public static final int CONTROL = NotificationHistoryLayout.CONTROL;

    private NotificationPlaceLayout() {
    }

    /** The map on a screen {@code width} by {@code height}. */
    public static NotificationHistoryLayout.Rect map(final int width, final int height) {
        final int top = HEADER + 6;
        return new NotificationHistoryLayout.Rect(PAD, top, width - 2 * PAD, height - FOOTER - 6 - top);
    }

    /** Done. */
    public static NotificationHistoryLayout.Rect done(final int width, final int height) {
        return new NotificationHistoryLayout.Rect(width - PAD - DONE_WIDTH, height - FOOTER + 6, DONE_WIDTH, CONTROL);
    }

    /** Where the line saying where the place is starts, and how wide it may be. */
    public static NotificationHistoryLayout.Rect where(final int width, final int height) {
        return new NotificationHistoryLayout.Rect(PAD, height - FOOTER + 12, width - 3 * PAD - DONE_WIDTH, 9);
    }

    /** The whole screen, as the overlap check reads it. */
    public static GuiLayout layout(final int width, final int height) {
        final GuiLayout layout = new GuiLayout(width, height);
        final NotificationHistoryLayout.Rect map = map(width, height);
        final NotificationHistoryLayout.Rect done = done(width, height);
        final NotificationHistoryLayout.Rect where = where(width, height);
        layout.text("title", PAD, TITLE_Y, 20, 1f);
        layout.box("map", map.x(), map.y(), map.w(), map.h());
        layout.box("done", done.x(), done.y(), done.w(), done.h());
        layout.textBox("where", where.x(), where.y(), where.w(), where.h());
        return layout;
    }
}
