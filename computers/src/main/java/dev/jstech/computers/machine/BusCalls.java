/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.bus.BusSettings;
import dev.jstech.computers.program.IqlEngine;
import dev.jstech.computers.program.iql.IqlBusStatement;
import dev.jstech.computers.vm.program.Halt;
import dev.jstech.computers.vm.program.IWorldCall;
import dev.jstech.computers.vm.program.Values;
import dev.jstech.computers.vm.system.MemberId;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * The calls a program makes on the buses of its machine's network, answered by its {@link BusService}: a bus found by
 * its name, and each of its settings, set as the IQL sets it and marked on the bus with the program's name. A setting
 * the bus's era cannot be set to, or a bus that is gone, stops the program, saying why, as a statement is refused.
 */
@TextHolder
final class BusCalls {

    private static final String STRING = "string";
    private static final String INT = "int";
    private static final String BOOL = "bool";
    /** What a setting is marked with when the program has no name to give. */
    private static final String UNNAMED = "Sigma";

    private static final TextKey NOT_AN_HOUR = TextKey.of("jsc.service.bus.not_an_hour",
            "%s is not an hour of the day");

    private BusCalls() {
    }

    static void bind(final Map<MemberId, MachineCalls.Binding<?>> bindings) {
        MachineCalls.bind(bindings, MachineServices::buses, "Bus", "Named",
                (buses, call, target, arguments, line) -> buses.exists(text(arguments, 0))
                        ? bus(text(arguments, 0)) : null, STRING);
        set(bindings, "On", arguments -> new IqlBusStatement.Power(true));
        set(bindings, "Off", arguments -> new IqlBusStatement.Power(false));
        set(bindings, "Continuous", arguments -> new IqlBusStatement.Mode(false));
        set(bindings, "OnDemand", arguments -> new IqlBusStatement.Mode(true));
        set(bindings, "ReadWrite", arguments -> new IqlBusStatement.Access(BusSettings.READ_WRITE));
        set(bindings, "ReadOnly", arguments -> new IqlBusStatement.Access(BusSettings.READ_ONLY));
        set(bindings, "WriteOnly", arguments -> new IqlBusStatement.Access(BusSettings.WRITE_ONLY));
        set(bindings, "Only", arguments -> new IqlBusStatement.Filter(false, list(text(arguments, 0))), STRING);
        set(bindings, "AllBut", arguments -> new IqlBusStatement.Filter(true, list(text(arguments, 0))), STRING);
        set(bindings, "Keep", arguments -> new IqlBusStatement.Quantities(number(arguments, 0),
                IqlBusStatement.UNCHANGED), INT);
        set(bindings, "Max", arguments -> new IqlBusStatement.Quantities(IqlBusStatement.UNCHANGED,
                number(arguments, 0)), INT);
        set(bindings, "Priority", arguments -> new IqlBusStatement.Priority(number(arguments, 0)), INT);
        set(bindings, "Fuzzy", arguments -> new IqlBusStatement.Match(arguments.length > 0
                && Boolean.TRUE.equals(arguments[0])), BOOL);
        set(bindings, "WhenStock", arguments -> new IqlBusStatement.Stock(text(arguments, 0), number(arguments, 1)),
                STRING, INT);
        set(bindings, "WhenStockTag", arguments -> new IqlBusStatement.Stock("#" + text(arguments, 0),
                number(arguments, 1)), STRING, INT);
        set(bindings, "After", arguments -> new IqlBusStatement.After(nameOf(arguments.length > 0 ? arguments[0]
                : null, "Name")), "Bus");
        MachineCalls.bind(bindings, MachineServices::buses, "Bus", "Between",
                (buses, call, target, arguments, line) -> {
                    final int from = hour(number(arguments, 0), line);
                    final int to = hour(number(arguments, 1), line);
                    return applied(buses.set(nameOf(target, "Name"), new IqlBusStatement.Hours(from, to),
                            by(call)), target, line);
                }, INT, INT);
        MachineCalls.bind(bindings, MachineServices::buses, "Bus", "Tag",
                (buses, call, target, arguments, line) -> {
                    final String name = nameOf(target, "Name");
                    final List<String> tags = new ArrayList<>(buses.tags(name));
                    tags.add(text(arguments, 0));
                    return applied(buses.set(name, new IqlBusStatement.Tags(tags), by(call)), target, line);
                }, STRING);
        MachineCalls.bind(bindings, MachineServices::buses, "Bus", "Item",
                (buses, call, target, arguments, line) -> {
                    final Values.Obj item = new Values.Obj("BusItem");
                    item.set("Bus", nameOf(target, "Name"));
                    item.set("Item", text(arguments, 0));
                    return item;
                }, STRING);
        MachineCalls.bind(bindings, MachineServices::buses, "BusItem", "Keep",
                (buses, call, target, arguments, line) -> applied(buses.set(nameOf(target, "Bus"),
                        new IqlBusStatement.ItemQuantities(nameOf(target, "Item"), number(arguments, 0),
                                IqlBusStatement.UNCHANGED), by(call)), target, line), INT);
        MachineCalls.bind(bindings, MachineServices::buses, "BusItem", "Max",
                (buses, call, target, arguments, line) -> applied(buses.set(nameOf(target, "Bus"),
                        new IqlBusStatement.ItemQuantities(nameOf(target, "Item"), IqlBusStatement.UNCHANGED,
                                number(arguments, 0)), by(call)), target, line), INT);
    }

    /* A setting of the bus the call is made on, made from what the call is handed. */
    private static void set(final Map<MemberId, MachineCalls.Binding<?>> bindings, final String name,
                            final Function<Object[], IqlBusStatement.Change> change, final String... parameters) {
        MachineCalls.bind(bindings, MachineServices::buses, "Bus", name,
                (buses, call, target, arguments, line) -> applied(buses.set(nameOf(target, "Name"),
                        change.apply(arguments), by(call)), target, line), parameters);
    }

    /* The bus handed back for the next setting, or the program stopped with why it was refused. */
    private static Object applied(final IqlEngine.Outcome outcome, final Object target, final int line) {
        if (!outcome.ok()) {
            throw new Halt(Halt.Reason.REFUSED, line, outcome.said());
        }
        return target;
    }

    private static Values.Obj bus(final String name) {
        final Values.Obj made = new Values.Obj("Bus");
        made.set("Name", name);
        return made;
    }

    /* What a setting is marked with: the class the program was started from. */
    private static String by(final IWorldCall call) {
        final String caller = call.caller();
        return caller == null || caller.isEmpty() ? UNNAMED : caller;
    }

    private static int hour(final int hour, final int line) {
        if (hour < 0 || hour > 23) {
            throw new Halt(Halt.Reason.OUT_OF_RANGE, line, NOT_AN_HOUR.with(hour));
        }
        return hour;
    }

    private static String nameOf(final Object held, final String field) {
        return held instanceof Values.Obj obj && obj.get(field) instanceof String value ? value : "";
    }

    private static List<String> list(final String items) {
        return Arrays.stream(items.split(",")).map(String::strip).filter(item -> !item.isEmpty()).toList();
    }

    private static String text(final Object[] arguments, final int at) {
        return arguments.length <= at ? "" : String.valueOf(arguments[at]);
    }

    private static int number(final Object[] arguments, final int at) {
        return arguments.length > at && arguments[at] instanceof Number value ? value.intValue() : 0;
    }
}
