/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.machine;

import java.util.Arrays;
import java.util.List;

/**
 * What upgrades do to a machine. Each is a factor of the machine as it was built, 1 for no change, except
 * {@code parallel}, the recipes it works at once besides the first. Several upgrades multiply their factors and add
 * their recipes at once, so two speed upgrades of 1.5 make a machine 2.25 times as fast; an upgrade that saves energy
 * spends less than once.
 *
 * <pre>{@code
 * UpgradeEffect.NONE.withSpeed(2.0).withEnergy(1.5)     // twice as fast, half as much energy again
 * UpgradeEffect.NONE.withParallel(1).withEnergy(1.8)    // two recipes at once
 * UpgradeEffect.NONE.withYield(1.25)                    // a quarter more of what it makes
 * }</pre>
 *
 * @param speed      how many times as fast it works
 * @param energy     how many times the energy a tick it spends
 * @param parallel   how many recipes it works at once besides the first, inputs and room allowing
 * @param yield      how many times what it makes, the part past a whole coming by chance
 * @param byproducts how many times as likely its byproducts come
 * @param use        how many times the fluid and the matter a recipe spends
 * @param heat       how many times the heat it gives off as it works
 * @param sound      how many times as loud it is
 * @param buffer     how many times the energy it stores
 */
public record UpgradeEffect(double speed, double energy, int parallel, double yield, double byproducts, double use,
                            double heat, double sound, double buffer) {

    /** No upgrade at all. */
    public static final UpgradeEffect NONE = new UpgradeEffect(1.0, 1.0, 0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0);
    /** The most recipes at once a machine works besides the first, however many upgrades say more. */
    public static final int MOST_PARALLEL = 1_024;
    /* How many factors an effect has besides its recipes at once. */
    private static final int FACTORS = 8;

    public UpgradeEffect {
        if (!(speed > 0.0) || Double.isInfinite(speed)) {
            throw new IllegalArgumentException("an upgrade has a speed above 0; got " + speed);
        }
        if (!(buffer > 0.0) || Double.isInfinite(buffer)) {
            throw new IllegalArgumentException("an upgrade leaves a buffer above 0; got " + buffer);
        }
        for (final double factor : new double[] {energy, yield, byproducts, use, heat, sound}) {
            if (!(factor >= 0.0) || Double.isInfinite(factor)) {
                throw new IllegalArgumentException("an upgrade's factors are 0 or more; got " + factor);
            }
        }
        if (parallel < 0) {
            throw new IllegalArgumentException("an upgrade adds no fewer than no recipes at once; got " + parallel);
        }
    }

    /** An upgrade of speed and energy alone. */
    public UpgradeEffect(final double speed, final double energy) {
        this(speed, energy, 0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0);
    }

    /** What every upgrade in {@code each} does together, each counted as many times as there are of it. */
    public static UpgradeEffect combine(final List<Counted> each) {
        double speed = 1.0;
        double energy = 1.0;
        long parallel = 0L;
        double yield = 1.0;
        double byproducts = 1.0;
        double use = 1.0;
        double heat = 1.0;
        double sound = 1.0;
        double buffer = 1.0;
        for (final Counted counted : each) {
            final UpgradeEffect effect = counted.effect();
            final int count = counted.count();
            speed *= Math.pow(effect.speed(), count);
            energy *= Math.pow(effect.energy(), count);
            parallel += (long) effect.parallel() * count;
            yield *= Math.pow(effect.yield(), count);
            byproducts *= Math.pow(effect.byproducts(), count);
            use *= Math.pow(effect.use(), count);
            heat *= Math.pow(effect.heat(), count);
            sound *= Math.pow(effect.sound(), count);
            buffer *= Math.pow(effect.buffer(), count);
        }
        // A machine stacked with a great many upgrades must still have an effect: past what a double holds the
        // product stays at the largest (or, for slowing ones, the smallest) value it can.
        return new UpgradeEffect(within(speed), finite(energy), (int) Math.min(MOST_PARALLEL, parallel),
                finite(yield), finite(byproducts), finite(use), finite(heat), finite(sound), within(buffer));
    }

    /**
     * What every upgrade in {@code each} does together where gains add up and savings multiply, as a mod whose
     * upgrades are real parts says: a factor above 1 adds what it gains, a factor below 1 multiplies, so four motors of
     * half again as fast make a machine 3 times as fast, and four drives of a fifth less energy leave it at 41%. Its
     * recipes at once add up, as in {@link #combine}.
     */
    public static UpgradeEffect addGains(final List<Counted> each) {
        final double[] gains = new double[FACTORS];
        final double[] savings = new double[FACTORS];
        Arrays.fill(savings, 1.0);
        long parallel = 0L;
        for (final Counted counted : each) {
            final double[] factors = counted.effect().factors();
            for (int k = 0; k < FACTORS; k++) {
                if (factors[k] >= 1.0) {
                    gains[k] += (factors[k] - 1.0) * counted.count();
                } else {
                    savings[k] *= Math.pow(factors[k], counted.count());
                }
            }
            parallel += (long) counted.effect().parallel() * counted.count();
        }
        final double[] together = new double[FACTORS];
        for (int k = 0; k < FACTORS; k++) {
            together[k] = (1.0 + gains[k]) * savings[k];
        }
        return new UpgradeEffect(within(together[0]), finite(together[1]), (int) Math.min(MOST_PARALLEL, parallel),
                finite(together[2]), finite(together[3]), finite(together[4]), finite(together[5]),
                finite(together[6]), within(together[7]));
    }

    /** The same, {@code to} times as fast. */
    public UpgradeEffect withSpeed(final double to) {
        return new UpgradeEffect(to, energy, parallel, yield, byproducts, use, heat, sound, buffer);
    }

    /** The same, spending {@code to} times the energy a tick. */
    public UpgradeEffect withEnergy(final double to) {
        return new UpgradeEffect(speed, to, parallel, yield, byproducts, use, heat, sound, buffer);
    }

    /** The same, working {@code to} recipes at once besides the first. */
    public UpgradeEffect withParallel(final int to) {
        return new UpgradeEffect(speed, energy, to, yield, byproducts, use, heat, sound, buffer);
    }

    /** The same, making {@code to} times as much. */
    public UpgradeEffect withYield(final double to) {
        return new UpgradeEffect(speed, energy, parallel, to, byproducts, use, heat, sound, buffer);
    }

    /** The same, its byproducts {@code to} times as likely. */
    public UpgradeEffect withByproducts(final double to) {
        return new UpgradeEffect(speed, energy, parallel, yield, to, use, heat, sound, buffer);
    }

    /** The same, a recipe spending {@code to} times its fluid and matter. */
    public UpgradeEffect withUse(final double to) {
        return new UpgradeEffect(speed, energy, parallel, yield, byproducts, to, heat, sound, buffer);
    }

    /** The same, giving off {@code to} times the heat. */
    public UpgradeEffect withHeat(final double to) {
        return new UpgradeEffect(speed, energy, parallel, yield, byproducts, use, to, sound, buffer);
    }

    /** The same, {@code to} times as loud. */
    public UpgradeEffect withSound(final double to) {
        return new UpgradeEffect(speed, energy, parallel, yield, byproducts, use, heat, to, buffer);
    }

    /** The same, storing {@code to} times the energy. */
    public UpgradeEffect withBuffer(final double to) {
        return new UpgradeEffect(speed, energy, parallel, yield, byproducts, use, heat, sound, to);
    }

    /** How many recipes it works at once: the first and the parallel ones. */
    public int recipesAtOnce() {
        return 1 + parallel;
    }

    /** The ticks a recipe of {@code ticks} takes with this effect: never under one, never past an int. */
    public int ticks(final int ticks) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(1L, (long) Math.ceil(ticks / speed)));
    }

    /** The energy a tick a recipe of {@code perTick} spends with this effect, rounded up. */
    public long energyPerTick(final long perTick) {
        return (long) Math.ceil(perTick * energy);
    }

    /** What a recipe that spends {@code amount} of a fluid or of matter spends with this effect, rounded up. */
    public long spent(final long amount) {
        return amount <= 0 ? 0L : (long) Math.min(Long.MAX_VALUE, Math.ceil(amount * use));
    }

    /**
     * How many of an output of {@code count} it makes with this effect, given {@code roll} between 0 and 1: the
     * whole part always, one more when the roll falls under what is left.
     */
    public int made(final int count, final double roll) {
        final double exact = count * yield;
        final long whole = (long) Math.floor(exact);
        final long made = whole + (roll < exact - whole ? 1L : 0L);
        return (int) Math.min(Integer.MAX_VALUE, made);
    }

    /** The most of an output of {@code count} it can make with this effect, for the room it needs. */
    public int mostMade(final int count) {
        return (int) Math.min(Integer.MAX_VALUE, (long) Math.ceil(count * yield));
    }

    /** The chance a byproduct of {@code chance} comes with this effect, at most a certainty. */
    public double byproductChance(final double chance) {
        return Math.min(1.0, chance * byproducts);
    }

    /** The energy a store of {@code capacity} holds with this effect, at least one. */
    public long storage(final long capacity) {
        return Math.max(1L, (long) Math.min(Long.MAX_VALUE, Math.floor(capacity * buffer)));
    }

    /* Its factors, in the order addGains works them: speed, energy, yield, byproducts, use, heat, sound, buffer. */
    private double[] factors() {
        return new double[] {speed, energy, yield, byproducts, use, heat, sound, buffer};
    }

    private static double within(final double factor) {
        return Math.max(Double.MIN_VALUE, Math.min(Double.MAX_VALUE, factor));
    }

    private static double finite(final double factor) {
        return Math.min(Double.MAX_VALUE, factor);
    }

    /**
     * One upgrade and how many of it a machine holds.
     *
     * @param effect what one does
     * @param count  how many there are
     */
    public record Counted(UpgradeEffect effect, int count) {
    }
}
