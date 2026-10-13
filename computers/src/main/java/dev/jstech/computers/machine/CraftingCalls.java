/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.program.IqlEngine;
import dev.jstech.computers.program.iql.IqlBusStatement;
import dev.jstech.computers.program.iql.IqlCraftingStatement;
import dev.jstech.computers.vm.program.Halt;
import dev.jstech.computers.vm.program.IWorldCall;
import dev.jstech.computers.vm.program.Values;
import dev.jstech.computers.vm.system.MemberId;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * The calls a program makes on the parts of its machine's crafting network, answered by its {@link CraftingService}:
 * a Crafting Interface or a Crafting Input Router found by its name, and each of its settings, set as the IQL sets it
 * and marked on the part with the program's name. A part that is gone, a pattern it does not hold or a setting the
 * router's era cannot take stops the program, saying why, as a statement is refused.
 */
final class CraftingCalls {

    private static final String STRING = "string";
    private static final String INT = "int";
    private static final String BOOL = "bool";
    private static final String INTERFACE = "CraftInterface";
    private static final String ROUTER = "CraftRouter";
    /** What a setting is marked with when the program has no name to give. */
    private static final String UNNAMED = "Sigma";

    private CraftingCalls() {
    }

    static void bind(final Map<MemberId, MachineCalls.Binding<?>> bindings) {
        MachineCalls.bind(bindings, MachineServices::crafting, INTERFACE, "Named",
                (crafting, call, target, arguments, line) -> crafting.interfaceExists(text(arguments, 0))
                        ? named(INTERFACE, text(arguments, 0)) : null, STRING);
        MachineCalls.bind(bindings, MachineServices::crafting, ROUTER, "Named",
                (crafting, call, target, arguments, line) -> crafting.routerExists(text(arguments, 0))
                        ? named(ROUTER, text(arguments, 0)) : null, STRING);
        onInterface(bindings, "Exclusive", arguments -> new IqlCraftingStatement.Exclusive(flag(arguments, 0)), BOOL);
        onInterface(bindings, "MaxJobs", arguments -> new IqlCraftingStatement.MaxJobs(number(arguments, 0)), INT);
        onInterface(bindings, "Pause", arguments -> new IqlCraftingStatement.Paused(true));
        onInterface(bindings, "Resume", arguments -> new IqlCraftingStatement.Paused(false));
        onInterface(bindings, "Route", arguments -> new IqlCraftingStatement.Route(text(arguments, 0),
                text(arguments, 1), arguments.length > 2 && arguments[2] != null ? nameOf(arguments[2]) : null),
                STRING, STRING, ROUTER);
        onRouter(bindings, "Only", arguments -> new IqlBusStatement.Filter(false, list(text(arguments, 0))),
                STRING);
        onRouter(bindings, "AllBut", arguments -> new IqlBusStatement.Filter(true, list(text(arguments, 0))),
                STRING);
        onRouter(bindings, "Any", arguments -> new IqlBusStatement.Filter(false, List.of()));
        onRouter(bindings, "Tag", arguments -> new IqlBusStatement.Tags(list(text(arguments, 0))), STRING);
        onRouter(bindings, "Fuzzy", arguments -> new IqlBusStatement.Match(flag(arguments, 0)), BOOL);
    }

    /* A setting of the interface the call is made on, made from what the call is handed. */
    private static void onInterface(final Map<MemberId, MachineCalls.Binding<?>> bindings, final String name,
                                    final Function<Object[], IqlCraftingStatement.Change> change,
                                    final String... parameters) {
        MachineCalls.bind(bindings, MachineServices::crafting, INTERFACE, name,
                (crafting, call, target, arguments, line) -> applied(crafting.set(IqlCraftingStatement.Part.INTERFACE,
                        nameOf(target), change.apply(arguments), by(call)), target, line), parameters);
    }

    /* A setting of the router the call is made on: one of a bus's, as SET ROUTER sets it. */
    private static void onRouter(final Map<MemberId, MachineCalls.Binding<?>> bindings, final String name,
                                 final Function<Object[], IqlBusStatement.Change> setting,
                                 final String... parameters) {
        MachineCalls.bind(bindings, MachineServices::crafting, ROUTER, name,
                (crafting, call, target, arguments, line) -> applied(crafting.set(IqlCraftingStatement.Part.ROUTER,
                        nameOf(target), new IqlCraftingStatement.RouterSetting(setting.apply(arguments)), by(call)),
                        target, line), parameters);
    }

    /* The part handed back for the next setting, or the program stopped with why it was refused. */
    private static Object applied(final IqlEngine.Outcome outcome, final Object target, final int line) {
        if (!outcome.ok()) {
            throw new Halt(Halt.Reason.REFUSED, line, outcome.said());
        }
        return target;
    }

    private static Values.Obj named(final String type, final String name) {
        final Values.Obj made = new Values.Obj(type);
        made.set("Name", name);
        return made;
    }

    /* What a setting is marked with: the class the program was started from. */
    private static String by(final IWorldCall call) {
        final String caller = call.caller();
        return caller == null || caller.isEmpty() ? UNNAMED : caller;
    }

    private static String nameOf(final Object held) {
        return held instanceof Values.Obj obj && obj.get("Name") instanceof String value ? value : "";
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

    /* A bool argument: the assembly writes true and false as a one and a zero, so a number stands for one. */
    private static boolean flag(final Object[] arguments, final int at) {
        if (arguments.length <= at) {
            return false;
        }
        return arguments[at] instanceof Number value ? value.longValue() != 0L : Boolean.TRUE.equals(arguments[at]);
    }
}
