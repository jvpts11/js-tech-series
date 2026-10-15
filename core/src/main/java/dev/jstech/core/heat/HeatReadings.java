/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.heat;

import dev.jstech.core.format.UnitFormatter;
import dev.jstech.core.signal.Reading;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * Temperature as sensors read it and speak it in redstone: a thermometer over the span of the air and of water, from
 * −40 °C to 100 °C, and a thermocouple over the span of a furnace's, from −200 °C to 1,350 °C, as a real type K one
 * reads; and the line that tells a player how hot a block is.
 *
 * <pre>{@code
 * public List<Reading> readings() {
 *     return List.of(HeatReadings.thermocouple(HeatPorts.at(level, below, Direction.UP).temperature()));
 * }
 * }</pre>
 */
@TextHolder
public final class HeatReadings {

    public static final TextKey TEMPERATURE = TextKey.of("jscore.heat.reading.temperature", "Temperature");
    public static final TextKey KELVIN = TextKey.of("jscore.heat.reading.kelvin", "K");
    public static final TextKey LINE = TextKey.of("jscore.heat.line", "Temperature: %s K (%s °C)");

    /** The coldest and the hottest a thermometer reads, in kelvin. */
    public static final double THERMOMETER_LEAST = 233.15;
    public static final double THERMOMETER_MOST = 373.15;
    /** The coldest and the hottest a thermocouple reads, in kelvin. */
    public static final double THERMOCOUPLE_LEAST = 73.15;
    public static final double THERMOCOUPLE_MOST = 1_623.15;

    private HeatReadings() {
    }

    /** A thermometer's reading of {@code kelvin}: no signal at −40 °C, the full 15 at 100 °C. */
    public static Reading thermometer(final double kelvin) {
        return new Reading(TEMPERATURE, kelvin, THERMOMETER_LEAST, THERMOMETER_MOST, KELVIN);
    }

    /** A thermocouple's reading of {@code kelvin}: no signal at −200 °C, the full 15 at 1,350 °C. */
    public static Reading thermocouple(final double kelvin) {
        return new Reading(TEMPERATURE, kelvin, THERMOCOUPLE_LEAST, THERMOCOUPLE_MOST, KELVIN);
    }

    /** The line that tells how hot {@code kelvin} is, in kelvin and in degrees Celsius. */
    public static Text line(final double kelvin) {
        final UnitFormatter format = UnitFormatter.forCurrentLocale();
        return LINE.with(Text.literal(format.decimal(kelvin)), Text.literal(format.decimal(HeatMath.celsius(kelvin))));
    }
}
