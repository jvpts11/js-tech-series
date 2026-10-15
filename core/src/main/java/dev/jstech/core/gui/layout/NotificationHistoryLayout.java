/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui.layout;

import java.util.ArrayList;
import java.util.List;

/**
 * Where the history of notifications puts its parts, on a screen of any size: the title over a line, the filters by
 * level on the left and the choices of mod and of place on the right (under the filters when the screen is too narrow
 * for both), the list of notifications, and a footer with "Do not disturb", a hint and Done. Each row of the list has
 * its icon, its title and what it says, its mod, its time and a cross that takes it off. Pure, so a unit test checks
 * that nothing overlaps on the smallest screen and the largest labels.
 */
public final class NotificationHistoryLayout {

    /** The room at the screen's sides. */
    public static final int PAD = 8;
    /** The title's line, and the line under it. */
    public static final int TITLE_Y = 8;
    public static final int HEADER = 22;
    /** A button's height, and the room a button keeps beside its label. */
    public static final int CONTROL = 20;
    public static final int BUTTON_PAD = 16;
    /** The room between two buttons, or two rows of buttons. */
    public static final int GAP = 4;
    /** A row of the list. */
    public static final int ROW = 29;
    /** The footer, from its line to the screen's foot. */
    public static final int FOOTER = 32;
    /** Done's width. */
    public static final int DONE_WIDTH = 80;
    /** A row's parts: its icon, its text, the cross, how wide its time and its mod's column are. */
    public static final int ICON_X = 8;
    public static final int TEXT_X = 32;
    public static final int CROSS = 14;
    public static final int TIME = 30;
    public static final int MOD = 110;
    /** A check box's square, and the room between it and its label. */
    public static final int BOX = 10;

    private NotificationHistoryLayout() {
    }

    /**
     * Where everything goes on a screen {@code width} by {@code height}, the labels of the filters, of the choice of
     * mod, of the choice of place and of the check box as wide as given.
     */
    public static Plan plan(final int width, final int height, final List<Integer> filterLabels, final int modsLabel,
                            final int placeLabel, final int checkboxLabel) {
        final int buttonsY = HEADER + 6;
        final List<Rect> filters = new ArrayList<>(filterLabels.size());
        int x = PAD;
        for (final int label : filterLabels) {
            final int w = label + BUTTON_PAD;
            filters.add(new Rect(x, buttonsY, w, CONTROL));
            x += w + GAP;
        }
        final int placeW = placeLabel + BUTTON_PAD;
        final int modsW = modsLabel + BUTTON_PAD;
        final int choicesWidth = modsW + GAP + placeW;
        // The choices sit on the filters' row while they leave it room, and under it otherwise.
        final boolean oneRow = x + choicesWidth <= width - PAD;
        final int choicesY = oneRow ? buttonsY : buttonsY + CONTROL + GAP;
        final Rect place = new Rect(width - PAD - placeW, choicesY, placeW, CONTROL);
        final Rect mods = new Rect(place.x() - GAP - modsW, choicesY, modsW, CONTROL);
        final int listTop = choicesY + CONTROL + 6;
        final int footerTop = height - FOOTER;
        final Rect list = new Rect(PAD, listTop, width - 2 * PAD, Math.max(ROW, footerTop - 2 - listTop));
        final Rect checkbox = new Rect(PAD, footerTop + 6, BOX + GAP + checkboxLabel, CONTROL);
        final Rect done = new Rect(width - PAD - DONE_WIDTH, footerTop + 6, DONE_WIDTH, CONTROL);
        final int hintX = checkbox.x() + checkbox.w() + 2 * GAP;
        return new Plan(List.copyOf(filters), mods, place, list, footerTop, checkbox, done, hintX,
                Math.max(0, done.x() - 2 * GAP - hintX));
    }

    /** The parts of a row of the list whose rectangle starts at {@code x} and is {@code w} wide. */
    public static Row row(final int x, final int w) {
        final int crossX = x + w - PAD - CROSS;
        final int timeRight = crossX - 6;
        final int modX = timeRight - TIME - 6 - MOD;
        final int textX = x + TEXT_X;
        return new Row(x + ICON_X, textX, Math.max(0, modX - 6 - textX), modX, timeRight, crossX);
    }

    /** The whole screen, as the overlap check reads it. */
    public static GuiLayout layout(final Plan plan, final int width, final int height) {
        final GuiLayout layout = new GuiLayout(width, height);
        layout.text("title", width / 2 - 40, TITLE_Y, 13, 1f);
        for (int i = 0; i < plan.filters().size(); i++) {
            final Rect filter = plan.filters().get(i);
            layout.box("filter" + i, filter.x(), filter.y(), filter.w(), filter.h());
        }
        layout.box("mods", plan.mods().x(), plan.mods().y(), plan.mods().w(), plan.mods().h());
        layout.box("place", plan.place().x(), plan.place().y(), plan.place().w(), plan.place().h());
        layout.box("list", plan.list().x(), plan.list().y(), plan.list().w(), plan.list().h());
        layout.box("checkbox", plan.checkbox().x(), plan.checkbox().y(), plan.checkbox().w(), plan.checkbox().h());
        layout.box("done", plan.done().x(), plan.done().y(), plan.done().w(), plan.done().h());
        final Row row = row(plan.list().x(), plan.list().w());
        layout.textBox("rowtext", row.textX(), plan.list().y() + 3, row.textRoom(), 9);
        layout.textBox("rowmod", row.modX(), plan.list().y() + 14, MOD, 9);
        return layout;
    }

    /**
     * Where everything of the screen goes.
     *
     * @param filters   the filters by level, left to right
     * @param mods      the choice of mod
     * @param place     the choice of place
     * @param list      the list of notifications
     * @param footerTop the footer's line
     * @param checkbox  "Do not disturb"
     * @param done      Done
     * @param hintX     where the hint starts
     * @param hintRoom  how wide it may be; nothing shows it when there is no room
     */
    public record Plan(List<Rect> filters, Rect mods, Rect place, Rect list, int footerTop, Rect checkbox, Rect done,
                       int hintX, int hintRoom) {

        public Plan {
            filters = List.copyOf(filters);
        }
    }

    /**
     * A rectangle.
     *
     * @param x its left
     * @param y its top
     * @param w its width
     * @param h its height
     */
    public record Rect(int x, int y, int w, int h) {

        /** Whether the point is inside it. */
        public boolean contains(final double px, final double py) {
            return px >= this.x && px < this.x + this.w && py >= this.y && py < this.y + this.h;
        }
    }

    /**
     * The parts of a row.
     *
     * @param iconX     its icon's left
     * @param textX     the left of its title and what it says
     * @param textRoom  how wide those may be
     * @param modX      its mod's left
     * @param timeRight where its time ends
     * @param crossX    the cross's left
     */
    public record Row(int iconX, int textX, int textRoom, int modX, int timeRight, int crossX) {
    }
}
