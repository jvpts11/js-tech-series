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

/**
 * A command that hands the terminal to an editor instead of printing to it.
 *
 * <p>Everything a machine has to decide is decided here, where the machine is: whether the program is
 * installed at all, and what the verb is called. What is left for the screen is the drawing, which is
 * the half a server cannot do.
 *
 * <p>One class serves every such editor, because they differ only in their name and in which program
 * has to be installed for them to exist. An addon's editor is another one of these.
 */
@TextHolder
public final class TtyEditorCommand implements ICliCommand, CliShell.IHandOver {

    private final String name;
    private final Text summary;
    private final String programId;

    /** What the editors that take the terminal say they are for, which is the same for all of them. */
    public static final TextKey EDITS_A_FILE = TextKey.of("jsc.cli.editor.summary", "edit a file in the terminal");

    private static final TextKey USAGE = TextKey.of("jsc.cli.editor.usage", "<file>");

    /**
     * @param name      the verb a player types
     * @param summary   what {@code help} says about it
     * @param programId the program that has to be installed for the verb to exist
     */
    public TtyEditorCommand(final String name, final TextKey summary, final String programId) {
        this.name = name;
        this.summary = summary.text();
        this.programId = programId;
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public Text summary() {
        return this.summary;
    }

    @Override
    public Text usage() {
        return USAGE.text();
    }

    @Override
    public CommandGroup group() {
        return CommandGroup.PROGRAMMING;
    }

    /** An editor is a program, and it is where a file can be edited at all, so a system with no files has none. */
    @Override
    public CommandScope scope() {
        return CommandScope.everywhere().needing(CommandScope.Need.FILES);
    }

    /** Only where it was installed: a machine that never got the editor does not offer the verb. */
    @Override
    public boolean available(final ICliComputer computer) {
        if (!ICliCommand.super.available(computer)) {
            return false;
        }
        /*
         * The machine lists a program by its full id, jsc:vim, and by the name it is typed as; the
         * editor is known here by that name, so both are looked at rather than only the one that
         * never matched.
         */
        for (final ICliComputer.ProgramInfo program : computer.programs()) {
            if (program.id().equals(this.programId) || program.id().endsWith(":" + this.programId)
                    || program.name().equals(this.programId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Prints only when it cannot hand over.
     *
     * <p>A file that is not there yet is opened empty, the way a real editor does: writing a program
     * starts by opening the name it will be saved under. So there is nothing to check and nothing to
     * say, and the shell carries the hand-over out on its own.
     */
    @Override
    public void run(final CliContext ctx) {
        if (!ctx.hasArgs()) {
            ctx.out().error(CliTexts.USAGE.with(this.name, USAGE));
        }
    }
}
