/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.grid;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import org.jetbrains.annotations.Nullable;

/**
 * How one connected part of a line shares, each tick, what it carries among the blocks its ends plug into, the same for
 * items, liquids and gases. Pure: no game types; each line's mover gives it its ends and its way of moving.
 *
 * <p>A block's own output (it gives what it holds and would not take it back) feeds the inputs (they take and give
 * nothing of it); what is left fills the stores (they take and give it, as a tank); and the stores feed what the inputs
 * still want. An end set to pull is an output whatever its block is, an end set to push an input; an end turned off is
 * no end at all. An end gives and takes only on its own channel, only what its filter lets through, at most its rate a
 * tick, and fills its block no further than the amount its filter keeps there.
 *
 * <p>Among the ends a share could go to, the highest priority fills first, and those of one priority take turns: each
 * tick the next of them is served first, so a lower one never gets what a higher one still wants. The whole part moves
 * no more in a tick than its slowest cable carries.
 */
public final class EndRouter {

    /** How many channels a line carries, each end on one of them. */
    public static final int CHANNELS = 16;

    private static final int ROUNDS = 3;

    private EndRouter() {
    }

    /**
     * Moves one tick's worth through a part whose ends are {@code ends}, at most {@code budget}, only what
     * {@code carries} lets the part's cables carry, moving each share with {@code move}; {@code turn} counts the ticks,
     * so the ends of one priority take turns. How much moved.
     */
    public static <R> long route(final List<? extends IEnd<R>> ends, final long budget, final long turn,
                                 final Predicate<R> carries, final IMove<R> move) {
        if (budget <= 0 || ends.size() < 2) {
            return 0L;
        }
        final long[] endLeft = new long[ends.size()];
        for (int i = 0; i < endLeft.length; i++) {
            final long rate = ends.get(i).rate();
            endLeft[i] = rate > 0 ? rate : Long.MAX_VALUE;
        }
        final long[] left = {budget};
        for (int round = 0; round < ROUNDS; round++) {
            for (int i = 0; i < ends.size() && left[0] > 0; i++) {
                give(ends, i, round, turn, carries, move, endLeft, left);
            }
        }
        return budget - left[0];
    }

    /* Gives what the end {@code from} offers to the ends that take it in {@code round}. */
    private static <R> void give(final List<? extends IEnd<R>> ends, final int from, final int round, final long turn,
                                 final Predicate<R> carries, final IMove<R> move, final long[] endLeft,
                                 final long[] left) {
        final IEnd<R> source = ends.get(from);
        if (!source.mode().draws() || endLeft[from] <= 0) {
            return;
        }
        // The first thing some end would take is offered, so what nobody wants never holds up what comes after it.
        final Offer<R> offer = source.offer(Math.min(left[0], endLeft[from]),
                resource -> source.allows(resource) && carries.test(resource) && taken(ends, from, resource, endLeft));
        if (offer == null || offer.amount() <= 0) {
            return;
        }
        final R resource = offer.resource();
        if (role(source, resource) != (round == 2 ? Role.STORE : Role.OUTPUT)) {
            return;
        }
        final Role wanted = round == 1 ? Role.STORE : Role.INPUT;
        long remaining = offer.amount();
        for (final int to : destinations(ends, from, resource, wanted, turn, endLeft)) {
            if (remaining <= 0 || left[0] <= 0 || endLeft[from] <= 0) {
                return;
            }
            final IEnd<R> target = ends.get(to);
            long want = target.take(resource, Math.min(Math.min(remaining, left[0]), Math.min(endLeft[to],
                    endLeft[from])));
            final long keep = target.keep(resource);
            if (keep > 0) {
                want = Math.min(want, Math.max(0L, keep - target.holding(resource)));
            }
            if (want <= 0) {
                continue;
            }
            final long moved = Math.max(0L, move.move(source, target, resource, want));
            remaining -= moved;
            left[0] -= moved;
            endLeft[from] -= moved;
            endLeft[to] -= moved;
        }
    }

