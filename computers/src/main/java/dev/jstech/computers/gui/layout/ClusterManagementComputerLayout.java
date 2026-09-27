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
 * Positions of the Cluster Management Computer's assembly screen: the same mould as the Crafting
 * Computer's hardware surface, with four tiles on the right (capacity, network, clusters in reach,
 * management) and the power button under them. The Menu places the slots from here and the Screen
 * draws from here, so the two can never disagree.
 */
public final class ClusterManagementComputerLayout {

    public static final int WIDTH = 244;
    public static final int HEIGHT = 216;

    public static final int HEADER_X = 6;
    public static final int HEADER_Y = 6;
    public static final int HEADER_W = 232;

    public static final int SLOT = 18;
    public static final int MOBO_X = 8;
    public static final int MOBO_Y = 40;
    public static final int PSU_X = 8;
    public static final int PSU_Y = 73;
    public static final int RIGHT_X = 44;
    public static final int CPU_Y = 40;
    public static final int RAM_Y = 73;
    public static final int RAM_SLOTS = 4;
    public static final int PCIE_Y = 106;
    public static final int PCIE_SLOTS = 4;
    public static final int DISK_Y = 106;
    public static final int DISK_SLOTS = 2;

    public static final int VLINE_X = 121;
    public static final int VLINE_Y = 24;
    public static final int VLINE_H = 108;

    public static final int COL_R = 126;
    public static final int COL_R_W = 110;
    public static final int TILE_H = 17;
    public static final int TILE_Y0 = 27;   // CAPACITY
    public static final int TILE_Y1 = 46;   // NETWORK
    public static final int TILE_Y2 = 65;   // CLUSTERS IN REACH
    public static final int TILE_Y3 = 84;   // MANAGEMENT
    public static final int BTN_H = 14;
    public static final int POWER_X = COL_R;
    public static final int POWER_Y = 105;
    public static final int AUTO_X = COL_R;
    public static final int AUTO_Y = 121;

    public static final int INV_X = 8;
    public static final int INV_Y = 138;
    public static final int HOTBAR_GAP = 58;

    // Hardware caption rows, shared by the left column's labels and the layout's own text() entries.
    public static final int LABEL_ROW_1_Y = 27;
    public static final int LABEL_ROW_2_Y = 60;
    public static final int LABEL_ROW_3_Y = 93;

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

    private ClusterManagementComputerLayout() {
    }

    public static GuiLayout layout() {
        final GuiLayout l = new GuiLayout(WIDTH, HEIGHT)
                .slot("mobo", MOBO_X, MOBO_Y)
                .slot("psu", PSU_X, PSU_Y)
                .slot("cpu", RIGHT_X, CPU_Y);
        for (int i = 0; i < RAM_SLOTS; i++) {
            l.slot("ram_" + i, RIGHT_X + i * SLOT, RAM_Y);
        }
        for (int i = 0; i < PCIE_SLOTS; i++) {
            l.slot("pcie_" + i, RIGHT_X + i * SLOT, PCIE_Y);
        }
        for (int i = 0; i < DISK_SLOTS; i++) {
            l.slot("disk_" + i, MOBO_X + i * SLOT, DISK_Y);
        }
        l.box("tileCapacity", COL_R, TILE_Y0, COL_R_W, TILE_H)
                .box("tileNetwork", COL_R, TILE_Y1, COL_R_W, TILE_H)
                .box("tileClusters", COL_R, TILE_Y2, COL_R_W, TILE_H)
                .box("tileManagement", COL_R, TILE_Y3, COL_R_W, TILE_H)
                .box("btnPower", POWER_X, POWER_Y, COL_R_W, BTN_H)
                .box("btnAuto", AUTO_X, AUTO_Y, COL_R_W, BTN_H);
        l.playerInventory(INV_X, INV_Y);
        l.text("titleCmc", 12, 11, 3, 1.0f);
        l.text("statusPill", WIDTH - 6 * 7, 11, 7, 1.0f);
        l.text("lblBoard", MOBO_X, LABEL_ROW_1_Y, 5, 1.0f);
        l.text("lblCpu", RIGHT_X, LABEL_ROW_1_Y, 3, 1.0f);
        l.text("lblPsu", MOBO_X, LABEL_ROW_2_Y, 3, 1.0f);
        l.text("lblRam", RIGHT_X, LABEL_ROW_2_Y, 3, 1.0f);
        l.text("lblDisk", MOBO_X, LABEL_ROW_3_Y, 4, 1.0f);
        l.text("lblPcie", RIGHT_X, LABEL_ROW_3_Y, 4, 1.0f);
        return l;
    }
}
