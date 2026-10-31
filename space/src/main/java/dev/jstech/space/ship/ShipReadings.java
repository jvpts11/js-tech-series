/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.ship;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * What a flying ship's console shows of its flight, kept rounded so it changes only as much as a pilot can see: how
 * fast it goes and climbs, its throttle and its boost, its air engines' nozzles and whether they blow down, its gear,
 * whether it flies too slowly to be held up, and how high it flies.
 *
 * @param speed    how fast it goes along its nose, in metres a second, to a tenth
 * @param climb    how fast it rises or sinks besides where its nose takes it, in metres a second, to a tenth
 * @param throttle how far its throttle is open, in hundredths
 * @param charge   how full its boost is, in hundredths
 * @param boosting whether it boosts
 * @param nozzles  how far its nozzles are turned down, in degrees
 * @param lifting  whether its nozzles hold it up, blowing down
 * @param gearDown whether its gear is down
 * @param stalled  whether it flies too slowly to be held up
 * @param altitude how high it flies, in metres ({@link ShipAltitude}), or {@link ShipAltitude#NONE}
 */
public record ShipReadings(double speed, double climb, int throttle, int charge, boolean boosting, int nozzles,
                           boolean lifting, boolean gearDown, boolean stalled, int altitude) {

    /** A ship at rest on the land. */
    public static final ShipReadings PARKED = new ShipReadings(0, 0, 0, 100, false, 0, false, true, false, 0);
    public static final Codec<ShipReadings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.fieldOf("speed").forGetter(ShipReadings::speed),
            Codec.DOUBLE.fieldOf("climb").forGetter(ShipReadings::climb),
            Codec.INT.fieldOf("throttle").forGetter(ShipReadings::throttle),
            Codec.INT.fieldOf("charge").forGetter(ShipReadings::charge),
            Codec.BOOL.fieldOf("boosting").forGetter(ShipReadings::boosting),
            Codec.INT.fieldOf("nozzles").forGetter(ShipReadings::nozzles),
            Codec.BOOL.fieldOf("lifting").forGetter(ShipReadings::lifting),
            Codec.BOOL.fieldOf("gear_down").forGetter(ShipReadings::gearDown),
            Codec.BOOL.fieldOf("stalled").forGetter(ShipReadings::stalled),
            Codec.INT.optionalFieldOf("altitude", 0).forGetter(ShipReadings::altitude)
    ).apply(instance, ShipReadings::new));
    /* Tenths and hundredths, as the readings are kept. */
    private static final double TENTHS = 10.0;
    private static final double HUNDREDTHS = 100.0;

    /** The readings of a tick of a ship's flight flying {@code altitude} metres high, rounded as they are shown. */
    public static ShipReadings of(final ShipFlightModel.Tick tick, final int altitude) {
        final ShipFlightModel.State state = tick.state();
        return new ShipReadings(Math.round(state.speed() * TENTHS) / TENTHS,
                Math.round((state.rise() - state.sink()) * TENTHS) / TENTHS,
                (int) Math.round(state.throttle() * HUNDREDTHS), (int) Math.round(state.charge() * HUNDREDTHS),
                tick.boosting(), (int) Math.round(state.nozzles()), tick.lifting(), tick.gearDown(), tick.stalled(),
                altitude);
    }
}
