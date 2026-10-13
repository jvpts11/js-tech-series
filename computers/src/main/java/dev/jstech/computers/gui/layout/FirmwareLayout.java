/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

import dev.jstech.computers.gui.MonitorGlass;
import dev.jstech.core.gui.layout.GuiLayout;

/**
 * Pure layout of the firmware setup, in each of its three eras' shapes: the green monochrome CLI, the
 * classic blue setup utility, and the modern graphical UEFI manager. Each shape is its own frame of boxes and
 * panels, since the three never show together. What is recorded is that frame: the boxes, the panels, the
 * rows' own pitch and the foot bar. The boot/order list and the tab strip grow with live state (the boot
 * entries a machine reports, the page names, the key a hint measures itself against), so their own hit boxes
 * stay in the screen, worked out against the font at render time, the way a clickable label always is.
 */
public final class FirmwareLayout {

    public static final int WIDTH = MonitorGlass.WIDTH;
    public static final int HEIGHT = MonitorGlass.HEIGHT;

    /** A list row's own height, shared by every look. */
    public static final int ROW_H = 12;

    /** The step from one key/value line to the next, in each look's own hand. */
    public static final int CLI_LINE_STEP = 11;
    public static final int BIOS_LINE_STEP = 13;
    public static final int UEFI_LINE_STEP = 15;

    /** Where a key/value pair's value column starts, and a storage row's detail column beside it. */
    public static final int VALUE_COLUMN = 110;
    public static final int DETAIL_COLUMN = 130;

    // CLI (Vintage green phosphor)

    public static final int CLI_MARGIN = 14;
    public static final int CLI_TITLE_Y = 10;
    public static final int CLI_TABS_DY = 11;
    public static final int CLI_TAB_H = 10;
    public static final int CLI_TAB_GAP = 8;
    public static final int CLI_CONTENT_DY = 14;
    public static final int CLI_CONTENT_WIDTH_MARGIN = 2 * CLI_MARGIN;
    public static final int CLI_LIST_HEADER_DY = ROW_H;
    public static final int CLI_BOOT_GAP_DY = 4;
    public static final int CLI_BUTTON_H = 13;
    public static final int CLI_BUTTON_GAP = 8;
    public static final int CLI_HINT_BOTTOM_MARGIN = 14;

    // BIOS (Legacy classic blue)

    public static final int BIOS_TITLE_BAR_H = 14;
    public static final int BIOS_TITLE_TEXT_Y = 3;
    public static final int BIOS_TAB_X = 8;
    public static final int BIOS_TAB_Y = 16;
    public static final int BIOS_TAB_H = 12;
    public static final int BIOS_TAB_TEXT_DY = 18;
    public static final int BIOS_TAB_GAP = 14;
    public static final int BIOS_EXIT_MARGIN = 8;
    public static final int BIOS_RULE_Y = 28;
    public static final int BIOS_CONTENT_TOP = 34;
    public static final int BIOS_BOX_W = 206;
    public static final int BIOS_BOX_X = 8;
    public static final int BIOS_HELP_GAP = 8;
    public static final int BIOS_HELP_WIDTH_MARGIN = 24;
    public static final int BIOS_CONTENT_BOTTOM_MARGIN = 24;
    /** The header strip every {@code drawBox} draws, and where its own caption sits inside it. */
    public static final int BIOS_BOX_HEADER_H = 12;
    public static final int BIOS_BOX_HEADER_TEXT_X = 6;
    public static final int BIOS_BOX_HEADER_TEXT_Y = 3;
    public static final int BIOS_CONTENT_INSET_X = 8;
    public static final int BIOS_CONTENT_INSET_Y = 18;
    public static final int BIOS_ROW_INSET = 2;
    public static final int BIOS_BOOT_GAP_DY = 6;
    public static final int BIOS_BUTTON_H = 13;
    public static final int BIOS_BUTTON_Y_OFFSET = 3;
    public static final int BIOS_BUTTON_ROW_GAP = 13;
    public static final int BIOS_HELP_TEXT_X_INSET = 6;
    public static final int BIOS_HELP_TEXT_WIDTH_MARGIN = 12;
    public static final int BIOS_FOOT_H = 16;
    public static final int BIOS_FOOT_TEXT_DY = 12;

    // UEFI (Standard modern graphical manager)

