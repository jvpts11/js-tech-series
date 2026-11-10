/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.chain.machine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The chain machines' shapes as their table writes them, read: a shape is its layers from the bottom, each its rows
 * from the front, split by commas, each row west to east; a legend is its letters, split by commas, each
 * {@code letter=block}. Pure.
 */
public final class ChainShapes {

    private ChainShapes() {
    }

    /** Each of {@code layers}, from the bottom, as its rows. */
    public static List<List<String>> layers(final String... layers) {
        final List<List<String>> out = new ArrayList<>();
        for (final String layer : layers) {
            out.add(List.of(layer.trim().split(",")));
        }
        return out;
    }

    /** The block of each letter of {@code legend}, in its order. */
    public static Map<Character, String> legend(final String legend) {
        final Map<Character, String> out = new LinkedHashMap<>();
        for (final String pair : legend.split(",")) {
            final String[] parts = pair.trim().split("=");
            if (parts.length != 2 || parts[0].length() != 1) {
                throw new IllegalArgumentException("not a letter and its block: " + pair);
            }
            out.put(parts[0].charAt(0), parts[1]);
        }
        return out;
    }

    /**
     * Whether {@code layers} is a whole box with one terminal: every layer as many rows, every row as long, every
     * letter in the legend, and the terminal {@code #} once.
     */
    public static boolean sound(final List<List<String>> layers, final Map<Character, String> legend) {
        int terminals = 0;
        for (final List<String> layer : layers) {
            if (layer.size() != layers.getFirst().size()) {
                return false;
            }
            for (final String row : layer) {
                if (row.length() != layers.getFirst().getFirst().length()) {
                    return false;
                }
                for (final char c : row.toCharArray()) {
                    if (c == '#') {
                        terminals++;
                    } else if (!legend.containsKey(c)) {
                        return false;
                    }
                }
            }
        }
        return terminals == 1;
    }
}
