/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.energy;

/**
 * Work paid for with the energy there is: a machine short of energy works slower in the same proportion, never in fits
 * and starts. Each tick it pays what it has, up to a tick's worth, toward its work, and a tick of work is done each time
 * a whole tick's energy has been paid; what is paid over carries on to the next. With half the energy it wants, it
 * works at half its speed. Pure: no game types.
 */
public final class PartialWork {

    private long paid;
    private int lastPercent = PERCENT;

    /** A whole, in percent. */
    public static final int PERCENT = 100;

    /**
     * Pays {@code spent} toward work that costs {@code perTick} a tick; how many ticks of work it has now done (0 or
     * 1 when no more than a tick's worth is paid at once).
     */
    public int pay(final long spent, final long perTick) {
        if (perTick <= 0) {
            this.lastPercent = PERCENT;
            return 1;
        }
        final long paying = Math.max(0L, spent);
        this.lastPercent = (int) Math.min(PERCENT, paying * PERCENT / perTick);
        this.paid += paying;
        final long done = this.paid / perTick;
        this.paid -= done * perTick;
        return (int) Math.min(Integer.MAX_VALUE, done);
    }

    /** How fast it worked at the last payment, in percent of its full speed. */
    public int lastPercent() {
        return this.lastPercent;
    }

    /** What is paid toward the next tick of work. */
    public long paid() {
        return this.paid;
    }

    /** Forgets what was paid, as when the work it was for stops. */
    public void reset() {
        this.paid = 0L;
        this.lastPercent = PERCENT;
    }

    /** Puts back what was paid when its block was saved. */
    public void restore(final long saved) {
        this.paid = Math.max(0L, saved);
    }
}