    public static final int UEFI_HEAD_H = 20;
    public static final int UEFI_ACCENT_H = 2;
    public static final int UEFI_HEADER_TEXT_X = 10;
    public static final int UEFI_HEADER_TEXT_Y = 6;
    public static final int UEFI_CONTENT_TOP = 30;
    public static final int UEFI_NAV_X = 10;
    public static final int UEFI_NAV_W = 92;
    public static final int UEFI_NAV_GAP = 8;
    public static final int UEFI_MAIN_WIDTH_MARGIN = 28;
    public static final int UEFI_PANEL_BOTTOM_MARGIN = 24;
    public static final int UEFI_NAV_ROW_DY = 6;
    public static final int UEFI_NAV_ITEM_H = 14;
    /** How far a nav item's box rises above its row, so the text sits in it rather than on its top edge. */
    public static final int UEFI_NAV_ITEM_RISE = 2;
    public static final int UEFI_NAV_TEXT_X = 8;
    public static final int UEFI_NAV_TEXT_DY = 1;
    public static final int UEFI_NAV_ROW_PITCH = 16;
    public static final int UEFI_EXIT_BOTTOM_MARGIN = 12;
    public static final int UEFI_PANEL_HEADER_H = 14;
    public static final int UEFI_PANEL_ACCENT_W = 3;
    public static final int UEFI_PANEL_HEADER_TEXT_X = 7;
    public static final int UEFI_PANEL_HEADER_TEXT_Y = 3;
    public static final int UEFI_CONTENT_INSET_X = 8;
    public static final int UEFI_CONTENT_INSET_Y = 22;
    public static final int UEFI_CONTENT_WIDTH_MARGIN = 16;
    public static final int UEFI_LIST_TOP_DY = 20;
    public static final int UEFI_LIST_MSG_DY = 2;
    public static final int UEFI_LIST_BOTTOM_MARGIN = 26;
    public static final int UEFI_LIST_BOTTOM_GAP = 4;
    public static final int UEFI_MORE_X = 20;
    public static final int UEFI_MORE_ABOVE = 9;
    public static final int UEFI_ROW_INSET = 4;
    public static final int UEFI_ROW_H_BONUS = 2;
    public static final int UEFI_ROW_ACCENT_W = 2;
    public static final int UEFI_DOT_X = 10;
    public static final int UEFI_DOT_W = 5;
    public static final int UEFI_DOT_DY = 4;
    public static final int UEFI_DOT_H = 5;
    public static final int UEFI_LABEL_X = 20;
    public static final int UEFI_LABEL_DY = 3;
    public static final int UEFI_DETAIL_WIDTH_MARGIN = 130;
    public static final int UEFI_DETAIL_RIGHT_MARGIN = 8;
    public static final int UEFI_ROW_PITCH_GAP = 4;
    public static final int UEFI_ACTION_BOTTOM_MARGIN = 26;
    public static final int UEFI_BOOT_BTN_X = 8;
    public static final int UEFI_BOOT_BTN_W = 64;
    public static final int UEFI_INSTALL_BTN_X = 80;
    public static final int UEFI_INSTALL_BTN_W = 100;
    public static final int UEFI_ACTION_BTN_H = 18;
    public static final int UEFI_ACTION_LABEL_DY = 5;
    public static final int UEFI_FOOT_H = 16;
    public static final int UEFI_FOOT_TEXT_DY = 12;

    private FirmwareLayout() {
    }

    public static int biosContentH() {
        return HEIGHT - BIOS_CONTENT_TOP - BIOS_CONTENT_BOTTOM_MARGIN;
    }

    public static int biosHelpX() {
        return BIOS_BOX_X + BIOS_BOX_W + BIOS_HELP_GAP;
    }

    public static int biosHelpW() {
        return WIDTH - BIOS_BOX_W - BIOS_HELP_WIDTH_MARGIN;
    }

    public static int uefiMainX() {
        return UEFI_NAV_X + UEFI_NAV_W + UEFI_NAV_GAP;
    }

    public static int uefiMainW() {
        return WIDTH - UEFI_NAV_W - UEFI_MAIN_WIDTH_MARGIN;
    }

    public static int uefiPanelH() {
        return HEIGHT - UEFI_CONTENT_TOP - UEFI_PANEL_BOTTOM_MARGIN;
    }

    /** The green CLI setup: the content box the tabs open onto, clear of the hint line under it. */
    public static GuiLayout cliLayout() {
        final GuiLayout l = new GuiLayout(WIDTH, HEIGHT);
        final int contentTop = CLI_TITLE_Y + CLI_TABS_DY + CLI_CONTENT_DY;
        l.box("content", CLI_MARGIN, contentTop, WIDTH - 2 * CLI_MARGIN,
                HEIGHT - CLI_HINT_BOTTOM_MARGIN - contentTop);
        return l;
    }

    /** The blue setup: the title bar, the content box and the help box beside it, and the foot bar. */
    public static GuiLayout biosLayout() {
        final GuiLayout l = new GuiLayout(WIDTH, HEIGHT);
        l.box("titleBar", 0, 0, WIDTH, BIOS_TITLE_BAR_H);
        l.box("content", BIOS_BOX_X, BIOS_CONTENT_TOP, BIOS_BOX_W, biosContentH());
        l.box("help", biosHelpX(), BIOS_CONTENT_TOP, biosHelpW(), biosContentH());
        l.box("footBar", 0, HEIGHT - BIOS_FOOT_H, WIDTH, BIOS_FOOT_H);
        return l;
    }

    /** The UEFI manager: the head bar, the side navigation and the main panel beside it, and the foot bar. */
    public static GuiLayout uefiLayout() {
        final GuiLayout l = new GuiLayout(WIDTH, HEIGHT);
        l.box("head", 0, 0, WIDTH, UEFI_HEAD_H + UEFI_ACCENT_H);
        l.box("nav", UEFI_NAV_X, UEFI_CONTENT_TOP, UEFI_NAV_W, uefiPanelH());
        l.box("main", uefiMainX(), UEFI_CONTENT_TOP, uefiMainW(), uefiPanelH());
        l.box("footBar", 0, HEIGHT - UEFI_FOOT_H, WIDTH, UEFI_FOOT_H);
        return l;
    }
}
