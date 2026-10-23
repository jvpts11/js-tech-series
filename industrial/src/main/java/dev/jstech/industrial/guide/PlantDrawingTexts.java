/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.guide;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * The names of the symbols the Plant Drawings' diagrams draw, as their legends write them: those of a single-line
 * diagram, of a piping and instrumentation diagram, and the fault of a geological section, whose rocks are named by
 * their blocks.
 */
@TextHolder
public final class PlantDrawingTexts {

    public static final TextKey GENERATOR = TextKey.of("jsindustrial.drawing.generator", "Generator");
    public static final TextKey BREAKER = TextKey.of("jsindustrial.drawing.breaker", "Circuit breaker");
    public static final TextKey TRANSFORMER = TextKey.of("jsindustrial.drawing.transformer", "Transformer");
    public static final TextKey BUS = TextKey.of("jsindustrial.drawing.bus", "Busbar");
    public static final TextKey LINE = TextKey.of("jsindustrial.drawing.line", "Cable or line");
    public static final TextKey MOTOR = TextKey.of("jsindustrial.drawing.motor", "A machine (a load)");
    public static final TextKey VESSEL = TextKey.of("jsindustrial.drawing.vessel", "Tank or vessel");
    public static final TextKey PUMP = TextKey.of("jsindustrial.drawing.pump", "Pump");
    public static final TextKey VALVE = TextKey.of("jsindustrial.drawing.valve", "Valve");
    public static final TextKey CHECK_VALVE = TextKey.of("jsindustrial.drawing.check_valve", "Check valve: one way");
    public static final TextKey INSTRUMENT = TextKey.of("jsindustrial.drawing.instrument",
            "Instrument: PI is a pressure gauge");
    public static final TextKey PIPE = TextKey.of("jsindustrial.drawing.pipe", "Pipe");
    public static final TextKey SIGNAL = TextKey.of("jsindustrial.drawing.signal", "Signal");
    public static final TextKey FAULT = TextKey.of("jsindustrial.drawing.fault", "Fault");
    /** Where the water of the P&amp;ID of the general drawings goes, as its arrow's note says. */
    public static final TextKey TO_BOILER = TextKey.of("jsindustrial.drawing.to_boiler", "TO BOILER");

    private PlantDrawingTexts() {
    }
}
