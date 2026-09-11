/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.cannon.machine;

import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.cannon.CannonCosts;
import dev.jstech.computers.cannon.run.Halt;
import dev.jstech.computers.cannon.run.IHost;
import dev.jstech.computers.cannon.run.Values;
import dev.jstech.computers.program.IqlEngine;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.ICliComputer;
import java.util.List;

/**
 * The network's own language, from a program.
 *
 * <p>A statement goes to the Mainframe's engine exactly as it would from the prompt or the management
 * studio, under the program's name, and comes back as rows a program can walk. A statement of the
 * network's second layer (a view, a procedure, a job) needs the engine installed there; a plain one
 * needs only a Mainframe.
 */
public final class HostIql {

    private static final int STATEMENT = CannonCosts.WRITE;

    /** How many rows one query may bring back, the same as the studio's default page. */
    private static final int ROW_LIMIT = 4096;

    private HostIql() {
    }

    /** Whether this is one of the calls handled here. */
    public static boolean handles(final String owner) {
        return "Iql".equals(owner);
    }

    /** Answers one of them. */
    public static IHost.Reply call(final ServerCliComputer shell, final String member,
                                  final List<Object> arguments, final int line) {
        final MainframeBlockEntity mainframe = shell.mainframe();
        if (mainframe == null) {
            throw new Halt(Halt.Reason.NO_NETWORK, line, "this computer is not on a network with a Mainframe");
        }
        final IqlEngine engine = new IqlEngine(mainframe, shell, ROW_LIMIT);
        return switch (member) {
            case "Run" -> result(engine.run(text(arguments, 0)));
            case "Query" -> {
                final IqlEngine.Outcome outcome = engine.run(text(arguments, 0));
                if (!outcome.ok()) {
                    throw new Halt(Halt.Reason.REFUSED, line, outcome.message());
                }
                yield IHost.Reply.of(rows(outcome), STATEMENT + outcome.rows().size());
            }
            case "Exec" -> {
                final StringBuilder statement = new StringBuilder("EXEC ").append(text(arguments, 0));
                if (arguments.size() > 1 && arguments.get(1) instanceof Values.ListValue given) {
                    for (final Object each : given.items()) {
                        statement.append(' ').append(each);
                    }
                }
                yield result(engine.run(statement.toString()));
            }
            case "RunFile" -> {
                final ICliComputer.FsResult read = shell.readFile(text(arguments, 0));
                if (!read.ok()) {
                    throw new Halt(Halt.Reason.NO_OBJECT, line, read.message());
                }
                // One statement a line, the way the studio saves them; the first refusal ends the run.
                IqlEngine.Outcome last = new IqlEngine.Outcome(true, "nothing to run", List.of());
                for (final String each : read.message().split("\\r?\\n")) {
                    if (each.isBlank() || each.strip().startsWith("--")) {
                        continue;
                    }
                    last = engine.run(each.strip());
                    if (!last.ok()) {
                        break;
                    }
                }
                yield result(last);
            }
            default -> throw new Halt(Halt.Reason.NO_SUCH_MEMBER, line, "Iql has no " + member);
        };
    }

    private static String text(final List<Object> arguments, final int index) {
        return arguments.size() > index && arguments.get(index) != null ? String.valueOf(arguments.get(index)) : "";
    }

    private static IHost.Reply result(final IqlEngine.Outcome outcome) {
        final Values.Obj made = new Values.Obj("IqlResult");
        made.set("Ok", outcome.ok());
        made.set("Message", outcome.message());
        made.set("Rows", rows(outcome));
        return IHost.Reply.of(made, STATEMENT + outcome.rows().size());
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
