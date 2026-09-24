/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.boot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.core.text.Text;
import org.junit.jupiter.api.Test;

class BootMenuTest {

    @Test
    void build_headsTheMenuWithItsManagersTitle() {
        final BootMenu menu = new BootMenu.Builder(BootManager.LOADER).entry(Text.literal("Boot"), 0, "jsc:freebsd")
                .build(200);
        assertEquals(BootManager.LOADER, menu.manager());
        assertEquals("Welcome to FreeBSD", menu.title().english());
        assertEquals(200, menu.countdownTicks());
    }

    @Test
    void restart_addsAnEntryThatBootsNothing() {
        final BootMenu menu = new BootMenu.Builder(BootManager.LOADER)
                .entry(Text.literal("Boot"), 0, "jsc:freebsd").defaultsToLast()
                .restart(Text.literal("Reboot"))
                .build(200);
        assertEquals(2, menu.entries().size());
        assertTrue(menu.entries().get(1).isRestart());
        assertFalse(menu.entries().get(1).isFirmware());
        assertFalse(menu.entries().get(0).isRestart());
        assertEquals(0, menu.defaultIndex());
    }

    @Test
    void firmware_isNeitherABootNorARestart() {
        final BootMenu menu = new BootMenu.Builder(BootManager.GRUB).firmware(Text.literal("Firmware Settings"))
                .build(0);
        assertTrue(menu.entries().get(0).isFirmware());
        assertFalse(menu.entries().get(0).isRestart());
    }

    @Test
    void constructor_keepsNoMoreEntriesThanAMenuMayHold() {
        final BootMenu.Builder out = new BootMenu.Builder(BootManager.GRUB);
        for (int i = 0; i < BootMenu.MOST_ENTRIES + 4; i++) {
            out.entry(Text.literal("System " + i), i, "jsc:system_" + i);
        }
        assertEquals(BootMenu.MOST_ENTRIES, out.build(0).entries().size());
    }

    @Test
    void constructor_readsAMissingManagerAsNone() {
        assertEquals(BootManager.NONE, new BootMenu(null, Text.EMPTY, null, 0, 0).manager());
        assertTrue(BootMenu.NONE.isEmpty());
    }

    @Test
    void secondsLeft_roundsUpSoTheCountNeverShowsZeroWhileItRuns() {
        assertEquals(10, BootMenu.NONE.secondsLeft(200));
        assertEquals(1, BootMenu.NONE.secondsLeft(1));
        assertEquals(0, BootMenu.NONE.secondsLeft(0));
    }
}
