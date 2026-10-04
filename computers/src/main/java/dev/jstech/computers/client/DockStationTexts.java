/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** The words of the Dock Station's window, kept apart from the screen so they can be read without the player's game. */
@TextHolder
final class DockStationTexts {

    static final TextKey HOST = TextKey.of("jsc.dock_station.host", "HOST");
    static final TextKey NO_HOST = TextKey.of("jsc.dock_station.no_host", "Not docked to a computer");
    static final TextKey BAY_HDD = TextKey.of("jsc.dock_station.bay_hdd", "3.5\" BAY");
    static final TextKey BAY_SSD = TextKey.of("jsc.dock_station.bay_ssd", "2.5\" BAY");
    static final TextKey BAY_NVME = TextKey.of("jsc.dock_station.bay_nvme", "M.2 TRAY");
    static final TextKey USB = TextKey.of("jsc.dock_station.usb", "USB");
    static final TextKey LETTERED = TextKey.of("jsc.dock_station.lettered", "%s: %s");
    static final TextKey USED = TextKey.of("jsc.dock_station.used", "%s, %s used");
    static final TextKey EMPTY_DISK = TextKey.of("jsc.dock_station.empty_disk", "%s, empty");
    static final TextKey FULL_PERCENT = TextKey.of("jsc.dock_station.full_percent", "%s%% used");
    static final TextKey MOUNTED = TextKey.of("jsc.dock_station.mounted", "mounted");
    static final TextKey DOCKED = TextKey.of("jsc.dock_station.docked", "docked");
    static final TextKey EMPTY = TextKey.of("jsc.dock_station.empty", "empty");
    static final TextKey EJECT = TextKey.of("jsc.dock_station.eject", "EJECT");
    static final TextKey NOTE_1 = TextKey.of("jsc.dock_station.note_1", "Any era's disk: IDE and SATA in the bays,");
    static final TextKey NOTE_2 = TextKey.of("jsc.dock_station.note_2", "NVMe in the tray, a stick in the port.");
    static final TextKey LINKED = TextKey.of("jsc.dock_station.linked", "Linked to a computer");
    static final TextKey OFFLINE = TextKey.of("jsc.dock_station.offline", "No computer linked");

    private DockStationTexts() {
    }
}
