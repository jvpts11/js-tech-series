/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
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
    Text summary();

    /**
     * The argument syntax shown by {@code help <name>}, e.g. {@code "<quantity> <item>"}. The switches are written
     * as the command takes them and the words in angle brackets are the reader's, so it is declared text.
     */
    default Text usage() {
        return Text.EMPTY;
    }

    /**
     * What the command does, in whole sentences: the paragraphs of its manual page, one to an entry. A paragraph
     * is one piece of text however long it is, since where its lines break is for whatever shows it to decide, in
     * the reader's language.
     *
     * <p>A command that says nothing here has a page made of what it does say, which is honest and thin. One
     * that says something has a page worth reading, and it is the only place the words are written: the
     * manual, the Help window on a desktop and {@code /?} at a DOS prompt all read this.
     */
    default List<Text> description() {
        return List.of();
    }

    /** What each of its switches does, for the OPTIONS part of the page. */
    default List<Option> options() {
        return List.of();
    }

    /** Lines worth trying, with what each one shows, for the EXAMPLES part. */
    default List<Example> examples() {
        return List.of();
    }

    /** The commands worth reading next, by name, for SEE ALSO. */
    default List<String> seeAlso() {
        return List.of();
    }

    /** What the command is for, the heading a listing of everything a machine can run puts it under. */
    default CommandGroup group() {
        return CommandGroup.SOFTWARE;
    }

    /**
     * One switch and what it does.
     *
     * @param flag how it is written, as the family this command belongs to writes it
     * @param what what it does, in one line
     */
    record Option(String flag, Text what) {

        public Option(final String flag, final TextKey what) {
            this(flag, what.text());
        }
    }

    /**
     * One line worth trying.
     *
     * @param line what to type
     * @param what what comes of it, in one line, or empty when the line speaks for itself
     */
    record Example(String line, Text what) {

        public Example(final String line, final TextKey what) {
            this(line, what.text());
        }

        public Example(final String line) {
            this(line, Text.EMPTY);
        }
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
