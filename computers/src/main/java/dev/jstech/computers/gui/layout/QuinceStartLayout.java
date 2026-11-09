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
 * Pure layout of what the firmware of Quince's machines puts on the glass (computers/docs/design/quince.md), as the
 * approved boot page drew it on the desktop's 512 by 342 and scaled here to the glass: the face of the Russet 128K and
 * the fruit while a machine starts, with the spinner or the bar under it; the Sad Quince and its lines; the start-up
 * chooser's volumes in a row with the button under the one picked; and Open Firmware's page of text.
 */
public final class QuinceStartLayout {

    public static final int WIDTH = MonitorGlass.WIDTH;
    public static final int HEIGHT = MonitorGlass.HEIGHT;

    /** The Russet 128K's face, 32 texels drawn twice their size, a little under the middle. */
    public static final int FACE = 32;
    public static final int FACE_SCALE = 2;
    public static final int FACE_DY = 4;
    /** The fruit, 16 texels drawn three times their size, over the middle. */
    public static final int FRUIT = 16;
    public static final int FRUIT_SCALE = 3;
    public static final int FRUIT_DY = -16;
    /** The Transition's spinner under the fruit, and the bar the later ones fill. */
    public static final int SPINNER_DY = 38;
    public static final int SPINNER_RADIUS = 8;
    public static final int BAR_W = 90;
    public static final int BAR_H = 2;
    public static final int BAR_DY = 34;

    /** The Sad Quince: the face higher, its fault under it; the later firmwares' fruit, its words and its fault. */
    public static final int SAD_FACE_DY = -10;
    public static final int SAD_FAULT_DY = 26;
    public static final int SAD_FRUIT_DY = -18;
    public static final int SAD_WORDS_DY = 33;
    public static final int SAD_CODE_DY = 44;
    /** Open Firmware's word on a fault, along the foot of the page. */
    public static final int SAD_FOOT_UP = 30;

    /** The start-up chooser: as many volumes as fit in a row, each a box with its icon and its name under it. */
    public static final int MOST_VOLUMES = 4;
    public static final int VOLUME_PITCH = 84;
    public static final int VOLUME_W = 52;
    public static final int VOLUME_H = 60;
    public static final int VOLUME_TOP = 82;
    public static final int ICON = 40;
    public static final int ICON_CENTER_Y = 110;
    public static final int NAME_Y = 132;
    /** The button under the volume picked: an arrow, or the Advanced's word. */
    public static final int BUTTON_TOP = 153;
    public static final int BUTTON_H = 13;
    public static final int ARROW_W = 14;
    public static final int BUTTON_PAD = 14;
    /** The Advanced's line over its volumes. */
    public static final int OPTIONS_LINE_Y = 45;

    /** Open Firmware's page: its lines from the top left, a wall of text's row apart. */
    public static final int PAGE_X = 8;
    public static final int PAGE_TOP = 9;
    public static final int PAGE_ROW = 8;
    /** As many lines as the page holds, the oldest scrolling off the top. */
    public static final int PAGE_LINES = (HEIGHT - 2 * PAGE_TOP) / PAGE_ROW;

    /** The words under a picture that has nothing to boot, in a box of the page's colours. */
    public static final int NO_BOOT_W = 224;
    public static final int NO_BOOT_TOP_DY = 44;
    public static final int NO_BOOT_PAD = 6;

    private QuinceStartLayout() {
    }

    /** Where the middle of the volume {@code index} of {@code count} stands across. */
    public static int volumeCenterX(final int index, final int count) {
        return WIDTH / 2 - (count - 1) * VOLUME_PITCH / 2 + index * VOLUME_PITCH;
    }

    /** The chooser at its fullest: every volume's box and name, and the button under the first. */
    public static GuiLayout chooserLayout() {
        final GuiLayout l = new GuiLayout(WIDTH, HEIGHT);
        for (int i = 0; i < MOST_VOLUMES; i++) {
            final int cx = volumeCenterX(i, MOST_VOLUMES);
            l.box("volume" + i, cx - VOLUME_W / 2, VOLUME_TOP, VOLUME_W, VOLUME_H);
            l.text("volumeName" + i, cx - VOLUME_W / 2, NAME_Y, 12, 0.75f);
        }
        final int cx = volumeCenterX(0, MOST_VOLUMES);
        l.box("button", cx - ARROW_W / 2, BUTTON_TOP, ARROW_W, BUTTON_H);
        l.text("optionsLine", WIDTH / 2 - 60, OPTIONS_LINE_Y, 32, 0.75f);
        return l;
    }

    /** Open Firmware's page, full: every line it holds. */
    public static GuiLayout openFirmwareLayout() {
        final GuiLayout l = new GuiLayout(WIDTH, HEIGHT);
        for (int i = 0; i < PAGE_LINES; i++) {
            l.text("line" + i, PAGE_X, PAGE_TOP + i * PAGE_ROW, 80, 0.75f);
        }
        return l;
    }

    /**
     * The Russet 128K's face as the ROM and Open Firmware start on it, the words under it when there is nothing to
     * boot, and the Sad face's fault and foot line.
     */
    public static GuiLayout faceLayout() {
        final GuiLayout l = new GuiLayout(WIDTH, HEIGHT);
        final int face = FACE * FACE_SCALE;
        l.box("face", (WIDTH - face) / 2, HEIGHT / 2 + FACE_DY - face / 2, face, face);
        l.box("noBoot", (WIDTH - NO_BOOT_W) / 2, noBootTop(), NO_BOOT_W, noBootH());
        l.text("sadFault", WIDTH / 2 - 80, HEIGHT / 2 + SAD_FAULT_DY, 40, 0.75f);
        l.text("sadFoot", WIDTH / 2 - 90, HEIGHT - SAD_FOOT_UP, 48, 0.75f);
        return l;
    }

    /**
     * The fruit as the later firmwares start on it, the bar under it, the words under it when there is nothing to
     * boot, and the Sad Quince's words and code.
     */
    public static GuiLayout fruitLayout() {
        final GuiLayout l = new GuiLayout(WIDTH, HEIGHT);
        final int fruit = FRUIT * FRUIT_SCALE;
        l.box("fruit", (WIDTH - fruit) / 2, HEIGHT / 2 + FRUIT_DY - fruit / 2, fruit, fruit);
        l.box("bar", (WIDTH - BAR_W) / 2, HEIGHT / 2 + BAR_DY, BAR_W, BAR_H);
        l.box("noBoot", (WIDTH - NO_BOOT_W) / 2, noBootTop(), NO_BOOT_W, noBootH());
        l.text("sadWords", WIDTH / 2 - 80, HEIGHT / 2 + SAD_WORDS_DY, 40, 0.75f);
        l.text("sadCode", WIDTH / 2 - 80, HEIGHT / 2 + SAD_CODE_DY, 40, 0.75f);
        return l;
    }

    /** Where the box of words under a picture with nothing to boot stands, under the bar. */
    public static int noBootTop() {
        return HEIGHT / 2 + NO_BOOT_TOP_DY;
    }

    /** How tall that box stands: two lines and its padding. */
    public static int noBootH() {
        return 2 * PAGE_ROW + 2 * NO_BOOT_PAD;
    }
}
