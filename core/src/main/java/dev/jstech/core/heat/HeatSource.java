/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.heat;

/**
 * Heat a block puts into itself or takes out of itself: a burner, an electric heater turning energy into heat, a
 * reactor's core, a cooler. Each runs at its power until the body reaches its limit, the hottest a heater makes it or
 * the coldest a cooler does, and no further, as a thermostat holds it. The block calls it from its tick while it runs,
 * and an electric heater takes from its energy what was put in, joule for joule.
 *
 * <pre>{@code
 * private static final HeatSource HEATER = HeatSource.heater(5_000, 900);   // 5 kW, up to 900 K
 * ...
 * double joules = HEATER.apply(heat, HeatMath.TICK_SECONDS);   // in its tick; the energy it used
 * }</pre>
 *
 * @param watts how much it puts in a second, positive for a heater and negative for a cooler
 * @param limit the temperature it stops at, in kelvin: the hottest for a heater, the coldest for a cooler
 */
public record HeatSource(double watts, double limit) {

    public HeatSource {
        if (Double.isNaN(watts) || !(limit >= 0.0)) {
            throw new IllegalArgumentException("a heat source has a power and a limit no colder than nothing");
        }
    }

    /** A heater of {@code watts}, up to {@code hottest} kelvin. */
    public static HeatSource heater(final double watts, final double hottest) {
        return new HeatSource(Math.abs(watts), hottest);
    }

    /** A cooler that takes {@code watts} out, down to {@code coldest} kelvin. */
    public static HeatSource cooler(final double watts, final double coldest) {
        return new HeatSource(-Math.abs(watts), coldest);
    }

    /**
     * The joules it puts into a body at {@code temperature} holding {@code capacity} joules a kelvin in
     * {@code seconds}, never past its limit: negative for a cooler.
     */
    public double joules(final double temperature, final double capacity, final double seconds) {
        if (capacity <= 0.0 || seconds <= 0.0 || Double.isNaN(temperature)) {
            return 0.0;
        }
        final double room = (this.limit - temperature) * capacity;
        final double wanted = this.watts * seconds;
        return this.watts >= 0.0 ? Math.max(0.0, Math.min(wanted, room)) : Math.min(0.0, Math.max(wanted, room));
    }

    /** Puts its heat for {@code seconds} into {@code port}, and says how many joules it put in or took out. */
    public double apply(final IHeatPort port, final double seconds) {
        final double joules = joules(port.temperature(), port.capacity(), seconds);
        if (joules != 0.0) {
            port.exchange(joules);
        }
        return joules;
    }
}
