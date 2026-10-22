/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.blockentity;

import java.util.concurrent.atomic.AtomicLong;
import org.jetbrains.annotations.ApiStatus;

/**
 * What has crossed a block entity's faces while the studio's block inspector watched it: the energy, items and fluid
 * others put in and took out through what it offers, counted as they go, never what it moves inside itself. The
 * inspector reads it each second to say what goes in and out. A block entity nobody watches offers its own handlers
 * as they are, so counting costs nothing in a game where nobody looks.
 */
public final class FieldFlow {

    private final AtomicLong energyIn = new AtomicLong();
    private final AtomicLong energyOut = new AtomicLong();
    private final AtomicLong itemsIn = new AtomicLong();
    private final AtomicLong itemsOut = new AtomicLong();
    private final AtomicLong fluidIn = new AtomicLong();
    private final AtomicLong fluidOut = new AtomicLong();
    /* The game time until which the inspector watches it; none before it first does. */
    private volatile long watchedUntil = Long.MIN_VALUE;

    /** All of it now. */
    public Totals totals() {
        return new Totals(energyIn.get(), energyOut.get(), itemsIn.get(), itemsOut.get(), fluidIn.get(),
                fluidOut.get());
    }

    /**
     * Watches it until game time {@code until}; whether it was not watched before at {@code now}, so its neighbours
     * have to ask for what it offers again to be counted. The studio's, and no promise.
     */
    @ApiStatus.Internal
    public boolean watch(final long now, final long until) {
        final boolean started = !watched(now);
        this.watchedUntil = Math.max(this.watchedUntil, until);
        return started;
    }

    /** Whether the inspector watches it at game time {@code now}. The studio's, and no promise. */
    @ApiStatus.Internal
    public boolean watched(final long now) {
        return now <= this.watchedUntil;
    }

    void energy(final long in, final long out) {
        this.energyIn.addAndGet(in);
        this.energyOut.addAndGet(out);
    }

    void items(final long in, final long out) {
        this.itemsIn.addAndGet(in);
        this.itemsOut.addAndGet(out);
    }

    void fluid(final long in, final long out) {
        this.fluidIn.addAndGet(in);
        this.fluidOut.addAndGet(out);
    }

    /**
     * What has crossed, all told.
     *
     * @param energyIn  energy put in, in FE
     * @param energyOut energy taken out
     * @param itemsIn   items put in
     * @param itemsOut  items taken out
     * @param fluidIn   fluid put in, in millibuckets
     * @param fluidOut  fluid taken out
     */
    public record Totals(long energyIn, long energyOut, long itemsIn, long itemsOut, long fluidIn, long fluidOut) {
    }
}
