/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

import dev.jstech.computers.gui.help.HelpForm;
import dev.jstech.computers.gui.help.HelpWindowTexts;
import dev.jstech.core.gui.layout.GuiLayout;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;

/**
 * Where everything sits in a desktop's help window, form by form: the band or the toolbar along the top, the search
 * field, the tree, the page, the trail of a page's place and the buttons.
 *
 * <p>Pure, and the window and its test read the same numbers, so a part that would overlap another is caught by
 * arithmetic rather than by a player finding it.
 */
public final class HelpViewerLayout {

    /** How tall a row of the tree is, and how far a row is set in for each book it sits in. */
    public static final int ROW_H = 11;
    public static final int INDENT = 10;

    /** Frames 95's hint over its tree: where it starts, how far apart its lines are, and how many it may take. */
    public static final int HINT_Y = 20;
    public static final int HINT_PITCH = 9;
    public static final int HINT_LINES = 3;
    private static final int HINT_END = HINT_Y + HINT_LINES * HINT_PITCH + 1;

    private HelpViewerLayout() {
    }

    /** Where a part of the window sits, from its top left corner. */
    public record Box(int x, int y, int w, int h) {

        /** Whether a point inside the window falls in it. */
        public boolean holds(final double px, final double py) {
            return px >= this.x && px < this.x + this.w && py >= this.y && py < this.y + this.h;
        }
    }

    /** A button of the window: where it is and what it says. */
    public record Button(Box box, TextKey label) {
    }

    /**
     * A form's window laid out.
     *
     * @param width   how wide it opens
     * @param height  how tall it opens
     * @param band    the strip along the top (a band, a toolbar or a menu bar), or null
     * @param search  the search field, or null for a form with none in view
     * @param tree    the tree of books, or null for a form showing none
     * @param page    the page
     * @param trail   the trail of where the page is, or null
     * @param buttons the buttons, in the order the window answers them
     * @param tabs    Frames 95's three tabs, or none
     */
    public record Frame(int width, int height, Box band, Box search, Box tree, Box page, Box trail,
                        List<Button> buttons, List<Button> tabs) {

        public Frame {
            buttons = List.copyOf(buttons);
            tabs = List.copyOf(tabs);
        }
    }

