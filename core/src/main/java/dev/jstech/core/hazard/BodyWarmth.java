/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.hazard;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * How warm a body is, as it is saved with it: its core, and how far it is used to a climate, which moves the edges of
 * its bands: below none, used to the cold, its cold bands lower; above none, used to the heat, its hot bands higher.
 *
 * @param coreKelvin  its core's temperature
 * @param acclimation how far it is used to a climate, in kelvin
 */
public record BodyWarmth(double coreKelvin, double acclimation) {

    /** A body as it is born: its core normal, used to no climate. */
    public static final BodyWarmth NORMAL = new BodyWarmth(BodyHeat.NORMAL, 0);
    /** How it is saved. */
    public static final Codec<BodyWarmth> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.optionalFieldOf("core", BodyHeat.NORMAL).forGetter(BodyWarmth::coreKelvin),
            Codec.DOUBLE.optionalFieldOf("acclimation", 0.0).forGetter(BodyWarmth::acclimation))
            .apply(instance, BodyWarmth::new));
}
