/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.cannon.run;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * The numbers a process's objects are written under in its snapshot.
 *
 * <p>Everything still held is numbered first, in the order it was allocated, so two saves of the same
 * program read the same. Something the program has freed is numbered only when something being written
 * down still points at it: that reference has to come back pointing at a freed thing, so using it still
 * stops the program, while a freed thing nothing points at is not written at all. Writing an object down
 * can number another, so whoever writes the objects walks them while this grows.
 */
final class HeldNumbers {

    private final Map<Object, Integer> numbers = new IdentityHashMap<>();
    private final List<Object> order = new ArrayList<>();
    private final Heap heap;

    HeldNumbers(final Heap heap) {
        this.heap = heap;
        for (final Object held : heap.live()) {
            this.add(held);
        }
    }

    /** The number a thing is written under, or null for something that is not the program's to write. */
    Integer numberOf(final Object thing) {
        final Integer known = this.numbers.get(thing);
        if (known != null || !this.heap.isFreed(thing)) {
            return known;
        }
        return this.add(thing);
    }

    /** How many things are numbered so far. */
    int size() {
        return this.order.size();
    }

    /** The thing written under that number. */
    Object thing(final int number) {
        return this.order.get(number);
    }

    private Integer add(final Object thing) {
        final int number = this.order.size();
        this.numbers.put(thing, number);
        this.order.add(thing);
        return number;
    }
}
