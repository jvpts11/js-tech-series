/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.machine;

import dev.jstech.core.gui.layout.GuiLayout;

/**
 * Where everything sits on the screen every machine shares, in the GUI's pixels from its top left, as the approved
 * design draws it: the five tabs on top, the window with the machine's steel face, and the player's inventory in a
 * window of its own below. A point on the face is given from the face's own top left ({@link #faceX}, {@link #faceY}).
 * Pure: the screen draws from these, the menu places its slots by them, and the test checks they keep apart.
 */
public final class MachineScreenLayout {

    /** The whole screen. */
    public static final int WIDTH = 244;
    public static final int HEIGHT = 257;
    /** The tabs along the top: Main, Sides, Redstone, Upgrades and Protection. */
    public static final int TABS = 5;
    public static final int TAB_X = 3;
    public static final int TAB_PITCH = 30;
    public static final int TAB_WIDTH = 28;
    public static final int TAB_HEIGHT = 26;
    public static final int TAB_ACTIVE_HEIGHT = 29;
    public static final int TAB_ICON = 6;
    /** The window the face is set in. */
    public static final int WINDOW_TOP = 27;
    public static final int WINDOW_HEIGHT = 126;
    /** The steel face, inside the window. */
    public static final int FACE_X = 7;
    public static final int FACE_Y = 34;
    public static final int FACE_WIDTH = 230;
    public static final int FACE_HEIGHT = 112;
    /** The seam between the face's two plates, from the face's left: the right column is every tab's own. */
    public static final int SEAM = 168;
    /** The player's inventory, in a window of its own. */
    public static final int INVENTORY_X = 34;
    public static final int INVENTORY_Y = 157;
    public static final int INVENTORY_WIDTH = 176;
    public static final int INVENTORY_HEIGHT = 100;
    /** A slot's side, frame included. */
    public static final int SLOT = 18;
    /** The small text of the face, the game's font at three quarters. */
    public static final float SMALL = 0.75F;

    /** The plates every tab shows: the machine's name and its class, from the face's top left. */
    public static final int PLATE_X = 6;
    public static final int NAME_PLATE_Y = 5;
    public static final int CLASS_PLATE_Y = 19;
    public static final int PLATE_HEIGHT = 12;
    /** The three lamps, RUN, WAIT and FAULT, one under the other, their words beside them. */
    public static final int LAMP_X = 178;
    public static final int LAMP_Y = 4;
    public static final int LAMP_PITCH = 10;
    public static final int LAMP_SIZE = 7;
    public static final int LAMP_WORD_X = 188;

    /** The Main tab's standard face: its meter, its LCD, its process window, its switch and its sight glass. */
    public static final int METER_X = 10;
    public static final int METER_Y = 37;
    public static final int METER_WIDTH = 18;
    public static final int METER_HEIGHT = 56;
    /** The meter's scale: four marks above its zero. */
    public static final int METER_MARKS = 4;
    public static final int LCD_X = 102;
    public static final int LCD_Y = 6;
    public static final int LCD_WIDTH = 56;
    public static final int LCD_HEIGHT = 22;
    public static final int PROCESS_X = 40;
    public static final int PROCESS_Y = 38;
    public static final int PROCESS_RIGHT = 162;
    public static final int PROCESS_BOTTOM = 102;
    public static final int SWITCH_X = 179;
    public static final int SWITCH_Y = 50;
    public static final int SWITCH_WIDTH = 14;
    public static final int SWITCH_HEIGHT = 22;
    public static final int GLASS_X = 214;
    public static final int GLASS_Y = 51;
    public static final int GLASS_WIDTH = 8;
    public static final int GLASS_HEIGHT = 36;
    /** A lane of the process window: its row, the arrow between its inputs and outputs. */
    public static final int LANE_PITCH = 20;
    public static final int ARROW_WIDTH = 16;

