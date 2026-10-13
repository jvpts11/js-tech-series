/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

import dev.jstech.core.gui.layout.GuiLayout;

/**
 * The layout of the Network Interactor's Update window, top to bottom: the title, a tab for each card's action
 * (Enchant, Smelt, Repair), the panel of the tab that is up, and the footer with its status line and the two buttons.
 * Inside the panel: the item with how many the network holds and where, the card that does the work, and the body
 * of the tab. Pure arithmetic, so a test can prove nothing in it overlaps and every line fits.
 *
 * <p>Window positions are measured from the window's corner; the panel's, from the panel's own corner.
 */
public final class UpdateWindowLayout {

    public static final int W = 236;
    public static final int H = 143;
    public static final int PAD = 4;
    public static final int TITLE_H = 13;
    public static final int TABS = 3;
    public static final int TAB_W = 54;
    public static final int TAB_H = 12;
    public static final int BUTTON_W = 52;
    public static final int BUTTON_H = 14;
    public static final int FOOT_Y = H - PAD - BUTTON_H;
    public static final int UPDATE_X = W - PAD - 2 * BUTTON_W - 4;
    public static final int CANCEL_X = W - PAD - BUTTON_W;
    public static final int STATUS_Y = FOOT_Y + 3;
    public static final int STATUS_W = UPDATE_X - PAD - 4;
    public static final int PANEL_X = PAD;
    public static final int PANEL_Y = TITLE_H + TAB_H;
    public static final int PANEL_W = W - 2 * PAD;
    public static final int PANEL_H = FOOT_Y - 3 - PANEL_Y;
    public static final int CELL = 18;
    /** The small text everything in the panel but the item's name is written in. */
    public static final float SMALL = 0.75f;
    public static final int LINE = 8;

    /** The item, its name and where the network holds it; the card under them; the body below. */
    public static final int ITEM_X = 4;
    public static final int ITEM_Y = 3;
    public static final int NAME_X = ITEM_X + CELL + 5;
    public static final int NAME_Y = 4;
    public static final int WHERE_Y = 14;
    public static final int CARD_Y = ITEM_Y + CELL + 3;
    public static final int BODY_Y = CARD_Y + 10;
    public static final int TEXT_W = PANEL_W - 8;

    /** Enchant: three offers, each a badge with the level it needs, the clue, and the price; the line on paying. */
    public static final int OFFER_X = 4;
    public static final int OFFER_H = 13;
    public static final int OFFER_GAP = 2;
    public static final int BADGE_W = 14;
    public static final int PAY_Y = BODY_Y + 3 * (OFFER_H + OFFER_GAP) + 2;

    /** Repair: the worn item, the material and the result in a row; the material line; the name; the cost. */
    public static final int LEFT_X = 4;
    public static final int RIGHT_X = LEFT_X + CELL + 14;
    public static final int ANVIL_ARROW_X = RIGHT_X + CELL + 6;
    public static final int ANVIL_ARROW_W = 16;
    public static final int RESULT_X = ANVIL_ARROW_X + ANVIL_ARROW_W + 6;
    public static final int WITH_Y = BODY_Y + CELL + 4;
    public static final int NAME_ROW_Y = WITH_Y + 10;
    public static final int NAME_LABEL_W = 28;
    public static final int FIELD_X = 4 + NAME_LABEL_W + 2;
    public static final int FIELD_W = 132;
    public static final int FIELD_H = 12;
    public static final int COST_Y = NAME_ROW_Y + FIELD_H + 4;

    /** Smelt: the amount on a stepper, the result; where it goes back; the progress bar; the wait line. */
    public static final int SMELT_LABEL_W = 28;
    public static final int QTY_X = 4 + SMELT_LABEL_W + 2;
    public static final int QTY_W = 110;
    public static final int MAX_X = QTY_X + QTY_W + 2;
    public static final int MAX_W = 26;
    public static final int SMELT_ARROW_X = MAX_X + MAX_W + 6;
    public static final int SMELT_ARROW_W = 14;
    public static final int SMELT_RESULT_X = SMELT_ARROW_X + SMELT_ARROW_W + 4;
    public static final int BACK_Y = BODY_Y + CELL + 4;
    public static final int BAR_Y = BACK_Y + 10;
    public static final int BAR_H = 8;
    public static final int WAIT_Y = BAR_Y + BAR_H + 4;

    /* The names the checks report the text elements by. */
    private static final String STATUS = "status";
    private static final String NAME = "name";
    private static final String WHERE = "where";
    private static final String CARD = "card";
    private static final String PAY = "pay";
    private static final String WITH = "with";
    private static final String NAME_LABEL = "nameLabel";
    private static final String COST = "cost";
    private static final String SMELT_LABEL = "smeltLabel";
    private static final String BACK = "back";
    private static final String WAIT_FIRST = "wait1";
    private static final String WAIT_SECOND = "wait2";

