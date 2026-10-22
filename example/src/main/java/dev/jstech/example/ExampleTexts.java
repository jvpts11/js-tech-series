/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Example.
 */
package dev.jstech.example;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * The sentences the example mod says, each declared once with its English. The data generation writes them into the
 * mod's English file, and a translation gives each its own words.
 */
@TextHolder
public final class ExampleTexts {

    public static final TextKey ENERGY_CABLE_JOB = TextKey.of("jsexample.cable.energy",
            "Energy line: carries power from the generator to the battery and the crusher");
    public static final TextKey FLUID_PIPE_JOB = TextKey.of("jsexample.cable.fluid",
            "Fluid line: carries oil and water between tanks");
    public static final TextKey SENSOR_READS = TextKey.of("jsexample.sensor.reads", "Reads %s of 15");
    public static final TextKey SENSOR_FULLNESS = TextKey.of("jsexample.sensor.fullness", "How full the block is");
    public static final TextKey LOGBOOK_TITLE = TextKey.of("jsexample.logbook.title", "Logbook");
    public static final TextKey LOGBOOK_MADE = TextKey.of("jsexample.logbook.made",
            "Energy made on this server: %s FE");
    public static final TextKey LOGBOOK_OPENED = TextKey.of("jsexample.logbook.opened",
            "You opened this logbook %s times");
    public static final TextKey LOGBOOK_START_OVER = TextKey.of("jsexample.logbook.start_over", "Start my count over");

    private ExampleTexts() {
    }
}
