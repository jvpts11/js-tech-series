/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program;

import dev.jstech.computers.advancement.Acting;
import dev.jstech.computers.advancement.JscEvents;
import dev.jstech.computers.advancement.MachineOperators;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.computers.program.iql.IIqlCondition;
import dev.jstech.computers.program.iql.IIqlView;
import dev.jstech.computers.program.iql.IqlDefinition;
import dev.jstech.computers.program.iql.IqlDefinitionParser;
import dev.jstech.computers.program.iql.IqlOperation;
import dev.jstech.computers.program.iql.IqlParseResult;
import dev.jstech.computers.program.iql.IqlParser;
import dev.jstech.computers.program.iql.IqlSavedObject;
import dev.jstech.computers.program.iql.IqlVerb;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;
import java.util.Locale;

/**
 * The IQL Engine's runtime: it takes a statement and either runs it as an immediate action/query (through the
 * two-method view it is handed) or, for a Layer-2 statement, stores/runs a saved object against the Mainframe's
 * catalog. CREATE/DROP touch the catalog; EXEC runs a procedure's statements in order, stopping at the
 * first error (the chosen default); a QUERY whose object is a view name runs the saved query. Touching the
 * catalog requires the Engine to be installed and running on the Mainframe; ad-hoc actions do not.
 */
@TextHolder
public final class IqlEngine {

    private final MainframeBlockEntity mainframe;
    private final IIqlView computer;
    private final int queryRowLimit;

    private static final int RECURSION_GUARD = 32;

    private static final TextKey TOO_DEEP = TextKey.of("jsc.service.iql.too_deep",
            "IQL recursion too deep (a procedure or view referencing itself?)");
    private static final TextKey SYNTAX = TextKey.of("jsc.service.iql.syntax", "syntax: %s");
    private static final TextKey ENGINE_NOT_RUNNING = TextKey.of("jsc.service.iql.engine_not_running",
            "the IQL Engine is not running on the Mainframe, install and start it first");
    private static final TextKey CREATED = TextKey.of("jsc.service.iql.created", "%s %s created");
    private static final TextKey NO_SUCH_OBJECT = TextKey.of("jsc.service.iql.no_such_object", "no %s named %s");
    private static final TextKey DROPPED = TextKey.of("jsc.service.iql.dropped", "%s %s dropped");
    private static final TextKey NO_PROCEDURE = TextKey.of("jsc.service.iql.no_procedure", "no procedure named %s");
    private static final TextKey PROCEDURE_STOPPED = TextKey.of("jsc.service.iql.procedure_stopped",
            "procedure %s stopped at statement %s: %s");
    private static final TextKey PROCEDURE_RAN_ONE =
            TextKey.of("jsc.service.iql.procedure_ran_one", "procedure %s ran %s statement");
    private static final TextKey PROCEDURE_RAN_MANY =
            TextKey.of("jsc.service.iql.procedure_ran_many", "procedure %s ran %s statements");
    private static final TextKey ROWS_ONE = TextKey.of("jsc.service.iql.rows_one", "%s row");
    private static final TextKey ROWS_MANY = TextKey.of("jsc.service.iql.rows_many", "%s rows");

    public IqlEngine(final MainframeBlockEntity mainframe, final IIqlView computer,
                     final int queryRowLimit) {
        this.mainframe = mainframe;
        this.computer = computer;
        this.queryRowLimit = queryRowLimit;
    }

    /** The same engine for whoever holds a whole computer: it is taken as the two things the engine asks of it. */
    public IqlEngine(final MainframeBlockEntity mainframe, final ICliComputer computer, final int queryRowLimit) {
        this(mainframe, viewOf(computer), queryRowLimit);
    }

    /** A computer seen as what the engine needs: one way to read, one way to act. */
    private static IIqlView viewOf(final ICliComputer computer) {
        return new IIqlView() {
            @Override
            public List<ICliComputer.StoredItem> queryObject(final String object,
                                                             final IIqlCondition
                                                                     where,
                                                             final String server, final int limit) {
                return computer.queryObject(object, where, server, limit);
            }

            @Override
            public ICliComputer.OpResult execute(final IqlOperation operation) {
                return computer.execute(operation);
            }
        };
    }

    /**
     * The result of running a statement: a status, what it says, and (for a read) the result rows.
     *
     * @param said what it says, in whatever language its reader reads
     */
    public record Outcome(boolean ok, Text said, List<ICliComputer.StoredItem> rows) {

