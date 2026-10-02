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
import dev.jstech.computers.program.iql.IqlBusStatement;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jetbrains.annotations.Nullable;

/**
 * The runtime of the network's language, as an engine that speaks it runs it: it takes a statement and either runs
 * it as an immediate action/query (through the two-method view it is handed) or, for a Layer-2 statement,
 * stores/runs a saved object against the Mainframe's catalog. CREATE/DROP touch the catalog; EXEC runs a procedure's
 * statements in order, stopping at the first error (the chosen default); a QUERY whose object is a view name runs
 * the saved query. Touching the catalog needs an engine that keeps views and procedures; ad-hoc actions do not.
 */
@TextHolder
public final class IqlEngine {

    private final MainframeBlockEntity mainframe;
    private final IIqlView computer;
    private final int queryRowLimit;
    /** Whether the engine running this keeps views and procedures; one that does not refuses Layer 2. */
    private final boolean savedObjects;

    private static final int RECURSION_GUARD = 32;

    private static final TextKey TOO_DEEP = TextKey.of("jsc.service.iql.too_deep",
            "IQL recursion too deep (a procedure or view referencing itself?)");
    private static final TextKey SYNTAX = TextKey.of("jsc.service.iql.syntax", "syntax: %s");
    private static final TextKey NO_SAVED_OBJECTS = TextKey.of("jsc.service.iql.no_saved_objects",
            "the network's engine keeps no views, procedures or jobs");
    private static final TextKey GUARD_NOT_MET =
            TextKey.of("jsc.service.iql.guard_not_met", "the IF did not hold, so the %s was not run");
    /** Why a job is not made: jobs are the Automation Engine's, whichever engine plans the network's work. */
    public static final TextKey JOBS_NEED_AUTOMATION = TextKey.of("jsc.service.iql.jobs_need_automation",
            "jobs are kept by the Automation Engine; install it on the Mainframe first");
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

    /* The job whose body is running now, on the server's thread, so what it sets on a bus is marked with its name. */
    private static final ThreadLocal<String> RUNNING_JOB = new ThreadLocal<>();

    public IqlEngine(final MainframeBlockEntity mainframe, final IIqlView computer, final int queryRowLimit,
                     final boolean savedObjects) {
        this.mainframe = mainframe;
        this.computer = computer;
        this.queryRowLimit = queryRowLimit;
        this.savedObjects = savedObjects;
    }