    /*
     * The ends a share of {@code resource} from {@code from} could go to, in the order they are served: the highest
     * priority first, and within one priority starting at the one whose turn it is.
     */
    private static <R> List<Integer> destinations(final List<? extends IEnd<R>> ends, final int from,
                                                  final R resource, final Role wanted, final long turn,
                                                  final long[] endLeft) {
        final IEnd<R> source = ends.get(from);
        final List<Integer> found = new ArrayList<>();
        for (int to = 0; to < ends.size(); to++) {
            final IEnd<R> target = ends.get(to);
            if (to != from && target.key() != source.key() && target.channel() == source.channel()
                    && target.mode().fills() && endLeft[to] > 0 && target.allows(resource)
                    && role(target, resource) == wanted) {
                found.add(to);
            }
        }
        found.sort(Comparator.comparingInt((Integer to) -> -ends.get(to).priority()));
        final List<Integer> ordered = new ArrayList<>(found.size());
        int start = 0;
        while (start < found.size()) {
            final int priority = ends.get(found.get(start)).priority();
            int end = start;
            while (end < found.size() && ends.get(found.get(end)).priority() == priority) {
                end++;
            }
            final int size = end - start;
            final int first = (int) Math.floorMod(turn, (long) size);
            for (int k = 0; k < size; k++) {
                ordered.add(found.get(start + (first + k) % size));
            }
            start = end;
        }
        return ordered;
    }

    /* Whether some end but {@code from}'s block would take any {@code resource} on its channel this tick. */
    private static <R> boolean taken(final List<? extends IEnd<R>> ends, final int from, final R resource,
                                     final long[] endLeft) {
        final IEnd<R> source = ends.get(from);
        for (int to = 0; to < ends.size(); to++) {
            final IEnd<R> target = ends.get(to);
            if (to == from || target.key() == source.key() || target.channel() != source.channel()
                    || !target.mode().fills() || endLeft[to] <= 0 || !target.allows(resource)) {
                continue;
            }
            final long keep = target.keep(resource);
            if ((keep <= 0 || target.holding(resource) < keep) && target.take(resource, 1L) > 0) {
                return true;
            }
        }
        return false;
    }

    /* What {@code end} is to a share of {@code resource}, by its block and its mode. */
    private static <R> Role role(final IEnd<R> end, final R resource) {
        return switch (end.mode()) {
            case PULL -> end.gives(resource) ? Role.OUTPUT : Role.NONE;
            case PUSH -> end.takes(resource) ? Role.INPUT : Role.NONE;
            case OFF -> Role.NONE;
            case NORMAL -> {
                final boolean gives = end.gives(resource);
                final boolean takes = end.takes(resource);
                yield gives && takes ? Role.STORE : gives ? Role.OUTPUT : takes ? Role.INPUT : Role.NONE;
            }
        };
    }

    /**
     * One end of a part: the block it plugs into, as the line sees it through that end, and how the end is set.
     *
     * @param <R> what the line carries, one kind of it, without an amount
     */
    public interface IEnd<R> {

        /** The block it plugs into, as a number: two ends of one block never pass to each other. */
        long key();

        EndMode mode();

        /** Its priority among the ends a share could go to: the highest fills first. */
        int priority();

        /** The most it gives or takes a tick; 0 for no limit but the line's. */
        long rate();

        /** The channel it gives and takes on, from 0 to {@link #CHANNELS} - 1. */
        int channel();

        /** Whether its filter lets {@code resource} through. */
        boolean allows(R resource);

        /** How much of {@code resource} its filter keeps in its block, filling it no further; 0 for no limit. */
        long keep(R resource);

        /** What its block would give, at most {@code most}, of the first thing {@code wanted} lets through; none. */
        @Nullable
        Offer<R> offer(long most, Predicate<R> wanted);

        /** Whether its block gives any {@code resource}. */
        boolean gives(R resource);

        /** Whether its block takes any {@code resource}. */
        boolean takes(R resource);

        /** How much of {@code resource}, at most {@code most}, its block would take. */
        long take(R resource, long most);

        /** How much of {@code resource} its block holds. */
        long holding(R resource);
    }

    /**
     * Moves {@code amount} of {@code resource} from one end's block to another's, all or nothing; how much moved.
     *
     * @param <R> what the line carries
     */
    @FunctionalInterface
    public interface IMove<R> {

        long move(IEnd<R> from, IEnd<R> to, R resource, long amount);
    }

    /**
     * What an end's block would give.
     *
     * @param resource what it is
     * @param amount   how much
     * @param <R>      what the line carries
     */
    public record Offer<R>(R resource, long amount) {
    }

    /* What an end is to one share: where it comes from, where it goes, or neither. */
    private enum Role {
        OUTPUT, INPUT, STORE, NONE
    }
}
