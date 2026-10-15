/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.heat;

/**
 * How much heat passes between two streams through the wall of an exchanger, by the effectiveness method engineers size
 * exchangers with: from what each stream carries for each kelvin it changes (its mass flow times its specific heat, in
 * watts a kelvin) and from how well the wall passes heat (its conductance, the area times how well each square metre
 * passes it, in watts a kelvin). Neither stream ever comes out past the temperature the other went in at, however big
 * the wall. Pure, so its numbers are tested without the game.
 *
 * <pre>{@code
 * // 2 kg/s of oil at 600 K against 1 kg/s of water at 300 K, through 5 kW/K of wall
 * double watts = HeatExchanger.counterflow(600, 2 * 2_000, 300, 1 * 4_186, 5_000);
 * }</pre>
 */
public final class HeatExchanger {

    private HeatExchanger() {
    }

    /**
     * The watts that pass from a hot stream to a cold one running the other way along the wall, as the best exchangers
     * run: positive from the hot to the cold, nothing when either carries nothing.
     *
     * @param hotIn       the temperature the hot stream goes in at, in kelvin
     * @param hotRate     what the hot stream carries for each kelvin it changes, in watts a kelvin
     * @param coldIn      the temperature the cold stream goes in at, in kelvin
     * @param coldRate    what the cold stream carries for each kelvin it changes, in watts a kelvin
     * @param conductance how well the wall passes heat, in watts a kelvin
     */
    public static double counterflow(final double hotIn, final double hotRate, final double coldIn,
                                     final double coldRate, final double conductance) {
        if (hotRate <= 0.0 || coldRate <= 0.0 || conductance <= 0.0) {
            return 0.0;
        }
        final double least = Math.min(hotRate, coldRate);
        final double ratio = least / Math.max(hotRate, coldRate);
        final double units = conductance / least;
        final double effectiveness;
        if (ratio > 1.0 - 1e-9) {
            effectiveness = units / (1.0 + units);
        } else {
            final double fall = Math.exp(-units * (1.0 - ratio));
            effectiveness = (1.0 - fall) / (1.0 - ratio * fall);
        }
        return effectiveness * least * (hotIn - coldIn);
    }

    /**
     * The watts that pass from a wall at {@code wall} kelvin into a stream that goes in at {@code streamIn}, as in a
     * boiler's tubes or a pipe through a hot block: positive into the stream, negative when the stream is the hotter.
     * The stream comes out no further than the wall's temperature.
     *
     * @param rate        what the stream carries for each kelvin it changes, in watts a kelvin
     * @param conductance how well the wall passes heat, in watts a kelvin
     */
    public static double wall(final double wall, final double streamIn, final double rate, final double conductance) {
        if (rate <= 0.0 || conductance <= 0.0) {
            return 0.0;
        }
        return (1.0 - Math.exp(-conductance / rate)) * rate * (wall - streamIn);
    }
}
