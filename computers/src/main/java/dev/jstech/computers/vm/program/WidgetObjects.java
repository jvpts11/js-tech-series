/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import dev.jstech.computers.vm.system.ConstructorSpec;
import dev.jstech.computers.vm.system.IMemberSpec;
import dev.jstech.computers.vm.system.SystemApi;
import dev.jstech.computers.vm.system.TypeSpec;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The windows and widgets a program makes: one maker for every type of them the system declares a constructor on.
 *
 * <p>A widget is known by a number from the moment it is made, so an event can say which one it happened to; a window
 * gets its number when it is opened. What either holds is the program's, and weighs as much.
 */
final class WidgetObjects {

    private static final Map<String, IObjectMaker> MAKERS = build();

    private WidgetObjects() {
    }

    /** What makes a window or a widget of that type, or null when the system declares no way of making one. */
    static IObjectMaker find(final String type) {
        return MAKERS.get(type);
    }

    private static Map<String, IObjectMaker> build() {
        final Map<String, IObjectMaker> makers = new HashMap<>();
        for (final TypeSpec type : SystemApi.types()) {
            final String name = type.name();
            for (final IMemberSpec member : type.members()) {
                if (member instanceof ConstructorSpec && UiWidgets.handles(name)) {
                    makers.putIfAbsent(name, (process, arguments, line) -> make(process, name, arguments, line));
                }
            }
        }
        return Map.copyOf(makers);
    }

    private static Values.Obj make(final Process process, final String type, final List<Object> arguments,
                                   final int line) {
        final Values.Obj made = UiWidgets.create(type, arguments, line);
        if (UiWidgets.isWidget(type)) {
            made.set(UiWidgets.ID, process.nextWidgetId());
        }
        process.heap().adopt(made, line);
        return made;
    }
}
