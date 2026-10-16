/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.conveyor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The items on one block of a conveyor, in order along it: each at how far along the block it has come, from 0 where
 * the block begins to 1 where it ends, never closer to the one ahead than the gap. Moved, each goes as far as the belt
 * goes, up to the one ahead less the gap, so a belt that cannot hand its first item on fills up behind it and
 * thousands of items cost a number each. Pure.
 *
 * @param <T> what an item is
 */
public final class BeltLane<T> {

    private final List<Entry<T>> entries = new ArrayList<>();
    private final double gap;

    private static final double END = 1.0;

    /** A lane whose items keep {@code gap} of a block between them: a quarter for four to a block. */
    public BeltLane(final double gap) {
        if (gap <= 0.0 || gap > END) {
            throw new IllegalArgumentException("an item keeps a gap between nothing and a whole block, got " + gap);
        }
        this.gap = gap;
    }

    /** The gap the items keep, in blocks. */
    public double gap() {
        return this.gap;
    }

    /** The items, the first along the belt first. */
    public List<Entry<T>> entries() {
        return Collections.unmodifiableList(this.entries);
    }

    /** Whether no item is on it. */
    public boolean isEmpty() {
        return this.entries.isEmpty();
    }

    /** How many items are on it. */
    public int size() {
        return this.entries.size();
    }

    /**
     * Whether an item can come on at {@code at}, from 0 to 1: no item within the gap of it.
     */
    public boolean roomAt(final double at) {
        for (final Entry<T> entry : this.entries) {
            if (Math.abs(entry.at() - at) < this.gap) {
                return false;
            }
        }
        return true;
    }

    /**
     * Puts {@code item} on at {@code at}, in its order along the belt, when there is room.
     *
     * @return whether it came on
     */
    public boolean put(final T item, final double at) {
        final double place = Math.clamp(at, 0.0, END);
        if (!roomAt(place)) {
            return false;
        }
        int index = 0;
        while (index < this.entries.size() && this.entries.get(index).at() > place) {
            index++;
        }
        this.entries.add(index, new Entry<>(item, place));
        return true;
    }

    /** Takes the first item off, the one nearest the end; nothing when it is empty. */
    public T takeFirst() {
        return this.entries.removeFirst().item();
    }

    /** Takes off the item at {@code index}, the first being 0. */
    public T take(final int index) {
        return this.entries.remove(index).item();
    }

    /** Swaps the item at {@code index} for {@code item}, where it stands: what a machine on the belt made of it. */
    public void replace(final int index, final T item) {
        this.entries.set(index, new Entry<>(item, this.entries.get(index).at()));
    }

    /**
     * Moves every item {@code distance} of a block along, the first up to {@code limit} (1 for the block's end, more
     * when the next block has room for it to go on into, less when it must stop sooner), each after it up to the one
     * ahead less the gap. An item {@code held} stays where it is, as one a machine works.
     *
     * @return whether the first item now stands at the end, ready to be handed on
     */
    public boolean advance(final double distance, final double limit, final int held) {
        return advance(distance, limit, held, Double.NEGATIVE_INFINITY);
    }

    /**
     * Moves every item as {@link #advance(double, double, int)} does, but the item {@code held} goes on only as far as
     * {@code heldAt}, where a machine stops it to work it, and no further.
     *
     * @return whether the first item now stands at the end, ready to be handed on
     */
    public boolean advance(final double distance, final double limit, final int held, final double heldAt) {
        double ahead = Double.POSITIVE_INFINITY;
        for (int i = 0; i < this.entries.size(); i++) {
            final Entry<T> entry = this.entries.get(i);
            double most = i == 0 ? Math.min(limit, END) : ahead - this.gap;
            if (i == held) {
                most = Math.min(most, Math.max(entry.at(), heldAt));
            }
            final double moved = Math.max(entry.at(), Math.min(entry.at() + distance, most));
            this.entries.set(i, new Entry<>(entry.item(), moved));
            ahead = moved;
        }
        return !this.entries.isEmpty() && this.entries.getFirst().at() >= END;
    }

    /** How far along the last item stands, the one nearest the start; 1 when it is empty. */
    public double lastAt() {
        return this.entries.isEmpty() ? END : this.entries.getLast().at();
    }

    /**
     * An item on the belt and how far along its block it has come.
     *
     * @param item the item
     * @param at   how far along, from 0 to 1
     * @param <T>  what an item is
     */
    public record Entry<T>(T item, double at) {
    }
}
