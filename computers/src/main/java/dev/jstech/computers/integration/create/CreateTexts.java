/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.integration.create;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** What J's Computers says about Create: a computer a contraption carried, and the kinetic networks' values. */
@TextHolder
public final class CreateTexts {

    /** A computer a contraption carried and set down, shut down. */
    public static final TextKey CARRIED_OFF = TextKey.of("jsc.create.carried_off",
            "Shut down: a Create contraption carried it here");
    static final TextKey SPEED = TextKey.of("jsc.create.parameter.speed", "Speed");
    static final TextKey STRESS = TextKey.of("jsc.create.parameter.stress", "Stress");
    static final TextKey CAPACITY = TextKey.of("jsc.create.parameter.capacity", "Capacity");
    static final TextKey TARGET_SPEED = TextKey.of("jsc.create.parameter.target_speed", "Target speed");
    static final TextKey RPM = TextKey.of("jsc.create.unit.rpm", "RPM");
    static final TextKey STRESS_UNITS = TextKey.of("jsc.create.unit.su", "su");
    static final TextKey KINETICS = TextKey.of("jsc.create.table.kinetics", "Create's kinetic networks");
    static final TextKey COLUMN_BLOCK = TextKey.of("jsc.create.column.block", "Block");
    static final TextKey COLUMN_OVERSTRESSED = TextKey.of("jsc.create.column.overstressed", "Overstressed");

    private CreateTexts() {
    }
}
