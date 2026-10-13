/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import dev.jstech.computers.program.tty.ITtyProcess;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * The buffer a command writes its result into. Style helpers keep command code terse and consistent ({@code out.ok(...)}, {@code out.error(...)}), and {@link #row} formats a two-column line so listings line up in the monospace console.
 */
public final class CliOutput {

    private final List<CliLine> lines = new ArrayList<>();
    private final int width;

    /** The tool this command left running in front of the terminal, if it left one. */
    private ITtyProcess started;

    public CliOutput() {
        this(52);
    }

    /**
     * @param width the console's character width, used to right-align the value column in {@link #row}
     */
    public CliOutput(final int width) {
        this.width = Math.max(16, width);
    }

    /** How many columns the terminal has, which a tool that sizes a bar or a table to it has to be told. */
    public int width() {
        return this.width;
    }

    /**
     * Leaves a tool running in front of the terminal.
     *
     * <p>What the command printed before this is printed first, and then the prompt stays away until the tool
     * is over: what it prints arrives while it works, what is typed goes to it, and Ctrl+C stops it.
     */
    public void start(final ITtyProcess tool) {
        this.started = tool;
    }

    /** The tool this command left running, or null for the nearly-all that simply answer. */
    @Nullable
    public ITtyProcess started() {
        return this.started;
    }

    /** A line of words that are data: a name, a path, a number, what was typed, a file's contents. */
    public void line(final String text) {
        lines.add(new CliLine(text, CliStyle.PLAIN));
    }

    public void line(final Text text) {
        lines.add(new CliLine(text, CliStyle.PLAIN));
    }

    public void line(final TextKey key) {
        line(key.text());
    }

    /** A line already put together, which is how one coloured in parts is written. */
    public void line(final CliLine line) {
        lines.add(line);
    }

    public void styled(final String text, final CliStyle style) {
        lines.add(new CliLine(text, style));
    }

    public void styled(final Text text, final CliStyle style) {
        lines.add(new CliLine(text, style));
    }

    public void ok(final String text) {
        styled(text, CliStyle.OK);
    }

    public void ok(final Text text) {
        styled(text, CliStyle.OK);
    }

    public void ok(final TextKey key) {
        styled(key.text(), CliStyle.OK);
    }

    public void error(final String text) {
        styled(text, CliStyle.ERROR);
    }

    public void error(final Text text) {
        styled(text, CliStyle.ERROR);
    }

    public void error(final TextKey key) {
        styled(key.text(), CliStyle.ERROR);
    }

    public void warn(final String text) {
        styled(text, CliStyle.WARN);
    }

    public void warn(final Text text) {
        styled(text, CliStyle.WARN);
    }

    public void warn(final TextKey key) {
        styled(key.text(), CliStyle.WARN);
    }

    public void info(final String text) {
        styled(text, CliStyle.INFO);
    }

    public void info(final Text text) {
        styled(text, CliStyle.INFO);
    }

    public void info(final TextKey key) {
        styled(key.text(), CliStyle.INFO);
    }

    public void dim(final String text) {
        styled(text, CliStyle.DIM);
    }

    public void dim(final Text text) {
        styled(text, CliStyle.DIM);
    }

    public void dim(final TextKey key) {
        styled(key.text(), CliStyle.DIM);
    }

    public void accent(final String text) {
        styled(text, CliStyle.ACCENT);
    }

    public void accent(final Text text) {
        styled(text, CliStyle.ACCENT);
    }

    public void accent(final TextKey key) {
        styled(key.text(), CliStyle.ACCENT);
    }

    public void header(final String text) {
        styled(text, CliStyle.HEADER);
    }

    public void header(final Text text) {
        styled(text, CliStyle.HEADER);
    }

    public void header(final TextKey key) {
        styled(key.text(), CliStyle.HEADER);
    }

    public void blank() {
        line("");
    }

    /**
     * A left label and a right value packed onto one line, the value pushed to the console's right
     * edge with dots filling the gap, so columns in a listing line up without a real table widget.
     *
     * <p>The dots are counted where the line is read, once the label is in that reader's language, so a label
     * that is longer in another language still leaves the value against the edge.
     */
    public void row(final Text label, final Text value) {
        line(CliLine.of(CliSpan.plain(label), CliSpan.fill(width, true), CliSpan.plain(value)));
    }

    public void row(final TextKey label, final String value) {
        row(label.text(), Text.literal(value));
    }

    public void row(final String label, final String value) {
        row(Text.literal(label), Text.literal(value));
    }

    /**
     * A name and what it does, in two columns: the dots run from the name up to {@code column}, so every
     * description begins in the same place however long the names are.
     *
     * <p>This is not {@link #row}, and the difference matters. A row pushes its value to the right edge,
     * which is right for a figure being read off a listing and wrong for a list of commands: a long
     * description leaves no room for dots at all, so some lines get them and some do not, and each
     * description starts somewhere different. Lining the descriptions up is the whole job here.
     */
    public void entry(final Text name, final Text description, final int column) {
        line(CliLine.of(CliSpan.plain(name), CliSpan.fill(column, false), CliSpan.plain(description)));
    }

    public void entry(final String name, final Text description, final int column) {
        entry(Text.literal(name), description, column);
    }

    public void entry(final String name, final TextKey description, final int column) {
        entry(Text.literal(name), description.text(), column);
    }

    public void entry(final String name, final String description, final int column) {
        entry(Text.literal(name), Text.literal(description), column);
    }

    public List<CliLine> lines() {
        return List.copyOf(lines);
    }

    public boolean isEmpty() {
        return lines.isEmpty();
    }
}
