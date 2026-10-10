/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui.layout;

import dev.jstech.core.config.ConfigDraft;
import java.util.List;

/**
 * Where everything on the Core's settings screen sits. A header with the mod's mark, its name and version, and the box
 * that searches its settings; a rail down the left with a tab for each kind of file (the world's, the player's, every
 * game's), a line saying where that kind is kept, and the sections of the open tab; the open section's title and what
 * it is for, then its settings as cards, each its name, its description, its default and bounds, and its control at
 * the card's right end; and a footer with the section's defaults on the left, how many changes wait in the middle, and
 * Cancel and Save on the right. The screen draws from these, and a test lays it out at the smallest size the game
 * allows and checks nothing collides.
 *
 * <p>Every x and y is from the top left of the screen. A card is as tall as its description: {@link #cardHeight}.
 */
public final class ConfigScreenLayout {

    public static final int HEADER = 32;
    public static final int FOOTER = 24;
    /** The rail's width, and the narrower one a screen under {@link #WIDE_SCREEN} across takes. */
    public static final int RAIL_WIDTH = 150;
    public static final int NARROW_RAIL_WIDTH = 96;
    public static final int WIDE_SCREEN = 400;
    public static final int PAD = 8;
    /** The mod's mark, a square at the header's left. */
    public static final int BADGE = 20;
    public static final int BADGE_Y = 6;
    /** The mod's name and the line under it. */
    public static final int NAME_Y = 7;
    public static final int SUBTITLE_Y = 20;
    /** The search box at the header's right. */
    public static final int SEARCH_WIDTH = 115;
    public static final int SEARCH_HEIGHT = 14;
    public static final int SEARCH_Y = 9;
    /** The tabs of the kinds of file, across the top of the rail. */
    public static final int TAB_INSET = 6;
    public static final int TAB_Y = HEADER + 6;
    public static final int TAB_HEIGHT = 14;
    /** How many tabs the rail has when every kind of file is present: the most the layout validates. */
    public static final int TAB_COUNT = 3;
    /** A line of small text: the notes under the tabs and a section's title, a card's description and default. */
    public static final int NOTE_LINE = 7;
    public static final int SCOPE_NOTE_LINES = 2;
    public static final int RAIL_ITEM = 16;
    /** Where a section's name starts in the rail. */
    public static final int RAIL_LABEL_X = 9;
    /** The open section's title and the cards under it. */
    public static final int TITLE_HEIGHT = 13;
    public static final int CARD_GAP = 3;
    public static final int CARD_PAD = 4;
    public static final int CARD_NAME = 10;
    public static final int META_GAP = 2;
    /** At most this many lines of a card's description; the rest is read where the pointer rests on it. */
    public static final int MOST_DESCRIPTION_LINES = 3;
    public static final int MOST_SECTION_NOTE_LINES = 2;
    /** The banner that says a tab's settings are only read here. */
    public static final int LOCK_HEIGHT = 16;
    /** The scrollbar at the right of the cards, and the room it keeps. */
    public static final int SCROLLBAR = 3;
    public static final int SCROLLBAR_GAP = 4;
    /** The controls at a card's right end. */
    public static final int CONTROL_HEIGHT = 13;
    public static final int SWITCH_WIDTH = 28;
    public static final int SWITCH_HEIGHT = 12;
    public static final int STATE_WIDTH = 14;
    public static final int STEP = 13;
    public static final int NUMBER_FIELD = 40;
    public static final int UNIT_WIDTH = 24;
    public static final int CONTROL_GAP = 3;
    public static final int CHOICE_WIDTH = 78;
    public static final int TEXT_WIDTH = 90;
    public static final int FIXED_WIDTH = 90;
    public static final int RESET_WIDTH = 32;
    /** The room between a card's text and its control, and its inner margin on the right. */
    public static final int NAME_GAP = 8;
    public static final int CARD_INSET = 6;
    public static final int BUTTON_HEIGHT = 15;
    public static final int DEFAULTS_WIDTH = 96;
    public static final int CANCEL_WIDTH = 46;
    public static final int SAVE_WIDTH = 42;
    /** A pointer's hint over a card: how wide it runs. */
    public static final int TOOLTIP_WIDTH = 150;
    /** The smallest screen the game lays out, in its own units. */
    public static final int SMALLEST_WIDTH = 320;
    public static final int SMALLEST_HEIGHT = 240;

