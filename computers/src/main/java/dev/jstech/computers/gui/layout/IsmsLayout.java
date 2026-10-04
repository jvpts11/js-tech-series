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
 * Where the IQL Server Management Studio's parts stand in its window, and its Profiler's in theirs. Pure, so the
 * geometry is tested without the game.
 *
 * <p>The studio is a resizable desktop window: a menu bar and a toolbar across the top, a status bar across the
 * bottom, and between them the Object Explorer on the left and the documents on the right, a query's editor over its
 * results. The explorer's width and the editor's height follow the two splitters, kept inside what the window can
 * afford; everything else is anchored to the window's edges. Coordinates are relative to the window's content.
 */
public final class IsmsLayout {

    // The window.
    public static final int DEFAULT_W = 400;
    public static final int DEFAULT_H = 236;
    public static final int MIN_W = 300;
    public static final int MIN_H = 170;

    // The strips across the top and the bottom.
    public static final int MENU_H = 10;
    public static final int TOOLBAR_Y = MENU_H;
    public static final int TOOLBAR_H = 16;
    public static final int BODY_Y = TOOLBAR_Y + TOOLBAR_H;
    public static final int STATUS_H = 11;
    public static final int BUTTON_H = 12;
    public static final int BUTTON_GAP = 3;
    public static final int BUTTON_PAD = 8;
    public static final int TOOLBAR_SEPARATOR = 7;

    // The Object Explorer.
    public static final int EXPLORER_W = 110;
    public static final int MIN_EXPLORER_W = 60;
    public static final int EX_HEAD_H = 11;
    public static final int TREE_PITCH = 10;
    public static final int INDENT = 8;
    public static final int PROPERTIES_H = 62;
    public static final int VSPLIT_W = 4;

    // The documents.
    public static final int TABS_H = 12;
    public static final int HSPLIT_H = 4;
    public static final int RES_TABS_H = 11;
    public static final int ROW_H = 10;
    public static final int MIN_EDITOR_H = 30;
    public static final int MIN_RESULTS_H = 24;
    /** The share of the document's height the editor starts with, in hundredths. */
    public static final int EDITOR_SHARE = 55;

    // The dialogs.
    public static final int CONFIRM_W = 230;
    public static final int CONFIRM_H = 96;
    public static final int DIALOG_PAD = 6;
    public static final int DIALOG_TITLE_H = 14;
    public static final int DIALOG_BUTTON_W = 46;
    public static final int FIELD_H = 12;
    public static final int TEMPLATE_W = 250;
    public static final int TEMPLATE_ROW_H = 16;
    public static final int MOST_TEMPLATE_ROWS = 6;
    public static final int OPTIONS_W = 240;
    public static final int OPTIONS_H = 84;
    public static final int CHECK_ROW_H = 14;
    public static final int INDEX_W = 240;
    public static final int INDEX_H = 72;
    public static final int NOTE_W = 250;
    public static final int NOTE_H = 96;

    // The Profiler.
    public static final int PROFILER_W = 400;
    public static final int PROFILER_H = 220;
    public static final int PROFILER_MIN_W = 300;
    public static final int PROFILER_MIN_H = 150;
    public static final int DETAIL_H = 46;

    /* Names of the regions, which a failed check reports. */
    private static final String MENU = "menu";
    private static final String TOOLBAR = "toolbar";
    private static final String EXPLORER_HEAD = "explorerHead";
    private static final String TREE = "tree";
    private static final String VSPLIT = "vsplit";
    private static final String TABS = "tabs";
    private static final String EDITOR = "editor";
    private static final String HSPLIT = "hsplit";
    private static final String RESULT_TABS = "resultTabs";
    private static final String RESULTS = "results";
    private static final String STATUS = "status";
    private static final String TITLE = "title";
    private static final String MESSAGE = "message";
    private static final String DONT_ASK = "dontAsk";
    private static final String YES = "yes";
    private static final String NO = "no";
    private static final String PARAMETER = "parameter";
    private static final String VALUE = "value";
    private static final String OK = "ok";
    private static final String CHECK = "check";
    private static final String GRID = "grid";
    private static final String DETAIL = "detail";

    private IsmsLayout() {
    }

    /** The status bar's top in a window {@code height} tall. */
    public static int statusY(final int height) {
        return height - STATUS_H;
    }

