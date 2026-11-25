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

/** The words of the lithography program's text screen on MC-DOS. */
@TextHolder
final class LithoTuiTexts {

    static final TextKey PROGRAM = TextKey.of("jsc.cli.litho.program", "Midsoft Lithography 1.0");
    static final TextKey EXIT = TextKey.of("jsc.cli.litho.exit", "F3=Exit");
    static final TextKey STATION_LINE = TextKey.of("jsc.cli.litho.station_line", "Lithography Station, %s");
    static final TextKey OF_STATIONS = TextKey.of("jsc.cli.litho.of_stations", "%s (station %s of %s)");
    static final TextKey HEAD_STATION = TextKey.of("jsc.cli.litho.head.station", "STATION");
    static final TextKey HEAD_MASKS = TextKey.of("jsc.cli.litho.head.masks", "MASKS");
    static final TextKey WRITE_HINT = TextKey.of("jsc.cli.litho.write_hint",
            "W writes the %s mask on the blank in the slot");
    static final TextKey KEYS = TextKey.of("jsc.cli.litho.keys", "Arrows move   Tab kind   W write mask   F3 exit");
    static final TextKey KEYS_STATIONS = TextKey.of("jsc.cli.litho.keys_stations",
            "Arrows move   Tab kind   W write mask   S station   F3 exit");
    static final TextKey HEAD_MOULDS = TextKey.of("jsc.cli.litho.head.moulds", "MOULDS");
    static final TextKey CUT_HINT = TextKey.of("jsc.cli.litho.cut_hint",
            "W cuts the %s mould on the blank in the slot");
    static final TextKey KEYS_MOULDER = TextKey.of("jsc.cli.litho.keys_moulder",
            "Arrows move   Tab kind   W cut mould   F3 exit");
    static final TextKey KEYS_MOULDER_STATIONS = TextKey.of("jsc.cli.litho.keys_moulder_stations",
            "Arrows move   Tab kind   W cut mould   S station   F3 exit");
    static final TextKey HEAD_FILMS = TextKey.of("jsc.cli.litho.head.films", "FILMS");
    static final TextKey PRINT_HINT = TextKey.of("jsc.cli.litho.print_hint",
            "W prints the %s on the Blank Film in the tray");
    static final TextKey KEYS_PRINTER = TextKey.of("jsc.cli.litho.keys_printer",
            "Arrows move   Tab kind   W print film   F3 exit");
    static final TextKey KEYS_PRINTER_STATIONS = TextKey.of("jsc.cli.litho.keys_printer_stations",
            "Arrows move   Tab kind   W print film   S station   F3 exit");
    static final TextKey VERSION = TextKey.of("jsc.cli.litho.version", "1.0");
    static final TextKey PERCENT = TextKey.of("jsc.cli.litho.percent", "%s%%");

    private LithoTuiTexts() {
    }
}
