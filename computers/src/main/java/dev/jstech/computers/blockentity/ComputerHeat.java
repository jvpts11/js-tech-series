/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import dev.jstech.computers.config.ComputersServerConfig;
import dev.jstech.computers.hardware.HeatCurve;
import dev.jstech.computers.hardware.ThermalModel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * How hot a machine is (computers/docs/design/temperature.md), a computer or a whole rack: where its temperature heads
 * for the heat it makes against what it takes away, how far it has got, what that costs its clock, and the trip when
 * not even a quarter of its clock holds the heat. A machine that tripped does not start again until it has cooled
 * below the settings' restart temperature.
 *
 * <p>While it runs, its curve follows the temperature its load would leave it at full clock, which the thresholds read
 * into what it shows and how fast it runs; once it stops, the curve follows what it really is, cooling to its
 * surroundings. The surroundings are read from its biome once it has a place. Each tick is one sum: nothing around the
 * machine is looked at.
 */
final class ComputerHeat {

    /** Where its temperature is heading, and from where; null until it has a place to read its surroundings from. */
    @Nullable
    private HeatCurve curve;
    private double ambient = Double.NaN;
    private boolean running;
    private ThermalModel.Reading reading = ThermalModel.Reading.resting(DEFAULT_AMBIENT);
    /* Whether it tripped and is still too hot to start. */
    private boolean cooling;
    /* How hot it was when it last tripped, in whole °C, which its next self-test warns of; 0 for no trip. */
    private int lastTrip;

    /** The surroundings of a machine that has never read its own: the plains'. */
    static final double DEFAULT_AMBIENT = 20.0;

    /**
     * A tick of tick {@code now} for a machine at {@code pos} that is {@code running}, making {@code heatWatts} and
     * taking away {@code budgetWatts}; says whether it trips now, which its owner then answers by stopping it.
     */
    boolean tick(final Level level, final BlockPos pos, final long now, final boolean running,
                 final double heatWatts, final double budgetWatts) {
        if (Double.isNaN(ambient)) {
            ambient = ComputersServerConfig.ambient(level.getBiome(pos).value().getBaseTemperature(),
                    level.dimension() == Level.NETHER);
        }
        if (curve == null) {
            curve = new HeatCurve(ambient, ambient, now);
        }
        if (this.running && !running) {
            // Stopped: from here the curve is what the machine really is, cooling from what it last showed.
            curve.restart(now, reading.celsius(), ambient);
        }
        this.running = running;
        curve.aim(now, running ? ThermalModel.raw(ambient, heatWatts, budgetWatts) : ambient);
        final double celsius = curve.at(now);
        reading = running ? ThermalModel.read(ambient, celsius)
                : new ThermalModel.Reading(celsius, 1.0, false, false);
        // Cool enough to start again; where the surroundings are warmer than that, once it is within a degree of them.
        if (cooling && reading.celsius() < Math.max(ComputersServerConfig.restartBelow(), ambient + 1.0)) {
            cooling = false;
        }
        if (running && reading.trips()) {
            cooling = true;
            lastTrip = (int) Math.round(Math.max(ThermalModel.TRIP_AT, reading.celsius()));
            return true;
        }
        return false;
    }

    /** What it reads now. */
    ThermalModel.Reading reading() {
        return reading;
    }

    /** Its surroundings, in °C; the plains' before it has read its own. */
    double ambient() {
        return Double.isNaN(ambient) ? DEFAULT_AMBIENT : ambient;
    }

    /** Whether it may start: not while it is still cooling from a trip. */
    boolean mayStart() {
        return !cooling;
    }

    /** How hot it was at its last trip, in whole °C, for its next self-test to warn of; 0 when it did not trip. */
    int lastTrip() {
        return lastTrip;
    }

    /** Its system came up after the self-test warned of the trip: the warning has been given. */
    void warned() {
        lastTrip = 0;
    }

    /**
     * Jumps to where its load leaves it, as if it had run long enough to get there: a machine a command or a test
     * sets up, which has no half a minute to wait.
     */
    void settle(final long now) {
        if (curve != null) {
            curve.settle(now);
        }
    }

    /** Reads its surroundings again at its next tick: a machine moved, or settings changed. */
    void forgetSurroundings() {
        ambient = Double.NaN;
    }

    void save(final CompoundTag tag) {
        if (curve == null) {
            return;
        }
        tag.putDouble("HeatFrom", curve.from());
        tag.putDouble("HeatTo", curve.target());
        tag.putLong("HeatSince", curve.since());
        tag.putBoolean("HeatRunning", running);
        tag.putBoolean("HeatCooling", cooling);
        tag.putInt("HeatTrip", lastTrip);
        tag.putDouble("HeatShown", reading.celsius());
    }

    void load(final CompoundTag tag) {
        if (!tag.contains("HeatSince")) {
            return;
        }
        curve = new HeatCurve(tag.getDouble("HeatFrom"), tag.getDouble("HeatTo"), tag.getLong("HeatSince"));
        running = tag.getBoolean("HeatRunning");
        cooling = tag.getBoolean("HeatCooling");
        lastTrip = tag.getInt("HeatTrip");
        // What it read when saved: a machine that stops on its first tick back cools from there, not from the plains.
        reading = ThermalModel.Reading.resting(tag.getDouble("HeatShown"));
    }
}