    /** How tall the body between the toolbar and the status bar is. */
    public static int bodyH(final int height) {
        return statusY(height) - BODY_Y;
    }

    /** The widest the explorer may be in a window {@code width} wide, leaving the documents room to work. */
    public static int maxExplorerW(final int width) {
        return Math.max(MIN_EXPLORER_W, width - 150);
    }

    /** {@code wanted} kept between the explorer's least and most widths. */
    public static int explorerW(final int width, final int wanted) {
        return Math.max(MIN_EXPLORER_W, Math.min(maxExplorerW(width), wanted));
    }

    /** How tall a query's document is, under its tab strip. */
    public static int documentH(final int height) {
        return bodyH(height) - TABS_H;
    }

    /** The tallest the editor may be, leaving the results their least. */
    public static int maxEditorH(final int height) {
        return Math.max(MIN_EDITOR_H, documentH(height) - HSPLIT_H - RES_TABS_H - MIN_RESULTS_H);
    }

    /** The editor's height a window starts with, its share of the document. */
    public static int defaultEditorH(final int height) {
        return editorH(height, documentH(height) * EDITOR_SHARE / 100);
    }

    /** {@code wanted} kept between the editor's least and most heights. */
    public static int editorH(final int height, final int wanted) {
        return Math.max(MIN_EDITOR_H, Math.min(maxEditorH(height), wanted));
    }

    /** The studio's regions at its default size. */
    public static GuiLayout layout() {
        return layout(DEFAULT_W, DEFAULT_H, EXPLORER_W, defaultEditorH(DEFAULT_H));
    }

    /** The studio's regions in a window {@code width} by {@code height}, with the splitters where they were left. */
    public static GuiLayout layout(final int width, final int height, final int wantedExplorer,
                                   final int wantedEditor) {
        final int ex = explorerW(width, wantedExplorer);
        final int right = ex + VSPLIT_W;
        final int rightW = width - right;
        final int editor = editorH(height, wantedEditor);
        final int editorY = BODY_Y + TABS_H;
        final int hsplitY = editorY + editor;
        final int resultTabsY = hsplitY + HSPLIT_H;
        final int resultsY = resultTabsY + RES_TABS_H;
        final int statusY = statusY(height);
        return new GuiLayout(width, height)
                .box(MENU, 0, 0, width, MENU_H)
                .box(TOOLBAR, 0, TOOLBAR_Y, width, TOOLBAR_H)
                .box(EXPLORER_HEAD, 0, BODY_Y, ex, EX_HEAD_H)
                .box(TREE, 0, BODY_Y + EX_HEAD_H, ex, statusY - BODY_Y - EX_HEAD_H)
                .box(VSPLIT, ex, BODY_Y, VSPLIT_W, statusY - BODY_Y)
                .box(TABS, right, BODY_Y, rightW, TABS_H)
                .box(EDITOR, right, editorY, rightW, editor)
                .box(HSPLIT, right, hsplitY, rightW, HSPLIT_H)
                .box(RESULT_TABS, right, resultTabsY, rightW, RES_TABS_H)
                .box(RESULTS, right, resultsY, rightW, statusY - resultsY)
                .box(STATUS, 0, statusY, width, STATUS_H);
    }

    /** The question asked before a statement that cannot be undone, at its longest. */
    public static GuiLayout confirm() {
        final int buttonsY = CONFIRM_H - DIALOG_PAD - FIELD_H;
        return new GuiLayout(CONFIRM_W, CONFIRM_H)
                .text(TITLE, DIALOG_PAD, 4, 7, 1.0f)
                .text(MESSAGE, DIALOG_PAD, DIALOG_TITLE_H + 4, 36, 1.0f)
                .box(DONT_ASK, DIALOG_PAD, buttonsY - CHECK_ROW_H - 2, CONFIRM_W - DIALOG_PAD * 2, CHECK_ROW_H - 2)
                .box(YES, CONFIRM_W - DIALOG_PAD * 2 - DIALOG_BUTTON_W * 2, buttonsY, DIALOG_BUTTON_W, FIELD_H)
                .box(NO, CONFIRM_W - DIALOG_PAD - DIALOG_BUTTON_W, buttonsY, DIALOG_BUTTON_W, FIELD_H);
    }

    /** How tall the template values dialog is with {@code rows} parameters, its rows capped at the most it shows. */
    public static int templateH(final int rows) {
        return DIALOG_TITLE_H + DIALOG_PAD + Math.min(MOST_TEMPLATE_ROWS, Math.max(1, rows)) * TEMPLATE_ROW_H
                + DIALOG_PAD + FIELD_H + DIALOG_PAD;
    }

