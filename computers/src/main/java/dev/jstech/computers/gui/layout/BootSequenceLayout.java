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
 * Pure layout of the self-test screen's own modern (UEFI) picture: the mark, the machine's name and the bar
 * under it, the foot line, and the dialog a machine with nothing to boot shows over it. The earlier firmware
 * looks (the green tube, the classic blue setup) draw through {@code PostWall} and {@code FirmwareBootMenus},
 * which are those classes' own geometry, not this screen's, so they are not recorded here.
 */
public final class BootSequenceLayout {

    public static final int WIDTH = MonitorGlass.WIDTH;
    public static final int HEIGHT = MonitorGlass.HEIGHT;

    /** The mark and the machine's name, stacked over the middle of the glass. */
    public static final int LOGO_CENTER_DY = -46;
    public static final int TITLE_CENTER_DY = 8;

    /** The bar that fills as the self-test runs, centred under the name. */
    public static final int BAR_W = 120;
    public static final int BAR_H = 3;
    public static final int BAR_CENTER_DY = 24;

    /** The foot line: what the machine is made of on one side, the two keys or "entering setup" on the other. */
    public static final int FOOTER_BOTTOM_MARGIN = 16;
    /** The margin the modern look's foot line keeps from either side edge. */
    public static final int FOOTER_X = 10;

    /** The dialog a machine with nothing to boot shows: its width, and the shape of its rows. */
    public static final int NO_BOOT_BOX_W = 224;
    /** The air the head strip is given when the dialog's own height is worked out. */
    public static final int NO_BOOT_HEAD_H = 16;
    /** The head strip and its alarm edge as they are actually drawn, a shade shorter than {@link #NO_BOOT_HEAD_H}. */
    public static final int NO_BOOT_HEADER_STRIP_H = 14;
    public static final int NO_BOOT_BOTTOM_PAD = 22;
    /** The complaint sits one pixel left of the listed devices, since it is written over the alarm strip. */
    public static final int NO_BOOT_COMPLAINT_X = 7;
    public static final int NO_BOOT_LIST_X = 8;
    public static final int NO_BOOT_COMPLAINT_DY = 4;
    public static final int NO_BOOT_LIST_TOP = 18;
    /** The step from one listed device to the next, the wall of text's own row height. */
    public static final int NO_BOOT_LIST_STEP = 8;
    public static final int NO_BOOT_INSERT_MEDIA_GAP = 4;
    /** As many devices as the dialog ever lists before it stops; {@code PostWall.MOST_DRIVES} reads this too. */
    public static final int MOST_LISTED_DEVICES = 10;

    private BootSequenceLayout() {
    }

    public static int barX() {
        return (WIDTH - BAR_W) / 2;
    }

    public static int barY() {
        return HEIGHT / 2 + BAR_CENTER_DY;
    }

    public static int footerY() {
        return HEIGHT - FOOTER_BOTTOM_MARGIN;
    }

    /** How tall the no-boot dialog stands with that many devices listed (at least one row for "nothing here"). */
    public static int noBootBoxH(final int listedDevices) {
        return NO_BOOT_HEAD_H + Math.max(1, listedDevices) * NO_BOOT_LIST_STEP + NO_BOOT_BOTTOM_PAD;
    }

    public static int noBootBoxX() {
        return (WIDTH - NO_BOOT_BOX_W) / 2;
    }

    public static int noBootBoxY(final int boxH) {
        return (HEIGHT - boxH) / 2;
    }

    /**
     * The bar and the centred title over it, with the foot line under. A machine with nothing to boot drops the bar
     * and the foot line and draws {@link #noBootLayout(int)} over the title instead, so the two are audited on their
     * own.
     */
    public static GuiLayout layout() {
        final GuiLayout l = new GuiLayout(WIDTH, HEIGHT);
        l.box("bar", barX(), barY(), BAR_W, BAR_H);
        l.text("title", WIDTH / 2 - 60, HEIGHT / 2 + TITLE_CENTER_DY, 20, 1.0f);
        l.text("footer", FOOTER_X, footerY(), 40, 1.0f);
        return l;
    }

    /** The dialog a machine with nothing to boot shows, in place of the bar, with that many devices listed. */
    public static GuiLayout noBootLayout(final int listedDevices) {
        final GuiLayout l = new GuiLayout(WIDTH, HEIGHT);
        final int boxH = noBootBoxH(listedDevices);
        final int bx = noBootBoxX();
        final int by = noBootBoxY(boxH);
        l.box("noBootDialog", bx, by, NO_BOOT_BOX_W, boxH);
        l.text("noBootComplaint", bx + NO_BOOT_COMPLAINT_X, by + NO_BOOT_COMPLAINT_DY, 34, 1.0f);
        int ly = by + NO_BOOT_LIST_TOP;
        for (int i = 0; i < Math.max(1, listedDevices); i++) {
            l.text("noBootEntry" + i, bx + NO_BOOT_LIST_X, ly, 34, 1.0f);
            ly += NO_BOOT_LIST_STEP;
        }
        l.text("noBootInsertMedia", bx + NO_BOOT_LIST_X, ly + NO_BOOT_INSERT_MEDIA_GAP, 34, 1.0f);
        return l;
    }
}
