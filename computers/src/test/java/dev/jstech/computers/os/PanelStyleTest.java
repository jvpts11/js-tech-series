/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PanelStyleTest {

    @Test
    void unixLike_isTrueForEveryDesktopOfAUnixFamily() {
        assertTrue(PanelStyle.KDE.unixLike());
        assertTrue(PanelStyle.GNOME.unixLike());
        assertTrue(PanelStyle.CINNAMON.unixLike());
        assertTrue(PanelStyle.CDE.unixLike());
    }

    @Test
    void unixLike_isFalseForTheFramesEditions() {
        assertFalse(PanelStyle.FRAMES_95.unixLike());
        assertFalse(PanelStyle.FRAMES_XP.unixLike());
        assertFalse(PanelStyle.FRAMES_11.unixLike());
    }
}
