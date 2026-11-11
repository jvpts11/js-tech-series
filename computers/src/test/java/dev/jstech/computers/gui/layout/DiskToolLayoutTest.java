/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

import dev.jstech.core.gui.layout.GuiLayout;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DiskToolLayoutTest {

    @Test
    void layout_everyToolIsClean() {
        for (final DiskToolLayout.Tool tool : DiskToolLayout.Tool.values()) {
            final GuiLayout layout = DiskToolLayout.layout(tool);
            assertTrue(layout.isClean(), tool + ": " + layout.overlaps() + " " + layout.outOfBounds());
        }
    }

    @Test
    void layout_diskManagementShowsThreeDisksAboveItsStatusBar() {
        final int last = DiskToolLayout.DM_MAP_Y + DiskToolLayout.DM_DISKS
                * (DiskToolLayout.DM_DISK_H + DiskToolLayout.DM_DISK_GAP);
        assertTrue(last <= DiskToolLayout.DM_STATUS_Y, "the disks end at " + last);
    }
}
