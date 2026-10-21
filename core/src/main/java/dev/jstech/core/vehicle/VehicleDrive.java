/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.vehicle;

import java.util.List;
import java.util.Objects;

/**
 * A vehicle's drivetrain: an engine that runs on what it burns or on its battery, a gearbox between it and the driven
 * wheels, and how hard the wheels push at each speed. A burning engine idles, pulls hardest near the top of its range
 * and stops pulling at its limit, its clutch slipping as it pulls away; an electric one pulls from a standstill. An
 * automatic gearbox changes up as the engine nears its limit and down as it labours; a manual one changes when told.
 * The engine warms to its working heat as it runs, more under load. One step is a tick of driving. Pure: no part of the
 * game.
 */
public final class VehicleDrive {

    private int gear = 1;
    private double rpm;
    private double temperature = Double.NaN;
    private double sinceShift = SHIFT_PAUSE;

    /* An automatic gearbox changes up past this share of the engine's top speed, and down below the second. */
    private static final double SHIFT_UP = 0.88;
    private static final double SHIFT_DOWN = 0.4;
    /* Seconds between two changes of an automatic gearbox, so it never hunts between gears. */
    private static final double SHIFT_PAUSE = 0.5;
    /* How far above its idle a slipping clutch lets the engine run while it pulls away, as a share of its range. */
    private static final double LAUNCH = 0.35;
    /* The share of the engine's push the gearbox and the axles pass on to the wheels. */
    private static final double TRANSMISSION = 0.9;
    /* The share of its top power an idling engine burns, to turn itself over. */
    private static final double IDLE_DRAW = 0.03;
    /* An electric motor gives its full turning force up to this share of its top speed, its full power above. */
    private static final double ELECTRIC_BASE = 0.3;
    /* The least share of its top power a burning engine gives, low in its range. */
    private static final double LEAST_POWER = 0.15;
    /* How many degrees above the air a burning engine runs when warm, and an electric one. */
    private static final double WARM_BURNING = 70;
    private static final double WARM_ELECTRIC = 30;
    /* How many degrees more it runs for a load past the first share, and as it is worn. */
    private static final double LOADED = 25;
    private static final double WORN = 60;
    private static final double LOAD_FROM = 0.8;
    /* Seconds it takes to come most of the way to its heat. */
    private static final double HEAT_TIME = 60;
    private static final double TO_RPM = 60 / (2 * Math.PI);
    private static final double STILL = 0.5;

    /** The gear it is in: below nought reverse, nought neutral, from one up the forward gears. */
    public int gear() {
        return gear;
    }

    /** How fast the engine turns, in turns a minute. */
    public double rpm() {
        return rpm;
    }

    /** How warm the engine is, in degrees; the air's until it first runs. */
    public double temperature(final double air) {
        return Double.isNaN(temperature) ? air : temperature;
    }

    /** Sets it as a vehicle saved it, or as the server sends it. */
    public void set(final int savedGear, final double savedRpm, final double savedTemperature) {
        this.gear = savedGear;
        this.rpm = Math.max(0, savedRpm);
        this.temperature = savedTemperature;
    }

    /**
     * One step of {@code seconds}: how hard the wheels push and what the engine burns, from the driver's
     * {@code controls}, the speed the wheels go at along the vehicle, in metres a second (below nought backwards),
     * and the air's heat.
     */
    public Result step(final Engine engine, final Gearbox gearbox, final double wheelRadius, final Controls controls,
                       final double wheelSpeed, final double air, final double seconds) {
        Objects.requireNonNull(engine, "engine");
        Objects.requireNonNull(gearbox, "gearbox");
        if (Double.isNaN(temperature)) {
            temperature = air;
        }
        sinceShift += seconds;
        if (!controls.running()) {
            rpm = Math.max(0, rpm - engine.maxRpm() * seconds);
            warm(engine, air, 0, false, seconds);
            return new Result(0, 0, rpm, gear, temperature, 0);
        }
        chooseGear(engine, gearbox, wheelRadius, controls, wheelSpeed);
        final double ratio = gearbox.ratio(gear) * gearbox.finalDrive();
        final double wheelRpm = Math.abs(wheelSpeed) / wheelRadius * TO_RPM;
        final double idle = engine.electric() ? 0 : engine.idleRpm();
        double turning = gear == 0 ? idle + controls.throttle() * (engine.maxRpm() - idle) : wheelRpm * ratio;
        if (gear != 0 && turning < idle) {
            // The clutch slips as it pulls away, so the engine runs above its idle while the wheels catch up.
            turning = idle + controls.throttle() * LAUNCH * (engine.maxRpm() - idle);
        }
        rpm = Math.min(engine.maxRpm(), turning);
        double force = 0;
        double load = 0;
        if (gear != 0 && controls.throttle() > 0 && turning < engine.maxRpm()) {
            // Its turning force: an electric motor's the same up to its base speed, its power's above; a burning
            // engine's what its power gives at the speed it turns.
            final double power = engine.electric() ? engine.power() : engine.power() * share(engine, rpm);
            final double spinning = Math.max(rpm, engine.electric() ? ELECTRIC_BASE * engine.maxRpm() : idle) / TO_RPM;
            force = power / spinning * ratio * TRANSMISSION / wheelRadius * controls.throttle() * Math.signum(gear);
            load = controls.throttle() * (engine.electric() ? 1 : share(engine, rpm));
        }
        final double delivered = Math.abs(force * wheelSpeed);
        final double burnt = (delivered / engine.efficiency()
                + (engine.electric() ? 0 : IDLE_DRAW * engine.power() / engine.efficiency())) * seconds;
        warm(engine, air, load, true, seconds);
        return new Result(force, burnt, rpm, gear, temperature, load);
    }

