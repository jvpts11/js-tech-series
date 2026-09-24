/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import dev.jstech.computers.program.cli.man.ManPage;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;

/**
 * The commands about the shell itself: what it knows, what it says and how it is left.
 *
 * <p>Moved here from BuiltinCommands, which keeps the lists that say which system gets which command.
 */
final class ShellCommands {

    private ShellCommands() {
    }

    @TextHolder
    static final class Help implements ICliCommand {

        private static final TextKey SUMMARY =
                TextKey.of("jsc.cli.shell.help.summary", "list commands, or show how one is used");
        private static final TextKey USAGE = TextKey.of("jsc.cli.shell.help.usage", "[command]");
        private static final TextKey NO_SUCH = TextKey.of("jsc.cli.shell.help.no_such", "no such command: %s");
        private static final TextKey ALSO = TextKey.of("jsc.cli.shell.help.also", "  also: %s");
        private static final TextKey COMMANDS = TextKey.of("jsc.cli.shell.help.commands", "commands");
        private static final TextKey DETAILS =
                TextKey.of("jsc.cli.shell.help.details", "'help <command>' for details");

        @Override public CommandScope scope() {
            return CommandScope.everywhere();
        }

        @Override public String name() {
            return "help";
        }

        @Override public CommandGroup group() {
            return CommandGroup.HELP;
        }

        @Override public List<String> aliases() {
            return List.of("?", "commands");
        }

        @Override public Text summary() {
            return SUMMARY.text();
        }

        @Override public Text usage() {
            return USAGE.text();
        }

        @Override public void run(final CliContext ctx) {
            if (ctx.hasArgs()) {
                final ICliCommand command = ctx.shell().find(ctx.arg(0));
                if (command == null || !command.available(ctx.computer())) {
                    ctx.out().error(NO_SUCH.with(ctx.arg(0)));
                    return;
                }
                /*
                 * The command's own page, in this family's voice: the same words the manual has on a Unix
                 * system, which is the whole point of there being one body of text about a command.
                 */
                ctx.out().line(ManPage.synopsis(command, CliStyle.ACCENT));
                for (final CliLine line : ManPage.lines(command, false)) {
                    ctx.out().line(line);
                }
                if (!command.aliases().isEmpty()) {
                    ctx.out().dim(ALSO.with(String.join(", ", command.aliases())));
                }
                return;
            }
            ctx.out().header(COMMANDS);
            /*
             * The dots stop at one column for the whole list, worked out from the longest name there is,
             * so every summary starts in the same place however long the names happen to be.
             */
            int column = 0;
            for (final ICliCommand command : ctx.shell().commands()) {
                if (command.available(ctx.computer())) {
                    column = Math.max(column, command.name().length());
                }
            }
            column += 6;
            for (final ICliCommand command : ctx.shell().commands()) {
                if (command.available(ctx.computer())) {
                    ctx.out().entry("  " + command.name(), command.summary(), column);
                }
            }
            ctx.out().blank();
            ctx.out().dim(DETAILS);
        }
    }

    @TextHolder
    static final class Clear implements ICliCommand, CliShell.IClearMarker {

        private static final TextKey SUMMARY = TextKey.of("jsc.cli.shell.cls.summary", "clear the console");

        /** The DOS family's own word for it; the Unix systems clear with {@code clear}. */
        @Override public CommandScope scope() {
            return CommandScope.on(CommandScope.DOS_SYSTEMS);
        }

        @Override public String name() {
            return "cls";
        }

        @Override public CommandGroup group() {
            return CommandGroup.MACHINE;
        }

        @Override public Text summary() {
            return SUMMARY.text();
        }

        @Override public void run(final CliContext ctx) {
            // The shell clears the scrollback because this command is a ClearMarker; nothing to print.
        }
    }

    @TextHolder
    static final class Echo implements ICliCommand {

        private static final TextKey SUMMARY = TextKey.of("jsc.cli.shell.echo.summary", "print the given text");
        private static final TextKey USAGE = TextKey.of("jsc.cli.shell.echo.usage", "<text>");

        @Override public CommandScope scope() {
            return CommandScope.everywhere();
        }

        @Override public String name() {
            return "echo";
        }

        @Override public CommandGroup group() {
            return CommandGroup.TEXT;
        }

        @Override public Text summary() {
            return SUMMARY.text();
        }

        @Override public Text usage() {
            return USAGE.text();
        }

        @Override public void run(final CliContext ctx) {
            ctx.out().line(ctx.rest(0));
        }
    }

    @TextHolder
    static final class Version implements ICliCommand {

        private static final TextKey SUMMARY = TextKey.of("jsc.cli.shell.version.summary", "show the shell version");

        /** {@code VER} is the DOS family's; a Unix system says what it is with {@code uname}. */
        @Override public CommandScope scope() {
            return CommandScope.on(CommandScope.DOS_SYSTEMS);
        }

        @Override public String name() {
            return "version";
        }

        @Override public CommandGroup group() {
            return CommandGroup.MACHINE;
        }

        @Override public List<String> aliases() {
            return List.of("ver");
        }

        @Override public Text summary() {
            return SUMMARY.text();
        }

        @Override public void run(final CliContext ctx) {
            // A product's name and number, which read the same in every language.
            ctx.out().accent(Text.literal("J's Computers Shell v1.0"));
        }
    }

    @TextHolder
    static final class Whoami implements ICliCommand {

        private static final TextKey SUMMARY =
                TextKey.of("jsc.cli.shell.whoami.summary", "show this computer's name and id");
        private static final TextKey NAME = TextKey.of("jsc.cli.shell.whoami.name", "name");
        private static final TextKey TYPE = TextKey.of("jsc.cli.shell.whoami.type", "type");
        private static final TextKey NODE = TextKey.of("jsc.cli.shell.whoami.node", "node");
        private static final TextKey UNNAMED = TextKey.of("jsc.cli.shell.whoami.unnamed", "(unnamed)");

        @Override public CommandScope scope() {
            return CommandScope.everywhere();
        }

        @Override public String name() {
            return "whoami";
        }

        @Override public CommandGroup group() {
            return CommandGroup.MACHINE;
        }

        @Override public Text summary() {
            return SUMMARY.text();
        }

        @Override public void run(final CliContext ctx) {
            final ICliComputer c = ctx.computer();
            ctx.out().row(NAME.text(), c.name().isEmpty() ? UNNAMED.text() : Text.literal(c.name()));
            ctx.out().row(TYPE, c.type());
            ctx.out().row(NODE, c.nodeId());
        }
    }

    /** Leaves a remote shell. With no session open there is nothing to leave but the window. */
    @TextHolder
    static final class Exit implements ICliCommand {

        private static final TextKey SUMMARY = TextKey.of("jsc.cli.shell.exit.summary",
                "close the remote shell and return to this computer");

        @Override public CommandScope scope() {
            return CommandScope.everywhere();
        }

        @Override public String name() {
            return "exit";
        }

        /** It leaves a shell on another machine, so it stands with the ways of reaching one. */
        @Override public CommandGroup group() {
            return CommandGroup.NETWORK;
        }

        @Override public List<String> aliases() {
            return List.of("logout");
        }

        @Override public Text summary() {
            return SUMMARY.text();
        }

        @Override public void run(final CliContext ctx) {
            final ICliComputer.OpResult result = ctx.computer().sshDisconnect();
            if (result.ok()) {
                ctx.out().ok(result.message());
            } else {
                ctx.out().error(result.message());
            }
        }
    }
}
