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
 * Geometry of the Network Gateway's own screen, shared by its menu (where the slots go) and its screen
 * (what it draws around them). A pure layout so a unit test can prove nothing overlaps or overflows.
 *
 * <p>Top to bottom: the title with the two link lights at its right, three status lines at the small font
 * (the name and host, how ComputerCraft sees it, where it is managed), the item buffer under its caption,
 * then the player's inventory under its label. Nothing here is configured: the buffer is what needs hands.
 */
public final class NetworkGatewayLayout {

    public static final int WIDTH = 176;
    public static final int HEIGHT = 170;

    public static final int SLOT = 18;
    public static final int TITLE_X = 8;
    public static final int TITLE_Y = 6;

    /** The two lights at the right of the title: our link, then ComputerCraft's, each with a short label. */
    public static final int LED = 5;
    public static final int LED_Y = 7;
    public static final int LED_JS_X = 134;
    public static final int LED_JS_LABEL_X = 141;
    public static final int LED_CC_X = 158;
    public static final int LED_CC_LABEL_X = 165;

    /** The three status lines at the small font. */
    public static final int INFO_X = 8;
    public static final int INFO_Y = 17;
    public static final int INFO_W = WIDTH - INFO_X * 2;
    public static final int LINE_H = 9;
    public static final float INFO_SCALE = 0.75f;
    public static final int INFO_CHARS = 35;

    /** The buffer's caption, its hint at the right, and the nine slots. */
    public static final int BUFFER_CAPTION_Y = 47;
    public static final int BUFFER_HINT_X = 92;
    public static final int BUFFER_X = 8;
    public static final int BUFFER_Y = 57;
    public static final int BUFFER_SLOTS = 9;

    /** The player's inventory under its label. */
    public static final int INV_X = 8;
    public static final int INV_LABEL_Y = 79;
    public static final int INV_Y = 88;

    private NetworkGatewayLayout() {
    }

    /** The x of buffer slot {@code slot}'s frame. */
    public static int bufferX(final int slot) {
        return BUFFER_X + slot * SLOT;
    }

    public static GuiLayout layout() {
        final GuiLayout layout = new GuiLayout(WIDTH, HEIGHT);
        layout.text("title", TITLE_X, TITLE_Y, 15, 1.0f);
        layout.box("ledJs", LED_JS_X, LED_Y, LED, LED);
        layout.text("ledJsLabel", LED_JS_LABEL_X, TITLE_Y, 3, INFO_SCALE);
        layout.box("ledCc", LED_CC_X, LED_Y, LED, LED);
        layout.text("ledCcLabel", LED_CC_LABEL_X, TITLE_Y, 2, INFO_SCALE);
        layout.text("nameLine", INFO_X, INFO_Y, INFO_CHARS, INFO_SCALE);
        layout.text("ccLine", INFO_X, INFO_Y + LINE_H, INFO_CHARS, INFO_SCALE);
        layout.text("managedLine", INFO_X, INFO_Y + LINE_H * 2, INFO_CHARS, INFO_SCALE);
        layout.text("bufferCaption", BUFFER_X, BUFFER_CAPTION_Y, 11, INFO_SCALE);
        layout.text("bufferHint", BUFFER_HINT_X, BUFFER_CAPTION_Y, 18, INFO_SCALE);
        for (int i = 0; i < BUFFER_SLOTS; i++) {
            layout.box("buffer" + i, bufferX(i), BUFFER_Y, SLOT, SLOT);
        }
        layout.text("inventoryLabel", INV_X, INV_LABEL_Y, 9, 1.0f);
        layout.playerInventory(INV_X, INV_Y);
        return layout;
    }
}
