/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.power;

import dev.jstech.core.id.IStableId;
import dev.jstech.core.id.StableIds;

/**
 * How a machine's UPS stands, as its system and its screens show it: on the line, on its battery, or in bypass, how
 * full the battery is and how long it lasts at the load it carries.
 *
 * <p>Pure: no game types.
 *
 * @param mode        where the load's power comes from
 * @param permille    how full the battery is, in thousandths
 * @param minutesLeft how long the battery lasts at the load, in whole minutes
 */
public record UpsReading(Mode mode, int permille, long minutesLeft) {

    /** No UPS: the machine runs straight from the line. */
    public static final UpsReading NONE = new UpsReading(Mode.NONE, 0, 0L);

    public UpsReading {
        mode = mode == null ? Mode.NONE : mode;
        permille = Math.max(0, Math.min(1_000, permille));
        minutesLeft = Math.max(0L, minutesLeft);
    }

    /** Whether there is a UPS at all. */
    public boolean present() {
        return this.mode != Mode.NONE;
    }

    /** Whether the load runs on the battery, the line gone. */
    public boolean onBattery() {
        return this.mode == Mode.ON_BATTERY;
    }

    /**
     * Whether the battery is near its end, down to where the system shuts itself down, which the red "replace
     * battery" lamp shows.
     */
    public boolean nearEnd() {
        return present() && this.permille <= UpsRating.SHUTDOWN_PERMILLE;
    }

    /** How full the battery is, in whole percent. */
    public int percent() {
        return this.permille / 10;
    }

    /** Where a UPS's load takes its power from; saved and sent by its id. */
    public enum Mode implements IStableId {
        /** There is no UPS. */
        NONE(0),
        /** The line carries the load and charges the battery. */
        ON_LINE(1),
        /** The line is gone and the battery carries the load. */
        ON_BATTERY(2),
        /** The load is more than the UPS carries: it runs straight from the line, unprotected. */
        BYPASS(3);

        private final int id;

        private static final StableIds<Mode> IDS = StableIds.of(Mode.class);

        Mode(final int id) {
            this.id = id;
        }

        /** The mode that declares {@code id}; an id no mode declares reads as no UPS. */
        public static Mode byId(final int id) {
            return IDS.byId(id, NONE);
        }

        @Override
        public int id() {
            return this.id;
        }
    }
}
