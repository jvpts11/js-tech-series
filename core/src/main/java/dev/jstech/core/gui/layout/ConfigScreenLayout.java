/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui.layout;

import dev.jstech.core.guide.TextTable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

/**
 * Where everything on the Core's settings screen sits, a menu over the whole game. Across the top, a tab for each kind
 * of file the mod keeps (the world's, the player's, every game's), centred, and the header's line under them; under it
 * a banner when there is something to say (an operator changing a server's settings, a world's settings only read here, a
 * rule broken). Down the left, the open tab's sections; on the right, a row with the search, the filter of what changed
 * and the presets, then the settings, grouped, each with its name on the left and its control at the right end. The
 * footer holds Defaults, Undo and Redo on the left, how many changes wait in the middle, and Export, Import and Done on
 * the right; on a screen too narrow for that in one row, how many wait goes on a row of its own above the buttons.
 *
 * <p>Every x and y is from the top left of the screen. The screen draws from these, and a test lays it out at the
 * smallest size the game allows and checks nothing collides.
 */
public final class ConfigScreenLayout {

    /** The smallest screen the game lays out, in GUI pixels. */
    public static final int SMALLEST_WIDTH = 320;
    public static final int SMALLEST_HEIGHT = 240;
    /** The margin along every edge. */
    public static final int PAD = 8;
    /** The tabs, as tall as the header, and the line under them. */
    public static final int HEADER = 24;
    public static final int TAB_WIDTH = 100;
    public static final int TAB_GAP = 4;
    public static final int SEPARATOR = 2;
    /** The banner under the header, when there is something to say. */
    public static final int BANNER_Y = HEADER + 7;
    public static final int BANNER_HEIGHT = 10;
    /** A control of the game's own height, and the gap between two side by side. */
    public static final int CONTROL_HEIGHT = 20;
    public static final int GAP = 4;
    /** A section of the rail. */
    public static final int RAIL_ITEM = 20;
    /** A heading of the rail, the series' or the other mods', and the size its words are drawn at. */
    public static final int RAIL_HEADING = 12;
    public static final float HEADING_SCALE = 0.75F;
    /** The rail at its narrowest and widest, and its share of the screen between. */
    public static final int NARROW_RAIL = 88;
    public static final int WIDE_RAIL = 140;
    public static final int RAIL_SHARE_PERCENT = 26;
    /** Room at a section's right for how many settings it holds, and the mark of one changed. */
    public static final int RAIL_COUNT = 30;
    public static final int RAIL_INSET = 7;
    /** The rows of the settings: a group's title, a setting, one with a line under its name, an entry of a list. */
    public static final int GROUP_ROW = 14;
    public static final int ROW = 24;
    public static final int NOTED_ROW = 28;
    public static final int ENTRY_ROW = 22;
    public static final int GROUP_GAP = 4;
    /** How far a row's text sits in from the list's edge, and the list's scroll bar. */
    public static final int ROW_INSET = 6;
    public static final int SCROLLBAR = 6;
    /** A control at its narrowest and widest, and its share of the settings' width between. */
    public static final int NARROWEST_CONTROL = 90;
    public static final int WIDEST_CONTROL = 150;
    public static final int CONTROL_SHARE_PERCENT = 45;
    /** The button that puts a setting back to its default, left of its control. */
    public static final int RESET = 20;
    /** The field beside a slider that takes the number typed. */
    public static final int NUMBER_FIELD = 42;
    /** The footer in one row, and in two when how many changes wait does not fit beside the buttons. */
    public static final int FOOTER = 30;
    public static final int TALL_FOOTER = 42;
    /** A footer button's room around its label, and the narrowest it is. */
    public static final int BUTTON_PADDING = 12;
    public static final int NARROWEST_BUTTON = 38;
    /** The search field at its narrowest; under it, the filter of what changed shows its box alone. */
    public static final int NARROWEST_SEARCH = 70;
    /** The check box of the filter, as the game draws its own. */
    public static final int CHECKBOX = 20;
    /** How tall a line of text is. */
    public static final int LINE = 9;
    /**
     * The most lines a setting's name or a section's takes, on a screen too narrow for it in one; what still does not
     * fit is cut short on the last.
     */
    public static final int NAME_LINES = 2;

