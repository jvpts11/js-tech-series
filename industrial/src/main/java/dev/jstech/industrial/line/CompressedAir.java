/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.line;

import dev.jstech.core.connect.Connection;
import dev.jstech.core.jstechseries.SharedCatalogue;
import dev.jstech.core.material.Material;
import dev.jstech.industrial.JsIndustrial;
import net.minecraft.resources.ResourceLocation;

/**
 * A factory's compressed air (industrial/docs/design/machines.md): J's Core's air, kept at the pressure real plant air
 * is, sent along a line of its own, the Compressed Air Line, which runs on J's Core's gas grid and never joins a gas
 * pipe. A machine with an air port ({@link AirPort}) spends it and works fifteen percent faster; a pneumatic post's
 * station spends some for each capsule it sends ({@link PneumaticStation}).
 */
public final class CompressedAir {

    /** The line compressed air travels along: its own, of no other pipe. */
    public static final Connection LINE = Connection.of(ResourceLocation.fromNamespaceAndPath(JsIndustrial.MODID,
            "compressed_air"));
    /** The pressure a factory keeps its air at, in atmospheres: real plant air is kept at six to seven bar. */
    public static final long WORKING_PRESSURE = 7L;
    /** What air gains a machine: fifteen percent of its speed, added beside its upgrades' gains. */
    public static final double SPEED_GAIN = 0.15;

    private CompressedAir() {
    }

    /** What it is: J's Core's air. */
    public static Material air() {
        return SharedCatalogue.AIR;
    }
}
