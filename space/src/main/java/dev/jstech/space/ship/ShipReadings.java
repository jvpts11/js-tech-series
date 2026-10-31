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
import net.minecraft.world.phys.Vec3;

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
 * @param pilot    its autopilot, as its HUD shows it
 */
public record ShipReadings(double speed, double climb, int throttle, int charge, boolean boosting, int nozzles,
                           boolean lifting, boolean gearDown, boolean stalled, int altitude, Pilot pilot) {

    /** A ship at rest on the land. */
    public static final ShipReadings PARKED = new ShipReadings(0, 0, 0, 100, false, 0, false, true, false, 0,
            Pilot.NONE);
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
            Codec.INT.optionalFieldOf("altitude", 0).forGetter(ShipReadings::altitude),
            Pilot.CODEC.optionalFieldOf("pilot", Pilot.NONE).forGetter(ShipReadings::pilot)
    ).apply(instance, ShipReadings::new));
    /* Tenths and hundredths, as the readings are kept. */
    private static final double TENTHS = 10.0;
    private static final double HUNDREDTHS = 100.0;

    /**
     * The readings of a tick of a ship's flight flying {@code altitude} metres high, its autopilot showing
     * {@code pilot}, rounded as they are shown.
     */
    public static ShipReadings of(final ShipFlightModel.Tick tick, final int altitude, final Pilot pilot) {
        final ShipFlightModel.State state = tick.state();
        return new ShipReadings(Math.round(state.speed() * TENTHS) / TENTHS,
                Math.round((state.rise() - state.sink()) * TENTHS) / TENTHS,
                (int) Math.round(state.throttle() * HUNDREDTHS), (int) Math.round(state.charge() * HUNDREDTHS),
                tick.boosting(), (int) Math.round(state.nozzles()), tick.lifting(), tick.gearDown(), tick.stalled(),
                altitude, pilot);
    }

    /**
     * A ship's autopilot as its HUD shows it: the mode engaged, none with none, where its symbol stands in the world,
     * and what it reads by it.
     *
     * @param mode    the mode engaged, by its name; empty with none
     * @param target  where its symbol stands: the landing spot, the climb ahead, the other ship's collar
     * @param range   how far off the target is, in metres, to a tenth
     * @param closing how fast it closes on it, in metres a second, to a tenth
     * @param aligned whether docking it is in line with the other's collar
     * @param apogee  how high the climb tops out, over the sea, in metres
     * @param seconds how long till then, in seconds
     */
    public record Pilot(String mode, Vec3 target, double range, double closing, boolean aligned, int apogee,
                        int seconds) {

        /** No autopilot engaged. */
        public static final Pilot NONE = new Pilot("", Vec3.ZERO, 0, 0, false, 0, 0);
        public static final Codec<Pilot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("mode").forGetter(Pilot::mode),
                Vec3.CODEC.fieldOf("target").forGetter(Pilot::target),
                Codec.DOUBLE.fieldOf("range").forGetter(Pilot::range),
                Codec.DOUBLE.fieldOf("closing").forGetter(Pilot::closing),
                Codec.BOOL.fieldOf("aligned").forGetter(Pilot::aligned),
                Codec.INT.fieldOf("apogee").forGetter(Pilot::apogee),
                Codec.INT.fieldOf("seconds").forGetter(Pilot::seconds)
        ).apply(instance, Pilot::new));

        /** {@code mode} engaged, its symbol at {@code target}, its readings rounded as they are shown. */
        public static Pilot of(final ShipAutopilot.Mode mode, final Vec3 target, final double range,
                               final double closing, final boolean aligned, final int apogee, final int seconds) {
            return new Pilot(mode.serializedName(), new Vec3(Math.round(target.x * TENTHS) / TENTHS,
                    Math.round(target.y * TENTHS) / TENTHS, Math.round(target.z * TENTHS) / TENTHS),
                    Math.round(range * TENTHS) / TENTHS, Math.round(closing * TENTHS) / TENTHS, aligned, apogee,
                    seconds);
        }
    }
}
