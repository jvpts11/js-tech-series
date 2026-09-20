/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import java.util.List;
import java.util.Map;

/**
 * Giving a name a value at the prompt, and reading back the names there are.
 *
 * <p>A name set here belongs to the machine rather than to the window it was typed in, so it is still there
 * at the monitor, in a session opened from another machine, and after a restart. That is the only way a name
 * is worth setting: the whole point of writing {@code set BASE=C:\work} is not writing it again.
 *
 * <p>Each family says it its own way. DOS writes {@code SET NAME=value} and reads back {@code %NAME%}; the
 * POSIX shells write {@code export NAME=value}, forget one with {@code unset}, and read back {@code $NAME}.
 */
final class VariableCommands {

    /** Setting a name is about the machine, so it is wherever a machine is. */
    private static final CommandScope ANY_MACHINE = CommandScope.everywhere();

    private VariableCommands() {
    }

    /** The one both families have: with nothing after it, the names there are. */
    static List<ICliCommand> shared() {
        return List.of(new Set());
    }

    /**
     * Whether a whole line is nothing but a name being given a value, which is how the POSIX shells have
     * always set one: no verb at all.
     */
    static boolean isAssignment(final String word) {
        final int equals = word.indexOf('=');
        return equals > 0 && named(word.substring(0, equals));
    }

    /** Sets what such a line says, and gives back what the machine made of it. */
    static ICliComputer.OpResult assignmentOf(final String word, final ICliComputer computer) {
        final int equals = word.indexOf('=');
        return computer.setShellVariable(word.substring(0, equals), word.substring(equals + 1));
    }

    /** The two the POSIX shells add: one that gives a name a value, and one that takes it away. */
    static List<ICliCommand> posix() {
        return List.of(new Export(), new Unset());
    }

    /** Prints every name there is, one to a line, the way both families print them. */
    private static void printAll(final CliContext ctx) {
        final Map<String, String> named = ctx.computer().shellVariables();
        if (named.isEmpty()) {
            ctx.out().dim("no names are set on this machine");
            return;
        }
        named.forEach((name, value) -> ctx.out().line(name + "=" + value));
    }

    /**
     * Takes {@code NAME=value} apart and sets it, and says what came of it.
     *
     * <p>The first equals sign is the one that counts, so a value with one in it keeps it: a path, a pattern
     * and a line of text are all things a person sets a name to.
     */
    private static void assign(final CliContext ctx, final String said, final String usage) {
        final int equals = said.indexOf('=');
        if (equals <= 0) {
            ctx.out().error(usage);
            return;
        }
        final String name = said.substring(0, equals);
        if (!named(name)) {
            ctx.out().error("a name is letters, digits and underscores, and does not start with a digit");
            return;
        }
        final ICliComputer.OpResult result = ctx.computer().setShellVariable(name, said.substring(equals + 1));
        if (!result.ok()) {
            ctx.out().error(result.message());
        }
    }

    /** Whether that is a name a shell would take, which is the same set of letters in every one of them. */
    static boolean named(final String name) {
        if (name.isEmpty() || Character.isDigit(name.charAt(0))) {
            return false;
        }
        for (int i = 0; i < name.length(); i++) {
            final char c = name.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '_') {
                return false;
            }
        }
        return true;
    }

    /** {@code SET}: the names there are, or one of them given a value. */
    static final class Set implements ICliCommand {
        @Override public CommandScope scope() {
            return ANY_MACHINE;
        }

        @Override public String name() {
            return "set";
        }

        @Override public String summary() {
            return "give a name a value, or list the names there are";
        }

        @Override public String usage() {
            return "[NAME=value]";
        }

        @Override public List<String> description() {
            return List.of("With nothing after it, lists every name this machine knows and what it stands",
                    "for. With NAME=value, gives that name a value; with NAME= and nothing after the equals",
                    "sign, forgets it.",
                    "",
                    "The names belong to the machine, not to this window: one set here is still set at the",
                    "monitor, in a session opened from another machine, and after a restart.");
        }

        @Override public List<Example> examples() {
            return List.of(
                    new Example("set", "every name this machine knows"),
                    new Example("set BASE=C:\\work", "give BASE a value"),
                    new Example("set BASE=", "and forget it again"));
        }

        @Override public List<String> seeAlso() {
            return List.of("echo", "export", "unset");
        }

        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs()) {
                printAll(ctx);
                return;
            }
            assign(ctx, String.join(" ", ctx.args()), "usage: set NAME=value");
        }
    }

    /** {@code export}: the POSIX way of writing the same thing. */
    static final class Export implements ICliCommand {
        @Override public CommandScope scope() {
            return CommandScope.on(CommandScope.UNIX_SYSTEMS);
        }

        @Override public String name() {
            return "export";
        }

        @Override public String summary() {
            return "give a name a value the shell and what it runs can see";
        }

        @Override public String usage() {
            return "[NAME=value]";
        }

        @Override public List<String> description() {
            return List.of("With NAME=value, gives that name a value on this machine. With nothing after it,",
                    "lists the names there are.",
                    "",
                    "The names belong to the machine, not to this window: one set here is still set at the",
                    "monitor, in a session opened from another machine, and after a restart.");
        }

        @Override public List<Example> examples() {
            return List.of(
                    new Example("export BASE=/home/player/work", "give BASE a value"),
                    new Example("echo $BASE", "and read it back"));
        }

        @Override public List<String> seeAlso() {
            return List.of("unset", "set", "echo");
        }

        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs()) {
                printAll(ctx);
                return;
            }
            assign(ctx, String.join(" ", ctx.args()), "usage: export NAME=value");
        }
    }

    /** {@code unset}: the POSIX way of taking a name away. */
    static final class Unset implements ICliCommand {
        @Override public CommandScope scope() {
            return CommandScope.on(CommandScope.UNIX_SYSTEMS);
        }

        @Override public String name() {
            return "unset";
        }

        @Override public String summary() {
            return "forget a name";
        }

        @Override public String usage() {
            return "<NAME>";
        }

        @Override public List<String> seeAlso() {
            return List.of("export", "set");
        }

        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs()) {
                ctx.out().error("usage: unset NAME");
                return;
            }
            for (final String name : ctx.args()) {
                final ICliComputer.OpResult result = ctx.computer().setShellVariable(name, "");
                if (!result.ok()) {
                    ctx.out().error(result.message());
                    return;
                }
            }
        }
    }
}
