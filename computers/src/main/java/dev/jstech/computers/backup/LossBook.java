/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.backup;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A network's book of losses: every time items stopped existing, how many, when and how. Restoring a backup of items
 * gives back, item by item, the lesser of what the backup holds and what the book lost since the backup was made, and
 * marks those losses given back, so each loss comes back once whatever is restored after it: a copied tape or two
 * backups restored one after the other yield nothing more (computers/docs/design/programs.md).
 *
 * <p>Free of Minecraft, the kind of item a type parameter, so the rule is tested without a world.
 *
 * @param <K> what an item is
 */
public final class LossBook<K> {

    private final List<Loss<K>> losses = new ArrayList<>();
    private long nextId = 1L;

    /** The most losses a network's book keeps, the oldest falling off first. */
    public static final int MOST = 2_000;

    /**
     * Writes a loss down: {@code count} of {@code key}, at the game time {@code at} and the world's clock
     * {@code clock}, lost by {@code cause} on {@code machine}'s disk {@code disk} (or on no disk in particular, -1).
     */
    public void lose(final K key, final long count, final long at, final long clock, final LossCause cause,
                     final String machine, final int disk) {
        if (count <= 0L) {
            return;
        }
        // A loss written in parts in the same tick, as a DROP's batches are, is one line of the book.
        if (!losses.isEmpty()) {
            final Loss<K> last = losses.getLast();
            if (last.at() == at && last.restored() == 0L && last.key().equals(key) && last.cause() == cause
                    && last.machine().equals(machine) && last.disk() == disk) {
                losses.set(losses.size() - 1, new Loss<>(last.id(), key, last.count() + count, 0L, at, last.clock(),
                        cause, machine, disk));
                return;
            }
        }
        losses.add(new Loss<>(nextId++, key, count, 0L, at, clock, cause, machine, disk));
        while (losses.size() > MOST) {
            losses.removeFirst();
        }
    }

    /** Every loss after the game time {@code at}, oldest first, those already given back among them. */
    public List<Loss<K>> since(final long at) {
        final List<Loss<K>> out = new ArrayList<>();
        for (final Loss<K> loss : losses) {
            if (loss.at() > at) {
                out.add(loss);
            }
        }
        return out;
    }

    /**
     * What restoring {@code held}, a backup made at the game time {@code at}, would give back now, item by item: the
     * lesser of what it holds and what was lost since and not given back yet. Nothing is marked.
     */
    public Map<K, Long> wouldRestore(final Map<K, Long> held, final long at) {
        final Map<K, Long> out = new LinkedHashMap<>();
        for (final Map.Entry<K, Long> item : held.entrySet()) {
            final long back = Math.min(item.getValue(), lostSince(item.getKey(), at));
            if (back > 0L) {
                out.put(item.getKey(), back);
            }
        }
        return out;
    }

    /**
     * Restores {@code held}, a backup made at the game time {@code at}: answers what comes back, item by item, and
     * marks the losses it came from given back, the oldest first, so no loss is given back twice.
     */
    public Map<K, Long> restore(final Map<K, Long> held, final long at) {
        final Map<K, Long> back = wouldRestore(held, at);
        for (final Map.Entry<K, Long> item : back.entrySet()) {
            giveBack(item.getKey(), item.getValue(), at);
        }
        return back;
    }

    /**
     * Marks {@code amount} of {@code key} lost after the game time {@code at} given back, the oldest losses first: what
     * a restore actually put back, which can be less than it meant to when the network had no room for the rest.
     */
    public void giveBack(final K key, final long amount, final long at) {
        long left = amount;
        for (int i = 0; i < losses.size() && left > 0L; i++) {
            final Loss<K> loss = losses.get(i);
            if (loss.at() > at && loss.key().equals(key) && loss.left() > 0L) {
                final long taken = Math.min(left, loss.left());
                losses.set(i, loss.givenBack(taken));
                left -= taken;
            }
        }
    }

    /** Every loss kept, oldest first, for writing the book down. */
    public List<Loss<K>> all() {
        return List.copyOf(losses);
    }

    /** Takes a whole book back, for one read off the save. */
    public void restoreAll(final List<Loss<K>> kept) {
        losses.clear();
        nextId = 1L;
        for (final Loss<K> loss : kept) {
            losses.add(loss);
            nextId = Math.max(nextId, loss.id() + 1L);
        }
    }

    private long lostSince(final K key, final long at) {
        long lost = 0L;
        for (final Loss<K> loss : losses) {
            if (loss.at() > at && loss.key().equals(key)) {
                lost += loss.left();
            }
        }
        return lost;
    }

    /**
     * One loss.
     *
     * @param id       its number in its book
     * @param key      the item lost
     * @param count    how many were lost
     * @param restored how many of them a backup has given back
     * @param at       the game time it happened
     * @param clock    the world's clock when it happened
     * @param cause    how
     * @param machine  the name of the machine it happened on, or of the one that ran the DROP
     * @param disk     the disk it happened on, counted from 1 among the machine's, or -1 when none in particular
     * @param <K>      what an item is
     */
    public record Loss<K>(long id, K key, long count, long restored, long at, long clock, LossCause cause,
                          String machine, int disk) {

        /** How many are still lost, not given back by any backup. */
        public long left() {
            return count - restored;
        }

        private Loss<K> givenBack(final long more) {
            return new Loss<>(id, key, count, restored + more, at, clock, cause, machine, disk);
        }
    }
}