    private ConfigScreenLayout() {
    }

    /** How wide each of {@code count} tabs is: the game's own width, narrower when they would not fit. */
    public static int tabWidth(final int width, final int count) {
        final int room = width - 2 * PAD - (count - 1) * TAB_GAP;
        return Math.max(1, Math.min(TAB_WIDTH, room / Math.max(1, count)));
    }

    /** Where the tab {@code index} of {@code count} starts, the tabs centred across the top. */
    public static int tabX(final int width, final int index, final int count) {
        final int each = tabWidth(width, count);
        final int all = count * each + (count - 1) * TAB_GAP;
        return (width - all) / 2 + index * (each + TAB_GAP);
    }

    /** The top of the rail and of the row over the settings: under the banner when there is one. */
    public static int contentTop(final boolean banner) {
        return banner ? BANNER_Y + BANNER_HEIGHT + 3 : HEADER + SEPARATOR + 4;
    }

    /** How wide the rail of sections is. */
    public static int railWidth(final int width) {
        return Math.max(NARROW_RAIL, Math.min(WIDE_RAIL, width * RAIL_SHARE_PERCENT / 100));
    }

    /** How wide a section's name may be on the rail, beside how many settings it holds. */
    public static int railLabelWidth(final int width) {
        return railWidth(width) - 2 * RAIL_INSET - RAIL_COUNT;
    }

    /** The top of the rail's sections. */
    public static int railTop(final boolean banner) {
        return contentTop(banner) + 2;
    }

    /** Where the right side, the settings and the row over them, starts and ends. */
    public static int paneX(final int width) {
        return PAD + railWidth(width) + PAD;
    }

    public static int paneRight(final int width) {
        return width - PAD;
    }

    /** The top of the list of settings, under the row of the search. */
    public static int listTop(final boolean banner) {
        return contentTop(banner) + CONTROL_HEIGHT + GAP;
    }

    /** How wide a setting's control is. */
    public static int controlWidth(final int width) {
        final int pane = paneRight(width) - paneX(width);
        return Math.max(NARROWEST_CONTROL, Math.min(WIDEST_CONTROL, pane * CONTROL_SHARE_PERCENT / 100));
    }

    /** Where a setting's control ends, clear of the list's scroll bar, and where it starts. */
    public static int controlRight(final int width) {
        return paneRight(width) - SCROLLBAR - ROW_INSET;
    }

    public static int controlX(final int width) {
        return controlRight(width) - controlWidth(width);
    }

    /** Where a setting's name starts. */
    public static int nameX(final int width) {
        return paneX(width) + ROW_INSET;
    }

    /** How wide a setting's name and the marks after it may be, beside its control and, when shown, its reset. */
    public static int nameWidth(final int width, final boolean reset) {
        return controlX(width) - GAP - (reset ? RESET + GAP : 0) - nameX(width);
    }

    /** Where the button that puts a setting back to its default sits. */
    public static int resetX(final int width) {
        return controlX(width) - GAP - RESET;
    }

    /** How tall a setting's row is, with a line under its name or without. */
    public static int rowHeight(final boolean noted) {
        return noted ? NOTED_ROW : ROW;
    }

    /** How tall a setting's row is, its name in {@code nameLines} lines: a line taller for each past the first. */
    public static int rowHeight(final boolean noted, final int nameLines) {
        return rowHeight(noted) + (Math.max(1, Math.min(NAME_LINES, nameLines)) - 1) * LINE;
    }

    /**
     * How tall a setting's row is, its name in {@code nameLines} lines {@code line} pixels apart: a text larger than
     * the normal makes each line, the line under the name and so the row taller by as much as each line grew.
     */
    public static int rowHeight(final boolean noted, final int nameLines, final int line) {
        final int lines = Math.max(1, Math.min(NAME_LINES, nameLines)) + (noted ? 1 : 0);
        return rowHeight(noted, nameLines) + lines * Math.max(0, line - LINE);
    }

