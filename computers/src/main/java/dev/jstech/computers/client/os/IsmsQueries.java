/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.operation.payload.IqlResultPayload;
import dev.jstech.computers.operation.payload.IsmsActionPayload;
import dev.jstech.computers.operation.payload.IsmsPlanPayload;
import dev.jstech.computers.operation.payload.RunIqlPayload;
import dev.jstech.computers.program.iql.IqlOperation;
import dev.jstech.computers.program.iql.IqlParseResult;
import dev.jstech.computers.program.iql.IqlParser;
import dev.jstech.computers.program.iql.IqlScript;
import dev.jstech.computers.program.iql.IqlVerb;
import dev.jstech.core.client.gui.logic.TextDocument;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.jetbrains.annotations.Nullable;

/**
 * What a query tab of the IQL Server Management Studio does with the network: it runs its script a statement at a
 * time, each sent only when the one before has come back; it parses the script without running it, underlining the
 * first word the language cannot read; it asks for the estimated plan of a CRAFT; and it stops a run, with whatever
 * the run set going.
 */
final class IsmsQueries {

    private final IsmsApp app;
    /** The tab the last plan was asked for, which the plan's answer is shown in. */
    private int planTab = -1;

    private static final long MILLIS_PER_SECOND = 1000L;
    private static final long SECONDS_PER_MINUTE = 60L;
    private static final long MINUTES_PER_HOUR = 60L;

    IsmsQueries(final IsmsApp app) {
        this.app = app;
    }

    /**
     * Runs {@code doc}'s script, or only what is selected in it; a statement that cannot be undone is asked about
     * first, when the studio's options say so.
     */
    void execute(final IsmsDocument doc, final boolean selection) {
        if (!doc.isQuery() || doc.running()) {
            return;
        }
        doc.clearOutput();
        if (!app.connected()) {
            refuse(doc, IsmsTexts.NOT_CONNECTED_TO.with(app.connectionProblem()));
            return;
        }
        final TextDocument text = doc.code.document();
        final String whole = doc.code.text();
        final boolean part = selection && text.hasSelection();
        final int base = part ? offsetOf(whole, text.selectionStart().line(), text.selectionStart().col()) : 0;
        final String script = part ? text.selectedText() : whole;
        final List<IqlScript.Statement> statements = IqlScript.split(script);
        if (statements.isEmpty()) {
            refuse(doc, IsmsTexts.NOTHING.text());
            return;
        }
        for (final IqlScript.Statement statement : statements) {
            if (statement.text().length() > RunIqlPayload.MAX_LEN) {
                final int line = IsmsDocument.lineOf(whole, base + statement.start());
                mark(doc, line, 0, 0, GameText.resolve(IsmsTexts.TOO_LONG.with(line,
                        statement.text().length(), RunIqlPayload.MAX_LEN)));
                refuse(doc, IsmsTexts.TOO_LONG.with(line, statement.text().length(), RunIqlPayload.MAX_LEN));
                return;
            }
        }
        final IqlScript.Statement risky = app.settings().askFirst ? destructive(statements) : null;
        if (risky != null) {
            app.dialogs().confirm(risky.text(), () -> start(doc, statements, whole, base));
            return;
        }
        start(doc, statements, whole, base);
    }

    /** An answer to a statement of one of the window's tabs, or to something asked of the network beside them. */
    void answer(final IqlResultPayload payload) {
        if (payload.tab() == IsmsActionPayload.NO_TAB) {
            final IsmsDocument doc = app.current();
            if (doc != null && !payload.message().isEmpty()) {
                doc.say(payload.message(), payload.ok() ? IsmsDocument.Tone.PLAIN : IsmsDocument.Tone.BAD);
                doc.status = payload.message();
                doc.statusOk = payload.ok();
            }
            return;
        }
        final IsmsDocument doc = app.document(payload.tab());
        if (doc == null || doc.run == null || payload.seq() != doc.run.next) {
            return;
        }
        final IsmsDocument.Run run = doc.run;
        if (!payload.columns().isEmpty()) {
            final List<List<String>> rows = new ArrayList<>(payload.rows().size());
            for (final List<Text> row : payload.rows()) {
                final List<String> cells = new ArrayList<>(row.size());
                row.forEach(cell -> cells.add(GameText.resolve(cell)));
                rows.add(cells);
            }
            doc.results.add(new IsmsDocument.ResultSet(payload.columns(), rows));
            doc.rowsRead += rows.size();
            doc.say(IsmsResultsView.rowsSaid(rows.size()), IsmsDocument.Tone.PLAIN);
        }
        if (!payload.ok()) {
            failed(doc, run, payload.message());
        } else if (payload.columns().isEmpty() && !payload.message().isEmpty()) {
            doc.say(payload.message(), IsmsDocument.Tone.PLAIN);
        }
        for (final String id : payload.started()) {
            run.started.add(id);
            doc.say(IsmsTexts.STARTED.with(id), IsmsDocument.Tone.GOOD);
        }
        run.next++;
        if (run.current() != null && !(run.failed && app.settings().stopOnError)) {
            send(doc);
            return;
        }
        finish(doc, false);
    }

