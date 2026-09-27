/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.core.gui.layout.GuiLayout;
import org.junit.jupiter.api.Test;

class KvmChannelLayoutTest {

    @Test
    void layout_isCleanWithNoChannels() {
        assertTrue(KvmChannelLayout.layout(0).isClean());
    }

    @Test
    void layout_isCleanWithTheMostChannelsASwitchReports() {
        final GuiLayout l = KvmChannelLayout.layout(KvmChannelLayout.MOST_CHANNELS);
        assertTrue(l.overlaps().isEmpty(), "overlaps: " + l.overlaps());
        assertTrue(l.outOfBounds().isEmpty(), "out of bounds: " + l.outOfBounds());
    }

    @Test
    void height_growsByOneRowPerChannel() {
        assertTrue(KvmChannelLayout.height(1) < KvmChannelLayout.height(2));
        assertTrue(KvmChannelLayout.height(2) - KvmChannelLayout.height(1) == KvmChannelLayout.ROW_H);
    }

    @Test
    void rowY_stepsByTheRowHeight() {
        assertTrue(KvmChannelLayout.rowY(1) - KvmChannelLayout.rowY(0) == KvmChannelLayout.ROW_H);
    }
}