    /** The template values dialog with {@code rows} parameters. */
    public static GuiLayout template(final int rows) {
        final int h = templateH(rows);
        final GuiLayout l = new GuiLayout(TEMPLATE_W, h).text(TITLE, DIALOG_PAD, 4, 38, 1.0f);
        final int shown = Math.min(MOST_TEMPLATE_ROWS, Math.max(1, rows));
        for (int i = 0; i < shown; i++) {
            final int y = DIALOG_TITLE_H + DIALOG_PAD + i * TEMPLATE_ROW_H;
            l.text(PARAMETER + i, DIALOG_PAD, y + 2, 16, 1.0f);
            l.box(VALUE + i, TEMPLATE_W / 2, y, TEMPLATE_W / 2 - DIALOG_PAD, FIELD_H);
        }
        return l.box(OK, TEMPLATE_W - DIALOG_PAD * 2 - DIALOG_BUTTON_W * 2, h - DIALOG_PAD - FIELD_H, DIALOG_BUTTON_W,
                        FIELD_H)
                .box(NO, TEMPLATE_W - DIALOG_PAD - DIALOG_BUTTON_W, h - DIALOG_PAD - FIELD_H, DIALOG_BUTTON_W,
                        FIELD_H);
    }

    /** The options dialogs: three rows of choices and a button to close them. */
    public static GuiLayout options() {
        final GuiLayout l = new GuiLayout(OPTIONS_W, OPTIONS_H).text(TITLE, DIALOG_PAD, 4, 13, 1.0f);
        for (int i = 0; i < 3; i++) {
            l.box(CHECK + i, DIALOG_PAD, DIALOG_TITLE_H + DIALOG_PAD + i * CHECK_ROW_H, OPTIONS_W - DIALOG_PAD * 2,
                    CHECK_ROW_H - 2);
        }
        return l.box(OK, OPTIONS_W - DIALOG_PAD - DIALOG_BUTTON_W, OPTIONS_H - DIALOG_PAD - FIELD_H, DIALOG_BUTTON_W,
                FIELD_H);
    }

    /** The Index Maintenance dialog: what the index is like, and its three jobs. */
    public static GuiLayout indexMaintenance() {
        final int buttonsY = INDEX_H - DIALOG_PAD - FIELD_H;
        final int w = (INDEX_W - DIALOG_PAD * 5) / 4;
        final GuiLayout l = new GuiLayout(INDEX_W, INDEX_H).text(TITLE, DIALOG_PAD, 4, 17, 1.0f)
                .text(MESSAGE, DIALOG_PAD, DIALOG_TITLE_H + 6, 38, 1.0f);
        for (int i = 0; i < 4; i++) {
            l.box(CHECK + i, DIALOG_PAD + i * (w + DIALOG_PAD), buttonsY, w, FIELD_H);
        }
        return l;
    }

    /** A note the studio shows with only an OK: a missing engine, the studio's version. */
    public static GuiLayout note() {
        return new GuiLayout(NOTE_W, NOTE_H)
                .text(TITLE, DIALOG_PAD, 4, 28, 1.0f)
                .text(MESSAGE, DIALOG_PAD, DIALOG_TITLE_H + 4, 39, 1.0f)
                .box(OK, NOTE_W - DIALOG_PAD - DIALOG_BUTTON_W, NOTE_H - DIALOG_PAD - FIELD_H, DIALOG_BUTTON_W,
                        FIELD_H);
    }

    /** The Profiler's regions in a window {@code width} by {@code height}. */
    public static GuiLayout profiler(final int width, final int height) {
        final int statusY = statusY(height);
        final int detailY = statusY - DETAIL_H;
        return new GuiLayout(width, height)
                .box(MENU, 0, 0, width, MENU_H)
                .box(TOOLBAR, 0, TOOLBAR_Y, width, TOOLBAR_H)
                .box(GRID, 0, BODY_Y, width, detailY - HSPLIT_H - BODY_Y)
                .box(HSPLIT, 0, detailY - HSPLIT_H, width, HSPLIT_H)
                .box(DETAIL, 0, detailY, width, DETAIL_H)
                .box(STATUS, 0, statusY, width, STATUS_H);
    }
}