    /**
     * {@code name} broken into the lines it takes in {@code room}, between words, at most {@link #NAME_LINES}; the
     * last line holds whatever is left, for the screen to cut short.
     */
    public static List<String> nameLines(final String name, final int room, final ToIntFunction<String> measure) {
        final List<String> lines = TextTable.wrap(name, room, measure);
        if (lines.size() <= NAME_LINES) {
            return lines.isEmpty() ? List.of(name) : lines;
        }
        final List<String> out = new ArrayList<>(lines.subList(0, NAME_LINES - 1));
        out.add(String.join(" ", lines.subList(NAME_LINES - 1, lines.size())));
        return out;
    }

    /** Where a control sits down a row of {@code height}. */
    public static int controlY(final int rowY, final int height) {
        return rowY + (height - CONTROL_HEIGHT) / 2;
    }

    /**
     * The row over the settings: the search, the filter of what changed and the presets, from left to right. The
     * filter's label is left out, its box alone, when the search would be narrower than {@link #NARROWEST_SEARCH}.
     *
     * @param width        the screen's width
     * @param filterLabel  how wide the filter's label is
     * @param presetsLabel how wide the presets button's label is
     */
    public static TopRow topRow(final int width, final int filterLabel, final int presetsLabel) {
        final int left = paneX(width);
        final int right = paneRight(width);
        final int presets = Math.max(NARROWEST_BUTTON, presetsLabel + BUTTON_PADDING);
        final int withLabel = CHECKBOX + GAP + filterLabel;
        final boolean labelled = right - left - presets - withLabel - 2 * PAD >= NARROWEST_SEARCH;
        final int filter = labelled ? withLabel : CHECKBOX;
        final int search = right - left - presets - filter - 2 * PAD;
        return new TopRow(new Strip(left, search), new Strip(left + search + PAD, filter), labelled,
                new Strip(right - presets, presets));
    }

    /**
     * The footer: its buttons, three at each end, each as wide as its label needs, and how many changes wait, between
     * them when it fits and on a row of its own above them when it does not.
     *
     * @param width       the screen's width
     * @param height      the screen's height
     * @param leftLabels  how wide the labels of Defaults, Undo and Redo are
     * @param rightLabels how wide the labels of Export, Import and Done are
     * @param statusWidth how wide the line saying how many changes wait is
     */
    public static Footer footer(final int width, final int height, final List<Integer> leftLabels,
                                final List<Integer> rightLabels, final int statusWidth) {
        final List<Strip> left = new ArrayList<>();
        int x = PAD;
        for (final int label : leftLabels) {
            final int w = Math.max(NARROWEST_BUTTON, label + BUTTON_PADDING);
            left.add(new Strip(x, w));
            x += w + GAP;
        }
        final int leftEnd = x - GAP;
        final List<Strip> right = new ArrayList<>();
        int end = width - PAD;
        for (int i = rightLabels.size() - 1; i >= 0; i--) {
            final int w = Math.max(NARROWEST_BUTTON, rightLabels.get(i) + BUTTON_PADDING);
            right.add(0, new Strip(end - w, w));
            end -= w + GAP;
        }
        final int rightStart = end + GAP;
        final boolean oneRow = rightStart - leftEnd >= statusWidth + 2 * PAD;
        final int tall = oneRow ? FOOTER : TALL_FOOTER;
        final int top = height - tall;
        final int buttonsY = height - CONTROL_HEIGHT - (FOOTER - CONTROL_HEIGHT) / 2 + 1;
        final int statusX = oneRow ? (leftEnd + rightStart - statusWidth) / 2 : (width - statusWidth) / 2;
        final int statusY = oneRow ? buttonsY + (CONTROL_HEIGHT - 8) / 2 : top + SEPARATOR + 3;
        return new Footer(top, tall, buttonsY, List.copyOf(left), List.copyOf(right), statusX, statusY);
    }

