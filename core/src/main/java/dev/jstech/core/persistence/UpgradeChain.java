/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.persistence;

import java.util.Collection;
import java.util.List;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.UnaryOperator;

/**
 * The steps that bring something written in an older layout up to today's, one version at a time.
 *
 * <p>A layout's version counts from 1; whatever was written before it carried a version at all is at version 0. Each
 * step is declared by the version it starts from and takes what it is given to the next one. Bringing something up
 * runs every step from the version it was found at to today's, in order. A version with no step is one whose layout
 * changed in no way a step has to mend, so nothing is done for it. Something found at a version newer than today's is
 * handed back as it is, since nothing here knows what a newer version meant by it.
 *
 * <p>Settings files and saves both count their versions with this, so a version and a step mean the same in both. It
 * holds nothing of the game.
 *
 * @param <T> what the steps rewrite
 */
public final class UpgradeChain<T> {

    private final String owner;
    private final int version;
    private final NavigableMap<Integer, UnaryOperator<T>> steps;

    private UpgradeChain(final Builder<T> builder) {
        this.owner = builder.owner;
        this.version = builder.version;
        this.steps = new TreeMap<>(builder.steps);
    }

    /** Starts declaring the steps of {@code owner}, named in words for whatever has to say what it upgrades. */
    public static <T> Builder<T> builder(final String owner) {
        return new Builder<>(owner);
    }

    /** What the steps upgrade, in words. */
    public String owner() {
        return this.owner;
    }

    /** Today's version of the layout. */
    public int version() {
        return this.version;
    }

    /** Whether {@code found} is a version newer than today's, written by a newer version of whatever wrote it. */
    public boolean isNewer(final int found) {
        return found > this.version;
    }

    /** Whether something found at {@code found} takes at least one step on its way to today's version. */
    public boolean needsSteps(final int found) {
        return !between(found).isEmpty();
    }

    /**
     * {@code value}, found at version {@code found}, brought up to today's version by every step from there on, in
     * order. A version below 0 is read as 0, the version of whatever was written before there were versions.
     */
    public T upgrade(final T value, final int found) {
        T at = value;
        for (final UnaryOperator<T> step : between(found)) {
            at = Objects.requireNonNull(step.apply(at), "a step of " + this.owner + " gave back nothing");
        }
        return at;
    }

    private Collection<UnaryOperator<T>> between(final int found) {
        final int from = Math.max(0, found);
        return from >= this.version ? List.of() : this.steps.subMap(from, true, this.version, false).values();
    }

    /**
     * Declares the steps, a version and a step at a time; {@link #build()} checks every step starts below today's
     * version.
     *
     * @param <T> what the steps rewrite
     */
    public static final class Builder<T> {

        private final String owner;
        private final NavigableMap<Integer, UnaryOperator<T>> steps = new TreeMap<>();
        private int version = 1;

        private Builder(final String owner) {
            this.owner = Objects.requireNonNull(owner, "owner");
        }

        /** Today's version of the layout, counted from 1; raised when something older needs a step to be read. */
        public Builder<T> version(final int layout) {
            if (layout < 1) {
                throw new IllegalArgumentException("a layout's version counts from 1, not " + layout);
            }
            this.version = layout;
            return this;
        }

        /** The step that takes something of version {@code from} to version {@code from + 1}. */
        public Builder<T> step(final int from, final UnaryOperator<T> step) {
            if (from < 0) {
                throw new IllegalArgumentException("a layout's version is never below 0: " + from);
            }
            if (this.steps.putIfAbsent(from, Objects.requireNonNull(step, "step")) != null) {
                throw new IllegalArgumentException("two steps from version " + from + " of " + this.owner);
            }
            return this;
        }

        public UpgradeChain<T> build() {
            if (!this.steps.isEmpty() && this.steps.lastKey() >= this.version) {
                throw new IllegalArgumentException("a step from version " + this.steps.lastKey() + " of "
                        + this.owner + ", which is only at version " + this.version);
            }
            return new UpgradeChain<>(this);
        }
    }
}
