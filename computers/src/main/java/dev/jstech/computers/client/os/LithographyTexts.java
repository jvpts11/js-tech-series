/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** The words of the lithography program's window. */
@TextHolder
final class LithographyTexts {

    static final TextKey TITLE = TextKey.of("jsc.lithography.title", "Lithography");
    static final TextKey MENU_FILE = TextKey.of("jsc.lithography.menu.file", "File");
    static final TextKey MENU_STATION = TextKey.of("jsc.lithography.menu.station", "Station");
    static final TextKey MENU_HELP = TextKey.of("jsc.lithography.menu.help", "Help");
    static final TextKey EXIT = TextKey.of("jsc.lithography.exit", "Exit");
    static final TextKey ABOUT = TextKey.of("jsc.lithography.about", "About Lithography");
    static final TextKey STATION_N = TextKey.of("jsc.lithography.station_n", "Station %s");
    static final TextKey GROUP_STATION = TextKey.of("jsc.lithography.group.station", "Station");
    static final TextKey ROW_STATION = TextKey.of("jsc.lithography.row.station", "Station");
    static final TextKey ROW_LINKED = TextKey.of("jsc.lithography.row.linked", "Linked on");
    static final TextKey PERIPHERAL_PORT = TextKey.of("jsc.lithography.peripheral_port", "peripheral port");
    static final TextKey ROW_STATUS = TextKey.of("jsc.lithography.row.status", "Status");
    static final TextKey ROW_WAFER = TextKey.of("jsc.lithography.row.wafer", "Wafer");
    static final TextKey ROW_LAYER = TextKey.of("jsc.lithography.row.layer", "Layer");
    static final TextKey ROW_LITHO = TextKey.of("jsc.lithography.row.litho", "litho");
    static final TextKey ROW_SLOT = TextKey.of("jsc.lithography.row.slot", "Mask slot");
    static final TextKey HELD = TextKey.of("jsc.lithography.held", "%s MB held");
    static final TextKey NOT_RUNNING = TextKey.of("jsc.lithography.not_running", "not running");
    static final TextKey MILLIBUCKETS = TextKey.of("jsc.lithography.millibuckets", "%s mB");
    static final TextKey SECONDS = TextKey.of("jsc.lithography.seconds", "%s s");
    static final TextKey WRITE_MASK = TextKey.of("jsc.lithography.write_mask", "Write Mask");
    static final TextKey NEVER_USED_UP = TextKey.of("jsc.lithography.never_used_up", "A mask is never used up");
    static final TextKey WRITING_THE = TextKey.of("jsc.lithography.writing_the", "Writing the %s mask");
    static final TextKey BLANK_HINT = TextKey.of("jsc.lithography.hint.blank",
            "A Mask Blank is in the slot: Write Mask writes the %s mask on it");
    static final TextKey MASK_HINT = TextKey.of("jsc.lithography.hint.mask",
            "%s in the slot: the station exposes through it");
    static final TextKey EMPTY_HINT = TextKey.of("jsc.lithography.hint.empty",
            "Put a Mask Blank in the station's mask slot to write a mask on it");
    static final TextKey NO_STATION = TextKey.of("jsc.lithography.no_station",
            "No Lithography Station is linked to this computer");
    static final TextKey NO_STATION_HINT = TextKey.of("jsc.lithography.no_station_hint",
            "Cable a Legacy or later station's peripheral port to this computer");

    private LithographyTexts() {
    }
}