    /** The Sides tab: the resources' pushbuttons, the two cubes, the face's knob, pushing and pulling. */
    public static final int RESOURCE_X = 6;
    public static final int RESOURCE_Y = 38;
    public static final int RESOURCE_PITCH = 16;
    public static final int RESOURCE_PITCH_TIGHT = 13;
    public static final int RESOURCES_SHOWN = 5;
    public static final int BUTTON_WIDTH = 14;
    public static final int BUTTON_HEIGHT = 12;
    public static final int CUBE_FRONT_X = 86;
    public static final int CUBE_REAR_X = 140;
    public static final int CUBE_Y = 30;
    public static final int CUBE_SIZE = 24;
    public static final int FACE_KNOB_X = 203;
    public static final int FACE_KNOB_Y = 64;
    public static final int FACE_KNOB_R = 9;
    public static final int PUSH_TOGGLE_X = 62;
    public static final int PULL_TOGGLE_X = 128;
    public static final int TOGGLE_Y = 101;
    public static final int TOGGLE_WIDTH = 8;
    public static final int TOGGLE_HEIGHT = 12;
    public static final int PUSH_WHEEL_X = 94;
    public static final int PULL_WHEEL_X = 160;
    public static final int WHEEL_DIGITS = 3;
    public static final int WHEEL_DIGIT = 7;
    public static final int WHEEL_HEIGHT = 11;
    public static final int FILTER_X = 186;
    public static final int FILTER_Y = 98;

    /** The Redstone tab: the knob of how it runs, the comparator's, the lamps of the signal in, the meter out. */
    public static final int RUNS_KNOB_X = 84;
    public static final int RUNS_KNOB_Y = 58;
    public static final int RUNS_KNOB_R = 14;
    public static final int READS_KNOB_X = 138;
    public static final int READS_KNOB_Y = 58;
    public static final int READS_KNOB_R = 11;
    public static final int SIGNAL_X = 62;
    public static final int SIGNAL_Y = 99;
    public static final int SIGNAL_PITCH = 7;
    public static final int OUT_METER_X = 195;
    public static final int OUT_METER_Y = 50;
    public static final int OUT_METER_WIDTH = 16;
    public static final int OUT_METER_HEIGHT = 42;

    /** The Upgrades tab: its bays, a row of them, and the bank of the effects' bars. */
    public static final int BAY_X = 66;
    public static final int BAY_Y = 44;
    public static final int BAY_PITCH = 32;
    public static final int BAYS = 3;
    public static final int BANK_X = 172;
    public static final int BANK_Y = 37;
    public static final int BANK_WIDTH = 51;
    public static final int BANK_HEIGHT = 66;

    /** The Protection tab: the fuse holder, the battery bay and the key switch of who may use it. */
    public static final int FUSE_X = 62;
    public static final int FUSE_Y = 44;
    public static final int FUSE_WIDTH = 44;
    public static final int FUSE_HEIGHT = 24;
    public static final int BATTERY_X = 118;
    public static final int BATTERY_Y = 44;
    public static final int BATTERY_WIDTH = 48;
    public static final int BATTERY_HEIGHT = 40;
    public static final int KEY_X = 203;
    public static final int KEY_Y = 66;
    public static final int KEY_R = 9;
    public static final int KEY_WORD_ACROSS = 19;
    public static final int KEY_WORD_DOWN = 17;
    public static final int OWNER_PLATE_X = 178;
    public static final int OWNER_PLATE_Y = 84;
    /** The most a plate is wide, so the left column of every tab keeps clear of what the tab draws beside it. */
    public static final int NAME_PLATE_MOST = 54;
    public static final int OWNER_PLATE_MOST = 50;
    private static final int DECADE = 10;
    private static final int[] NICE_MARKS = {1, 2, 5};

    private MachineScreenLayout() {
    }

    /** The screen's x of the face's {@code x}. */
    public static int faceX(final int x) {
        return FACE_X + x;
    }

    /** The screen's y of the face's {@code y}. */
    public static int faceY(final int y) {
        return FACE_Y + y;
    }

    /** The left of tab {@code index}. */
    public static int tabX(final int index) {
        return TAB_X + index * TAB_PITCH;
    }

    /** The tab under ({@code x}, {@code y}), or -1 for none. */
    public static int tabAt(final double x, final double y) {
        for (int i = 0; i < TABS; i++) {
            if (x >= tabX(i) && x < tabX(i) + TAB_WIDTH && y >= 0 && y < TAB_HEIGHT) {
                return i;
            }
        }
        return -1;
    }

    /** Where the player's inventory grid starts, the hotbar under it. */
    public static GuiLayout.SlotPosition playerInventory() {
        return new GuiLayout.SlotPosition(INVENTORY_X + 8, INVENTORY_Y + 18);
    }

    /** Where upgrade bay {@code index}'s slot sits, the item inside its frame. */
    public static GuiLayout.SlotPosition bay(final int index) {
        return new GuiLayout.SlotPosition(faceX(BAY_X + index * BAY_PITCH) + 1, faceY(BAY_Y) + 1);
    }

