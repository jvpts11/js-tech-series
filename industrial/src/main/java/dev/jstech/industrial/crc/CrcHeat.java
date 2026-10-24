/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.crc;

/**
 * A second of a Computational Research Centre's heat, with nothing of the game in it. All the power its processors
 * draw becomes heat in its body; then each way of cooling takes away what it can, up to its rating and never bringing
 * the body below what it cools to: the air round a heat sink, the hot water a liquid cooler's water comes out as, the
 * far end of a heat pipe. When the cooling is enough the temperature settles; when it is not, it rises, and past
 * {@link #BURNS_AT} the part that heats most burns (industrial/docs/design/science.md). Pure.
 */
public final class CrcHeat {

    /** The temperature past which a part burns: 100 degrees Celsius, in kelvin. */
    public static final double BURNS_AT = 373.15;

    private CrcHeat() {
    }

    /**
     * The body's heat a second on: at {@code kelvin}, holding {@code capacity} joules a kelvin, given {@code made}
     * joules by its processors and cooled by {@code air}, {@code water} and {@code pipe}, in that order.
     */
    public static Step second(final double kelvin, final double capacity, final double made, final Way air,
                              final Way water, final Way pipe) {
        if (!(capacity > 0)) {
            throw new IllegalArgumentException("a body holds some heat a kelvin; got " + capacity);
        }
        double now = kelvin + Math.max(0.0, made) / capacity;
        final double byAir = air.takes(now, capacity);
        now -= byAir / capacity;
        final double byWater = water.takes(now, capacity);
        now -= byWater / capacity;
        final double byPipe = pipe.takes(now, capacity);
        now -= byPipe / capacity;
        return new Step(now, byAir, byWater, byPipe);
    }

    /**
     * A way of cooling, for a second.
     *
     * @param joules the most it takes away in the second
     * @param floor  the coldest it brings the body to, in kelvin
     */
    public record Way(double joules, double floor) {

        /** A way that takes nothing. */
        public static final Way NONE = new Way(0.0, 0.0);

        public Way {
            joules = Math.max(0.0, joules);
        }

        /* What it takes from a body at {@code kelvin} holding {@code capacity} a kelvin. */
        private double takes(final double kelvin, final double capacity) {
            return Math.min(this.joules, Math.max(0.0, (kelvin - this.floor) * capacity));
        }
    }

    /**
     * The heat a second on.
     *
     * @param kelvin the body's temperature
     * @param air    what the air took away, in joules
     * @param water  what the water took away
     * @param pipe   what the heat pipe took away
     */
    public record Step(double kelvin, double air, double water, double pipe) {

        /** Whether the body is past {@link #BURNS_AT}, so the part that heats most burns. */
        public boolean burns() {
            return this.kelvin > BURNS_AT;
        }
    }
}