    /* The share of its top power a burning engine gives turning at {@code turning}. */
    private static double share(final Engine engine, final double turning) {
        final double x = Math.clamp(turning / engine.maxRpm(), 0, 1);
        return Math.clamp(x * (2 - x), LEAST_POWER, 1);
    }

    /* Puts it in the gear the driver or the speed asks for. */
    private void chooseGear(final Engine engine, final Gearbox gearbox, final double wheelRadius,
                            final Controls controls, final double wheelSpeed) {
        final int top = gearbox.ratios().size();
        if (controls.reverse() && wheelSpeed < STILL) {
            gear = -1;
            return;
        }
        if (!controls.reverse() && gear < 0 && wheelSpeed > -STILL) {
            gear = 1;
        }
        if (gear == 0 && controls.throttle() > 0) {
            gear = 1;
        }
        if (!controls.automatic()) {
            if (controls.shift() != 0 && gear >= 0) {
                gear = Math.clamp(gear + controls.shift(), 0, top);
            }
            return;
        }
        if (gear <= 0 || sinceShift < SHIFT_PAUSE) {
            return;
        }
        final double wheelRpm = Math.abs(wheelSpeed) / wheelRadius * TO_RPM;
        final double turning = wheelRpm * gearbox.ratio(gear) * gearbox.finalDrive();
        if (turning > SHIFT_UP * engine.maxRpm() && gear < top) {
            gear++;
            sinceShift = 0;
        } else if (turning < SHIFT_DOWN * engine.maxRpm() && gear > 1) {
            gear--;
            sinceShift = 0;
        }
    }

    /* Brings its heat toward where it runs, warmer under load and as it wears. */
    private void warm(final Engine engine, final double air, final double load, final boolean running,
                      final double seconds) {
        double target = air;
        if (running) {
            target += (engine.electric() ? WARM_ELECTRIC : WARM_BURNING)
                    + LOADED * Math.max(0, load - LOAD_FROM) / (1 - LOAD_FROM) + WORN * (1 - engine.health());
        }
        temperature += (target - temperature) * Math.min(1, seconds / HEAT_TIME);
    }

    /**
     * An engine as the drivetrain needs it.
     *
     * @param power      its top power, in watts
     * @param idleRpm    how fast it idles, in turns a minute; ignored for an electric one
     * @param maxRpm     how fast it turns at most
     * @param efficiency the share of what it burns that it turns into work, above nought
     * @param electric   whether it is an electric motor
     * @param health     how worn it is, 1 new and nought broken
     */
    public record Engine(double power, double idleRpm, double maxRpm, double efficiency, boolean electric,
                         double health) {

        public Engine {
            if (power <= 0 || maxRpm <= 0 || efficiency <= 0 || idleRpm < 0 || idleRpm >= maxRpm) {
                throw new IllegalArgumentException("an engine has power, a top speed above its idle and an"
                        + " efficiency: " + power + ", " + idleRpm + ", " + maxRpm + ", " + efficiency);
            }
            health = Math.clamp(health, 0, 1);
        }
    }

    /**
     * A gearbox: its forward gears' ratios, from the first, its reverse's, and the final drive after them all.
     *
     * @param ratios     the forward gears, the first the lowest
     * @param reverse    the reverse gear's ratio
     * @param finalDrive the ratio of the axle after the gearbox
     */
    public record Gearbox(List<Double> ratios, double reverse, double finalDrive) {

        /** A single gear, as an electric car or a gearbox that never changes has. */
        public static final Gearbox SINGLE = new Gearbox(List.of(1.0), 1.0, 8.0);

        public Gearbox {
            ratios = List.copyOf(ratios);
            if (ratios.isEmpty() || reverse <= 0 || finalDrive <= 0) {
                throw new IllegalArgumentException("a gearbox has a gear, a reverse and a final drive");
            }
        }

        /** The ratio of {@code gear}: reverse's below nought, none in neutral. */
        public double ratio(final int gear) {
            if (gear < 0) {
                return reverse;
            }
            if (gear == 0) {
                return 0;
            }
            return ratios.get(Math.min(gear, ratios.size()) - 1);
        }
    }

    /**
     * What the driver asks.
     *
     * @param throttle  how far the throttle is down, from nought to 1
     * @param reverse   whether they want to go backwards
     * @param automatic whether the gearbox changes by itself
     * @param shift     a change asked of a manual gearbox: 1 up, -1 down, nought none
     * @param running   whether the engine runs: its ignition on, and fuel to burn
     */
    public record Controls(double throttle, boolean reverse, boolean automatic, int shift, boolean running) {

        public Controls {
            throttle = Math.clamp(throttle, 0, 1);
        }
    }

    /**
     * What a step gives.
     *
     * @param force       how hard the driven wheels push along the vehicle, in newtons, below nought backwards
     * @param joules      what the engine burnt or drew in the step
     * @param rpm         how fast the engine turns
     * @param gear        the gear it is in
     * @param temperature the engine's heat, in degrees
     * @param load        how hard the engine works, from nought to 1
     */
    public record Result(double force, double joules, double rpm, int gear, double temperature, double load) {
    }
}