    /** Where the fuse sits in its holder, and the battery in its bay. */
    public static GuiLayout.SlotPosition fuse() {
        return new GuiLayout.SlotPosition(faceX(FUSE_X + 13) + 1, faceY(FUSE_Y + 3) + 1);
    }

    public static GuiLayout.SlotPosition battery() {
        return new GuiLayout.SlotPosition(faceX(BATTERY_X + 5) + 1, faceY(BATTERY_Y + 5) + 1);
    }

    /**
     * Where the standard face puts a lane's slot, the item inside its frame: the lane's inputs on the left of the
     * process window, its outputs on the right after the arrow, the lanes one under the other.
     *
     * @param lane     the lane
     * @param lanes    how many lanes there are
     * @param index    the slot's place among the lane's inputs, or among its outputs
     * @param output   whether it is an output
     * @param inputs   how many inputs a lane has
     */
    public static GuiLayout.SlotPosition laneSlot(final int lane, final int lanes, final int index,
                                                  final boolean output, final int inputs) {
        final int top = PROCESS_Y + (PROCESS_BOTTOM - PROCESS_Y - lanes * LANE_PITCH) / 2 + 1 + lane * LANE_PITCH;
        final int left = PROCESS_X + 6 + (output ? inputs * SLOT + ARROW_WIDTH + 6 : 0) + index * SLOT;
        return new GuiLayout.SlotPosition(faceX(left) + 1, faceY(top) + 1);
    }

    /** Where the arrow of lane {@code lane} starts on the face. */
    public static int arrowX(final int inputs) {
        return PROCESS_X + 6 + inputs * SLOT + 3;
    }

    /** The y of lane {@code lane}'s row on the face. */
    public static int laneY(final int lane, final int lanes) {
        return PROCESS_Y + (PROCESS_BOTTOM - PROCESS_Y - lanes * LANE_PITCH) / 2 + 1 + lane * LANE_PITCH;
    }

    /** Whether the standard face has room for {@code lanes} lanes of {@code inputs} and {@code outputs} slots each. */
    public static boolean fits(final int lanes, final int inputs, final int outputs) {
        return lanes * LANE_PITCH <= PROCESS_BOTTOM - PROCESS_Y
                && 6 + (inputs + outputs) * SLOT + ARROW_WIDTH + 6 + 6 <= PROCESS_RIGHT - PROCESS_X;
    }

    /**
     * Which face of an isometric cube is under ({@code x}, {@code y}): 0 its top, 1 its left, 2 its right; -1 for
     * none. The cube's top corner is at ({@code ox}, {@code oy}) and its half width {@code s}, as the Sides tab draws
     * it.
     */
    public static int cubeFaceAt(final double x, final double y, final int ox, final int oy, final int s) {
        for (int face = 0; face < 3; face++) {
            final int[][] corners = cubeFace(face, ox, oy, s);
            if (inside(x, y, corners)) {
                return face;
            }
        }
        return -1;
    }

    /**
     * The four corners of an isometric cube's face, in order round it: 0 its top, 1 its left, 2 its right; the cube's
     * top corner at ({@code ox}, {@code oy}), its half width {@code s} and its height six fifths of it.
     */
    public static int[][] cubeFace(final int face, final int ox, final int oy, final int s) {
        final int half = s / 2;
        final int tall = s * 6 / 5;
        return switch (face) {
            case 0 -> new int[][] {{ox, oy}, {ox + s, oy + half}, {ox, oy + s}, {ox - s, oy + half}};
            case 1 -> new int[][] {{ox - s, oy + half}, {ox, oy + s}, {ox, oy + s + tall},
                    {ox - s, oy + half + tall}};
            case 2 -> new int[][] {{ox, oy + s}, {ox + s, oy + half}, {ox + s, oy + half + tall},
                    {ox, oy + s + tall}};
            default -> throw new IllegalArgumentException("a cube shows three faces, not face " + face);
        };
    }

    /**
     * The angle of a four-way knob's position {@code index}, clockwise from the right: up and left first, then up and
     * right, down and right, down and left; the order of the choices it turns through.
     */
    public static double knobAngle(final int index) {
        return Math.PI * (1.25 + 0.5 * index);
    }

    /** The angle of the key switch's position {@code index}: left, up, then right. */
    public static double keyAngle(final int index) {
        return Math.PI * (1.0 + 0.5 * index);
    }

