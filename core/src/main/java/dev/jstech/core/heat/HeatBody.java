/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.heat;

import dev.jstech.core.blockentity.IFieldPart;
import java.util.Objects;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

/**
 * The heat a block holds: its temperature in kelvin, and what it is made of, which says how much heat it holds a
 * kelvin and how well it conducts. A block entity keeps one as a part of its fields, saved and sent to the players who
 * see it, and offers it on {@link HeatPorts#HEAT}; the level's heat ({@link LevelHeat}) carries heat between it and its
 * neighbours and out to the place around it.
 *
 * <pre>{@code
 * private final HeatBody heat = HeatBody.of(Thermal.of(copper, false), 1.0).exposed(4.0);   // with fins
 * ...
 * HeatPorts.keep(this, heat);   // in the constructor: saved, sent to the players, and woken as it changes
 * }</pre>
 *
 * <p>It starts at the temperature of the place it stands in. Anything that changes it wakes it, and the level's heat
 * leaves it to sleep once it is in balance with what is around it.
 */
public final class HeatBody implements IHeatPort, IFieldPart {

    private Thermal thermal;
    private final double volume;
    private double extraArea;
    private double temperature = Double.NaN;
    private double notified = Double.NaN;
    private Runnable changed = () -> { };
    private Runnable wake = () -> { };

    private static final String KEY = "Heat";
    private static final String TEMPERATURE = "temperature";
    /* How far its temperature moves before the players who see it are told again. */
    private static final double NOTIFY_STEP = 0.5;

    private HeatBody(final Thermal thermal, final double volume) {
        this.thermal = Objects.requireNonNull(thermal, "thermal");
        if (!(volume > 0.0)) {
            throw new IllegalArgumentException("a body that holds heat has a volume above 0; got " + volume);
        }
        this.volume = volume;
    }

    /** A body of {@code cubicMetres} of a material of {@code thermal}: one for a whole block. */
    public static HeatBody of(final Thermal thermal, final double cubicMetres) {
        return new HeatBody(thermal, cubicMetres);
    }

    /**
     * The same with {@code squareMetres} more surface to the air and to space than its faces, as a radiator's fins
     * give it; its faces' square metres count by themselves.
     */
    public HeatBody exposed(final double squareMetres) {
        this.extraArea = Math.max(0.0, squareMetres);
        return this;
    }

    /** The same, telling {@code changedThen} when it changes and {@code wakeThen} when it must be looked at again. */
    public HeatBody listening(final Runnable changedThen, final Runnable wakeThen) {
        this.changed = Objects.requireNonNull(changedThen, "changedThen");
        this.wake = Objects.requireNonNull(wakeThen, "wakeThen");
        return this;
    }

    @Override
    public double temperature() {
        return this.temperature;
    }

    /** Whether it has a temperature yet: a body placed this tick takes the place's at its first look. */
    public boolean known() {
        return !Double.isNaN(this.temperature);
    }

    /** Sets its temperature, in kelvin. */
    public void set(final double kelvin) {
        this.temperature = Math.max(0.0, kelvin);
        notifyIfMoved();
        this.wake.run();
    }

    @Override
    public double capacity() {
        return this.thermal.capacity(this.volume);
    }

    @Override
    public double conductivity() {
        return this.thermal.conductivity();
    }

    /** How well its surface radiates. */
    public double emissivity() {
        return this.thermal.emissivity();
    }

    /** Its surface to the air and to space besides its faces, in square metres. */
    public double extraArea() {
        return this.extraArea;
    }

    /** What it is made of. */
    public Thermal thermal() {
        return this.thermal;
    }

    /** Makes it of a material of {@code to}, its temperature kept, as a machine of another casing is. */
    public void madeOf(final Thermal to) {
        this.thermal = Objects.requireNonNull(to, "to");
        this.wake.run();
    }

    @Override
    public void exchange(final double joules) {
        if (!known() || joules == 0.0 || Double.isNaN(joules)) {
            return;
        }
        final double capacity = capacity();
        if (capacity <= 0.0) {
            return;
        }
        this.temperature = Math.max(0.0, this.temperature + joules / capacity);
        notifyIfMoved();
        this.wake.run();
    }

    @Override
    public void save(final CompoundTag tag, final HolderLookup.Provider registries) {
        final CompoundTag own = new CompoundTag();
        if (known()) {
            own.putDouble(TEMPERATURE, this.temperature);
        }
        tag.put(KEY, own);
    }

    @Override
    public void load(final CompoundTag tag, final HolderLookup.Provider registries) {
        final CompoundTag own = tag.getCompound(KEY);
        this.temperature = own.contains(TEMPERATURE) ? own.getDouble(TEMPERATURE) : Double.NaN;
        this.notified = this.temperature;
    }

    @Override
    public void writeClient(final CompoundTag tag, final HolderLookup.Provider registries) {
        save(tag, registries);
    }

    @Override
    public void readClient(final CompoundTag tag, final HolderLookup.Provider registries) {
        load(tag, registries);
    }

    /* Tells its block when its temperature has moved far enough since last told, not on every small step. */
    private void notifyIfMoved() {
        if (Double.isNaN(this.notified) || Math.abs(this.temperature - this.notified) >= NOTIFY_STEP) {
            this.notified = this.temperature;
            this.changed.run();
        }
    }
}
