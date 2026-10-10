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
 * Positions of the Server's assembly screen: the spec tiles and the power-headroom track across the top,
 * the problems strip below them, then the board, PSU, CPU, RAM and GPU bays (no disk bays: a server's
 * drives live in the rack's hotswap slots, not in the chassis). The Menu places the slots from here and
 * the Screen draws from here, so the two can never disagree.
 */
public final class ServerAssemblyLayout {

    public static final int WIDTH = 244;
    public static final int HEIGHT = 294;

    public static final int HEADER_X = 6;
    public static final int HEADER_Y = 6;
    public static final int HEADER_W = 232;

    public static final int TILE_X_ORCHESTRATION = 8;
    public static final int TILE_X_QUEUES = 67;
    public static final int TILE_X_RAM = 126;
    public static final int TILE_X_DRAW = 185;
    public static final int TILE_W = 55;
    public static final int TILE_W_LAST = 51;
    public static final int TILE_Y = 26;
    public static final int TILE_H = 18;

    public static final int TRACK_X = 52;
    public static final int TRACK_Y = 49;
    public static final int TRACK_W = 130;

    public static final int PROBLEMS_X = 8;
    public static final int PROBLEMS_Y = 68;
    public static final int PROBLEMS_W = 228;
    public static final int PROBLEMS_H = 12;

    public static final int SLOT = 18;
    public static final int MOBO_X = 8;
    public static final int MOBO_Y = 96;
    public static final int PSU_X = 26;
    public static final int PSU_Y = 96;
    public static final int RIGHT_X = 52;
    public static final int CPU_Y = 96;
    public static final int CPU_SLOTS = 4;
    public static final int RAM_Y = 126;
    public static final int RAM_SLOTS = 8;
    public static final int RAM_COLUMNS = 4;
    public static final int GPU_Y = 174;
    public static final int GPU_SLOTS = 6;
    public static final int GPU_COLUMNS = 3;

    public static final int INV_X = 8;
    public static final int INV_Y = 214;
    public static final int HOTBAR_GAP = 58;

    // Header rename field: the well it sits over and the EditBox itself.
    public static final int NAME_WELL_LEFT = 50;
    public static final int NAME_WELL_TOP = 7;
    public static final int NAME_WELL_RIGHT = 158;
    public static final int NAME_BOX_X = 52;
    public static final int NAME_BOX_Y = 8;
    public static final int NAME_BOX_W = 104;

    // Power and storage caption rows, above the problems strip.
    public static final int STAT_ROW_POWER_Y = 49;
    public static final int STAT_ROW_STORAGE_Y = 59;
    public static final int STAT_LABEL_X = PROBLEMS_X;
    public static final int STAT_VALUE_X = PROBLEMS_X + PROBLEMS_W;

    // The problems strip's own text, inset from the strip it is drawn over.
    public static final int PROBLEMS_TEXT_X = PROBLEMS_X + 4;
    public static final int PROBLEMS_TEXT_Y = PROBLEMS_Y + 3;
    public static final int PROBLEMS_MORE_X = PROBLEMS_X + PROBLEMS_W - 2;

    // Hardware bay caption rows, shared by the bay labels and the layout's own text() entries.
    public static final int LABEL_ROW_1_Y = 87;
    public static final int LABEL_ROW_2_Y = 117;
    public static final int LABEL_ROW_3_Y = 165;

    private ServerAssemblyLayout() {
    }

    /**
     * Builds the full element layout for the worst case: every hardware slot present. A clean result
     * here guarantees the real screen's elements never overlap and nothing spills outside the panel.
     */
    public static GuiLayout layout() {
        final GuiLayout l = new GuiLayout(WIDTH, HEIGHT)
                .box("tileOrchestration", TILE_X_ORCHESTRATION, TILE_Y, TILE_W, TILE_H)
                .box("tileQueues", TILE_X_QUEUES, TILE_Y, TILE_W, TILE_H)
                .box("tileRamBuffer", TILE_X_RAM, TILE_Y, TILE_W, TILE_H)
                .box("tileDraw", TILE_X_DRAW, TILE_Y, TILE_W_LAST, TILE_H)
                .box("track", TRACK_X, TRACK_Y, TRACK_W, 8)
                .box("problems", PROBLEMS_X, PROBLEMS_Y, PROBLEMS_W, PROBLEMS_H)
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

        l.playerInventory(INV_X, INV_Y);

        l.text("titleServer", 12, 11, 6, 1.0f);
        l.text("statusPill", WIDTH - Math.round(6 * GuiLayout.GLYPH_WIDTH), 11, 6, 1.0f);
        l.text("lblBoard", MOBO_X, LABEL_ROW_1_Y, 5, 1.0f);
        l.text("lblCpu", RIGHT_X, LABEL_ROW_1_Y, 3, 1.0f);
        l.text("lblRam", RIGHT_X, LABEL_ROW_2_Y, 3, 1.0f);
        l.text("lblGpu", RIGHT_X, LABEL_ROW_3_Y, 3, 1.0f);

        return l;
    }
}
