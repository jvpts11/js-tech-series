/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.hazard;

import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

/**
 * What cleans one kind of pollution from the chunk it stands in: each second, up to its rate, as a scrubber on a
 * chimney, a filter on a vent or a device carried about does, for each mod's cleaning equipment to build on. It keeps
 * what it has taken until its mod empties it, into a full filter or a byproduct, and is saved with its block entity.
 *
 * <pre>{@code
 * private final PollutionCleaner scrubber = new PollutionCleaner(SeriesPollution.ACID, 5);
 *
 * public void serverTick() {
 *     scrubber.tick((ServerLevel) level, worldPosition);
 *     if (scrubber.taken() >= FILTER_HOLDS) { scrubber.empty(); fillFilter(); }
 * }
 * }</pre>
 */
public final class PollutionCleaner {

    private final PollutionKind kind;
    private final double perSecond;
    private double taken;
    private int ticks;

    private static final int TICKS_A_SECOND = 20;
    private static final String TAKEN = "taken";

    /** One that takes up to {@code perSecond} of {@code kind}, in its unit, a second. */
    public PollutionCleaner(final PollutionKind kind, final double perSecond) {
        this.kind = Objects.requireNonNull(kind, "kind");
        if (perSecond < 0) {
            throw new IllegalArgumentException("a cleaner takes no less than nothing a second, not " + perSecond);
        }
        this.perSecond = perSecond;
    }

    /** The kind it cleans. */
    public PollutionKind kind() {
        return this.kind;
    }

    /** How much it takes at most a second, in the kind's unit. */
    public double perSecond() {
        return this.perSecond;
    }

    /**
     * A tick of its work at {@code pos}, called each tick by its block entity or its device: once a second it takes up
     * to its rate from the chunk there. What it took this tick: nothing on most ticks.
     */
    public double tick(final ServerLevel level, final BlockPos pos) {
        if (++this.ticks < TICKS_A_SECOND) {
            return 0;
        }
        this.ticks = 0;
        final double now = Pollution.takeUp(level, pos, this.kind, this.perSecond);
        this.taken += now;
        return now;
    }

    /** What it has taken since it was last emptied, in the kind's unit. */
    public double taken() {
        return this.taken;
    }

    /** Empties it, and says what it had taken. */
    public double empty() {
        final double was = this.taken;
        this.taken = 0;
        return was;
    }

    /** Writes what it has taken into {@code tag}. */
    public void save(final CompoundTag tag) {
        tag.putDouble(TAKEN, this.taken);
    }

    /** Reads what it had taken back from {@code tag}. */
    public void load(final CompoundTag tag) {
        this.taken = Math.max(0, tag.getDouble(TAKEN));
    }
}
