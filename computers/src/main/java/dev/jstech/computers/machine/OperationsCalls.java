/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.operation.MoveLabels;
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.computers.vm.program.Halt;
import dev.jstech.computers.vm.program.Values;
import dev.jstech.computers.vm.system.MemberId;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.Locale;
import java.util.Map;

/**
 * A program asking the network to move and make things, through its computer's {@link OperationsService}.
 *
 * <p>Every ask is signed with the script that made it, so the row it leaves in the network's log says which of a
 * base's programs to go and look at. Asking is dear, and deliberately so: it is the network's time being spent. A
 * refusal is answered rather than thrown, so a script can carry on and try something else.
 */
@TextHolder
final class OperationsCalls {

    private static final String STRING = "string";
    private static final String LONG = "long";

    /** Why a program is stopped: it is said in English, the language a program's run keeps what stopped it in. */
    private static final TextKey NO_NETWORK =
            TextKey.of("jsc.service.operations.calls.no_network", "this computer is not on a network");

    private OperationsCalls() {
    }

    static void bind(final Map<MemberId, MachineCalls.Binding<?>> bindings) {
        operations(bindings, "Pull", (ops, call, target, arguments, line) -> started(ops,
                ops.pull(item(arguments), amount(arguments), MoveLabels.sigma(call.caller()))), STRING, LONG);
        operations(bindings, "Push", (ops, call, target, arguments, line) -> started(ops,
                ops.push(item(arguments), amount(arguments), MoveLabels.sigma(call.caller()))), STRING, LONG);
        operations(bindings, "Craft", (ops, call, target, arguments, line) -> started(ops,
                ops.craft(item(arguments), amount(arguments), MoveLabels.sigma(call.caller()))), STRING, LONG);
        operations(bindings, "Cancel",
                (ops, call, target, arguments, line) -> asked(ops.cancel(item(arguments))), STRING);
        operations(bindings, "Reprioritise", (ops, call, target, arguments, line) -> asked(ops.reprioritise(
                item(arguments), arguments.length < 2 ? "" : String.valueOf(arguments[1]))), STRING, STRING);
        operations(bindings, "Get", (ops, call, target, arguments, line) -> {
            // An Operation that has settled is no longer in flight, and saying so is the answer.
            final ICliComputer.ActiveOp op = ops.get(item(arguments));
            return op == null ? null : shot(op);
        }, STRING);
        operations(bindings, "List", (ops, call, target, arguments, line) -> {
            final Values.ListValue all = new Values.ListValue();
            for (final ICliComputer.ActiveOp op : ops.list()) {
                all.items().add(shot(op));
            }
            return all;
        });
    }

    /** Binds one of the calls, every one of which needs the machine to be on a network. */
    private static void operations(final Map<MemberId, MachineCalls.Binding<?>> bindings, final String name,
                                   final MachineCalls.IServiceFunction<OperationsService> function,
                                   final String... parameters) {
        MachineCalls.bind(bindings, MachineServices::operations, "Operations", name,
                (ops, call, target, arguments, line) -> {
                    if (!ops.onNetwork()) {
                        throw new Halt(Halt.Reason.NO_NETWORK, line, NO_NETWORK.text().english());
                    }
                    return function.call(ops, call, target, arguments, line);
                }, parameters);
    }

    /* The answer to a call that sets an Operation going, which is what a program running one has earned. */
    private static Values.Obj started(final OperationsService ops, final ICliComputer.OpResult result) {
        if (result.ok()) {
            ops.creditProgram();
        }
        return asked(result);
    }

    /** What came of asking: whether the network took it, and why not when it did not. */
    private static Values.Obj asked(final ICliComputer.OpResult result) {
        final Values.Obj made = new Values.Obj("AskResult");
        made.set("Ok", result.ok());
        // A program reads in the machine's language, and keeps only strings on its heap.
        made.set("Message", result.message().english());
        return made;
    }

    private static Values.Obj shot(final ICliComputer.ActiveOp op) {
        final Values.Obj made = new Values.Obj("OperationInfo");
        made.set("Id", op.id());
        made.set("Type", op.type().toLowerCase(Locale.ROOT));
        made.set("Item", op.item());
        made.set("Moved", op.progress());
        made.set("Requested", op.total());
        made.set("Status", op.status().toLowerCase(Locale.ROOT));
        made.set("Priority", op.priority().toLowerCase(Locale.ROOT));
        return made;
    }

    private static String item(final Object[] arguments) {
        return arguments.length == 0 ? "" : String.valueOf(arguments[0]);
    }

    private static long amount(final Object[] arguments) {
        return arguments.length > 1 && arguments[1] instanceof Number number ? number.longValue() : 0L;
    }
}
