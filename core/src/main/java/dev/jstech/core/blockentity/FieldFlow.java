/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.blockentity;

import java.util.concurrent.atomic.AtomicLong;

/**
 * What has crossed a block entity's faces since it was made: the energy, items and fluid others put in and took out
 * through what it offers, counted as they go, never what it moves inside itself. The studio's block inspector reads it
 * each second to say what goes in and out.
 */
public final class FieldFlow {

    private final AtomicLong energyIn = new AtomicLong();
    private final AtomicLong energyOut = new AtomicLong();
    private final AtomicLong itemsIn = new AtomicLong();
    private final AtomicLong itemsOut = new AtomicLong();
    private final AtomicLong fluidIn = new AtomicLong();
    private final AtomicLong fluidOut = new AtomicLong();

    /** All of it now. */
    public Totals totals() {
        return new Totals(energyIn.get(), energyOut.get(), itemsIn.get(), itemsOut.get(), fluidIn.get(),
                fluidOut.get());
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
