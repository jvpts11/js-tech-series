/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.os.edit.CodeRuns;
import dev.jstech.computers.os.edit.InkPalette;
import dev.jstech.computers.program.iql.IqlColouring;
import dev.jstech.computers.program.iql.IqlScript;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.jetbrains.annotations.Nullable;

/**
 * One document tab of the IQL Server Management Studio: a query with its text, the tables its statements read, what
 * they said and the plan last estimated, or one of the studio's own pages (the Object Explorer's details, the
 * Activity Monitor, the templates, the reference).
 *
 * <p>A query keeps what a run of it is doing: the statements left to send, which one is out, the Operations it
 * started, and when it began. A script goes a statement at a time, the next only when the last has come back, so
 * every answer lands in the tab, and against the statement, that asked.
 */
final class IsmsDocument {

    /** The tab's number in its window, which an answer from the network carries back. */
    final int id;
    final Kind kind;
    /** The editor, for a query; an unused one for the studio's own pages. */
    final CodeArea code = new CodeArea();
    final List<ResultSet> results = new ArrayList<>();
    final List<Message> messages = new ArrayList<>();
    final List<Text> plan = new ArrayList<>();
    final List<Integer> planDepth = new ArrayList<>();
    /** The file the query was opened from or saved to; empty until it has one. */
    String path = "";
    /** What the tab is called while it has no file. */
    String name;
    boolean dirty;
    ResultsTab tab = ResultsTab.RESULTS;
    IsmsSettings.Results mode = IsmsSettings.Results.GRID;
    @Nullable
    Run run;
    /** How many rows the last run read, and how long it took, for the status bar. */
    int rowsRead;
    long elapsedMillis;
    Text status = IsmsTexts.READY.text();
    boolean statusOk = true;
    int resultsScroll;
    /** What the Object Explorer Details page searches for. */
    String search = "";

    /** The most messages a tab keeps, the oldest going first. */
    private static final int MOST_MESSAGES = 200;
    private static final String EXTENSION = ".iql";

    IsmsDocument(final int id, final Kind kind, final String name) {
        this.id = id;
        this.kind = kind;
        this.name = name;
        code.setPalette(InkPalette.LIGHT).setColouring(lines -> CodeRuns.byLine(lines, IqlColouring.spans(lines)));
        code.setOnEdit(this::edited);
    }

    /** What its tab says: the file's name, or what it is called, with a star while it has changes not saved. */
    String title() {
        final String base = path.isEmpty() ? name : path.substring(Math.max(path.lastIndexOf('/'),
                path.lastIndexOf('\\')) + 1);
        return dirty ? base + " *" : base;
    }

    /** The name a Save As offers: the file's, or the tab's with the scripts' extension. */
    String suggestedFile() {
        if (!path.isEmpty()) {
            return path.substring(Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\')) + 1);
        }
        return name.replace(' ', '_').toLowerCase(Locale.ROOT) + EXTENSION;
    }

    boolean isQuery() {
        return kind == Kind.QUERY;
    }

    boolean running() {
        return run != null;
    }

    /** Empties what the last run left: its tables, its messages and its marks; the plan stays until replaced. */
    void clearOutput() {
        results.clear();
        messages.clear();
        rowsRead = 0;
        resultsScroll = 0;
        code.setMarks(List.of());
    }

    void say(final Text text, final Tone tone) {
        messages.add(new Message(GameText.resolve(text), tone));
        while (messages.size() > MOST_MESSAGES) {
            messages.remove(0);
        }
    }

    /** The line, counted from one, that character {@code offset} of {@code text} stands on. */
    static int lineOf(final String text, final int offset) {
        int line = 1;
        for (int i = 0; i < Math.min(offset, text.length()); i++) {
            if (text.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }

    /** The column, counted from one, that character {@code offset} of {@code text} stands at. */
    static int columnOf(final String text, final int offset) {
        final int clamped = Math.min(offset, text.length());
        return clamped - text.lastIndexOf('\n', clamped - 1);
    }

    private void edited() {
        dirty = true;
        code.setMarks(List.of());
    }

    /** What the tab is. */
    enum Kind { QUERY, DETAILS, ACTIVITY, TEMPLATES, REFERENCE, SHORTCUTS }

    /** Which pane under a query is in front. */
    enum ResultsTab { RESULTS, MESSAGES, PLAN }

    /** How a message reads: plain, an Operation set going, or an error. */
    enum Tone { PLAIN, GOOD, BAD }

    /** A table a statement read, its cells already in the player's language. */
    record ResultSet(List<String> columns, List<List<String>> rows) {
    }

    /** A line of the Messages pane. */
    record Message(String text, Tone tone) {
    }

    /**
     * A run of a query: the statements it sends, the text they were taken from and where in it, the one out now, the
     * Operations they set going, and when it began.
     */
    static final class Run {

        final List<IqlScript.Statement> statements;
        final String script;
        final int base;
        final long startedAt;
        final List<String> started = new ArrayList<>();
        int next;
        boolean failed;

        Run(final List<IqlScript.Statement> statements, final String script, final int base, final long startedAt) {
            this.statements = List.copyOf(statements);
            this.script = script;
            this.base = base;
            this.startedAt = startedAt;
        }

        /** The statement out now, or null once every statement has been answered. */
        @Nullable
        IqlScript.Statement current() {
            return next < statements.size() ? statements.get(next) : null;
        }

        /** The line of the script the statement out now starts on. */
        int line() {
            final IqlScript.Statement statement = current();
            return statement == null ? 1 : lineOf(script, base + statement.start());
        }
    }
}
