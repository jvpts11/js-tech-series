/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.energy;

import dev.jstech.core.gui.layout.GuiLayout;

/**
 * Where everything sits on a converter's screen, in the GUI's pixels from the top left of its picture: the Main tab,
 * the converter cabinet's steel face (its plates, the display, the lamps, the FE and JE meters either side of the
 * window into the bridge, the switch, the class keys or the ceiling, the limit's thumbwheels and the direction's knob)
 * and the player's inventory below. Pure: the screen draws from these, the menu places the inventory by them, and the
 * test checks they keep apart.
 */
public final class EnergyConverterLayout {

    /** The screen, tabs to inventory. */
    public static final int WIDTH = 244;
    public static final int HEIGHT = 255;
    /** The plates: the converter's name, and its class below it; neither runs into the display. */
    public static final int PLATE_X = 13;
    public static final int NAME_PLATE_Y = 39;
    public static final int CLASS_PLATE_Y = 53;
    public static final int PLATE_HEIGHT = 12;
    public static final int PLATE_PADDING = 8;
    public static final int PLATE_RIGHT = 104;
    /** The display and its two lines. */
    public static final int DISPLAY_X = 109;
    public static final int DISPLAY_Y = 40;
    public static final int DISPLAY_WIDTH = 56;
    public static final int DISPLAY_HEIGHT = 22;
    public static final int DISPLAY_TEXT_X = 113;
    public static final int DISPLAY_LINE_Y = 43;
    public static final int DISPLAY_SECOND_Y = 53;
    /** The three lamps, RUN, LIMIT and FAULT, one under the other, each with its word. */
    public static final int LAMP_X = 185;
    public static final int LAMP_Y = 38;
    public static final int LAMP_PITCH = 10;
    public static final int LAMP_SIZE = 7;
    public static final int LAMP_WORD_X = 195;
    public static final int LAMP_WORD_Y = 39;
    /** The meters, FE on the left and JE on the right: their faces, their marks' numbers and their units. */
    public static final int FE_METER_X = 15;
    public static final int JE_METER_X = 163;
    public static final int METER_Y = 72;
    public static final int METER_WIDTH = 20;
    public static final int METER_HEIGHT = 32;
    public static final int METER_NUMBER_X = 7;
    public static final int FE_UNIT_X = 25;
    public static final int JE_UNIT_X = 173;
    public static final int UNIT_Y = 110;
    /** The window into the bridge, its busbar's middle row, its power modules and the ends the pulses run along. */
    public static final int BRIDGE_X = 43;
    public static final int BRIDGE_Y = 68;
    public static final int BRIDGE_WIDTH = 114;
    public static final int BRIDGE_HEIGHT = 40;
    public static final int BUSBAR_Y = 87;
    public static final int FE_BUS_X = 46;
    public static final int JE_BUS_X = 144;
    public static final int JE_BUS_RIGHT = 153;
    public static final int BUS_RUN = 18;
    public static final int MODULE_X = 62;
    public static final int MODULE_PITCH = 22;
    public static final int MODULES = 3;
    public static final int MODULE_LED_X = 13;
    public static final int MODULE_LED_Y = 98;
    public static final int MODULE_GLOW_Y = 83;
    /** The on and off switch, and its two words. */
    public static final int SWITCH_X = 193;
    public static final int SWITCH_Y = 72;
    public static final int SWITCH_WIDTH = 15;
    public static final int SWITCH_HEIGHT = 23;
    public static final int ON_WORD_X = 210;
    public static final int ON_WORD_Y = 75;
    public static final int OFF_WORD_Y = 87;
    /** The word over the class keys, or over the ceiling's plate, and over the thumbwheels. */
    public static final int CLASS_WORD_X = 13;
    public static final int LIMIT_WORD_X = 129;
    public static final int WORD_ROW_Y = 121;
    /** The five class keys of a configurable converter, or the ceiling's plate of a fixed one, in the same place. */
    public static final int KEY_X = 13;
    public static final int KEY_Y = 130;
    public static final int KEY_PITCH = 21;
    public static final int KEY_WIDTH = 20;
    public static final int KEY_HEIGHT = 12;
    public static final int KEYS = 5;
    /** The limit's thumbwheels: four digit drums and the prefix's drum, then the unit. */
    public static final int DRUM_X = 130;
    public static final int DRUM_Y = 131;
    public static final int DRUM_PITCH = 7;
    public static final int DRUM_WIDTH = 6;
    public static final int DRUM_HEIGHT = 11;
    public static final int DRUMS = 5;
    public static final int WHEEL_UNIT_X = 171;
    public static final int WHEEL_UNIT_Y = 133;
    /** The direction's knob, its two positions, and the words naming them over it. */
    public static final int KNOB_X = 208;
    public static final int KNOB_Y = 133;
    public static final int KNOB_RADIUS = 7;
    public static final int WAY_WORD_Y = 116;
    public static final int TO_FE_WORD_X = 180;
    public static final int TO_JE_WORD_X = 210;
    public static final int WAY_WORD_WIDTH = 26;
    /** The player's inventory, its word, and the top left of its grid's first slot. */
    public static final int INVENTORY_WORD_X = 42;
    public static final int INVENTORY_WORD_Y = 163;
    public static final int INVENTORY_X = 42;
    public static final int INVENTORY_Y = 175;
    /** The small text of the face, as the game's font is drawn at three quarters. */
    public static final float SMALL = 0.75F;

    /** The sprites below the screen in its picture: the lamps lit, a class key unlit and lit, the switch on and off. */
    public static final int SPRITES_Y = 256;
    public static final int LAMP_SPRITE = 11;
    public static final int KEY_SPRITE_X = 36;
    public static final int KEY_SPRITE_PITCH = 24;
    public static final int KEY_SPRITE_WIDTH = 22;
    public static final int KEY_SPRITE_HEIGHT = 15;
    public static final int SWITCH_SPRITE_X = 84;
    public static final int SWITCH_SPRITE_PITCH = 16;
    public static final int PICTURE_WIDTH = 256;
    public static final int PICTURE_HEIGHT = 320;

