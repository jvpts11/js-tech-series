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
 * Where everything of the desktops' disk tools stands (computers/docs/design/storage-devices.md): Disk Management's
 * volumes over its map of disks and its status bar, the KDE Partition Manager's devices beside its bar, its table and
 * its pending operations, and GNOME Disks' list of disks beside the chosen one's details and volumes. Pure, so each
 * window draws from it and a test holds it to account.
 */
public final class DiskToolLayout {

    /** How big every tool's window is. */
    public static final int W = 340;
    public static final int H = 220;
    /** The step from one row of text to the next, and the height of a bar along the top or the foot. */
    public static final int ROW = 9;
    public static final int BAR_H = 11;

    /* Disk Management: the menu, a toolbar, the volumes, the map of disks, the status bar. */
    public static final int DM_TOOLBAR_Y = 11;
    public static final int DM_LIST_Y = 22;
    public static final int DM_LIST_ROWS = 7;
    public static final int DM_LIST_H = ROW * (DM_LIST_ROWS + 1) + 2;
    public static final int[] DM_COLUMNS = {2, 62, 96, 126, 166, 244, 280, 318};
    public static final int DM_MAP_Y = DM_LIST_Y + DM_LIST_H + 4;
    public static final int DM_DISK_H = 30;
    public static final int DM_DISK_GAP = 3;
    public static final int DM_DISKS = 3;
    public static final int DM_LABEL_W = 56;
    public static final int DM_STATUS_Y = H - BAR_H;

    /* The KDE Partition Manager: the menu, the toolbar, the devices, the bar, the table, the pending operations. */
    public static final int KPM_TOOLBAR_Y = 11;
    public static final int KPM_SIDE_W = 82;
    public static final int KPM_TOP = 24;
    public static final int KPM_BAR_H = 20;
    public static final int KPM_TABLE_Y = KPM_TOP + KPM_BAR_H + 4;
    public static final int KPM_TABLE_ROWS = 8;
    public static final int[] KPM_COLUMNS = {0, 56, 96, 136, 172, 206, 236};
    public static final int KPM_PENDING_Y = KPM_TABLE_Y + ROW * (KPM_TABLE_ROWS + 1) + 4;
    public static final int KPM_STATUS_Y = H - BAR_H;

    /* GNOME Disks: the list of disks, the chosen one's details, its volumes, the buttons under them. */
    public static final int GD_SIDE_W = 92;
    public static final int GD_ENTRY_H = 24;
    public static final int GD_DETAILS_Y = 28;
    public static final int GD_VOLUMES_Y = 86;
    public static final int GD_BAR_Y = 98;
    public static final int GD_BAR_H = 34;
    public static final int GD_BUTTONS_Y = 136;
    public static final int GD_BUTTON = 14;
    public static final int GD_VOLUME_Y = 156;
    public static final int GD_MENU_W = 16;

    /** A dialog of a disk tool. */
    public static final int FORM_W = 240;
    public static final int FORM_H = 150;

    private DiskToolLayout() {
    }

    /** The tool's window laid out. */
    public static GuiLayout layout(final Tool tool) {
        final GuiLayout l = new GuiLayout(W, H);
        switch (tool) {
            case DISK_MANAGEMENT -> {
                l.box("list", 0, DM_LIST_Y, W, DM_LIST_H);
                for (int disk = 0; disk < DM_DISKS; disk++) {
                    final int y = DM_MAP_Y + disk * (DM_DISK_H + DM_DISK_GAP);
                    l.box("disk" + disk, 2, y, DM_LABEL_W, DM_DISK_H);
                    l.box("blocks" + disk, DM_LABEL_W + 4, y, W - DM_LABEL_W - 6, DM_DISK_H);
                }
                l.box("status", 0, DM_STATUS_Y, W, BAR_H);
            }
            case PARTITION_MANAGER -> {
                l.box("devices", 0, KPM_TOP, KPM_SIDE_W, KPM_STATUS_Y - KPM_TOP - 2);
                l.box("bar", KPM_SIDE_W + 2, KPM_TOP, W - KPM_SIDE_W - 4, KPM_BAR_H);
                l.box("table", KPM_SIDE_W + 2, KPM_TABLE_Y, W - KPM_SIDE_W - 4, ROW * (KPM_TABLE_ROWS + 1));
                l.box("pending", KPM_SIDE_W + 2, KPM_PENDING_Y, W - KPM_SIDE_W - 4, KPM_STATUS_Y - KPM_PENDING_Y - 2);
                l.box("status", 0, KPM_STATUS_Y, W, BAR_H);
            }
            case GNOME_DISKS -> {
                l.box("disks", 0, 0, GD_SIDE_W, H);
                l.box("bar", GD_SIDE_W + 6, GD_BAR_Y, W - GD_SIDE_W - 12, GD_BAR_H);
                for (int button = 0; button < 3; button++) {
                    l.box("button" + button, GD_SIDE_W + 6 + button * (GD_BUTTON + 2), GD_BUTTONS_Y, GD_BUTTON,
                            GD_BUTTON);
                }
                l.box("menu", W - GD_MENU_W - 4, 2, GD_MENU_W, GD_MENU_W);
                l.text("details", GD_SIDE_W + 6, GD_DETAILS_Y, 40, 1.0f);
                l.text("volume", GD_SIDE_W + 6, GD_VOLUME_Y, 40, 1.0f);
            }
        }
        return l;
    }

    /** The three tools. */
    public enum Tool {
        DISK_MANAGEMENT, PARTITION_MANAGER, GNOME_DISKS
    }
}
