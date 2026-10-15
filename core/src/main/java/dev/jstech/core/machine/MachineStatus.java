/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.machine;

import dev.jstech.core.id.IStableName;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * What a machine is doing, always in view on its screen and to whoever looks at it: working, waiting for input, its
 * output full, without energy, on undervoltage, its fuse blown, damaged, turned off by redstone, or switched off by
 * hand. Each lights one of the three lamps every machine's face has, RUN, WAIT or FAULT.
 */
@TextHolder
public enum MachineStatus implements IStableName {

    WORKING("working", TextKey.of("jscore.machine.status.working", "Working"), Lamp.RUN),
    WAITING("waiting", TextKey.of("jscore.machine.status.waiting", "Waiting for input"), Lamp.WAIT),
    OUTPUT_FULL("output_full", TextKey.of("jscore.machine.status.output_full", "Output full"), Lamp.WAIT),
    NO_ENERGY("no_energy", TextKey.of("jscore.machine.status.no_energy", "No energy"), Lamp.FAULT),
    UNDERVOLTAGE("undervoltage", TextKey.of("jscore.machine.status.undervoltage", "Undervoltage"), Lamp.FAULT),
    FUSE_BLOWN("fuse_blown", TextKey.of("jscore.machine.status.fuse_blown", "Fuse blown"), Lamp.FAULT),
    /** Driven by a shaft that turns slower than the least it works at. */
    TOO_SLOW("too_slow", TextKey.of("jscore.machine.status.too_slow", "Turning too slowly"), Lamp.FAULT),
    /** Past the hottest it stands: stopped to cool. */
    OVERHEATED("overheated", TextKey.of("jscore.machine.status.overheated", "Overheated"), Lamp.FAULT),
    DAMAGED("damaged", TextKey.of("jscore.machine.status.damaged", "Damaged"), Lamp.FAULT),
    OFF_BY_REDSTONE("off_by_redstone", TextKey.of("jscore.machine.status.off_by_redstone", "Off by redstone"),
            Lamp.WAIT),
    SWITCHED_OFF("switched_off", TextKey.of("jscore.machine.status.switched_off", "Switched off"), Lamp.WAIT);

    private final String serializedName;
    private final TextKey name;
    private final Lamp lamp;

    MachineStatus(final String serializedName, final TextKey name, final Lamp lamp) {
        this.serializedName = serializedName;
        this.name = name;
        this.lamp = lamp;
    }

    @Override
    public String serializedName() {
        return this.serializedName;
    }

    /** What a player calls it. */
    public TextKey text() {
        return this.name;
    }

    /** The lamp it lights. */
    public Lamp lamp() {
        return this.lamp;
    }

    /** Whether it is a state the machine can rest in, asleep, until something it waits on changes. */
    public boolean restful() {
        return this != WORKING && this != UNDERVOLTAGE;
    }

    /** The three lamps of a machine's face. */
    public enum Lamp {
        /** It works. */
        RUN,
        /** It waits: for input, for room, for a signal, or for its switch. */
        WAIT,
        /** Something is wrong that a player has to see to: energy, voltage, a fuse, damage. */
        FAULT
    }
}
