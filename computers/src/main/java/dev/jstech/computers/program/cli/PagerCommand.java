/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import dev.jstech.computers.os.Platform;
import java.util.List;
import java.util.Set;

/**
 * Reading a file a page at a time: the verb that hands the terminal to the pager.
 *
 * <p>Like the editors, and unlike them: the terminal is given away the same way, and what takes it never
 * writes. It is part of the system rather than a package, because a machine that can show a file has always
 * been able to show a long one, and a player with no pager is a player who cannot read their own logs.
 *
 * <p>The words after it are a file. With a pipe feeding it instead, there is nothing to hand the glass to, so
 * it prints what it was given, which is what the older of the two names has always done.
 */
final class PagerCommand implements ICliCommand, CliShell.IHandOver {

    private final String name;
    private final Set<Platform> systems;

    /**
     * @param name    the word this family types
     * @param systems the systems that word belongs to
     */
    PagerCommand(final String name, final Set<Platform> systems) {
        this.name = name;
        this.systems = systems;
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public String summary() {
        return "read a file a page at a time";
    }

    @Override
    public String usage() {
        return "<file>";
    }

    @Override
    public CommandScope scope() {
        return CommandScope.on(this.systems).needing(CommandScope.Need.FILES);
    }

    @Override
    public List<String> description() {
        return List.of("Takes the terminal and shows the file, a page at a time. Space and Page Down go on, b",
                "and Page Up go back, the arrows move a line, / looks for something and n finds the next one",
                "of those, and q gives the terminal back.");
    }

    @Override
    public List<String> seeAlso() {
        return List.of("cat", "grep", "tail");
    }

    /** With no file named there is nothing to hand the glass to, so the terminal is kept. */
    @Override
    public String fileOf(final ICliComputer computer, final List<String> args) {
        return args.isEmpty() ? null : CliShell.IHandOver.super.fileOf(computer, args);
    }

    /**
     * Prints only when it is not handing the terminal over: a pipe feeding it has no file to show, so what it
     * was handed is printed, which is what the older of these two names always did.
     */
    @Override
    public void run(final CliContext ctx) {
        if (ctx.hasArgs()) {
            return;
        }
        if (!ctx.hasInput()) {
            ctx.out().error("usage: " + this.name + " <file>");
            return;
        }
        for (final String line : ctx.input()) {
            ctx.out().line(line);
        }
    }
}