    private UpdateWindowLayout() {
    }

    /** The x of tab {@code tab}, from the left. */
    public static int tabX(final int tab) {
        return PAD + tab * (TAB_W + 2);
    }

    /** The y of offer {@code offer} in the panel. */
    public static int offerY(final int offer) {
        return BODY_Y + offer * (OFFER_H + OFFER_GAP);
    }

    /** The longest line the panel's small text holds, in characters. */
    public static int smallChars() {
        return (int) (TEXT_W / (GuiLayout.GLYPH_WIDTH * SMALL));
    }

    /** The window's solid regions, for the overlap check. */
    public static GuiLayout layout() {
        final GuiLayout layout = new GuiLayout(W, H);
        for (int i = 0; i < TABS; i++) {
            layout.box("tab" + i, tabX(i), TITLE_H, TAB_W, TAB_H);
        }
        return layout.box("panel", PANEL_X, PANEL_Y, PANEL_W, PANEL_H)
                .box("update", UPDATE_X, FOOT_Y, BUTTON_W, BUTTON_H)
                .box("cancel", CANCEL_X, FOOT_Y, BUTTON_W, BUTTON_H)
                .text(STATUS, PAD, STATUS_Y, (int) (STATUS_W / (GuiLayout.GLYPH_WIDTH * SMALL)), SMALL);
    }

    /*
     * The tabs share the panel and only one shows at a time, so each is checked on its own with the header above it,
     * measured from the panel's corner.
     */

    /** The Enchant tab: the header, the three offers and the line on paying. */
    public static GuiLayout enchant() {
        final GuiLayout layout = header();
        for (int i = 0; i < 3; i++) {
            layout.box("offer" + i, OFFER_X, offerY(i), TEXT_W, OFFER_H);
        }
        return layout.text(PAY, OFFER_X, PAY_Y, smallChars(), SMALL);
    }

    /** The Repair tab: the header, the row of three cells, the material line, the name field and the cost. */
    public static GuiLayout repair() {
        return header()
                .box("left", LEFT_X, BODY_Y, CELL, CELL)
                .box("right", RIGHT_X, BODY_Y, CELL, CELL)
                .box("arrow", ANVIL_ARROW_X, BODY_Y + 5, ANVIL_ARROW_W, 8)
                .box("result", RESULT_X, BODY_Y, CELL, CELL)
                .text(WITH, LEFT_X, WITH_Y, smallChars(), SMALL)
                .text(NAME_LABEL, LEFT_X, NAME_ROW_Y + 2, (int) (NAME_LABEL_W / (GuiLayout.GLYPH_WIDTH * SMALL)),
                        SMALL)
                .box("field", FIELD_X, NAME_ROW_Y, FIELD_W, FIELD_H)
                .text(COST, LEFT_X, COST_Y, smallChars(), SMALL);
    }

    /** The Smelt tab: the header, the amount row, the line on where it goes back, the bar and the wait line. */
    public static GuiLayout smelt() {
        return header()
                .text(SMELT_LABEL, 4, BODY_Y + 5, (int) (SMELT_LABEL_W / (GuiLayout.GLYPH_WIDTH * SMALL)), SMALL)
                .box("qty", QTY_X, BODY_Y + 3, QTY_W, FIELD_H)
                .box("max", MAX_X, BODY_Y + 3, MAX_W, FIELD_H)
                .box("arrow", SMELT_ARROW_X, BODY_Y + 5, SMELT_ARROW_W, 8)
                .box("result", SMELT_RESULT_X, BODY_Y, CELL, CELL)
                .text(BACK, 4, BACK_Y, smallChars(), SMALL)
                .box("bar", 4, BAR_Y, TEXT_W, BAR_H)
                .text(WAIT_FIRST, 4, WAIT_Y, smallChars(), SMALL)
                .text(WAIT_SECOND, 4, WAIT_Y + LINE, smallChars(), SMALL);
    }

    /* The panel's top: the item's cell, its name, where it is, and the card's line. */
    private static GuiLayout header() {
        return new GuiLayout(PANEL_W, PANEL_H)
                .box("item", ITEM_X, ITEM_Y, CELL, CELL)
                .text(NAME, NAME_X, NAME_Y, (int) ((PANEL_W - NAME_X - 4) / GuiLayout.GLYPH_WIDTH), 1.0f)
                .text(WHERE, NAME_X, WHERE_Y, (int) ((PANEL_W - NAME_X - 4) / (GuiLayout.GLYPH_WIDTH * SMALL)),
                        SMALL)
                .text(CARD, 4, CARD_Y, smallChars(), SMALL);
    }
}