    /** A computer seen as what the engine needs: one way to read, one way to act. */
    public static IIqlView viewOf(final ICliComputer computer) {
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

    /** Runs {@code body} as the job named {@code job}: what it sets on a bus is marked with the job's name. */
    public static <T> T asJob(final String job, final Supplier<T> body) {
        final String outer = RUNNING_JOB.get();
        RUNNING_JOB.set(job);
        try {
            return body.get();
        } finally {
            if (outer == null) {
                RUNNING_JOB.remove();
            } else {
                RUNNING_JOB.set(outer);
            }
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
        if (parsed.isBus()) {
            final String job = RUNNING_JOB.get();
            return IqlBusSetter.apply(mainframe, parsed.bus(), job == null ? IqlBusSetter.TYPED : job);
        }
        return runOperation(parsed.operation(), depth);
    }

    private Outcome runDefinition(final IqlDefinition definition, final int depth) {
        if (!savedObjects) {
            return Outcome.fail(NO_SAVED_OBJECTS.text());
        }
        return switch (definition.verb()) {
            case CREATE -> create(definition);
            case DROP -> drop(definition);
            case EXEC -> runProcedure(definition.name(), depth);
        };
    }

    private Outcome create(final IqlDefinition definition) {
        /*
         * A job that switches a bus on between two hours of the day is the bus's own hours, kept on the bus and marked
         * with the job's name: the bus moves only then, which a job firing once as the hours begin could not say.
         */
        final IqlBusStatement hours = busHours(definition);
        if (hours != null) {
            return IqlBusSetter.apply(mainframe, hours, definition.name());
        }
        // A job is the Automation Engine's to keep and fire; the statement only asks for one.
        if (definition.objectType() == IqlDefinition.ObjectType.JOB && !mainframe.isAutomationEngineInstalled()) {
            return Outcome.fail(JOBS_NEED_AUTOMATION.text());
        }
        mainframe.iqlCatalog().put(IqlSavedObject.from(definition));
        mainframe.markIqlCatalogChanged();
        // A job fires later with nobody at the keyboard; it is credited to whoever set it up.
        if (definition.objectType() == IqlDefinition.ObjectType.JOB) {
            Acting.current().ifPresent(player -> MachineOperators.note(mainframe, player));
        }
        return Outcome.ok(CREATED.with(typeName(definition.objectType()), definition.name()));
    }

    /* {@code CREATE JOB n AS SET BUS 'x' ON WHEN TIME BETWEEN a AND b} as the bus's hours, or null for any other. */
    @Nullable
    private static IqlBusStatement busHours(final IqlDefinition definition) {
        if (definition.objectType() != IqlDefinition.ObjectType.JOB
                || definition.triggerKind() != IqlDefinition.TriggerKind.WHEN
                || !IqlBusStatement.isSetBus(definition.body())) {
            return null;
        }
        final int[] hours = IqlBusStatement.hoursOf(definition.triggerSpec());
        if (hours == null) {
            return null;
        }
        try {
            final IqlBusStatement body = IqlBusStatement.parse(definition.body());
            return body.change() instanceof IqlBusStatement.Power power && power.on()
                    ? new IqlBusStatement(body.bus(), new IqlBusStatement.Hours(hours[0], hours[1])) : null;
        } catch (final IllegalArgumentException e) {
            return null;
        }
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
            final boolean ordered = !operation.orderBy().isEmpty();
            /*
             * Pass the whole WHERE so the read filters on every field (qty/name/damaged/...), not just name. An
             * ORDER BY sorts every row there is before the LIMIT takes the first ones, so the limit is applied after.
             */
            final List<ICliComputer.StoredItem> rows =
                    computer.queryObject(operation.item(), operation.where(), "", ordered ? queryRowLimit : limit);
            return Outcome.rows(ordered ? ordered(rows, operation).stream().limit(limit).toList() : rows);
        }
        // The IF guard decides whether the action runs at all, read against the network's holding of its item.
        if (operation.guard() != null && !operation.guard().matches(guardRow(operation))) {
            return Outcome.ok(GUARD_NOT_MET.with(operation.verb().name()));
        }
        final ICliComputer.OpResult result = computer.execute(operation);
        return new Outcome(result.ok(), result.message(), List.of());
    }

    /** The rows sorted by the statement's ORDER BY: quantity as a number, name and item as words. */
    private static List<ICliComputer.StoredItem> ordered(final List<ICliComputer.StoredItem> rows,
                                                         final IqlOperation operation) {
        final Comparator<ICliComputer.StoredItem> order = switch (operation.orderBy().toLowerCase(Locale.ROOT)) {
            case "qty", "count", "amount", "quantity" -> Comparator.comparingLong(ICliComputer.StoredItem::quantity);
            case "name", "item" -> Comparator.comparing(row -> row.name().english().toLowerCase(Locale.ROOT));
            default -> null;
        };
        if (order == null) {
            return rows;
        }
        final List<ICliComputer.StoredItem> sorted = new ArrayList<>(rows);
        sorted.sort(operation.orderByDescending() ? order.reversed() : order);
        return sorted;
    }

    /**
     * What an IF guard reads: the item the statement names, as the network holds it ({@code qty} is all of it, every
     * variant counted), or the whole network's holding for a {@code *}.
     */
    private Function<String, String> guardRow(final IqlOperation operation) {
        final String item = operation.isAnyItem() ? null
                : operation.item().substring(operation.item().indexOf(':') + 1);
        final IIqlCondition only = item == null ? null
                : new IIqlCondition.Comparison("item", IIqlCondition.Op.EQ, item);
        final List<ICliComputer.StoredItem> rows = computer.queryObject("items", only, "", queryRowLimit);
        final long total = rows.stream().mapToLong(ICliComputer.StoredItem::quantity).sum();
        final String name = rows.isEmpty() ? operation.item() : rows.get(0).name().english();
        return field -> switch (field.toLowerCase(Locale.ROOT)) {
            case "qty", "count", "amount" -> Long.toString(total);
            case "item" -> item == null ? IqlOperation.ANY_ITEM : item;
            case "name" -> name;
            default -> null;
        };
    }

    private static String typeName(final IqlDefinition.ObjectType type) {
        return type.name().toLowerCase(Locale.ROOT);
    }
}