    /** Stops {@code doc}'s run: nothing more is sent, and every Operation it set going is asked to stop. */
    void cancel(final IsmsDocument doc) {
        final IsmsDocument.Run run = doc.run;
        if (run == null) {
            return;
        }
        for (final String id : run.started) {
            app.action(IsmsActionPayload.CANCEL, id, 0);
            doc.say(IsmsTexts.STOPPED_OPERATION.with(id), IsmsDocument.Tone.PLAIN);
        }
        doc.say(IsmsTexts.CANCELLED.text(), IsmsDocument.Tone.BAD);
        finish(doc, true);
    }

    /** Reads {@code doc}'s script without running it, and points at the first word the language cannot read. */
    void parse(final IsmsDocument doc) {
        if (!doc.isQuery()) {
            return;
        }
        doc.clearOutput();
        final String whole = doc.code.text();
        for (final IqlScript.Statement statement : IqlScript.split(whole)) {
            final IqlParseResult parsed = IqlParser.tryParse(statement.text());
            if (!parsed.ok()) {
                final Text error = parsed.error();
                final int line = pointAt(doc, whole, 0, statement, parsed.position(), GameText.resolve(error));
                doc.say(IsmsTexts.LINE_ERROR.with(line, error), IsmsDocument.Tone.BAD);
                doc.status = IsmsTexts.PARSE_FAILED.text();
                doc.statusOk = false;
                doc.tab = IsmsDocument.ResultsTab.MESSAGES;
                return;
            }
        }
        doc.say(IsmsTexts.PARSED.text(), IsmsDocument.Tone.PLAIN);
        doc.status = IsmsTexts.PARSED.text();
        doc.statusOk = true;
        doc.tab = IsmsDocument.ResultsTab.MESSAGES;
    }

    /** Asks for the estimated plan of the CRAFT selected, or of the one the caret stands in. */
    void plan(final IsmsDocument doc) {
        if (!doc.isQuery()) {
            return;
        }
        final TextDocument text = doc.code.document();
        final String whole = doc.code.text();
        final IqlScript.Statement statement = text.hasSelection()
                ? first(IqlScript.split(text.selectedText()))
                : IqlScript.at(whole, offsetOf(whole, text.cursorLine(), text.cursorCol()));
        final IqlParseResult parsed = statement == null ? null : IqlParser.tryParse(statement.text());
        final IqlOperation craft = parsed == null ? null : parsed.operation();
        if (craft == null || craft.verb() != IqlVerb.CRAFT) {
            doc.say(IsmsTexts.PLAN_CRAFT_ONLY.text(), IsmsDocument.Tone.BAD);
            doc.tab = IsmsDocument.ResultsTab.MESSAGES;
            return;
        }
        if (!app.connected()) {
            refuse(doc, IsmsTexts.NOT_CONNECTED_TO.with(app.connectionProblem()));
            return;
        }
        planTab = doc.id;
        final long quantity = craft.quantity() <= 0 ? 1L : craft.quantity();
        app.action(IsmsActionPayload.PLAN, craft.item(), (int) Math.min(Integer.MAX_VALUE, quantity));
        doc.tab = IsmsDocument.ResultsTab.PLAN;
    }

    /** The plan the network estimated, shown in the tab that asked for it. */
    void acceptPlan(final IsmsPlanPayload payload) {
        final IsmsDocument asked = app.document(planTab);
        final IsmsDocument doc = asked != null ? asked : app.current();
        if (doc == null) {
            return;
        }
        doc.plan.clear();
        doc.planDepth.clear();
        doc.plan.addAll(payload.lines());
        doc.planDepth.addAll(payload.depth());
        doc.tab = IsmsDocument.ResultsTab.PLAN;
        doc.resultsScroll = 0;
        if (!payload.ok() && !payload.lines().isEmpty()) {
            doc.status = payload.lines().get(0);
            doc.statusOk = false;
        }
    }

    /** How long a run took, as the status bar and the messages write it: hours, minutes and seconds. */
    static String clock(final long millis) {
        final long seconds = millis / MILLIS_PER_SECOND;
        return String.format(Locale.ROOT, "%02d:%02d:%02d", seconds / (SECONDS_PER_MINUTE * MINUTES_PER_HOUR),
                seconds / SECONDS_PER_MINUTE % MINUTES_PER_HOUR, seconds % SECONDS_PER_MINUTE);
    }

