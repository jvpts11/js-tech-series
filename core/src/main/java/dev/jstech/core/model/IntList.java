/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.model;

import java.util.Arrays;

/** Whole numbers gathered one by one as a file is read, in an array that grows, with no number boxed. Pure. */
final class IntList {

    private int[] values = new int[FIRST];
    private int size;

    private static final int FIRST = 64;

    void add(final int value) {
        if (this.size == this.values.length) {
            this.values = Arrays.copyOf(this.values, this.size * 2);
        }
        this.values[this.size++] = value;
    }

    int get(final int index) {
        return this.values[index];
    }

    int size() {
        return this.size;
    }

    int[] toArray() {
        return Arrays.copyOf(this.values, this.size);
    }
}
