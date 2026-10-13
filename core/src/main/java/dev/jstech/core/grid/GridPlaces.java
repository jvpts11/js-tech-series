/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.grid;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

/**
 * The numbers a dimension's grids know their places by. A {@link Grid} holds numbers and nothing of the world; this
 * gives each {@link GridPlace} its own, the same for as long as the place is in use, and tells which place a number
 * stands for. One table serves every grid of a dimension: a lane holds one wire of one kind, and a device that is in
 * several grids is the same whole block in each.
 *
 * <p>Nothing here is saved: the places are numbered again as the blocks load.
 */
public final class GridPlaces {

    private final Map<GridPlace, Long> numbers = new HashMap<>();
    private final Map<Long, GridPlace> places = new HashMap<>();
    private final Map<Long, Set<GridPlace>> byBlock = new HashMap<>();
    private long next;

    /** The number of {@code place}: the one it has, or a new one. */
    public long number(final GridPlace place) {
        final Long known = this.numbers.get(place);
        if (known != null) {
            return known;
        }
        final long number = this.next++;
        this.numbers.put(place, number);
        this.places.put(number, place);
        this.byBlock.computeIfAbsent(place.pos(), pos -> new LinkedHashSet<>()).add(place);
        return number;
    }

    /** The number of {@code place}, or empty when it has none. */
    public OptionalLong find(final GridPlace place) {
        final Long known = this.numbers.get(place);
        return known == null ? OptionalLong.empty() : OptionalLong.of(known);
    }

    /** The place {@code number} stands for, or null when it stands for none. */
    public @Nullable GridPlace place(final long number) {
        return this.places.get(number);
    }

    /** Every place numbered in the block at {@code pos}. */
    public Set<GridPlace> at(final long pos) {
        final Set<GridPlace> found = this.byBlock.get(pos);
        return found == null ? Set.of() : Collections.unmodifiableSet(found);
    }

    /** Takes {@code place}'s number away; a place numbered again later gets a new one. */
    public void forget(final GridPlace place) {
        final Long number = this.numbers.remove(place);
        if (number == null) {
            return;
        }
        this.places.remove(number);
        final Set<GridPlace> block = this.byBlock.get(place.pos());
        if (block != null) {
            block.remove(place);
            if (block.isEmpty()) {
                this.byBlock.remove(place.pos());
            }
        }
    }

    /** How many places are numbered. */
    public int size() {
        return this.numbers.size();
    }
}
