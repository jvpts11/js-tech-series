/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** The sentences J's Industrial's content says of itself. */
@TextHolder
public final class IndustrialTexts {

    /** What the energy cable is for, as its tooltip says it. */
    public static final TextKey ENERGY_CABLE_JOB = TextKey.of("jsindustrial.cable.energy",
            "Energy line: carries power from the generators to the machines, any distance, losing none");

    private IndustrialTexts() {
    }
}
