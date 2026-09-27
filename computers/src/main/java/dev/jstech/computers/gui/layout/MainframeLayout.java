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
 * Positions of the Mainframe's assembly screen: 24 hardware slots on the left (motherboard, PSU, four
 * CPUs, eight RAM in a 4-wide grid, six GPUs in a 3-wide grid, four disks in a 2-wide grid), the capacity,
 * queues, RAM buffer and operations tiles on the right, and the power/autostart/failover row under them.
 * The Menu places the slots from here and the Screen draws from here, so the two can never disagree.
 */
public final class MainframeLayout {

    public static final int WIDTH = 244;
    public static final int HEIGHT = 262;

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
    public static final int CPU_SLOTS = 4;
    public static final int RAM_Y = 73;
    public static final int RAM_SLOTS = 8;
    public static final int RAM_COLUMNS = 4;
    public static final int GPU_Y = 124;
    public static final int GPU_SLOTS = 6;
    public static final int GPU_COLUMNS = 3;
    public static final int DISK_Y = 124;
    public static final int DISK_SLOTS = 4;
    public static final int DISK_COLUMNS = 2;

    public static final int VLINE_X = 121;
    public static final int VLINE_Y = 24;
    public static final int VLINE_H = 134;

    public static final int COL_R = 126;
    public static final int COL_R_W = 110;
    public static final int TILE_Y_CAPACITY = 27;
    public static final int TILE_H_CAPACITY = 22;
    public static final int TILE_Y_QUEUES = 52;
    public static final int TILE_H_QUEUES = 18;
    public static final int TILE_Y_RAM_BUFFER = 73;
    public static final int TILE_H_RAM_BUFFER = 18;
    public static final int TILE_Y_OPERATIONS = 108;
    public static final int TILE_H_OPERATIONS = 42;

    public static final int BTN_Y = 162;
    public static final int BTN_H = 14;
    public static final int BTN_W = 54;
    public static final int POWER_X = 8;
    public static final int AUTO_X = 66;
    public static final int FAILOVER_X = 124;

    public static final int INV_X = 8;
    public static final int INV_Y = 182;
    public static final int HOTBAR_GAP = 58;

    // Hardware caption rows, shared by the left column's labels and the layout's own text() entries.
    public static final int LABEL_ROW_1_Y = 27;
    public static final int LABEL_ROW_2_Y = 60;
    public static final int LABEL_ROW_3_Y = 111;
    public static final int NETWORK_Y = 96;

    /* The PSU health dot drawn next to its caption. */
    public static final int PSU_LED_X = 30;
    public static final int PSU_LED_Y = LABEL_ROW_2_Y + 1;
    public static final int PSU_LED_SIZE = 4;

    // Operations dispatch: one row per metric, inside the operations tile.
    public static final int OPS_ROW_Y0 = TILE_Y_OPERATIONS + 5;
    public static final int OPS_ROW_PITCH = 11;
    public static final int TILE_INSET = 4;

    private MainframeLayout() {
    }

    /**
     * Builds the full element layout for the worst case: every hardware slot present. A clean result
     * here guarantees the real screen's elements never overlap and nothing spills outside the panel.
     */
    public static GuiLayout layout() {
        final GuiLayout l = new GuiLayout(WIDTH, HEIGHT)
                .slot("mobo", MOBO_X, MOBO_Y)
                .slot("psu", PSU_X, PSU_Y);

        for (int i = 0; i < CPU_SLOTS; i++) {
            l.slot("cpu_" + i, RIGHT_X + i * SLOT, CPU_Y);
        }
        for (int i = 0; i < RAM_SLOTS; i++) {
            l.slot("ram_" + i, RIGHT_X + (i % RAM_COLUMNS) * SLOT, RAM_Y + (i / RAM_COLUMNS) * SLOT);
        }
        for (int i = 0; i < GPU_SLOTS; i++) {
            l.slot("gpu_" + i, RIGHT_X + (i % GPU_COLUMNS) * SLOT, GPU_Y + (i / GPU_COLUMNS) * SLOT);
        }
        for (int i = 0; i < DISK_SLOTS; i++) {
            l.slot("disk_" + i, MOBO_X + (i % DISK_COLUMNS) * SLOT, DISK_Y + (i / DISK_COLUMNS) * SLOT);
        }

        l.box("tileCapacity", COL_R, TILE_Y_CAPACITY, COL_R_W, TILE_H_CAPACITY)
                .box("tileQueues", COL_R, TILE_Y_QUEUES, COL_R_W, TILE_H_QUEUES)
                .box("tileRamBuffer", COL_R, TILE_Y_RAM_BUFFER, COL_R_W, TILE_H_RAM_BUFFER)
                .box("tileOperations", COL_R, TILE_Y_OPERATIONS, COL_R_W, TILE_H_OPERATIONS)
                .box("btnPower", POWER_X, BTN_Y, BTN_W, BTN_H)
                .box("btnAuto", AUTO_X, BTN_Y, BTN_W, BTN_H)
                .box("btnFailover", FAILOVER_X, BTN_Y, BTN_W, BTN_H);

        l.playerInventory(INV_X, INV_Y);

        l.text("titleMainframe", 12, 11, 9, 1.0f);
        l.text("statusPill", WIDTH - 6 * 8, 11, 8, 1.0f);
        l.text("lblBoard", MOBO_X, LABEL_ROW_1_Y, 5, 1.0f);
        l.text("lblCpu", RIGHT_X, LABEL_ROW_1_Y, 3, 1.0f);
        l.text("lblPsu", MOBO_X, LABEL_ROW_2_Y, 3, 1.0f);
        l.text("lblRam", RIGHT_X, LABEL_ROW_2_Y, 3, 1.0f);
        l.text("lblDisk", MOBO_X, LABEL_ROW_3_Y, 4, 1.0f);
        l.text("lblGpu", RIGHT_X, LABEL_ROW_3_Y, 3, 1.0f);
        l.text("lblNetwork", COL_R, NETWORK_Y, 7, 1.0f);

        return l;
    }
}
