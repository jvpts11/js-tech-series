/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.engine.prophet;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * The states Prophet YourIQL holds a network to, and its watches, with what it last saw of each: the evaluation that
 * says what to do next.
 *
 * <p>It does only what changed. When the network's catalog has not changed since it last looked and no work it set
 * going has settled, an evaluation is one comparison and nothing else, which is what a network standing still costs.
 * When something did change, each state reads the one count it is about and goes no further when that count and the
 * work it has on its way are what they were. A state below its band asks for what it lacks, counting what is already
 * on its way, so a level that is coming back up is not asked for twice. A watch fires once when its condition comes
 * true and is armed again only when it stops being true.
 *
 * <p>Pure logic: what the network holds comes through {@link IHoldings}, and what to do goes back as
 * {@link Reaction}s for the engine to carry out, so its tests and its benchmarks run without the game.
 */
public final class ProphetStates {

    private final List<KeepState> keeps = new ArrayList<>();
    private final List<WatchState> watches = new ArrayList<>();
    private long lastVersion = Long.MIN_VALUE;
    private boolean dirty = true;
    private int nextWatch = 1;

    /** How many samples of a state's level are kept for its graph. */
    public static final int SAMPLES = 48;

    /** What the network holds of an item, by its id. */
    @FunctionalInterface
    public interface IHoldings {
        long held(String item);
    }

    /** Keeps {@code item} in its band, replacing the state it had; answers the state. */
    public KeepState keep(final String item, final long lower, final long upper) {
        KeepState state = keepOf(item);
        if (state == null) {
            state = new KeepState(item, lower, upper);
            keeps.add(state);
        } else {
            state.lower = lower;
            state.upper = upper;
            state.evaluated = false;
        }
        dirty = true;
        return state;
    }

    /** Adds a watch; answers it, with the number it is known by. */
    public WatchState watch(final String item, final IProphetStatement.Comparison comparison, final long threshold,
                            final String action) {
        final WatchState watch = new WatchState(nextWatch++, item, comparison, threshold, action);
        watches.add(watch);
        dirty = true;
        return watch;
    }

    /** Lets go of the state kept for {@code item}; false when there was none. */
    public boolean forget(final String item) {
        return keeps.removeIf(state -> state.item.equals(item));
    }

    /** Lets go of watch number {@code number}; false when there was none. */
    public boolean forgetWatch(final int number) {
        return watches.removeIf(watch -> watch.number == number);
    }

    /** The state kept for {@code item}, or null. */
    @Nullable
    public KeepState keepOf(final String item) {
        for (final KeepState state : keeps) {
            if (state.item.equals(item)) {
                return state;
            }
        }
        return null;
    }

    /** Every state kept, in the order they were declared. */
    public List<KeepState> keeps() {
        return List.copyOf(keeps);
    }

    /** Every watch, in the order they were declared. */
    public List<WatchState> watches() {
        return List.copyOf(watches);
    }

    /**
     * What to do now. {@code version} changes whenever what the network holds does; {@code now} is the time the
     * reactions and the samples are stamped with. With nothing changed since the last call, nothing is read.
     */
    public List<Reaction> evaluate(final long version, final long now, final IHoldings holdings) {
        if (version == lastVersion && !dirty) {
            return List.of();
        }
        lastVersion = version;
        dirty = false;
        List<Reaction> out = null;
        for (final KeepState state : keeps) {
            // What arrived counts until the catalog has changed since it arrived, by which time it is in the count.
            if (state.arriving > 0 && version != state.arrivedAt) {
                state.arriving = 0;
            }
            final long held = holdings.held(state.item);
            if (state.evaluated && held == state.held && state.inFlight == state.seenInFlight
                    && state.arriving == state.seenArriving) {
                continue;
            }
            state.evaluated = true;
            state.held = held;
            state.seenInFlight = state.inFlight;
            state.seenArriving = state.arriving;
            state.sample(now);
            final long level = held + state.inFlight + state.arriving;
            if (level < state.lower) {
                state.status = state.cannotHold ? Status.CANNOT_HOLD : Status.WORKING;
                if (!state.cannotHold) {
                    out = add(out, new Reaction(Reaction.CRAFT, state.item, state.lower - level, ""));
                }
            } else {
                state.cannotHold = false;
                state.status = state.inFlight > 0 ? Status.WORKING : held > state.upper ? Status.OVER : Status.HOLDING;
            }
        }
        for (final WatchState watch : watches) {
            final long held = holdings.held(watch.item);
            if (watch.evaluated && held == watch.held) {
                continue;
            }
            watch.evaluated = true;
            watch.held = held;
            if (watch.comparison.test(held, watch.threshold)) {
                if (watch.armed) {
                    watch.armed = false;
                    watch.status = Status.FIRED;
                    watch.fired++;
                    out = add(out, new Reaction(Reaction.FIRE, watch.item, watch.number, watch.action));
                }
            } else {
                watch.armed = true;
                watch.status = Status.ARMED;
            }
        }
        return out == null ? List.of() : out;
    }

    /** Work for {@code item}'s state was set going: {@code amount} is on its way. */
    public void started(final String item, final long amount) {
        final KeepState state = keepOf(item);
        if (state != null) {
            state.inFlight += amount;
            state.reactions++;
        }
    }

