/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.Nullable;

/**
 * One record of an FBX file, binary or text alike: its name, its properties in order, and the records inside it.
 * A property is a {@link Long}, a {@link Double}, a {@link Boolean}, a {@link String}, a {@code byte[]} of raw data,
 * or an array of numbers, a {@code long[]} or a {@code double[]}.
 *
 * @param name       what it is
 * @param properties its properties
 * @param children   the records inside it
 */
record FbxNode(String name, List<Object> properties, List<FbxNode> children) {

    FbxNode {
        Objects.requireNonNull(name, "name");
        properties = List.copyOf(properties);
        children = List.copyOf(children);
    }

    /** The first record inside it named {@code childName}, or none. */
    @Nullable
    FbxNode child(final String childName) {
        for (final FbxNode child : this.children) {
            if (child.name.equals(childName)) {
                return child;
            }
        }
        return null;
    }

    /** Every record inside it named {@code childName}, in their order. */
    List<FbxNode> all(final String childName) {
        final List<FbxNode> out = new ArrayList<>();
        for (final FbxNode child : this.children) {
            if (child.name.equals(childName)) {
                out.add(child);
            }
        }
        return out;
    }

    /** Its {@code index}th property as a number, or {@code otherwise} for none or not a number. */
    double number(final int index, final double otherwise) {
        return index < this.properties.size() && this.properties.get(index) instanceof Number value
                ? value.doubleValue() : otherwise;
    }

    /** Its {@code index}th property as a whole number. */
    long whole(final int index) {
        return index < this.properties.size() && this.properties.get(index) instanceof Number value
                ? value.longValue() : 0;
    }

    /** Its {@code index}th property as text, or empty. */
    String text(final int index) {
        return index < this.properties.size() && this.properties.get(index) instanceof String value ? value : "";
    }

    /**
     * The numbers it holds: its first property's array, or, as the text form can write a list, every property that
     * is a number.
     */
    double[] doubles() {
        if (!this.properties.isEmpty()) {
            final Object first = this.properties.getFirst();
            if (first instanceof double[] values) {
                return values.clone();
            }
            if (first instanceof long[] values) {
                final double[] out = new double[values.length];
                for (int i = 0; i < values.length; i++) {
                    out[i] = values[i];
                }
                return out;
            }
        }
        final List<Double> numbers = new ArrayList<>();
        for (final Object property : this.properties) {
            if (property instanceof Number value) {
                numbers.add(value.doubleValue());
            }
        }
        final double[] out = new double[numbers.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = numbers.get(i);
        }
        return out;
    }

    /** The whole numbers it holds, as {@link #doubles()}. */
    long[] longs() {
        if (!this.properties.isEmpty() && this.properties.getFirst() instanceof long[] values) {
            return values.clone();
        }
        final double[] numbers = doubles();
        final long[] out = new long[numbers.length];
        for (int i = 0; i < numbers.length; i++) {
            out[i] = (long) numbers[i];
        }
        return out;
    }

    /** The numbers the record {@code childName} inside it holds, or none. */
    double[] doublesOf(final String childName) {
        final FbxNode child = child(childName);
        return child == null ? new double[0] : child.doubles();
    }

    /** The whole numbers the record {@code childName} inside it holds, or none. */
    long[] longsOf(final String childName) {
        final FbxNode child = child(childName);
        return child == null ? new long[0] : child.longs();
    }

    /** The text of the record {@code childName} inside it, or empty. */
    String textOf(final String childName) {
        final FbxNode child = child(childName);
        return child == null ? "" : child.text(0);
    }
}
