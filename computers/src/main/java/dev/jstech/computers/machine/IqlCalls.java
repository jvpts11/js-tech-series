/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.program.IqlEngine;
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.computers.vm.program.Halt;
import dev.jstech.computers.vm.program.IWorldCall;
import dev.jstech.computers.vm.program.Values;
import dev.jstech.computers.vm.system.MemberId;
import java.util.Map;

/**
 * The network's own language, from a program, through its computer's {@link IqlService}.
 *
 * <p>Every call needs a network with a Mainframe, and stops the program on a machine without one. What a statement
 * answers comes back as rows a program can walk, and every row it brings back adds to what the statement costs,
 * whether the rows are the answer or are held in the record that is.
 */
final class IqlCalls {

    private static final String STRING = "string";

    /** The Java that answers a call with the engine on the network's Mainframe in hand. */
    @FunctionalInterface
    private interface IEngineFunction {
        Object call(IqlService iql, IqlEngine engine, IWorldCall call, Object[] arguments, int line);
    }

    private IqlCalls() {
    }

    static void bind(final Map<MemberId, MachineCalls.Binding<?>> bindings) {
        iql(bindings, "Run", (iql, engine, call, arguments, line) -> result(call, engine.run(text(arguments, 0))),
                STRING);
        iql(bindings, "Query", (iql, engine, call, arguments, line) -> {
            final IqlEngine.Outcome outcome = engine.run(text(arguments, 0));
            if (!outcome.ok()) {
                throw new Halt(Halt.Reason.REFUSED, line, outcome.message());
            }
            return rows(outcome);
        }, STRING);
        final IEngineFunction exec = (iql, engine, call, arguments, line) -> {
            final StringBuilder statement = new StringBuilder("EXEC ").append(text(arguments, 0));
            if (arguments.length > 1 && arguments[1] instanceof Values.ListValue given) {
                for (final Object each : given.items()) {
                    statement.append(' ').append(each);
                }
            }
            return result(call, engine.run(statement.toString()));
        };
        iql(bindings, "Exec", exec, STRING);
        iql(bindings, "Exec", exec, STRING, "List<string>");
        iql(bindings, "RunFile", (iql, engine, call, arguments, line) -> {
            final ICliComputer.FsResult read = iql.read(text(arguments, 0));
            if (!read.ok()) {
                throw new Halt(Halt.Reason.NO_OBJECT, line, read.message().english());
            }
            return result(call, IqlService.runEach(engine, read.message().english()));
        }, STRING);
    }

    /** Binds a call that needs the engine on the network's Mainframe, and stops the program when there is none. */
    private static void iql(final Map<MemberId, MachineCalls.Binding<?>> bindings, final String name,
                            final IEngineFunction function, final String... parameters) {
        MachineCalls.bind(bindings, MachineServices::iql, "Iql", name, (iql, call, target, arguments, line) -> {
            final IqlEngine engine = iql.engine();
            if (engine == null) {
                throw new Halt(Halt.Reason.NO_NETWORK, line, "this computer is not on a network with a Mainframe");
            }
            return function.call(iql, engine, call, arguments, line);
        }, parameters);
    }

    private static String text(final Object[] arguments, final int index) {
        return arguments.length > index && arguments[index] != null ? String.valueOf(arguments[index]) : "";
    }

    /** What a statement answered, its rows held in the record and counted towards what it costs. */
    private static Values.Obj result(final IWorldCall call, final IqlEngine.Outcome outcome) {
        call.rows(outcome.rows().size());
        final Values.Obj made = new Values.Obj("IqlResult");
        made.set("Ok", outcome.ok());
        made.set("Message", outcome.message());
        made.set("Rows", rows(outcome));
        return made;
    }

    /** The rows a read brought back, each a map keyed by its columns. */
    private static Values.ListValue rows(final IqlEngine.Outcome outcome) {
        final Values.ListValue all = new Values.ListValue();
        for (final ICliComputer.StoredItem item : outcome.rows()) {
            final Values.MapValue row = new Values.MapValue();
            row.entries().put("name", item.name());
            row.entries().put("quantity", item.quantity());
            row.entries().put("detail", item.detail() == null ? "" : item.detail());
            all.items().add(row);
        }
        return all;
    }
}