    /** Where the word of a knob's position at {@code angle} is centred across, and its top: {x, y}. */
    public static int[] knobWord(final int cx, final int cy, final int r, final double angle) {
        return new int[] {cx + (int) Math.round((r + 9) * Math.cos(angle)),
                cy + (int) Math.round((r + 8) * Math.sin(angle)) - 3};
    }

    /** Where the word of the key switch's position at {@code angle} is centred across, and its top: {x, y}. */
    public static int[] keyWord(final int cx, final int cy, final double angle) {
        return new int[] {cx + (int) Math.round(KEY_WORD_ACROSS * Math.cos(angle)),
                cy + (int) Math.round(KEY_WORD_DOWN * Math.sin(angle)) - 3};
    }

    /** Whether ({@code x}, {@code y}) is on a word centred across {@code word} and {@code width} wide. */
    public static boolean onWord(final double x, final double y, final int[] word, final int width) {
        return x >= word[0] - width / 2.0 - 2 && x < word[0] + width / 2.0 + 2 && y >= word[1] - 2 && y < word[1] + 8;
    }

    /** Whether ({@code x}, {@code y}) is on the disc of radius {@code r} round ({@code cx}, {@code cy}). */
    public static boolean onDisc(final double x, final double y, final int cx, final int cy, final int r) {
        return Math.hypot(x - cx, y - cy) <= r + 1;
    }

    /** The drum of a thumbwheel whose left is at {@code wx} under ({@code x}, {@code y}); -1 for none. */
    public static int wheelDrumAt(final double x, final double y, final int wx, final int wy) {
        if (y < wy || y >= wy + WHEEL_HEIGHT) {
            return -1;
        }
        for (int i = 0; i < WHEEL_DIGITS; i++) {
            final int dx = wx + 1 + i * WHEEL_DIGIT;
            if (x >= dx && x < dx + WHEEL_DIGIT - 1) {
                return i;
            }
        }
        return -1;
    }

    /** The top of resource pushbutton {@code index} of {@code count}, closer together when there are five. */
    public static int resourceY(final int index, final int count) {
        return RESOURCE_Y + index * (count >= RESOURCES_SHOWN ? RESOURCE_PITCH_TIGHT : RESOURCE_PITCH);
    }

    /** Whether ({@code x}, {@code y}) is on the part at ({@code px}, {@code py}) sized {@code w} by {@code h}. */
    public static boolean on(final double x, final double y, final int px, final int py, final int w, final int h) {
        return x >= px && x < px + w && y >= py && y < py + h;
    }

    /**
     * What one mark of the meter's scale is worth for the most it has shown, {@code peak}: one, two or five of a power
     * of ten, the least of them that puts the peak on the scale.
     */
    public static long meterMark(final long peak) {
        final long wanted = Math.max(1L, peak / METER_MARKS + (peak % METER_MARKS == 0 ? 0 : 1));
        long power = 1L;
        while (power <= Long.MAX_VALUE / DECADE / DECADE) {
            for (final int nice : NICE_MARKS) {
                if (nice * power >= wanted) {
                    return nice * power;
                }
            }
            power *= DECADE;
        }
        return wanted;
    }

    /** The layout, for the overlap and bounds checks. */
    public static GuiLayout layout() {
        final GuiLayout layout = new GuiLayout(WIDTH, HEIGHT);
        for (int i = 0; i < TABS; i++) {
            layout.box("tab" + i, tabX(i), 0, TAB_WIDTH, TAB_HEIGHT);
        }
        layout.box("window", 0, WINDOW_TOP, WIDTH, WINDOW_HEIGHT);
        layout.box("inventory", INVENTORY_X, INVENTORY_Y, INVENTORY_WIDTH, INVENTORY_HEIGHT);
        return layout;
    }

    /* Whether ({@code x}, {@code y}) is inside the polygon of {@code corners}, by the crossings of a ray. */
    private static boolean inside(final double x, final double y, final int[][] corners) {
        boolean in = false;
        for (int i = 0, j = corners.length - 1; i < corners.length; j = i++) {
            final double xi = corners[i][0];
            final double yi = corners[i][1];
            final double xj = corners[j][0];
            final double yj = corners[j][1];
            if (yi > y != yj > y && x < (xj - xi) * (y - yi) / (yj - yi) + xi) {
                in = !in;
            }
        }
        return in;
    }
}
