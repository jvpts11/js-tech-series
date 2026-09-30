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

/** Virtual Studio's Properties window: nothing overlaps and nothing leaves the window, at the most it holds. */
class StudioPropertiesLayoutTest {

    @Test
    void layout_isCleanWithEveryPlatformAndEveryVersion() {
        final GuiLayout layout = StudioPropertiesLayout.layout(5, 4);
        assertTrue(layout.isClean(), () -> "overlaps " + layout.overlaps() + ", out of the window "
                + layout.outOfBounds());
    }

    @Test
    void layout_theVersionsSitUnderThePlatformHintAndAboveClose() {
        assertTrue(StudioPropertiesLayout.VERSION_LABEL_Y > StudioPropertiesLayout.PLATFORM_HINT_Y
                + StudioPropertiesLayout.LINE_H);
        assertTrue(StudioPropertiesLayout.versionHintY(StudioPropertiesLayout.VERSION_HINTS)
                <= StudioPropertiesLayout.CLOSE_Y);
    }
}