    /**
     * Work for {@code item}'s state settled when the catalog stood at {@code version}: {@code amount} is no longer on
     * its way. What it made is counted as arriving until the catalog changes, since the network's count of what it
     * holds catches up a little after the work hands its items over; without that, a state would ask again for what
     * just came.
     */
    public void settled(final String item, final long amount, final long version) {
        final KeepState state = keepOf(item);
        if (state != null) {
            final long settled = Math.min(amount, state.inFlight);
            state.inFlight -= settled;
            state.arriving += settled;
            state.arrivedAt = version;
            dirty = true;
        }
    }

    /** Nothing could be set going for {@code item}'s state: it cannot hold until something changes. */
    public void cannotHold(final String item) {
        final KeepState state = keepOf(item);
        if (state != null) {
            state.cannotHold = true;
            state.status = Status.CANNOT_HOLD;
        }
    }

    /** Has every state that could not hold try again at the next evaluation, as something may have changed since. */
    public void retry() {
        for (final KeepState state : keeps) {
            if (state.cannotHold) {
                state.cannotHold = false;
                state.evaluated = false;
                dirty = true;
            }
        }
    }

    /** Has the next evaluation read every state again, whatever changed. */
    public void touch() {
        dirty = true;
        for (final KeepState state : keeps) {
            state.evaluated = false;
        }
        for (final WatchState watch : watches) {
            watch.evaluated = false;
        }
    }

    /** Sets the number the next watch gets, as a save read back says it. */
    public void nextWatch(final int number) {
        nextWatch = Math.max(nextWatch, number);
    }

    /** The number the next watch gets. */
    public int nextWatchNumber() {
        return nextWatch;
    }

    private static List<Reaction> add(@Nullable final List<Reaction> out, final Reaction reaction) {
        final List<Reaction> list = out == null ? new ArrayList<>() : out;
        list.add(reaction);
        return list;
    }

    /** Where a state or a watch stands. */
    public enum Status {
        /** Not looked at yet. */
        NEW,
        /** In its band, nothing on its way. */
        HOLDING,
        /** Work on its way to bring or keep it in its band. */
        WORKING,
        /** Below its band, and nothing could be set going. */
        CANNOT_HOLD,
        /** Above its band; nothing is thrown away to bring it down. */
        OVER,
        /** A watch waiting for its condition. */
        ARMED,
        /** A watch that fired and waits for its condition to stop. */
        FIRED
    }

    /**
     * Something to do.
     *
     * @param kind   {@link #CRAFT} for a state below its band, {@link #FIRE} for a watch
     * @param item   the item it is about
     * @param amount how many to make, for a state; the watch's number, for a watch
     * @param action the statement a watch runs; empty for a state
     */
    public record Reaction(byte kind, String item, long amount, String action) {

        public static final byte CRAFT = 0;
        public static final byte FIRE = 1;
    }

    /** One level kept, as seen at a moment: what was held and what was on its way. */
    public record Sample(long at, long held, long inFlight) {
    }

    /** A state the network is held to, with what was last seen of it. */
    public static final class KeepState {

        private final String item;
        private final Deque<Sample> samples = new ArrayDeque<>();
        private long lower;
        private long upper;
        private long held;
        private long inFlight;
        private long seenInFlight;
        private long arriving;
        private long seenArriving;
        private long arrivedAt;
        private boolean evaluated;
        private boolean cannotHold;
        private int reactions;
        private Status status = Status.NEW;

        KeepState(final String item, final long lower, final long upper) {
            this.item = item;
            this.lower = lower;
            this.upper = upper;
        }

        public String item() {
            return item;
        }

        public long lower() {
            return lower;
        }

        public long upper() {
            return upper;
        }

        /** What the network held when last looked at. */
        public long held() {
            return held;
        }

        /** What is on its way. */
        public long inFlight() {
            return inFlight;
        }

        public Status status() {
            return status;
        }

        /** How many times work was set going for it. */
        public int reactions() {
            return reactions;
        }

        /** The levels seen lately, the oldest first. */
        public List<Sample> samples() {
            return List.copyOf(samples);
        }

        /* Notes the level at {@code now}, letting the oldest go past {@link #SAMPLES}. */
        private void sample(final long now) {
            if (!samples.isEmpty() && samples.peekLast().at() == now) {
                samples.pollLast();
            }
            samples.addLast(new Sample(now, held, inFlight));
            while (samples.size() > SAMPLES) {
                samples.pollFirst();
            }
        }
    }

    /** A watch, with what was last seen of it. */
    public static final class WatchState {

        private final int number;
        private final String item;
        private final IProphetStatement.Comparison comparison;
        private final long threshold;
        private final String action;
        private long held;
        private boolean evaluated;
        private boolean armed = true;
        private int fired;
        private Status status = Status.NEW;

        WatchState(final int number, final String item, final IProphetStatement.Comparison comparison,
                   final long threshold, final String action) {
            this.number = number;
            this.item = item;
            this.comparison = comparison;
            this.threshold = threshold;
            this.action = action;
        }

        public int number() {
            return number;
        }

        public String item() {
            return item;
        }

        public IProphetStatement.Comparison comparison() {
            return comparison;
        }

        public long threshold() {
            return threshold;
        }

        public String action() {
            return action;
        }

        public boolean armed() {
            return armed;
        }

        /** Sets whether it is armed, as a save read back says. */
        public void armed(final boolean value) {
            this.armed = value;
        }

        /** How many times it fired. */
        public int fired() {
            return fired;
        }

        public Status status() {
            return status;
        }

        /** How the watch is written. */
        public String written() {
            return "WATCH " + item + " " + comparison.symbol() + " " + threshold + " DO " + action;
        }
    }
}
