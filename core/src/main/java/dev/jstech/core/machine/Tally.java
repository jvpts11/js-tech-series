/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.machine;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A machine's count of what it makes a share at a time: a byproduct that comes a quarter of an item each recipe, a
 * scrap every eight drawings. Each share is added to what the count already holds, the whole items come out, and the
 * part short of a whole stays for the next time, so the machine makes exactly its proportion and nothing is left to
 * chance: a quarter of an item each recipe is one item every fourth recipe, always.
 *
 * <p>Pure: no game types.
 */
public final class Tally {

    private final Map<String, Double> held = new HashMap<>();

    /* What a sum may fall short of a whole by and still be one, so that ten tenths make an item. */
    private static final double SLACK = 1e-9;

    /**
     * Adds {@code amount} to the count of {@code key}, and takes out the whole items it now holds.
     *
     * @return how many whole items come out
     */
    public int add(final String key, final double amount) {
        final double sum = sum(key, amount);
        final double whole = whole(sum);
        final double rest = Math.max(0.0, sum - whole);
        if (rest < SLACK) {
            this.held.remove(key);
        } else {
            this.held.put(key, rest);
        }
        return (int) Math.min(Integer.MAX_VALUE, whole);
    }

    /** How many whole items adding {@code amount} to the count of {@code key} would take out, changing nothing. */
    public int peek(final String key, final double amount) {
        return (int) Math.min(Integer.MAX_VALUE, whole(sum(key, amount)));
    }

    /** What the count of {@code key} holds short of a whole item. */
    public double held(final String key) {
        return this.held.getOrDefault(key, 0.0);
    }

    /** Every count that holds something, by key, as it is kept. */
    public Map<String, Double> snapshot() {
        return Map.copyOf(this.held);
    }

    /** Takes up the counts {@code saved} holds, as kept, in place of its own. */
    public void restore(final Map<String, Double> saved) {
        this.held.clear();
        saved.forEach((key, value) -> {
            if (value != null && value > 0.0 && value < 1.0) {
                this.held.put(key, value);
            }
        });
    }

    /** Forgets every count. */
    public void clear() {
        this.held.clear();
    }

    private double sum(final String key, final double amount) {
        Objects.requireNonNull(key, "key");
        if (!(amount >= 0.0) || Double.isInfinite(amount)) {
            throw new IllegalArgumentException("a count takes no less than nothing; got " + amount);
        }
        return held(key) + amount;
    }

    private static double whole(final double sum) {
        return Math.floor(sum + SLACK);
    }
}