    private ConfigScreenLayout() {
    }

    /** The left edge of the name and the line under it, beside the mark. */
    public static int nameX() {
        return PAD + BADGE + 7;
    }

    public static int searchX(final int width) {
        return width - PAD - SEARCH_WIDTH;
    }

    /** How wide the rail is on a screen that wide: narrower on a small screen, so a card's name keeps its room. */
    public static int railWidth(final int width) {
        return width < WIDE_SCREEN ? NARROW_RAIL_WIDTH : RAIL_WIDTH;
    }

    /** How wide each tab is when there are {@code tabs} of them. */
    public static int tabWidth(final int width, final int tabs) {
        return (railWidth(width) - 2 * TAB_INSET) / Math.max(1, tabs);
    }

    public static int tabX(final int width, final int index, final int tabs) {
        return TAB_INSET + index * tabWidth(width, tabs);
    }

    /** Where the note saying where a tab's settings are kept starts. */
    public static int scopeNoteY() {
        return TAB_Y + TAB_HEIGHT + 4;
    }

    /** Where the first section of the rail starts, under the tabs and their note. */
    public static int railTop() {
        return scopeNoteY() + SCOPE_NOTE_LINES * NOTE_LINE + 4;
    }

    public static int railItemY(final int index) {
        return railTop() + index * RAIL_ITEM;
    }

    /** How wide a section's name may run in the rail, the mark of a change and its count of settings beside it. */
    public static int railLabelWidth(final int width) {
        return railWidth(width) - RAIL_LABEL_X - 25;
    }

    /** The left edge of the cards beside the rail. */
    public static int contentLeft(final int width) {
        return railWidth(width) + 9;
    }

    /** The right edge of the cards, the scrollbar's room beyond it. */
    public static int contentRight(final int width) {
        return width - PAD - SCROLLBAR - SCROLLBAR_GAP;
    }

    public static int scrollbarX(final int width) {
        return width - PAD - SCROLLBAR;
    }

    public static int titleY() {
        return HEADER + 7;
    }

    /** Where the open section's note starts, under its title. */
    public static int sectionNoteY() {
        return titleY() + TITLE_HEIGHT;
    }

    /** Where the first card starts, under the section's title and its note of so many lines. */
    public static int cardsTop(final int noteLines) {
        return sectionNoteY() + noteLines * NOTE_LINE + 5;
    }

    /** The bottom of the cards' room, above the footer. */
    public static int cardsBottom(final int height) {
        return height - FOOTER - 3;
    }

    /** How tall a card is with so many lines of description: its name, the lines, its default, and its margins. */
    public static int cardHeight(final int descriptionLines) {
        return CARD_PAD + CARD_NAME + descriptionLines * NOTE_LINE + META_GAP + NOTE_LINE + CARD_PAD;
    }

    /** The left edge of a card's name and description. */
    public static int textX(final int width) {
        return contentLeft(width) + CARD_INSET;
    }

    /** How wide a control of that kind is, a number's unit included when it has one. */
    public static int controlWidth(final ConfigDraft.Control control, final boolean unit) {
        return switch (control) {
            case TOGGLE -> STATE_WIDTH + CONTROL_GAP + SWITCH_WIDTH;
            case NUMBER -> STEP + CONTROL_GAP + NUMBER_FIELD + CONTROL_GAP + STEP
                    + (unit ? CONTROL_GAP + UNIT_WIDTH : 0);
            case CHOICE -> CHOICE_WIDTH;
            case TEXT -> TEXT_WIDTH;
            case FIXED -> FIXED_WIDTH;
        };
    }

    /** Where a control of that kind starts, at the right end of its card. */
    public static int controlX(final int width, final ConfigDraft.Control control, final boolean unit) {
        return contentRight(width) - CARD_INSET - controlWidth(control, unit);
    }

    /** Where a card's Default button starts, before its control. */
    public static int resetX(final int width, final ConfigDraft.Control control, final boolean unit) {
        return controlX(width, control, unit) - CONTROL_GAP - RESET_WIDTH;
    }

