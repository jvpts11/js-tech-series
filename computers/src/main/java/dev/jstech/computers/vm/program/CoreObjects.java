/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import java.util.Map;

/**
 * The objects the language's core brings for a program to make: a list and a map.
 *
 * <p>A program reaches them through the {@code new} it loaded with, and the runtime asked to make one by name reaches
 * them here too, so each is made in one place.
 */
final class CoreObjects {

    private static final Map<String, IObjectMaker> MAKERS = Map.of(
            "List", (process, arguments, line) -> {
                final Values.ListValue made = new Values.ListValue();
                return process.heap().allocate(made, made.bytes(), line);
            },
            "Map", (process, arguments, line) -> {
                final Values.MapValue made = new Values.MapValue();
                return process.heap().allocate(made, made.bytes(), line);
            });

    private CoreObjects() {
    }

    /** What makes the core's object of that name, written without what it holds, or null for any other type. */
    static IObjectMaker find(final String bare) {
        return MAKERS.get(bare);
    }
}
