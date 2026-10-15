/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.energy;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** What the Core says about energy: amounts, rates and a block's port. */
@TextHolder
public final class EnergyTexts {

    /** An amount, the SI prefix of its scale and its unit: "1.5 kJ". */
    public static final TextKey AMOUNT = TextKey.of("jscore.energy.amount", "%s %s%s");
    /** An amount a tick: "40 FE/t". */
    public static final TextKey PER_TICK = TextKey.of("jscore.energy.per_tick", "%s %s%s/t");
    /** A port's class: "Port: Low voltage". */
    public static final TextKey PORT = TextKey.of("jscore.energy.port", "Port: %s");
    /** A port's class and its rating: "Port: Low voltage, 400 V". */
    public static final TextKey PORT_RATED = TextKey.of("jscore.energy.port_rated", "Port: %s, %s");
    /** What a store holds: "Stored: 1.5K J of 10K J". */
    public static final TextKey STORED = TextKey.of("jscore.energy.stored", "Stored: %s of %s");
    /** What a generator gives at most, and the least it always makes. */
    public static final TextKey OUTPUT = TextKey.of("jscore.energy.output", "Output: up to %s");
    public static final TextKey OUTPUT_MINIMUM = TextKey.of("jscore.energy.output_minimum",
            "Output: %s to %s");
    /** A machine whose fuse blew, so its energy is cut. */
    public static final TextKey FUSE_BLOWN = TextKey.of("jscore.energy.fuse_blown", "Fuse blown: put in a new one");
    /** A store a line of a class too high burnt. */
    public static final TextKey DAMAGED = TextKey.of("jscore.energy.damaged", "Damaged: repair it to run again");
    /** A store a live line of a class below meets. */
    public static final TextKey UNDERVOLTAGE = TextKey.of("jscore.energy.undervoltage", "Undervoltage");
    /** A store a live line of a class above meets, where its energy lets it refuse to start rather than burn. */
    public static final TextKey OVERVOLTAGE = TextKey.of("jscore.energy.overvoltage", "Overvoltage");

    private EnergyTexts() {
    }
}