    /** The bottom of the rail and of the settings: over the footer. */
    public static int contentBottom(final Footer footer) {
        return footer.top() - 2;
    }

    /**
     * The whole screen laid out, for a test: the tabs, the banner, the rail with its first sections, the row over the
     * settings, a group with a setting of each height, and the footer.
     */
    public static GuiLayout layout(final int width, final int height, final boolean banner, final int tabs,
                                   final int filterLabel, final int presetsLabel, final List<Integer> leftLabels,
                                   final List<Integer> rightLabels, final int statusWidth) {
        final GuiLayout layout = new GuiLayout(width, height);
        for (int i = 0; i < tabs; i++) {
            layout.box("tab " + i, tabX(width, i, tabs), 0, tabWidth(width, tabs), HEADER);
        }
        if (banner) {
            layout.textBox("banner", PAD, BANNER_Y, width - 2 * PAD, BANNER_HEIGHT);
        }
        final Footer footer = footer(width, height, leftLabels, rightLabels, statusWidth);
        final int bottom = contentBottom(footer);
        final int railTop = railTop(banner);
        for (int i = 0; railTop + (i + 1) * RAIL_ITEM <= bottom && i < 3; i++) {
            layout.box("section " + i, PAD, railTop + i * RAIL_ITEM, railWidth(width), RAIL_ITEM);
        }
        final TopRow row = topRow(width, filterLabel, presetsLabel);
        final int top = contentTop(banner);
        layout.box("search", row.search().x(), top, row.search().width(), CONTROL_HEIGHT);
        layout.box("filter", row.filter().x(), top, row.filter().width(), CONTROL_HEIGHT);
        layout.box("presets", row.presets().x(), top, row.presets().width(), CONTROL_HEIGHT);
        int y = listTop(banner) + GROUP_GAP;
        layout.textBox("group", nameX(width), y, controlRight(width) - nameX(width), GROUP_ROW);
        y += GROUP_ROW;
        for (final boolean noted : List.of(false, true)) {
            final int h = rowHeight(noted);
            if (y + h > bottom) {
                break;
            }
            layout.textBox("name " + y, nameX(width), y, nameWidth(width, true), h);
            layout.box("reset " + y, resetX(width), controlY(y, h), RESET, CONTROL_HEIGHT);
            layout.box("control " + y, controlX(width), controlY(y, h), controlWidth(width), CONTROL_HEIGHT);
            y += h;
        }
        for (int i = 0; i < footer.left().size(); i++) {
            layout.box("left" + i, footer.left().get(i).x(), footer.buttonsY(), footer.left().get(i).width(),
                    CONTROL_HEIGHT);
        }
        for (int i = 0; i < footer.right().size(); i++) {
            layout.box("right" + i, footer.right().get(i).x(), footer.buttonsY(), footer.right().get(i).width(),
                    CONTROL_HEIGHT);
        }
        layout.box("status", footer.statusX(), footer.statusY(), statusWidth, LINE - 1);
        return layout;
    }

    /**
     * A stretch across the screen.
     *
     * @param x     where it starts
     * @param width how wide it is
     */
    public record Strip(int x, int width) {

        /** The first x past its right end. */
        public int right() {
            return x + width;
        }
    }

    /**
     * The row over the settings.
     *
     * @param search   the search field
     * @param filter   the filter of what changed, its box and, when shown, its label
     * @param labelled whether the filter's label is shown
     * @param presets  the presets button
     */
    public record TopRow(Strip search, Strip filter, boolean labelled, Strip presets) {
    }

    /**
     * The footer.
     *
     * @param top      the line over it
     * @param height   how tall it is
     * @param buttonsY the top of its buttons
     * @param left     Defaults, Undo and Redo
     * @param right    Export, Import and Done
     * @param statusX  where the line saying how many changes wait starts
     * @param statusY  where it sits
     */
    public record Footer(int top, int height, int buttonsY, List<Strip> left, List<Strip> right, int statusX,
                         int statusY) {

        public Footer {
            left = List.copyOf(left);
            right = List.copyOf(right);
        }
    }
}
