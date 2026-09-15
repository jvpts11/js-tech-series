/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import dev.jstech.computers.vm.system.EventSpec;
import dev.jstech.computers.vm.system.IMemberSpec;
import dev.jstech.computers.vm.system.MemberId;
import dev.jstech.computers.vm.system.MemberKind;
import dev.jstech.computers.vm.system.PropertySpec;
import dev.jstech.computers.vm.system.SigmaCosts;
import dev.jstech.computers.vm.system.SystemApi;
import dev.jstech.computers.vm.system.TypeSpec;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The values a program reads that its own process or the language's core answers: the program's name and arguments,
 * the thread asking, the length of a text and the size of a collection.
 *
 * <p>A program reaches them through the field it loaded with, never by their names while it runs. Every binding names a
 * value the system declares as the process's on the side of the type it is read from, or one of the core's, and that is
 * checked when the bindings are made.
 */
final class ProcessValues {

    /**
     * One value the process or the core answers.
     *
     * @param id     the value, taking nothing
     * @param read   the Java that reads it
     * @param write  the Java that writes it, or null when nothing here writes it
     * @param onType whether it is read from the type rather than from an object of it
     */
    record Binding(MemberId id, IProcessValue read, IProcessWrite write, boolean onType) {
    }

    /** The namespace a program's windows and widgets are declared in. */
    private static final String UI = "System.UI";

    private static final Map<MemberId, Binding> BINDINGS = build();

    private ProcessValues() {
    }

    /** The binding for that value on that side of its type, or null when nothing here reads it. */
    static Binding find(final String owner, final String name, final boolean onType) {
        final Binding found = BINDINGS.get(new MemberId(owner, name, List.of()));
        return found != null && found.onType() == onType ? found : null;
    }

    /** Every binding. */
    static Iterable<Binding> all() {
        return BINDINGS.values();
    }

    private static Map<MemberId, Binding> build() {
        final Map<MemberId, Binding> bindings = new HashMap<>();
        // What a program knows about itself, and which of its threads is asking.
        onType(bindings, "Program", "Name", (process, target, line) -> process.heap().text(process.name(), line));
        onType(bindings, "Program", "Args", (process, target, line) -> process.argsList(line));
        onType(bindings, "Program", "DroppedEvents", (process, target, line) -> process.droppedEvents());
        onType(bindings, "Program", "Current", (process, target, line) -> process.selfToken(line));
        onType(bindings, "Thread", "Current", (process, target, line) -> process.tokenFor(process.current(), line));
        // What the language's core keeps on a text and on the two collections.
        core(bindings, "string", "Length",
                (process, target, line) -> target instanceof String text ? text.length() : nothing("Length", line));
        core(bindings, "List", "Count", (process, target, line) ->
                target instanceof Values.ListValue list ? list.size() : nothing("Count", line));
        core(bindings, "Map", "Count", (process, target, line) ->
                target instanceof Values.MapValue map ? map.entries().size() : nothing("Count", line));
        // What a window and its widgets hold, which is read for nothing and written at the price of a draw.
        for (final TypeSpec type : SystemApi.types()) {
            if (UI.equals(type.namespace())) {
                widgetValues(bindings, type);
            }
        }
        return Map.copyOf(bindings);
    }

    /**
     * Binds what a window or a widget of that type holds, its events included. Reading hands out what it holds, a text
     * as a copy that stays the program's; writing costs a draw and goes through the windows' one door, which keeps what
     * a widget holds within its bounds and refuses the fields the runtime keeps for itself.
     */
    private static void widgetValues(final Map<MemberId, Binding> bindings, final TypeSpec type) {
        for (final IMemberSpec member : type.members()) {
            if (member instanceof PropertySpec || member instanceof EventSpec) {
                final String name = member.id().name();
                final IProcessValue read = (process, target, line) -> readWidget(process, target, name, line);
                final IProcessWrite write =
                        (process, target, value, line) -> writeWidget(process, target, name, value, line);
                add(bindings, new Binding(member.id(), read, write, false));
            }
        }
    }

    private static Object readWidget(final Process process, final Object target, final String name, final int line) {
        if (!(target instanceof Values.Obj widget)) {
            return nothing(name, line);
        }
        final Object held = widget.get(name);
        return held instanceof String said ? process.heap().text(said, line) : held;
    }

    private static void writeWidget(final Process process, final Object target, final String name,
                                    final Object value, final int line) {
        if (!(target instanceof Values.Obj widget)) {
            throw new Halt(Halt.Reason.NO_OBJECT, line, "there is no object to write " + name + " on");
        }
        process.charge(SigmaCosts.DRAW);
        process.windows0().mutator().write(widget, name, value, line);
    }

    /** Binds a value the system declares as the process's, read from the type. */
    private static void onType(final Map<MemberId, Binding> bindings, final String owner, final String name,
                               final IProcessValue read) {
        boolean declared = false;
        for (final IMemberSpec member : SystemApi.members(owner, name)) {
            declared |= member instanceof PropertySpec && member.kind() == MemberKind.PROCESS && member.isStatic();
        }
        if (!declared) {
            throw new IllegalStateException(
                    owner + "." + name + " is not a value the system declares as the process's");
        }
        add(bindings, new Binding(new MemberId(owner, name, List.of()), read, null, true));
    }

    /** Binds one of the values the language's core keeps on its own objects. */
    private static void core(final Map<MemberId, Binding> bindings, final String owner, final String name,
                             final IProcessValue read) {
        if (!ProgramImage.CORE_VALUES.contains(owner + "." + name)) {
            throw new IllegalStateException(owner + "." + name + " is not a value of the language's core");
        }
        add(bindings, new Binding(new MemberId(owner, name, List.of()), read, null, false));
    }

    private static void add(final Map<MemberId, Binding> bindings, final Binding binding) {
        if (bindings.putIfAbsent(binding.id(), binding) != null) {
            throw new IllegalStateException(binding.id().describe() + " is bound twice");
        }
    }

    private static Object nothing(final String name, final int line) {
        throw new Halt(Halt.Reason.NO_SUCH_MEMBER, line, "there is no " + name + " to read here");
    }
}
