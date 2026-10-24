/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.crc;

import dev.jstech.core.gui.layout.GuiLayout;
import dev.jstech.core.machine.MachineScreenLayout;

/**
 * Where everything sits on the Computational Research Centre's screen, in the GUI's pixels from its top left, as the
 * approved design draws it: six tabs on top (Processors, Research, Group, then Sides, Redstone and Protection), a
 * window wider than every other machine's, and the player's inventory centred below it on the tabs that take items.
 * Processors holds the three sections of places and the data; Research and Group hold no items, so their face takes
 * the whole window. Pure: the screen draws from these, the menu places its slots by them, and a test checks they keep
 * apart.
 */
public final class CrcLayout {

    /** The whole screen. */
    public static final int WIDTH = 330;
    public static final int HEIGHT = 257;
    /** The tabs along the top, as every machine's are drawn: its own three, then three of every machine's. */
    public static final int TABS = 6;
    /** Its own tabs after its Main tab, Processors, in the menu's numbering. */
    public static final int RESEARCH_TAB = MachineScreenLayout.TABS;
    public static final int GROUP_TAB = MachineScreenLayout.TABS + 1;
    /** The window the face is set in: short over the inventory, or the whole screen. */
    public static final int WINDOW_TOP = 27;
    public static final int WINDOW_HEIGHT = 126;
    /** The enamel face, inside the window: short over the inventory, tall on the tabs without one. */
    public static final int FACE_X = 7;
    public static final int FACE_Y = 34;
    public static final int FACE_WIDTH = 316;
    public static final int FACE_HEIGHT = 112;
    public static final int TALL_FACE_HEIGHT = HEIGHT - FACE_X - FACE_Y;
    /** The player's inventory, centred under the window. */
    public static final int INVENTORY_WIDTH = 176;
    public static final int INVENTORY_HEIGHT = 100;
    public static final int INVENTORY_X = (WIDTH - INVENTORY_WIDTH) / 2;
    public static final int INVENTORY_Y = 157;
    /** A slot's side, frame included. */
    public static final int SLOT = 18;
    /** The face's small text, the game's font at three quarters: its height, and its line's. */
    public static final int SMALL_TEXT = 6;
    public static final int LINE = 7;

    /** The plates every tab of its own shows on top, the second just after the first. */
    public static final int PLATE_X = 13;
    public static final int PLATE_Y = 40;
    public static final int PLATE_HEIGHT = 12;
    public static final int PLATE_GAP = 4;
    /** A plate's room for its words: the plate is 16 wider. */
    public static final int PLATE_PADDING = 16;
    public static final int NAME_PLATE_MOST = 144;
    public static final int CLASS_PLATE_MOST = 40;
    /** The register of small bulbs after the plates, two rows that flicker as it computes. */
    public static final int REGISTER_GAP = 9;
    public static final int REGISTER_Y = 41;
    public static final int REGISTER_COLUMNS = 15;
    public static final int REGISTER_ROWS = 2;
    public static final int REGISTER_PITCH = 5;
    /** The lamps, RUN, WAIT and HOT, one under the other on Processors, their words beside them. */
    public static final int LAMP_X = 290;
    public static final int LAMP_Y = 40;
    public static final int LAMP_PITCH = 11;
    public static final int LAMP_SIZE = 7;
    public static final int LAMP_WORD_GAP = 11;
    public static final int LAMP_WORD_ROOM = 20;

