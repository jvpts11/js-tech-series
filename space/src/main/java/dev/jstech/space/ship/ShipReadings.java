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
 * whether it flies too slowly to be held up, how high it flies, and its pulse jump.
 *
 * @param speed    how fast it goes along its nose, in metres a second, to a tenth
 * @param climb    how fast it rises or sinks besides where its nose takes it, in metres a second, to a tenth
 * @param throttle how far its throttle is open, in hundredths
 * @param boost    how far into its boost's speed above its throttle's it is, in hundredths; nought while not boosting
 * @param boosting whether it boosts
 * @param nozzles  how far its nozzles are turned down, in degrees
 * @param lifting  whether its nozzles hold it up, blowing down
 * @param thrusting whether its thrusters hold it up, firing down
 * @param gearDown whether its gear is down
 * @param stalled  whether it flies too slowly to be held up
 * @param altitude how high it flies, in metres ({@link ShipAltitude}), or {@link ShipAltitude#NONE}
 * @param grounded whether it stands on the land, where its wheels roll
 * @param pilot    its autopilot, as its HUD shows it
 * @param pulse    its pulse jump, as its HUD shows it
 */
public record ShipReadings(double speed, double climb, int throttle, int boost, boolean boosting, int nozzles,
                           boolean lifting, boolean thrusting, boolean gearDown, boolean stalled, int altitude,
                           boolean grounded, Pilot pilot, Pulse pulse) {

    /** A ship at rest on the land. */
    public static final ShipReadings PARKED = new ShipReadings(0, 0, 0, 0, false, 0, false, false, true, false, 0,
            true, Pilot.NONE, Pulse.NONE);
    public static final Codec<ShipReadings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.fieldOf("speed").forGetter(ShipReadings::speed),
            Codec.DOUBLE.fieldOf("climb").forGetter(ShipReadings::climb),
            Codec.INT.fieldOf("throttle").forGetter(ShipReadings::throttle),
            Codec.INT.optionalFieldOf("boost", 0).forGetter(ShipReadings::boost),
            Codec.BOOL.fieldOf("boosting").forGetter(ShipReadings::boosting),
            Codec.INT.fieldOf("nozzles").forGetter(ShipReadings::nozzles),
            Codec.BOOL.fieldOf("lifting").forGetter(ShipReadings::lifting),
            Codec.BOOL.optionalFieldOf("thrusting", false).forGetter(ShipReadings::thrusting),
            Codec.BOOL.fieldOf("gear_down").forGetter(ShipReadings::gearDown),
            Codec.BOOL.fieldOf("stalled").forGetter(ShipReadings::stalled),
            Codec.INT.optionalFieldOf("altitude", 0).forGetter(ShipReadings::altitude),
            Codec.BOOL.optionalFieldOf("grounded", false).forGetter(ShipReadings::grounded),
            Pilot.CODEC.optionalFieldOf("pilot", Pilot.NONE).forGetter(ShipReadings::pilot),
            Pulse.CODEC.optionalFieldOf("pulse", Pulse.NONE).forGetter(ShipReadings::pulse)
    ).apply(instance, ShipReadings::new));
    /* Tenths and hundredths, as the readings are kept. */
    private static final double TENTHS = 10.0;
    private static final double HUNDREDTHS = 100.0;

    /**
     * The readings of a tick of a ship's flight flying {@code altitude} metres high, or standing on the land
     * ({@code grounded}), its autopilot showing {@code pilot} and its pulse jump {@code pulse}, rounded as they are
     * shown.
     */
    public static ShipReadings of(final ShipFlightModel.Tick tick, final boolean grounded, final int altitude,
                                  final Pilot pilot, final Pulse pulse) {
        final ShipFlightModel.State state = tick.state();
        return new ShipReadings(Math.round(state.speed() * TENTHS) / TENTHS,
                Math.round((state.rise() - state.sink()) * TENTHS) / TENTHS,
                (int) Math.round(state.throttle() * HUNDREDTHS), (int) Math.round(tick.reached() * HUNDREDTHS),
                tick.boosting(), (int) Math.round(state.nozzles()), tick.lifting(), tick.thrusting(), tick.gearDown(),
                tick.stalled(), altitude, grounded, pilot, pulse);
    }

    /**
     * The readings of a tick of a ship's pulse jump, its flight standing as {@code state} under it: going at the
     * jump's speed, level, its gear up, high over nothing, its jump shown as {@code pulse}.
     */
    public static ShipReadings jumping(final ShipFlightModel.State state, final Pulse pulse) {
        return new ShipReadings(PulseCourse.SPEED, 0, (int) Math.round(state.throttle() * HUNDREDTHS), 0, false,
                (int) Math.round(state.nozzles()), false, false, false, false, ShipAltitude.NONE, false, Pilot.NONE,
                pulse);
    }

    /** The same readings, its pulse jump shown as {@code shown}. */
    public ShipReadings withPulse(final Pulse shown) {
        return new ShipReadings(this.speed, this.climb, this.throttle, this.boost, this.boosting, this.nozzles,
                this.lifting, this.thrusting, this.gearDown, this.stalled, this.altitude, this.grounded, this.pilot,
                shown);
    }

    /**
     * A ship's pulse jump as its HUD shows it: its drive spooling, how far and how long it has still to go; in the
     * jump, the world whose low orbit is nearest, how far off and how long away at the jump's speed; and a moment
     * after it dropped out, why.
     *
     * @param spool   how far its drive has spooled, in hundredths; nought while it does not
     * @param left    how long its drive has still to spool, in tenths of a second
     * @param on      whether it jumps
     * @param near    the nearest world in the jump, as its body is named in lower case; empty out of it
     * @param range   how far off that world's low orbit is, in blocks
     * @param seconds how long away it is at the jump's speed, in seconds
     * @param dropped why it dropped out of its jump, a moment after ({@link ShipPulse.Drop}, or a world's name);
     *                empty otherwise
     */
    public record Pulse(int spool, int left, boolean on, String near, int range, int seconds, String dropped) {

        /** No jump spooling, under way or just dropped out of. */
        public static final Pulse NONE = new Pulse(0, 0, false, "", 0, 0, "");
        public static final Codec<Pulse> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("spool").forGetter(Pulse::spool),
                Codec.INT.fieldOf("left").forGetter(Pulse::left),
                Codec.BOOL.fieldOf("on").forGetter(Pulse::on),
                Codec.STRING.fieldOf("near").forGetter(Pulse::near),
                Codec.INT.fieldOf("range").forGetter(Pulse::range),
                Codec.INT.fieldOf("seconds").forGetter(Pulse::seconds),
                Codec.STRING.fieldOf("dropped").forGetter(Pulse::dropped)
        ).apply(instance, Pulse::new));
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