    /** Where line {@code line}, column {@code col} of {@code text} is, as a character offset, both from zero. */
    static int offsetOf(final String text, final int line, final int col) {
        int offset = 0;
        int at = 0;
        while (at < line) {
            final int next = text.indexOf('\n', offset);
            if (next < 0) {
                return text.length();
            }
            offset = next + 1;
            at++;
        }
        return Math.min(text.length(), offset + col);
    }

    private void start(final IsmsDocument doc, final List<IqlScript.Statement> statements, final String whole,
                       final int base) {
        doc.run = new IsmsDocument.Run(statements, whole, base, System.currentTimeMillis());
        doc.status = IsmsTexts.EXECUTING.text();
        doc.statusOk = true;
        doc.tab = IsmsDocument.ResultsTab.RESULTS;
        send(doc);
    }

    private void send(final IsmsDocument doc) {
        final IsmsDocument.Run run = doc.run;
        final IqlScript.Statement statement = run == null ? null : run.current();
        if (statement != null) {
            app.send(new RunIqlPayload(app.monitor(), app.host(), app.window(), doc.id, run.next, statement.text()));
        }
    }

    /* A statement the network refused: the reason against its line, and the word it stopped at underlined. */
    private void failed(final IsmsDocument doc, final IsmsDocument.Run run, final Text message) {
        run.failed = true;
        final IqlScript.Statement statement = run.current();
        final int line = run.line();
        doc.say(IsmsTexts.LINE_ERROR.with(line, message), IsmsDocument.Tone.BAD);
        if (statement != null) {
            final IqlParseResult parsed = IqlParser.tryParse(statement.text());
            if (!parsed.ok()) {
                pointAt(doc, run.script, run.base, statement, parsed.position(), GameText.resolve(message));
            } else {
                mark(doc, line, 0, 0, GameText.resolve(message));
            }
        }
    }

    private void finish(final IsmsDocument doc, final boolean cancelled) {
        final IsmsDocument.Run run = doc.run;
        if (run == null) {
            return;
        }
        doc.run = null;
        doc.elapsedMillis = System.currentTimeMillis() - run.startedAt;
        doc.say(IsmsTexts.COMPLETION.with(clock(doc.elapsedMillis)), IsmsDocument.Tone.PLAIN);
        doc.status = cancelled ? IsmsTexts.CANCELLED.text() : run.failed ? IsmsTexts.WITH_ERRORS.text()
                : IsmsTexts.EXECUTED.text();
        doc.statusOk = !cancelled && !run.failed;
        if (cancelled || run.failed || doc.results.isEmpty()) {
            doc.tab = IsmsDocument.ResultsTab.MESSAGES;
        }
        if (doc.mode == IsmsSettings.Results.FILE && !doc.results.isEmpty()) {
            app.saveResults(doc);
        }
        // A run may have saved, dropped or changed what the explorer shows: it asks the network again.
        app.askSchema();
    }

    private static void refuse(final IsmsDocument doc, final Text why) {
        doc.say(why, IsmsDocument.Tone.BAD);
        doc.status = why;
        doc.statusOk = false;
        doc.tab = IsmsDocument.ResultsTab.MESSAGES;
    }

    /*
     * Underlines token {@code position} of {@code statement}, a statement of the text that starts {@code base}
     * characters into the editor's {@code whole}, and says which line it is on.
     */
    private static int pointAt(final IsmsDocument doc, final String whole, final int base,
                               final IqlScript.Statement statement, final int position, final String message) {
        final int[] span = IqlScript.tokenSpan(statement.text(), position);
        final int at = base + statement.start() + (span == null ? 0 : span[0]);
        final int line = IsmsDocument.lineOf(whole, at);
        mark(doc, line, span == null ? 0 : IsmsDocument.columnOf(whole, at), span == null ? 0 : span[1] - span[0],
                message);
        return line;
    }

    private static void mark(final IsmsDocument doc, final int line, final int column, final int length,
                             final String message) {
        doc.code.setMarks(List.of(new CodeArea.Mark(line, column, Math.max(column == 0 ? 0 : 1, length), true,
                message)));
    }

    @Nullable
    private static IqlScript.Statement destructive(final List<IqlScript.Statement> statements) {
        for (final IqlScript.Statement statement : statements) {
            if (IqlScript.destructive(statement.text())) {
                return statement;
            }
        }
        return null;
    }

    @Nullable
    private static IqlScript.Statement first(final List<IqlScript.Statement> statements) {
        return statements.isEmpty() ? null : statements.get(0);
    }
}
