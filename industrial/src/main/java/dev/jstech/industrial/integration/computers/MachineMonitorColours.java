/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.integration.computers;

import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.industrial.JsIndustrial;

/**
 * The Machine Monitor's colours, as data: the lamp of a critical alert, red; of a warning, amber; of a notice, grey.
 * Apart from the window, and naming nothing of J's Computers, since a player's game reads every palette as it loads,
 * whether J's Computers is there or not.
 */
@PaletteHolder
public final class MachineMonitorColours {

    /** The alerts' lamps: app/machine_monitor. */
    public static final Palette<Colours> PALETTE = Palettes.declare(JsIndustrial.MODID, "app/machine_monitor",
            new Colours(0xFFD6342A, 0xFFE8A317, 0xFF8A8F96));

    private MachineMonitorColours() {
    }

    /** The lamp of each grade of alert. */
    public record Colours(int critical, int warning, int notice) {
    }
}