        /** What it says in English, the machine's language: what a program is handed and a log keeps. */
        public String message() {
            return this.said.english();
        }

        static Outcome ok(final Text message) {
            return new Outcome(true, message, List.of());
        }

        static Outcome fail(final Text message) {
            return new Outcome(false, message, List.of());
        }

        static Outcome rows(final List<ICliComputer.StoredItem> rows) {
            return new Outcome(true, (rows.size() == 1 ? ROWS_ONE : ROWS_MANY).with(rows.size()), rows);
        }
    }

    public Outcome run(final String statement) {
        final Outcome outcome = run(statement, 0);
        // Somebody's statement, not a job firing on its own: those run with nobody acting.
        if (outcome.ok()) {
            JscEvents.awardActing(mainframe.getLevel(), JscEvents.IQL_QUERY, "");
        }
        return outcome;
    }

    private Outcome run(final String statement, final int depth) {
        if (depth > RECURSION_GUARD) {
            return Outcome.fail(TOO_DEEP.text());
        }
        final IqlParseResult parsed = IqlParser.tryParse(statement);
        if (!parsed.ok()) {
            return Outcome.fail(SYNTAX.with(parsed.error()));
        }
        if (parsed.isDefinition()) {
            return runDefinition(parsed.definition(), depth);
        }
        return runOperation(parsed.operation(), depth);
    }

    private Outcome runDefinition(final IqlDefinition definition, final int depth) {
        if (!mainframe.isIqlEngineActive()) {
            return Outcome.fail(ENGINE_NOT_RUNNING.text());
        }
        return switch (definition.verb()) {
            case CREATE -> create(definition);
            case DROP -> drop(definition);
            case EXEC -> runProcedure(definition.name(), depth);
        };
    }

    private Outcome create(final IqlDefinition definition) {
        mainframe.iqlCatalog().put(IqlSavedObject.from(definition));
        mainframe.markIqlCatalogChanged();
        // A job fires later with nobody at the keyboard; it is credited to whoever set it up.
        if (definition.objectType() == IqlDefinition.ObjectType.JOB) {
            Acting.current().ifPresent(player -> MachineOperators.note(mainframe, player));
        }
        return Outcome.ok(CREATED.with(typeName(definition.objectType()), definition.name()));
    }

    private Outcome drop(final IqlDefinition definition) {
        if (!mainframe.iqlCatalog().remove(definition.objectType(), definition.name())) {
            return Outcome.fail(NO_SUCH_OBJECT.with(typeName(definition.objectType()), definition.name()));
        }
        mainframe.markIqlCatalogChanged();
        return Outcome.ok(DROPPED.with(typeName(definition.objectType()), definition.name()));
    }

    private Outcome runProcedure(final String name, final int depth) {
        final IqlSavedObject procedure = mainframe.iqlCatalog().get(IqlDefinition.ObjectType.PROCEDURE, name);
        if (procedure == null) {
            return Outcome.fail(NO_PROCEDURE.with(name));
        }
        final List<String> statements = IqlDefinitionParser.splitBody(procedure.body());
        int ran = 0;
        for (final String statement : statements) {
            final Outcome result = run(statement, depth + 1);
            if (!result.ok()) {
                return Outcome.fail(PROCEDURE_STOPPED.with(name, ran + 1, result.said()));
            }
            ran++;
        }
        return Outcome.ok((ran == 1 ? PROCEDURE_RAN_ONE : PROCEDURE_RAN_MANY).with(name, ran));
    }

    private Outcome runOperation(final IqlOperation operation, final int depth) {
        if (operation.verb() == IqlVerb.QUERY || operation.verb() == IqlVerb.COUNT) {
            final IqlSavedObject view =
                    mainframe.iqlCatalog().get(IqlDefinition.ObjectType.VIEW, operation.item());
            if (view != null) {
                return run(view.body(), depth + 1); // QUERY <view> runs the saved query
            }
            final int limit = operation.limit() > 0 ? operation.limit() : queryRowLimit;
            // Pass the whole WHERE so the read filters on every field (qty/name/damaged/...), not just name.
            return Outcome.rows(computer.queryObject(operation.item(), operation.where(), "", limit));
        }
        final ICliComputer.OpResult result = computer.execute(operation);
        return new Outcome(result.ok(), result.message(), List.of());
    }

    private static String typeName(final IqlDefinition.ObjectType type) {
        return type.name().toLowerCase(Locale.ROOT);
    }
}
