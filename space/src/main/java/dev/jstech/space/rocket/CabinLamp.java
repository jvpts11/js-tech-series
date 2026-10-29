/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.rocket;

import dev.jstech.core.id.IStableId;
import dev.jstech.core.id.IStableName;
import java.util.List;

/**
 * The lamps of a capsule's panel: each lights in its own colour when what it tells of is so, as a real spacecraft's
 * caution and warning lamps do, green for what is working, amber for what wants the crew's eye, red for what is
 * wrong. They stand in their bank of two columns and five rows as {@link #BANK} lists them, read along each row.
 */
public enum CabinLamp implements IStableName, IStableId {

    /** The engines of the stage that flies are burning. */
    ENGINE("engine", 0, Colour.GREEN),
    /** The next stage is ready to part. */
    STAGE("stage", 1, Colour.AMBER),
    /** The Flight Computer flies the rocket. */
    GUIDANCE("guidance", 2, Colour.GREEN),
    /** The flight is being given up, the capsule pulled clear. */
    ABORT("abort", 3, Colour.RED),
    /** Something aboard is wrong. */
    MASTER_ALARM("master_alarm", 4, Colour.RED),
    /** The cabin holds its air. */
    CABIN_AIR("cabin_air", 5, Colour.GREEN),
    /** The stage that flies is nearly out of propellant. */
    LOW_PROPELLANT("low_propellant", 6, Colour.AMBER),
    /** The parachutes are armed, or out. */
    PARACHUTES("parachutes", 7, Colour.GREEN),
    /** The legs are spread to land. */
    LEGS("legs", 8, Colour.GREEN),
    /** The docking port holds another craft. */
    DOCKED("docked", 9, Colour.GREEN);

    /** The lamps as they stand in their bank, row by row, the left one of each row first. */
    public static final List<CabinLamp> BANK = List.of(ENGINE, STAGE, GUIDANCE, ABORT, MASTER_ALARM, CABIN_AIR,
            LOW_PROPELLANT, PARACHUTES, LEGS, DOCKED);

    private final String serializedName;
    private final int id;
    private final Colour colour;

    CabinLamp(final String serializedName, final int id, final Colour colour) {
        this.serializedName = serializedName;
        this.id = id;
        this.colour = colour;
    }

    @Override
    public String serializedName() {
        return this.serializedName;
    }

    @Override
    public int id() {
        return this.id;
    }

    /** The colour it lights in. */
    public Colour colour() {
        return this.colour;
    }

    /** The colours a lamp lights in. */
    public enum Colour {
        GREEN, AMBER, RED
    }
}
