/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import java.util.List;

/**
 * A single shell command. Add-ons implement this and register it with {@link CliCommands#register} (or through the {@code RegisterCliCommandsEvent}) to extend the Command Prompt with their own verbs; the command set is deliberately open.
 *
 * <p>Every command says where it exists, with {@link #scope()}. Nothing is on every machine by default: a command
 * belongs to the systems it names, on the computers it names, and only when the machine has what it needs. Everything
 * that shows commands to a player reads that one answer, so a machine never offers what it cannot do.
 */
public interface ICliCommand {

    /** The primary word that invokes this command (lower-case, no spaces). */
    String name();

    /** Alternative words that also invoke it; empty by default. */
    default List<String> aliases() {
        return List.of();
    }

    /** A one-line description shown by {@code help}. */
    String summary();

    /** The argument syntax shown by {@code help <name>}, e.g. {@code "<quantity> <item>"}. */
    default String usage() {
        return "";
    }

    /**
     * Where this command exists: the systems that have it and what the machine must have for it to be there. There is
     * no default, so a new command cannot forget to say.
     */
    CommandScope scope();

    /** Runs the command, writing its result into {@code context.out()}. Must not throw for ordinary errors. */
    void run(CliContext context);

    /**
     * Whether this command is on a given computer. The scope answers it; a command with something else to ask (a
     * card in a slot, a service already running) overrides this and asks that as well.
     */
    default boolean available(final ICliComputer computer) {
        return CommandAccess.allows(scope(), computer);
    }
}
