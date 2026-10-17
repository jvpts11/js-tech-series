/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.air.AirMix;
import java.util.Optional;

/**
 * What a dimension is like at one height: its pressure, its temperature and its air there, each optional, the
 * dimension's own where it is not said. Between two bands the pressure and the temperature run straight from one to
 * the other; the air is the lower band's.
 *
 * <pre>{@code
 * "bands": [{ "at": 0, "pressure": 9300, "temperature": 464 }, { "at": 220, "pressure": 101, "temperature": 30 }]
 * }</pre>
 *
 * @param at          the height it holds at
 * @param pressure    the pressure there, in kilopascals
 * @param temperature the temperature there, in degrees Celsius
 * @param air         the air there, from this height up to the next band
 */
public record AltitudeBand(double at, Optional<Double> pressure, Optional<Double> temperature, Optional<AirMix> air) {

    public static final Codec<AltitudeBand> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.doubleRange(-4_096, 4_096).fieldOf("at").forGetter(AltitudeBand::at),
            Codec.doubleRange(0, 100_000).optionalFieldOf("pressure").forGetter(AltitudeBand::pressure),
            Codec.doubleRange(-273.15, 10_000).optionalFieldOf("temperature").forGetter(AltitudeBand::temperature),
            AirMix.CODEC.optionalFieldOf("air").forGetter(AltitudeBand::air))
            .apply(instance, AltitudeBand::new));
}
