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
 * What makes the objects of the language's core that a program makes with {@code new}: its collections, a queue and a
 * stack being lists and a set a map whose keys are its members, told apart by the type the program wrote and never by
 * what they hold; and a text built a piece at a time.
 */
final class CoreObjects {

    private static final IObjectMaker LIST = (process, arguments, line) -> {
        final Values.ListValue made = new Values.ListValue();
        return process.heap().allocate(made, made.bytes(), line);
    };
    private static final IObjectMaker MAP = (process, arguments, line) -> {
        final Values.MapValue made = new Values.MapValue();
        return process.heap().allocate(made, made.bytes(), line);
    };
    private static final Map<String, IObjectMaker> MAKERS = Map.of("List", LIST, "Queue", LIST, "Stack", LIST,
            "Map", MAP, "Set", MAP, TextToolFunctions.BUILDER,
            (process, arguments, line) -> TextToolFunctions.newBuilder(process.heap(), line));

    private CoreObjects() {
    }

    /** What makes the core's object of that name, written without what it holds, or null for any other type. */
    static IObjectMaker find(final String bare) {
        return MAKERS.get(bare);
    }
}
