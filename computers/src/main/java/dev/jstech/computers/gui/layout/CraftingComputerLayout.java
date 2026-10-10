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
 * Pure layout for the Crafting Computer assembly screen: every drawn element's position and size, with
 * no Minecraft dependency, so {@link #layout()} can be unit-tested without the game running.
 *
 * <p>Both {@code CraftingComputerMenu} (which places the real slots) and {@code CraftingComputerScreen}
 * (which draws the frames) consume these constants, one source of truth so moving any element or adding
 * a new control updates the layout, the test, and the in-game screen all at once.
 *
 * <p>Hardware slots occupy the left half (x up to ~116); the right column starts at {@link #COL_R}.
 * A vertical separator at {@code COL_R - 5} divides the two halves. The assembly surface is hardware
 * only: recipe files are managed by the Crafting Manager program on the linked monitor, not here.
 *
 * <p>All coordinates are relative to the panel's top-left corner ({@code leftPos}/{@code topPos}).
 */
public final class CraftingComputerLayout {

    // Panel dimensions

    public static final int WIDTH = 244;

    /**
     * Panel height. The hardware block and the right-column controls end by y=135 (the Auto button);
     * the player inventory starts at y={@link #INV_Y}=138 and its hotbar bottom is 138+58+18=214,
     * which fits inside this height.
     */
    public static final int HEIGHT = 216;

    // Header bar

    public static final int HEADER_X = 6;
    public static final int HEADER_Y = 6;
    public static final int HEADER_W = 232;

    // Left column: hardware slots

    public static final int SLOT = 18;

    // Motherboard (left edge) and PSU share the x=8 column.
    public static final int MOBO_X = 8;
    public static final int MOBO_Y = 40;

    public static final int PSU_X = 8;
    public static final int PSU_Y = 73;

    // CPU, RAM, PCIe start at x=44 and step right by SLOT.
    public static final int RIGHT_X = 44;

    public static final int CPU_Y = 40;   // one slot

    public static final int RAM_Y = 73;   // up to RAM_SLOTS = 4
    public static final int RAM_SLOTS = 4;

    public static final int PCIE_Y = 106; // up to PCIE_SLOTS = 4
    public static final int PCIE_SLOTS = 4;

    // Disk slots share the left column (x=8) at the same y as PCIe.
    public static final int DISK_Y = 106; // up to DISK_SLOTS = 2
    public static final int DISK_SLOTS = 2;

    // Vertical separator

    public static final int VLINE_X = 121;  // COL_R - 5
    public static final int VLINE_Y = 24;
    public static final int VLINE_H = 108;

    // Right column: metrics tiles and control buttons

    public static final int COL_R = 126;
    public static final int COL_R_W = 110;

    public static final int TILE_H = 20;
    public static final int TILE_Y0 = 27;  // CAPACITY
    public static final int TILE_Y1 = 49;  // CRAFTING
    public static final int TILE_Y2 = 71;  // RECIPE ROM

    public static final int BTN_H = 14;
    public static final int POWER_X = COL_R;
    public static final int POWER_Y = 105;
    public static final int AUTO_X = COL_R;
    public static final int AUTO_Y = 121;

    // Player inventory: starts below the hardware block and the control buttons.

    /**
     * Y of the player inventory top row. The Auto button (the lowest right-column control) ends at
     * AUTO_Y + BTN_H = 135, so the inventory starts at 138 with a small clearance.
     */
    public static final int INV_X = 8;
    public static final int INV_Y = 138;
    public static final int HOTBAR_GAP = 58;

    /*
     * The case's left side, off or on: a small button beside the inventory, under the end of the right column, its
     * caption written small over its state. Its widest words (the Portuguese ones) are seven letters.
     */
    public static final int SIDE_X = 176;
    public static final int SIDE_Y = INV_Y;
    public static final int SIDE_W = 60;
    public static final int SIDE_H = 20;
    public static final int SIDE_CHARS = 7;

    // Hardware caption rows, shared by the left column's labels and the layout's own text() entries.
    public static final int LABEL_ROW_1_Y = 27;
    public static final int LABEL_ROW_2_Y = 60;
    public static final int LABEL_ROW_3_Y = 93;
    public static final int NETWORK_Y = 95;

    /* The PSU health dot drawn next to its caption. */
    public static final int PSU_LED_X = 30;
    public static final int PSU_LED_Y = LABEL_ROW_2_Y + 1;
    public static final int PSU_LED_SIZE = 4;

    // Header rename field: the well it sits over and the EditBox itself.
    public static final int NAME_WELL_LEFT = 26;
    public static final int NAME_WELL_TOP = 7;
    public static final int NAME_WELL_RIGHT = 158;
    public static final int NAME_BOX_X = 28;
    public static final int NAME_BOX_Y = 8;
    public static final int NAME_BOX_W = 126;

    private CraftingComputerLayout() {
    }

    /**
     * Builds the full element layout for the worst case: all hardware slots populated. A clean result
     * here guarantees that the real screen's elements never overlap and nothing spills outside the panel.
     *
     * <p>{@code CraftingComputerMenu} and {@code CraftingComputerScreen} both consume the constants
     * above, so a clean layout here covers the real slot positions too.
     */
    public static GuiLayout layout() {
        final GuiLayout l = new GuiLayout(WIDTH, HEIGHT)
                // Left column: hardware slots (worst case: all slots present)
                .slot("mobo",  MOBO_X,              MOBO_Y)
                .slot("psu",   PSU_X,               PSU_Y)
                .slot("cpu",   RIGHT_X,             CPU_Y);

        for (int i = 0; i < RAM_SLOTS; i++) {
            l.slot("ram_" + i, RIGHT_X + i * SLOT, RAM_Y);
        }
        for (int i = 0; i < PCIE_SLOTS; i++) {
            l.slot("pcie_" + i, RIGHT_X + i * SLOT, PCIE_Y);
        }
        for (int i = 0; i < DISK_SLOTS; i++) {
            l.slot("disk_" + i, MOBO_X + i * SLOT, DISK_Y);
        }

        l
                // Right column: tiles and buttons
                .box("tileCapacity", COL_R, TILE_Y0, COL_R_W, TILE_H)
                .box("tileCraft",    COL_R, TILE_Y1, COL_R_W, TILE_H)
                .box("tileRom",      COL_R, TILE_Y2, COL_R_W, TILE_H)
                .box("btnPower",     POWER_X, POWER_Y, COL_R_W, BTN_H)
                .box("btnAuto",      AUTO_X,  AUTO_Y,  COL_R_W, BTN_H)
                .box("btnSide",      SIDE_X,  SIDE_Y,  SIDE_W,  SIDE_H);

        // Player inventory
        l.playerInventory(INV_X, INV_Y);

        // Text labels (captions, excluded from overlap check, only checked for bounds)
        l.text("titleCC",     12,                    11, 2, 1.0f);
        l.text("statusPill",  WIDTH - Math.round(7 * GuiLayout.GLYPH_WIDTH), 11, 7, 1.0f);  // "OFFLINE" = 7 chars
        l.text("lblBoard",     MOBO_X,               LABEL_ROW_1_Y, 5, 1.0f);  // "BOARD"
        l.text("lblCpu",       RIGHT_X,              LABEL_ROW_1_Y, 3, 1.0f);  // "CPU"
        l.text("lblPsu",       MOBO_X,               LABEL_ROW_2_Y, 3, 1.0f);  // "PSU"
        l.text("lblRam",       RIGHT_X,              LABEL_ROW_2_Y, 3, 1.0f);  // "RAM"
        l.text("lblDisk",      MOBO_X,               LABEL_ROW_3_Y, 4, 1.0f);  // "DISK"
        l.text("lblPcie",      RIGHT_X,              LABEL_ROW_3_Y, 4, 1.0f);  // "PCIE"
        l.text("lblNetwork",   COL_R,                NETWORK_Y,     7, 1.0f);  // "NETWORK"
        l.text("lblSide",      SIDE_X + 3,           SIDE_Y + 3,    SIDE_CHARS, 0.75f);  // "SIDE"
        l.text("valSide",      SIDE_X + 3,           SIDE_Y + 11,   SIDE_CHARS, 0.75f);  // "CLOSED"

        return l;
    }
}