    /** The three sections of places, a bay of three by three slots each. */
    public static final int SECTIONS = 3;
    public static final int SECTION_SIDE = 3;
    public static final int SECTION_X = 13;
    public static final int SECTION_PITCH = 64;
    public static final int BAY_Y = 62;
    public static final int SECTION_LABEL_Y = 54;
    /** The frame round a bay's slots. */
    public static final int FRAME = 3;
    public static final int BAY = SECTION_SIDE * SLOT + 2 * FRAME;
    /** The data, two by two: sample, crystal, spectral, particle. */
    public static final int DATA_X = 205;
    public static final int DATA_SIDE = 2;
    public static final int DATA_BAY = DATA_SIDE * SLOT + 2 * FRAME;
    public static final int DATA_LABEL_X = 226;
    /** The lamps of the two ways its heat leaves by liquid and by pipe, under the data. */
    public static final int COOLANT_LAMP_X = 206;
    public static final int COOLANT_LAMP_Y = 107;
    public static final int HEAT_PORT_LAMP_Y = 117;
    public static final int COOLING_WORD_ROOM = 41;
    /** The readouts under the bays: research, draw and shed, each a word, a counter and its unit. */
    public static final int READOUT_Y = 127;
    public static final int READOUT_WORD_Y = 129;
    public static final int[] READOUT_WORD_X = {13, 90, 150};
    public static final int[] READOUT_COUNTER_X = {52, 111, 171};
    public static final int[] READOUT_UNIT_X = {72, 131, 191};
    public static final int READOUT_DIGITS = 3;
    /** A counter's wheel, and its height. */
    public static final int DIGIT = 6;
    public static final int COUNTER_HEIGHT = 9;
    /** The thermometer, 0 to 120 degrees, its scale's words beside it. */
    public static final int THERMOMETER_X = 264;
    public static final int THERMOMETER_TOP = 60;
    public static final int THERMOMETER_BOTTOM = 128;
    public static final int THERMOMETER_MOST = 120;
    public static final int SCALE_X = THERMOMETER_X + 11;
    /** Its switch, its words beside it. */
    public static final int ROCKER_X = 289;
    public static final int ROCKER_Y = 84;
    public static final int ROCKER_WIDTH = 14;
    public static final int ROCKER_HEIGHT = 22;
    public static final int ROCKER_WORD_X = ROCKER_X + 17;

    /** The Research and Group tabs' lamps: RUN, then on Research HOT, each word after its lamp. */
    public static final int TOP_LAMP_X = 270;
    public static final int TOP_HOT_X = 297;
    public static final int TOP_LAMP_Y = 41;
    public static final int TOP_RUN_ROOM = 14;
    public static final int TOP_HOT_ROOM = 14;
    /** The first plate of the Research and Group tabs at its widest; the second runs to the lamps. */
    public static final int TOP_PLATE_MOST = 90;
    public static final int TOP_PLATES_END = TOP_LAMP_X - 6;
    /** The chart of discoveries by the year they were made, on squared paper in its bezel. */
    public static final int CHART_LEFT = 16;
    public static final int CHART_TOP = 61;
    public static final int CHART_RIGHT = 214;
    public static final int CHART_BOTTOM = 199;
    public static final int LANE_TOP = 71;
    public static final int LANE_HEIGHT = 25;
    public static final int LANES_SHOWN = 4;
    public static final int AXIS_Y = LANE_TOP + LANES_SHOWN * LANE_HEIGHT + 2;
    public static final int YEAR_LEFT = 26;
    public static final int YEAR_RIGHT = 196;
    public static final int FUTURE_X = 207;
    public static final int FIRST_YEAR = 1900;
    public static final int LAST_YEAR = 2010;
    public static final int LEGEND_Y = CHART_BOTTOM - 7;
    /** The card of the discovery picked on the chart, its buttons at its foot. */
    public static final int CARD_LEFT = 220;
    public static final int CARD_TOP = 58;
    public static final int CARD_RIGHT = 317;
    public static final int CARD_BOTTOM = 217;
    public static final int INDEX_TOP = 69;
    public static final int INDEX_BOTTOM = 105;
    public static final int CARD_FLOW_TOP = 109;
    public static final int CARD_FLOW_BOTTOM = 170;
    public static final int COMPUTING_Y = 172;
    public static final int BUTTON_Y = 202;
    public static final int BUTTON_HEIGHT = 13;
    public static final int QUEUE_BUTTON_X = CARD_LEFT + 3;
    public static final int QUEUE_BUTTON_WIDTH = 38;
    public static final int RESEARCH_BUTTON_X = CARD_LEFT + 44;
    public static final int RESEARCH_BUTTON_WIDTH = CARD_RIGHT - CARD_LEFT - 47;
    /** What runs now under the chart: its plate, ten lamps of its progress and its counter. */
    public static final int NOW_WORD_Y = 205;
    public static final int NOW_PLATE_Y = 213;
    public static final int NOW_PLATE_MOST = 88;
    public static final int PROGRESS_LAMPS = 10;
    public static final int PROGRESS_PITCH = 9;
    public static final int STATUS_Y = 230;
    public static final int STATUS_PITCH = 9;
    public static final int STATUS_ROOM = CARD_LEFT - 3 - PLATE_X;
    /** The queue under the card. */
    public static final int QUEUE_WORD_Y = 221;
    public static final int QUEUE_Y = 229;
    public static final int QUEUE_SHOWN = 3;

