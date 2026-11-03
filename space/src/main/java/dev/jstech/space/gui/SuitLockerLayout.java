/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.gui;

import dev.jstech.core.gui.layout.GuiLayout;
import java.util.List;

/**
 * Where the suit locker's screen puts its things, as the approved one draws it, in the screen's own pixels: the open
 * locker between the airlock's walls, the four bottles' cradles on its shelf, the helmet, the suit, the trousers and
 * the boots down its rack in the body's order, the kit box at its foot, and the player's inventory under it. The
 * screen draws them here and the menu puts its slots here, so the two always agree. Pure, so it is tested without the
 * game.
 */
public final class SuitLockerLayout {

    /** The screen's size. */
    public static final int WIDTH = 176;
    public static final int HEIGHT = 218;
    /** Where its title is written, and the inventory's. */
    public static final int TITLE_X = 8;
    public static final int TITLE_Y = 5;
    public static final int INVENTORY_LABEL_Y = 124;
    /** Where the player's inventory's grid starts, its hotbar under it as the game lays it. */
    public static final int INVENTORY_X = 8;
    public static final int INVENTORY_Y = 136;
    /**
     * Each of the locker's places, in its order, the top-left corner of what is in it: the helmet, the suit, the
     * trousers and the boots down the rack; the four bottles along the shelf; the kit box.
     */
    public static final List<int[]> PLACES = List.of(new int[] {80, 42}, new int[] {80, 61}, new int[] {80, 80},
            new int[] {80, 99}, new int[] {50, 19}, new int[] {70, 19}, new int[] {90, 19}, new int[] {110, 19},
            new int[] {110, 99});
    /* How wide the title and the inventory's label are, in characters, at most. */
    private static final int TITLE_CHARS = 18;
    private static final int LABEL_CHARS = 12;

    private SuitLockerLayout() {
    }

    /** The screen's layout: its places, its labels and the player's inventory. */
    public static GuiLayout layout() {
        final GuiLayout layout = new GuiLayout(WIDTH, HEIGHT);
        layout.text("title", TITLE_X, TITLE_Y, TITLE_CHARS, 1.0F);
        layout.text("inventory", TITLE_X, INVENTORY_LABEL_Y, LABEL_CHARS, 1.0F);
        layout.playerInventory(INVENTORY_X, INVENTORY_Y);
        for (int k = 0; k < PLACES.size(); k++) {
            layout.slot("place" + k, PLACES.get(k)[0], PLACES.get(k)[1]);
        }
        return layout;
    }
}
