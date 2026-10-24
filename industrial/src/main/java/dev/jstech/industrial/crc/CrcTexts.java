/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.crc;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** What the Computational Research Centre and its parts say to the player. */
@TextHolder
public final class CrcTexts {

    /** The machine's name, which its terminal's screen opens under. */
    public static final TextKey NAME = TextKey.of("jsindustrial.crc.name", "Computational Research Centre");
    /** A processor's tooltip: its tier, the research it makes and the power it draws, all of which is heat. */
    public static final TextKey PROCESSOR = TextKey.of("jsindustrial.crc.part.processor",
            "%s: %s research a second, drawing %s kW, all of it heat");
    public static final TextKey HEAT_SINK = TextKey.of("jsindustrial.crc.part.heat_sink",
            "%s: sheds up to %s kW to the air, by itself");
    public static final TextKey LIQUID_COOLER = TextKey.of("jsindustrial.crc.part.liquid_cooler",
            "%s: sheds up to %s kW into the coolant's water, with the Coolant Inlet and Outlet");
    public static final TextKey HEAT_PIPE_COOLER = TextKey.of("jsindustrial.crc.part.heat_pipe_cooler",
            "%s: sheds up to %s kW out through the Heat Port, by heat pipe");
    /** Where a part goes. */
    public static final TextKey GOES_IN = TextKey.of("jsindustrial.crc.part.goes_in",
            "Goes in a place of a Computational Research Centre");
    /** What a part that burnt says, to the owner. */
    public static final TextKey BURNT = TextKey.of("jsindustrial.crc.burnt",
            "A Computational Research Centre at %s, %s, %s ran past 100 °C: its %s burnt.");

    private CrcTexts() {
    }
}
