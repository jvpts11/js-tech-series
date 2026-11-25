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
    static final TextKey ROW_PLASTIC = TextKey.of("jsc.lithography.row.plastic", "Plastic");
    static final TextKey ROW_PART = TextKey.of("jsc.lithography.row.part", "Part");
    static final TextKey ROW_MOULD = TextKey.of("jsc.lithography.row.mould", "mould");
    static final TextKey ROW_MOULD_SLOT = TextKey.of("jsc.lithography.row.mould_slot", "Mould slot");
    static final TextKey CUT_MOULD = TextKey.of("jsc.lithography.cut_mould", "Cut Mould");
    static final TextKey TOOL_NEVER_USED_UP = TextKey.of("jsc.lithography.tool_never_used_up",
            "A tool is never used up");
    static final TextKey CUTTING_THE = TextKey.of("jsc.lithography.cutting_the", "Cutting the %s mould");
    static final TextKey MOULD_BLANK_HINT = TextKey.of("jsc.lithography.hint.mould_blank",
            "A Mould Blank is in the slot: Cut Mould cuts the %s mould on it");
    static final TextKey MOULD_HINT = TextKey.of("jsc.lithography.hint.mould",
            "%s in the slot: the moulder moulds in it");
    static final TextKey MOULD_EMPTY_HINT = TextKey.of("jsc.lithography.hint.mould_empty",
            "Put a Mould Blank in the moulder's mould slot to cut a mould on it");
    static final TextKey DEVICE_PORT = TextKey.of("jsc.lithography.device_port", "device port");
    static final TextKey ROW_TRAY = TextKey.of("jsc.lithography.row.tray", "Tray");
    static final TextKey PRINT_FILM = TextKey.of("jsc.lithography.print_film", "Print Film");
    static final TextKey PHOTOTOOL_NEVER_USED_UP = TextKey.of("jsc.lithography.phototool_never_used_up",
            "A phototool is never used up");
    static final TextKey LAYOUT_USED_UP = TextKey.of("jsc.lithography.layout_used_up",
            "A Layout is used up writing its mask");
    static final TextKey PRINTING_THE = TextKey.of("jsc.lithography.printing_the", "Printing the %s");
    static final TextKey FILM_BLANK_HINT = TextKey.of("jsc.lithography.hint.film_blank",
            "Blank Film is in the tray: Print Film prints the %s on it");
    static final TextKey NO_STATION = TextKey.of("jsc.lithography.no_station",
            "No Lithography Station is linked to this computer");
    static final TextKey NO_STATION_HINT = TextKey.of("jsc.lithography.no_station_hint",
            "Cable a Legacy or later station's peripheral port to this computer");

    private LithographyTexts() {
    }
}