    private static final int KNOB_REACH = 3;

    private EnergyConverterLayout() {
    }

    /** The layout, for the overlap and bounds checks; {@code fixed} for a converter of one class. */
    public static GuiLayout layout(final boolean fixed) {
        final GuiLayout layout = new GuiLayout(WIDTH, HEIGHT);
        layout.box("display", DISPLAY_X - 3, DISPLAY_Y - 3, DISPLAY_WIDTH + 6, DISPLAY_HEIGHT + 6);
        for (int i = 0; i < 3; i++) {
            layout.box("lamp" + i, LAMP_X - 1, LAMP_Y + i * LAMP_PITCH - 1, LAMP_SIZE + 2, LAMP_SIZE + 2);
            layout.text("lampWord" + i, LAMP_WORD_X, LAMP_WORD_Y + i * LAMP_PITCH, 6, SMALL);
        }
        layout.box("feMeter", FE_METER_X - 4, METER_Y - 4, METER_WIDTH + 8, METER_HEIGHT + 8);
        layout.box("jeMeter", JE_METER_X - 4, METER_Y - 4, METER_WIDTH + 8, METER_HEIGHT + 8);
        layout.box("bridge", BRIDGE_X, BRIDGE_Y, BRIDGE_WIDTH, BRIDGE_HEIGHT);
        layout.box("switch", SWITCH_X, SWITCH_Y, SWITCH_WIDTH, SWITCH_HEIGHT);
        layout.box("thumbwheels", DRUM_X - 3, DRUM_Y - 2, DRUMS * DRUM_PITCH + 6, DRUM_HEIGHT + 4);
        layout.box("knob", KNOB_X - KNOB_RADIUS - KNOB_REACH, KNOB_Y - KNOB_RADIUS - KNOB_REACH,
                (KNOB_RADIUS + KNOB_REACH) * 2 + 1, (KNOB_RADIUS + KNOB_REACH) * 2 + 1);
        if (fixed) {
            layout.box("ceiling", KEY_X, KEY_Y, KEY_PITCH * 3, KEY_HEIGHT + 1);
        } else {
            for (int k = 0; k < KEYS; k++) {
                layout.box("key" + k, KEY_X + k * KEY_PITCH, KEY_Y, KEY_WIDTH + 1, KEY_HEIGHT + 1);
            }
        }
        layout.text("name", PLATE_X + PLATE_PADDING, NAME_PLATE_Y + 3, 18, SMALL);
        layout.text("class", PLATE_X + PLATE_PADDING, CLASS_PLATE_Y + 3, 18, SMALL);
        layout.text("toFe", TO_FE_WORD_X, WAY_WORD_Y, 5, SMALL);
        layout.text("toJe", TO_JE_WORD_X, WAY_WORD_Y, 5, SMALL);
        layout.playerInventory(INVENTORY_X, INVENTORY_Y);
        return layout;
    }

    /** The class key under ({@code x}, {@code y}), 0 for the low class to 4, or -1 for none. */
    public static int keyAt(final double x, final double y) {
        if (y < KEY_Y || y >= KEY_Y + KEY_HEIGHT) {
            return -1;
        }
        for (int k = 0; k < KEYS; k++) {
            final int left = KEY_X + k * KEY_PITCH;
            if (x >= left && x < left + KEY_WIDTH) {
                return k;
            }
        }
        return -1;
    }

    /** The drum under ({@code x}, {@code y}): 0 to 3 the digits from the thousands, 4 the prefix, -1 none. */
    public static int drumAt(final double x, final double y) {
        if (y < DRUM_Y || y >= DRUM_Y + DRUM_HEIGHT) {
            return -1;
        }
        for (int d = 0; d < DRUMS; d++) {
            final int left = DRUM_X + d * DRUM_PITCH;
            if (x >= left && x < left + DRUM_WIDTH) {
                return d;
            }
        }
        return -1;
    }

    /** Whether ({@code x}, {@code y}) falls on a drum's upper half, which turns it up; the lower half turns it down. */
    public static boolean upperHalf(final double y) {
        return y < DRUM_Y + DRUM_HEIGHT / 2.0;
    }

    /** Whether ({@code x}, {@code y}) falls on the switch. */
    public static boolean onSwitch(final double x, final double y) {
        return x >= SWITCH_X && x < SWITCH_X + SWITCH_WIDTH && y >= SWITCH_Y && y < SWITCH_Y + SWITCH_HEIGHT;
    }

    /** Whether ({@code x}, {@code y}) falls on the knob. */
    public static boolean onKnob(final double x, final double y) {
        final double dx = x - KNOB_X;
        final double dy = y - KNOB_Y;
        final int reach = KNOB_RADIUS + KNOB_REACH;
        return dx * dx + dy * dy <= reach * reach;
    }

    /** Whether ({@code x}, {@code y}) falls on the word of the way toward FE, at the knob's left. */
    public static boolean onToFeWord(final double x, final double y) {
        return onWord(x, y, TO_FE_WORD_X);
    }

    /** Whether ({@code x}, {@code y}) falls on the word of the way toward JE, at the knob's right. */
    public static boolean onToJeWord(final double x, final double y) {
        return onWord(x, y, TO_JE_WORD_X);
    }

    private static boolean onWord(final double x, final double y, final int left) {
        return x >= left && x < left + WAY_WORD_WIDTH && y >= WAY_WORD_Y - 1 && y < WAY_WORD_Y + 7;
    }
}