    /** The window of a form. */
    public static Frame of(final HelpForm form) {
        return switch (form) {
            case FRAMES_95 -> new Frame(196, 200, null, new Box(10, HINT_END, 176, 12),
                    new Box(10, HINT_END, 176, 118), new Box(10, HINT_END + 16, 176, 102), null,
                    List.of(new Button(new Box(52, 178, 44, 14), HelpWindowTexts.DISPLAY),
                            new Button(new Box(100, 178, 44, 14), HelpWindowTexts.PRINT),
                            new Button(new Box(148, 178, 44, 14), HelpWindowTexts.CANCEL)),
                    List.of(new Button(new Box(6, 4, 48, 13), HelpWindowTexts.CONTENTS_TAB),
                            new Button(new Box(54, 4, 36, 13), HelpWindowTexts.INDEX_TAB),
                            new Button(new Box(90, 4, 30, 13), HelpWindowTexts.FIND_TAB)));
            case FRAMES_95_TOPIC -> new Frame(230, 200, new Box(0, 0, 230, 19), null, null,
                    new Box(0, 19, 230, 181), null,
                    List.of(new Button(new Box(4, 3, 66, 13), HelpWindowTexts.HELP_TOPICS),
                            new Button(new Box(74, 3, 36, 13), HelpWindowTexts.BACK),
                            new Button(new Box(114, 3, 44, 13), HelpWindowTexts.OPTIONS)), List.of());
            case FRAMES_XP -> new Frame(340, 230, new Box(0, 0, 340, 22), new Box(238, 5, 96, 12),
                    new Box(0, 52, 110, 178), new Box(111, 36, 229, 194), null,
                    List.of(new Button(new Box(4, 23, 36, 12), HelpWindowTexts.BACK),
                            new Button(new Box(42, 23, 12, 12), null),
                            new Button(new Box(58, 23, 32, 12), HelpWindowTexts.HOME),
                            new Button(new Box(92, 23, 34, 12), HelpWindowTexts.INDEX),
                            new Button(new Box(128, 23, 50, 12), HelpWindowTexts.FAVORITES),
                            new Button(new Box(180, 23, 42, 12), HelpWindowTexts.HISTORY),
                            new Button(new Box(224, 23, 42, 12), HelpWindowTexts.OPTIONS)), List.of());
            case FRAMES_7 -> new Frame(340, 230, new Box(0, 0, 340, 18), new Box(30, 3, 220, 12),
                    new Box(4, 22, 104, 204), new Box(112, 22, 224, 204), null,
                    List.of(new Button(new Box(3, 3, 12, 12), null), new Button(new Box(16, 3, 12, 12), null),
                            new Button(new Box(270, 3, 66, 12), HelpWindowTexts.BROWSE_HELP)), List.of());
            case GET_HELP -> new Frame(340, 236, null, new Box(4, 4, 332, 13), new Box(4, 22, 104, 210),
                    new Box(112, 22, 224, 210), null, List.of(), List.of());
            case KDE -> new Frame(340, 224, null, null, new Box(0, 0, 112, 224), new Box(113, 0, 227, 224), null,
                    List.of(), List.of());
            case YELP -> new Frame(320, 224, new Box(0, 0, 320, 17), new Box(70, 3, 180, 11), null,
                    new Box(0, 31, 320, 193), new Box(10, 19, 300, 10),
                    List.of(new Button(new Box(4, 3, 14, 11), null), new Button(new Box(300, 3, 14, 11), null)),
                    List.of());
            case CDE -> new Frame(300, 240, new Box(0, 0, 300, 12), null, new Box(4, 14, 292, 54),
                    new Box(4, 72, 292, 144), null,
                    List.of(new Button(new Box(4, 222, 66, 14), HelpWindowTexts.BACKTRACK),
                            new Button(new Box(74, 222, 64, 14), HelpWindowTexts.HISTORY_DOTS),
                            new Button(new Box(142, 222, 58, 14), HelpWindowTexts.INDEX_DOTS),
                            new Button(new Box(204, 222, 64, 14), HelpWindowTexts.TOP_LEVEL)), List.of());
        };
    }

    /**
     * Everything solid in a form's window, for the test that says none of it overlaps. The band along the top is a
     * strip of colour the buttons and the search field stand on, not a part of its own. Frames 95's field and list
     * stand where its tree does, on the tabs that show them instead, so its tree is left out of its own check.
     */
    public static GuiLayout layout(final HelpForm form) {
        final Frame frame = of(form);
        final GuiLayout layout = new GuiLayout(frame.width(), frame.height());
        final List<Box> solid = new ArrayList<>();
        if (frame.tree() != null && form != HelpForm.FRAMES_95) {
            solid.add(frame.tree());
        }
        if (frame.search() != null) {
            solid.add(frame.search());
        }
        solid.add(frame.page());
        for (final Box box : solid) {
            layout.box("part " + solid.indexOf(box), box.x(), box.y(), box.w(), box.h());
        }
        int index = 0;
        for (final Button button : frame.buttons()) {
            layout.box("button " + index++, button.box().x(), button.box().y(), button.box().w(), button.box().h());
            if (button.label() != null) {
                layout.text("label " + index, button.box().x() + 3, button.box().y() + 3,
                        button.label().english().length(), 1.0f);
            }
        }
        for (final Button tab : frame.tabs()) {
            layout.box(tab.label().english(), tab.box().x(), tab.box().y(), tab.box().w(), tab.box().h());
        }
        if (frame.trail() != null) {
            layout.text("trail", frame.trail().x(), frame.trail().y(), 40, 1.0f);
        }
        return layout;
    }
}
