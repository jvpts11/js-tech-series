/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
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
@TextHolder
final class VariableCommands {

    /** Setting a name is about the machine, so it is wherever a machine is. */
    private static final CommandScope ANY_MACHINE = CommandScope.everywhere();

    private static final TextKey NONE_SET = TextKey.of("jsc.cli.variable.none_set", "no names are set on this machine");
    private static final TextKey BAD_NAME = TextKey.of("jsc.cli.variable.bad_name",
            "a name is letters, digits and underscores, and does not start with a digit");

    /** What both the DOS and the POSIX way of setting a name say about where the name lives. */
    private static final TextKey BELONGS = TextKey.of("jsc.cli.variable.belongs", "The names belong to the machine,"
            + " not to this window: one set here is still set at the monitor, in a session opened from another"
            + " machine, and after a restart.");

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

    /** Prints every name there is, one to a line, the way both families print them. */
    private static void printAll(final CliContext ctx) {
        final Map<String, String> named = ctx.computer().shellVariables();
        if (named.isEmpty()) {
            ctx.out().dim(NONE_SET);
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
    private static void assign(final CliContext ctx, final String said, final TextKey usage) {
        final int equals = said.indexOf('=');
        if (equals <= 0) {
            ctx.out().error(usage);
            return;
        }
        final String name = said.substring(0, equals);
        if (!named(name)) {
            ctx.out().error(BAD_NAME);
            return;
        }
        final ICliComputer.OpResult result = ctx.computer().setShellVariable(name, said.substring(equals + 1));
        if (!result.ok()) {
            ctx.out().error(result.message());
        }
    }

    /** {@code SET}: the names there are, or one of them given a value. */
    @TextHolder
    static final class Set implements ICliCommand {

        private static final TextKey SUMMARY =
                TextKey.of("jsc.cli.variable.set.summary", "give a name a value, or list the names there are");
        private static final TextKey USAGE = TextKey.of("jsc.cli.variable.set.usage", "[NAME=value]");
        private static final TextKey ABOUT = TextKey.of("jsc.cli.variable.set.about", "With nothing after it, lists"
                + " every name this machine knows and what it stands for. With NAME=value, gives that name a value;"
                + " with NAME= and nothing after the equals sign, forgets it.");
        private static final TextKey EVERY_NAME =
                TextKey.of("jsc.cli.variable.set.example.every_name", "every name this machine knows");
        private static final TextKey GIVE = TextKey.of("jsc.cli.variable.set.example.give", "give BASE a value");
        private static final TextKey FORGET =
                TextKey.of("jsc.cli.variable.set.example.forget", "and forget it again");
        private static final TextKey USAGE_ERROR =
                TextKey.of("jsc.cli.variable.set.usage_error", "usage: set NAME=value");

        @Override public CommandScope scope() {
            return ANY_MACHINE;
        }

        @Override public String name() {
            return "set";
        }

        @Override public CommandGroup group() {
            return CommandGroup.MACHINE;
        }

        @Override public Text summary() {
            return SUMMARY.text();
        }

        @Override public Text usage() {
            return USAGE.text();
        }

        @Override public List<Text> description() {
            return List.of(ABOUT.text(), BELONGS.text());
        }

        @Override public List<Example> examples() {
            return List.of(
                    new Example("set", EVERY_NAME),
                    new Example("set BASE=C:\\work", GIVE),
                    new Example("set BASE=", FORGET));
        }

        @Override public List<String> seeAlso() {
            return List.of("echo", "export", "unset");
        }

        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs()) {
                printAll(ctx);
                return;
            }
            assign(ctx, String.join(" ", ctx.args()), USAGE_ERROR);
        }
    }

    /** {@code export}: the POSIX way of writing the same thing. */
    @TextHolder
    static final class Export implements ICliCommand {

        private static final TextKey SUMMARY = TextKey.of("jsc.cli.variable.export.summary",
                "give a name a value the shell and what it runs can see");
        private static final TextKey USAGE = TextKey.of("jsc.cli.variable.export.usage", "[NAME=value]");
        private static final TextKey ABOUT = TextKey.of("jsc.cli.variable.export.about", "With NAME=value, gives"
                + " that name a value on this machine. With nothing after it, lists the names there are.");
        private static final TextKey GIVE = TextKey.of("jsc.cli.variable.export.example.give", "give BASE a value");
        private static final TextKey READ_BACK =
                TextKey.of("jsc.cli.variable.export.example.read_back", "and read it back");
        private static final TextKey USAGE_ERROR =
                TextKey.of("jsc.cli.variable.export.usage_error", "usage: export NAME=value");

        @Override public CommandScope scope() {
            return CommandScope.on(CommandScope.UNIX_SYSTEMS);
        }

        @Override public String name() {
            return "export";
        }

        @Override public CommandGroup group() {
            return CommandGroup.MACHINE;
        }

        @Override public Text summary() {
            return SUMMARY.text();
        }

        @Override public Text usage() {
            return USAGE.text();
        }

        @Override public List<Text> description() {
            return List.of(ABOUT.text(), BELONGS.text());
        }

        @Override public List<Example> examples() {
            return List.of(
                    new Example("export BASE=/home/player/work", GIVE),
                    new Example("echo $BASE", READ_BACK));
        }

        @Override public List<String> seeAlso() {
            return List.of("unset", "set", "echo");
        }

        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs()) {
                printAll(ctx);
                return;
            }
            assign(ctx, String.join(" ", ctx.args()), USAGE_ERROR);
        }
    }

    /** {@code unset}: the POSIX way of taking a name away. */
    @TextHolder
    static final class Unset implements ICliCommand {

        private static final TextKey SUMMARY = TextKey.of("jsc.cli.variable.unset.summary", "forget a name");
        private static final TextKey USAGE = TextKey.of("jsc.cli.variable.unset.usage", "<NAME>");
        private static final TextKey USAGE_ERROR =
                TextKey.of("jsc.cli.variable.unset.usage_error", "usage: unset NAME");

        @Override public CommandScope scope() {
            return CommandScope.on(CommandScope.UNIX_SYSTEMS);
        }

        @Override public String name() {
            return "unset";
        }

        @Override public CommandGroup group() {
            return CommandGroup.MACHINE;
        }

        @Override public Text summary() {
            return SUMMARY.text();
        }

        @Override public Text usage() {
            return USAGE.text();
        }

        @Override public List<String> seeAlso() {
            return List.of("export", "set");
        }

        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs()) {
                ctx.out().error(USAGE_ERROR);
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
