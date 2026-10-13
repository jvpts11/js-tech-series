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
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The windows and widgets a program makes: one maker for every type of them the system declares a constructor on.
 *
 * <p>A widget is known by a number from the moment it is made, so an event can say which one it happened to; a window
 * gets its number when it is opened. What either holds is the program's, and weighs as much.
 */
@TextHolder
final class WidgetObjects {

    private static final Map<String, IObjectMaker> MAKERS = build();
    private static final TextKey NOT_IN_TEXT = TextKey.of("jsc.vm.widget_objects.not_in_text",
            "there is no %s on a screen of letters: this machine draws its windows in text");
    private static final TextKey OUTSIDE_OFF = TextKey.of("jsc.vm.widget_objects.outside_off",
            "this server keeps components that reach outside the game off, %s among them");

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

    /*
     * A widget a machine cannot show is refused as it is made, so the program hears of it at the line that asked: a
     * picture on a screen of letters, or a component reaching outside the game on a server that keeps those off. A
     * context menu is made for a widget, and is that widget's from the start.
     */
    private static Values.Obj make(final Process process, final String type, final List<Object> arguments,
                                   final int line) {
        if (process.host().textMode() && !UiWidgets.drawnInText(type)) {
            throw new Halt(Halt.Reason.REFUSED, line, NOT_IN_TEXT.with(type));
        }
        final Values.Obj owner = UiWidgets.CONTEXT_MENU.equals(type)
                ? UiWidgets.placeableWidget(arguments.isEmpty() ? null : arguments.getFirst(), line) : null;
        final Values.Obj made = UiWidgets.create(type, arguments, line);
        if (UiWidgets.GENERIC.equals(type)) {
            final IComponentRule kind = ComponentRules.find(String.valueOf(made.get(UiWidgets.KIND)));
            if (kind != null && kind.reachesOutside() && !process.host().outsideComponents()) {
                throw new Halt(Halt.Reason.REFUSED, line, OUTSIDE_OFF.with(kind.id()));
            }
        }
        if (UiWidgets.isWidget(type)) {
            made.set(UiWidgets.ID, process.nextWidgetId());
        }
        process.heap().adopt(made, line);
        if (owner != null) {
            process.windows0().mutator().attachMenu(owner, made, line);
        }
        return made;
    }
}
