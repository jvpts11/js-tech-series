/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import dev.jstech.computers.program.cli.man.ManPage;
import java.util.List;

/**
 * The commands about the shell itself: what it knows, what it says and how it is left.
 *
 * <p>Moved here from BuiltinCommands, which keeps the lists that say which system gets which command.
 */
final class ShellCommands {

    private ShellCommands() {
    }

    static final class Help implements ICliCommand {
        @Override public CommandScope scope() {
            return CommandScope.everywhere();
        }

        @Override public String name() {
            return "help";
        }

        @Override public List<String> aliases() {
            return List.of("?", "commands");
        }

        @Override public String summary() {
            return "list commands, or show how one is used";
        }

        @Override public String usage() {
            return "[command]";
        }

        @Override public void run(final CliContext ctx) {
            if (ctx.hasArgs()) {
                final ICliCommand command = ctx.shell().find(ctx.arg(0));
                if (command == null || !command.available(ctx.computer())) {
                    ctx.out().error("no such command: " + ctx.arg(0));
                    return;
                }
                /*
                 * The command's own page, in this family's voice: the same words the manual has on a Unix
                 * system, which is the whole point of there being one body of text about a command.
                 */
                ctx.out().accent(command.name() + (command.usage().isEmpty() ? "" : " " + command.usage()));
                for (final String line : ManPage.lines(command, false)) {
                    ctx.out().line(line);
                }
                if (!command.aliases().isEmpty()) {
                    ctx.out().dim("  also: " + String.join(", ", command.aliases()));
                }
                return;
            }
            ctx.out().header("commands");
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
            ctx.out().dim("'help <command>' for details");
        }
    }

    static final class Clear implements ICliCommand, CliShell.IClearMarker {
        /** The DOS family's own word for it; the Unix systems clear with {@code clear}. */
        @Override public CommandScope scope() {
            return CommandScope.on(CommandScope.DOS_SYSTEMS);
        }

        @Override public String name() {
            return "cls";
        }

        @Override public String summary() {
            return "clear the console";
        }

        @Override public void run(final CliContext ctx) {
            // The shell clears the scrollback because this command is a ClearMarker; nothing to print.
        }
    }

    static final class Echo implements ICliCommand {
        @Override public CommandScope scope() {
            return CommandScope.everywhere();
        }

        @Override public String name() {
            return "echo";
        }

        @Override public String summary() {
            return "print the given text";
        }

        @Override public String usage() {
            return "<text>";
        }

        @Override public void run(final CliContext ctx) {
            ctx.out().line(ctx.rest(0));
        }
    }

    static final class Version implements ICliCommand {
        /** {@code VER} is the DOS family's; a Unix system says what it is with {@code uname}. */
        @Override public CommandScope scope() {
            return CommandScope.on(CommandScope.DOS_SYSTEMS);
        }

        @Override public String name() {
            return "version";
        }

        @Override public List<String> aliases() {
            return List.of("ver");
        }

        @Override public String summary() {
            return "show the shell version";
        }

        @Override public void run(final CliContext ctx) {
            ctx.out().accent("J's Computers Shell v1.0");
        }
    }

    static final class Whoami implements ICliCommand {
        @Override public CommandScope scope() {
            return CommandScope.everywhere();
        }

        @Override public String name() {
            return "whoami";
        }

        @Override public String summary() {
            return "show this computer's name and id";
        }

        @Override public void run(final CliContext ctx) {
            final ICliComputer c = ctx.computer();
            ctx.out().row("name", c.name().isEmpty() ? "(unnamed)" : c.name());
            ctx.out().row("type", c.type());
            ctx.out().row("node", c.nodeId());
        }
    }

    /** Leaves a remote shell. With no session open there is nothing to leave but the window. */
    static final class Exit implements ICliCommand {
        @Override public CommandScope scope() {
            return CommandScope.everywhere();
        }

        @Override public String name() {
            return "exit";
        }

        @Override public List<String> aliases() {
            return List.of("logout");
        }

        @Override public String summary() {
            return "close the remote shell and return to this computer";
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