    /** The Group tab's drawing of the CRCs on their cable. */
    public static final int SCHEMATIC_LEFT = 13;
    public static final int SCHEMATIC_TOP = 56;
    public static final int SCHEMATIC_RIGHT = 317;
    public static final int SCHEMATIC_BOTTOM = 104;
    public static final int CABINET_Y = 74;
    public static final int CABINET_WIDTH = 39;
    public static final int CABINET_HEIGHT = 22;
    public static final int CABLE_Y = 86;
    public static final int CABINETS_SHOWN = 6;
    /** The table of what each CRC computes and how much its team counts. */
    public static final int TABLE_LEFT = 13;
    public static final int TABLE_TOP = 110;
    public static final int TABLE_RIGHT = 317;
    public static final int TABLE_BOTTOM = 186;
    public static final int HEAD_Y = 114;
    public static final int[] COLUMN_X = {17, 27, 82, 158, 200, 240, 283};
    public static final int ROW_Y = 126;
    public static final int ROW_PITCH = 11;
    public static final int ROWS_SHOWN = 3;
    public static final int TOTALS_RULE_Y = 160;
    public static final int TOTALS_Y = 164;
    public static final int TOTALS_PITCH = 10;
    public static final int TEAMS_SHOWN = 2;
    /** The rule of a group, engraved under the table. */
    public static final int[] RULE_Y = {193, 203, 212, 225, 234};

    private CrcLayout() {
    }

    /**
     * Where place {@code index} of the CRC's 27 sits, the item inside its frame: section after section from the left,
     * in each the rows from the top, in each row the columns from the left, as the formed model numbers them.
     */
    public static GuiLayout.SlotPosition place(final int index) {
        final int section = index / (SECTION_SIDE * SECTION_SIDE);
        final int row = index % (SECTION_SIDE * SECTION_SIDE) / SECTION_SIDE;
        final int column = index % SECTION_SIDE;
        return new GuiLayout.SlotPosition(sectionX(section) + FRAME + column * SLOT + 1,
                BAY_Y + FRAME + row * SLOT + 1);
    }

    /** Where data slot {@code index} (0 sample, 1 crystal, 2 spectral, 3 particle) sits, the item inside its frame. */
    public static GuiLayout.SlotPosition data(final int index) {
        return new GuiLayout.SlotPosition(DATA_X + FRAME + index % DATA_SIDE * SLOT + 1,
                BAY_Y + FRAME + index / DATA_SIDE * SLOT + 1);
    }

    /** The left of section {@code section}'s bay. */
    public static int sectionX(final int section) {
        return SECTION_X + section * SECTION_PITCH;
    }

    /** Where the player's inventory's first slot sits, the item inside its frame. */
    public static GuiLayout.SlotPosition playerInventory() {
        return MachineScreenLayout.playerInventory(WIDTH);
    }

    /** The left of tab {@code place} along the top. */
    public static int tabX(final int place) {
        return MachineScreenLayout.tabX(place);
    }

    /** The x on the chart of {@code year}, the years before and after its axis at its ends. */
    public static int yearX(final int year) {
        final int clamped = Math.clamp(year, FIRST_YEAR, LAST_YEAR);
        return YEAR_LEFT + (clamped - FIRST_YEAR) * (YEAR_RIGHT - YEAR_LEFT) / (LAST_YEAR - FIRST_YEAR);
    }

    /** The y of the middle of lane {@code lane} shown, from the top. */
    public static int laneY(final int lane) {
        return LANE_TOP + lane * LANE_HEIGHT + LANE_HEIGHT * 3 / 5;
    }

    /** The left of cabinet {@code index} of {@code count} on the Group tab's drawing, spread evenly across it. */
    public static int cabinetX(final int index, final int count) {
        final int room = SCHEMATIC_RIGHT - SCHEMATIC_LEFT - 6;
        final int each = room / Math.max(1, count);
        return SCHEMATIC_LEFT + 3 + index * each + (each - CABINET_WIDTH) / 2;
    }

