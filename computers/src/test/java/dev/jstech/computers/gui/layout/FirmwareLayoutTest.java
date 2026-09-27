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

class FirmwareLayoutTest {

    @Test
    void cliLayout_isClean() {
        assertTrue(FirmwareLayout.cliLayout().isClean());
    }

    @Test
    void biosLayout_isClean() {
        final GuiLayout l = FirmwareLayout.biosLayout();
        assertTrue(l.overlaps().isEmpty(), "overlaps: " + l.overlaps());
        assertTrue(l.outOfBounds().isEmpty(), "out of bounds: " + l.outOfBounds());
    }

    @Test
    void uefiLayout_isClean() {
        final GuiLayout l = FirmwareLayout.uefiLayout();
        assertTrue(l.overlaps().isEmpty(), "overlaps: " + l.overlaps());
        assertTrue(l.outOfBounds().isEmpty(), "out of bounds: " + l.outOfBounds());
    }

    @Test
    void bios_helpBoxStandsClearOfTheContentBox() {
        assertTrue(FirmwareLayout.biosHelpX() > FirmwareLayout.BIOS_BOX_X + FirmwareLayout.BIOS_BOX_W);
    }

    @Test
    void uefi_mainPanelStandsClearOfTheNav() {
        assertTrue(FirmwareLayout.uefiMainX() > FirmwareLayout.UEFI_NAV_X + FirmwareLayout.UEFI_NAV_W);
    }
}
