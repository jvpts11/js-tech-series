/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.fire;

import dev.jstech.core.fire.Fires;
import dev.jstech.core.hazard.Pollution;
import dev.jstech.core.jstechseries.hazard.SeriesPollution;

/** The series' fire: every fire burning gives off soot into the chunk it burns in, as the series' pollution counts. */
public final class SeriesFire {

    /* How much soot a fire gives off a second, as a chimney's forty. */
    private static final double SOOT_A_SECOND = 10;

    private SeriesFire() {
    }

    /** Watches the fires, from the Core's constructor, after the series' pollution is declared. */
    public static void declare() {
        Fires.watch((level, fire) -> Pollution.emit(level, fire, SeriesPollution.SOOT, SOOT_A_SECOND));
    }
}