    /** Whether ({@code x}, {@code y}) is from ({@code x0}, {@code y0}) to before ({@code x1}, {@code y1}). */
    public static boolean on(final double x, final double y, final int x0, final int y0, final int x1, final int y1) {
        return x >= x0 && x < x1 && y >= y0 && y < y1;
    }

    /** The Processors tab, its plates at their widest: the places, the data and what they read apart and inside. */
    public static GuiLayout processors() {
        final GuiLayout layout = new GuiLayout(WIDTH, HEIGHT);
        plates(layout, NAME_PLATE_MOST, CLASS_PLATE_MOST);
        final int register = PLATE_X + NAME_PLATE_MOST + PLATE_GAP + CLASS_PLATE_MOST + REGISTER_GAP;
        layout.box("register", register - 1, REGISTER_Y - 1, REGISTER_COLUMNS * REGISTER_PITCH,
                REGISTER_ROWS * REGISTER_PITCH + 1);
        lamps(layout, LAMP_X, LAMP_Y, 3, LAMP_WORD_ROOM);
        for (int section = 0; section < SECTIONS; section++) {
            layout.box("section " + section, sectionX(section), BAY_Y, BAY, BAY);
            layout.textBox("section-word-" + section, sectionX(section), SECTION_LABEL_Y, BAY, SMALL_TEXT);
        }
        layout.box("data", DATA_X, BAY_Y, DATA_BAY, DATA_BAY);
        layout.textBox("data-word", DATA_X, SECTION_LABEL_Y, DATA_BAY, SMALL_TEXT);
        for (final int y : new int[] {COOLANT_LAMP_Y, HEAT_PORT_LAMP_Y}) {
            layout.box("cooling-lamp-" + y, COOLANT_LAMP_X - 1, y - 1, LAMP_SIZE + 2, LAMP_SIZE + 2);
            layout.box("cooling-word-" + y, COOLANT_LAMP_X + LAMP_WORD_GAP, y + 1, COOLING_WORD_ROOM, SMALL_TEXT);
        }
        for (int i = 0; i < READOUT_WORD_X.length; i++) {
            layout.box("readout-word-" + i, READOUT_WORD_X[i], READOUT_WORD_Y,
                    READOUT_COUNTER_X[i] - 2 - READOUT_WORD_X[i], SMALL_TEXT);
            layout.box("readout " + i, READOUT_COUNTER_X[i] - 1, READOUT_Y - 1, READOUT_DIGITS * DIGIT + 1,
                    COUNTER_HEIGHT + 2);
            layout.box("readout-unit-" + i, READOUT_UNIT_X[i], READOUT_WORD_Y, 12, SMALL_TEXT);
        }
        layout.box("thermometer", THERMOMETER_X - 3, THERMOMETER_TOP - 3, 12, THERMOMETER_BOTTOM
                - THERMOMETER_TOP + 12);
        layout.box("scale", SCALE_X, THERMOMETER_TOP, 12, THERMOMETER_BOTTOM - THERMOMETER_TOP);
        layout.box("rocker", ROCKER_X, ROCKER_Y, ROCKER_WIDTH, ROCKER_HEIGHT);
        layout.box("rocker-words", ROCKER_WORD_X, ROCKER_Y + 2, FACE_X + FACE_WIDTH - 4 - ROCKER_WORD_X,
                ROCKER_HEIGHT - 2);
        layout.playerInventory(playerInventory().x() - 1, playerInventory().y() - 1);
        return layout;
    }