    /** How tall a control is: a toggle's switch is a little shorter than the other controls. */
    public static int controlHeight(final ConfigDraft.Control control) {
        return control == ConfigDraft.Control.TOGGLE ? SWITCH_HEIGHT : CONTROL_HEIGHT;
    }

    /** The top of a control in the card at {@code cardY} that tall, centred down the card. */
    public static int controlY(final int cardY, final int cardHeight, final ConfigDraft.Control control) {
        return cardY + (cardHeight - controlHeight(control)) / 2;
    }

    /** Where a number control's field starts, given where the control starts: after its minus button. */
    public static int numberFieldX(final int controlX) {
        return controlX + STEP + CONTROL_GAP;
    }

    /** Where a number control's plus button starts, given where the control starts: after its field. */
    public static int plusX(final int controlX) {
        return numberFieldX(controlX) + NUMBER_FIELD + CONTROL_GAP;
    }

    /** How wide a card's name and description may run before its control, and its Default button when it has one. */
    public static int nameWidth(final int width, final ConfigDraft.Control control, final boolean unit,
                                final boolean reset) {
        final int end = reset ? resetX(width, control, unit) : controlX(width, control, unit);
        return end - NAME_GAP - textX(width);
    }

    public static int footerButtonY(final int height) {
        return height - FOOTER + (FOOTER - BUTTON_HEIGHT) / 2;
    }

    public static int saveX(final int width) {
        return width - PAD - SAVE_WIDTH;
    }

    public static int cancelX(final int width) {
        return saveX(width) - 5 - CANCEL_WIDTH;
    }

    /**
     * The screen with cards of those controls, as many as fit, under a section note of so many lines: every card
     * given the most lines of description, a number's unit and a Default button, the widest each can be.
     */
    public static GuiLayout layout(final int width, final int height, final int noteLines,
                                   final List<ConfigDraft.Control> cards) {
        final GuiLayout layout = new GuiLayout(width, height)
                .box("badge", PAD, BADGE_Y, BADGE, BADGE)
                .box("search", searchX(width), SEARCH_Y, SEARCH_WIDTH, SEARCH_HEIGHT)
                .box("footer_defaults", PAD, footerButtonY(height), DEFAULTS_WIDTH, BUTTON_HEIGHT)
                .box("footer_cancel", cancelX(width), footerButtonY(height), CANCEL_WIDTH, BUTTON_HEIGHT)
                .box("footer_save", saveX(width), footerButtonY(height), SAVE_WIDTH, BUTTON_HEIGHT);
        for (int tab = 0; tab < TAB_COUNT; tab++) {
            layout.box("tab_" + tab, tabX(width, tab, TAB_COUNT), TAB_Y, tabWidth(width, TAB_COUNT), TAB_HEIGHT);
        }
        final int cardHeight = cardHeight(MOST_DESCRIPTION_LINES);
        int y = cardsTop(noteLines);
        for (int card = 0; card < cards.size() && y + cardHeight <= cardsBottom(height); card++) {
            final ConfigDraft.Control control = cards.get(card);
            final boolean unit = control == ConfigDraft.Control.NUMBER;
            layout.text("name_" + card, textX(width), y + CARD_PAD, nameWidth(width, control, unit, true) / 6, 1.0F);
            layout.box("reset_" + card, resetX(width, control, unit), controlY(y, cardHeight, control), RESET_WIDTH,
                    CONTROL_HEIGHT);
            final int x = controlX(width, control, unit);
            final int top = controlY(y, cardHeight, control);
            if (control == ConfigDraft.Control.NUMBER) {
                layout.box("minus_" + card, x, top, STEP, CONTROL_HEIGHT);
                layout.box("field_" + card, numberFieldX(x), top, NUMBER_FIELD, CONTROL_HEIGHT);
                layout.box("plus_" + card, plusX(x), top, STEP, CONTROL_HEIGHT);
            } else {
                layout.box("control_" + card, x, top, controlWidth(control, false), controlHeight(control));
            }
            y += cardHeight + CARD_GAP;
        }
        return layout;
    }
}