    /** The Research tab: the chart, the card and its buttons, what runs now and the queue apart and inside. */
    public static GuiLayout research() {
        final GuiLayout layout = new GuiLayout(WIDTH, HEIGHT);
        plates(layout, TOP_PLATE_MOST, TOP_PLATES_END - PLATE_X - TOP_PLATE_MOST - PLATE_GAP);
        lamps(layout, TOP_LAMP_X, TOP_LAMP_Y, 1, TOP_RUN_ROOM);
        lamps(layout, TOP_HOT_X, TOP_LAMP_Y, 1, TOP_HOT_ROOM);
        layout.box("chart", CHART_LEFT - 3, CHART_TOP - 3, CHART_RIGHT - CHART_LEFT + 6,
                CHART_BOTTOM - CHART_TOP + 6);
        layout.box("card", CARD_LEFT, CARD_TOP, CARD_RIGHT - CARD_LEFT, CARD_BOTTOM - CARD_TOP);
        layout.textBox("queue-button", QUEUE_BUTTON_X, BUTTON_Y, QUEUE_BUTTON_WIDTH, BUTTON_HEIGHT);
        layout.textBox("research-button", RESEARCH_BUTTON_X, BUTTON_Y, RESEARCH_BUTTON_WIDTH, BUTTON_HEIGHT);
        layout.box("now-word", PLATE_X, NOW_WORD_Y, 30, SMALL_TEXT);
        final int lamps = PLATE_X + NOW_PLATE_MOST + 6;
        layout.box("now-plate", PLATE_X, NOW_PLATE_Y, NOW_PLATE_MOST, PLATE_HEIGHT);
        layout.box("progress", lamps - 1, NOW_PLATE_Y + 1, PROGRESS_LAMPS * PROGRESS_PITCH, LAMP_SIZE + 2);
        layout.box("progress-counter", lamps + 91, NOW_PLATE_Y, 2 * DIGIT + 1, COUNTER_HEIGHT + 2);
        layout.box("percent", lamps + 105, NOW_PLATE_Y + 3, 5, SMALL_TEXT);
        for (int i = 0; i < 2; i++) {
            layout.box("status " + i, PLATE_X, STATUS_Y + i * STATUS_PITCH, STATUS_ROOM, SMALL_TEXT + 1);
        }
        layout.box("queue-word", CARD_LEFT, QUEUE_WORD_Y, CARD_RIGHT - CARD_LEFT, SMALL_TEXT);
        for (int i = 0; i < QUEUE_SHOWN; i++) {
            layout.box("queue " + i, CARD_LEFT, QUEUE_Y + i * LINE, CARD_RIGHT - CARD_LEFT, SMALL_TEXT);
        }
        return layout;
    }

    /** The Group tab: the drawing, the table and the rule apart and inside. */
    public static GuiLayout group() {
        final GuiLayout layout = new GuiLayout(WIDTH, HEIGHT);
        plates(layout, TOP_PLATE_MOST, TOP_PLATES_END - PLATE_X - TOP_PLATE_MOST - PLATE_GAP);
        lamps(layout, TOP_LAMP_X, TOP_LAMP_Y, 1, TOP_RUN_ROOM);
        layout.box("schematic", SCHEMATIC_LEFT, SCHEMATIC_TOP, SCHEMATIC_RIGHT - SCHEMATIC_LEFT,
                SCHEMATIC_BOTTOM - SCHEMATIC_TOP);
        for (int i = 0; i < CABINETS_SHOWN; i++) {
            layout.textBox("cabinet " + i, cabinetX(i, CABINETS_SHOWN) - 1, CABINET_Y - 1, CABINET_WIDTH + 2,
                    CABINET_HEIGHT + 2);
        }
        layout.box("table", TABLE_LEFT, TABLE_TOP, TABLE_RIGHT - TABLE_LEFT, TABLE_BOTTOM - TABLE_TOP);
        for (int i = 0; i < RULE_Y.length; i++) {
            layout.box("rule " + i, PLATE_X, RULE_Y[i], FACE_X + FACE_WIDTH - 4 - PLATE_X, SMALL_TEXT + 1);
        }
        return layout;
    }

    /* The two plates on top, at the widths given. */
    private static void plates(final GuiLayout layout, final int first, final int second) {
        layout.box("name-plate", PLATE_X, PLATE_Y, first, PLATE_HEIGHT);
        layout.box("second-plate", PLATE_X + first + PLATE_GAP, PLATE_Y, second, PLATE_HEIGHT);
    }

    /* {@code count} lamps one under the other from ({@code x}, {@code y}), their words {@code room} wide by them. */
    private static void lamps(final GuiLayout layout, final int x, final int y, final int count, final int room) {
        for (int i = 0; i < count; i++) {
            layout.box("lamp " + x + " " + i, x - 1, y - 1 + i * LAMP_PITCH, LAMP_WORD_GAP + 1 + room,
                    LAMP_SIZE + 2);
        }
    }
}
