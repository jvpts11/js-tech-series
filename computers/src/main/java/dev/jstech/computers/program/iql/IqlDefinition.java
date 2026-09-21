/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.iql;

import dev.jstech.core.id.IStableId;
import dev.jstech.core.id.StableIds;

/**
 * A parsed IQL Layer-2 statement: a CREATE/DROP of a saved object (a view, a procedure, or a job) or an
 * EXEC of a procedure. Where an {@link IqlOperation} is one immediate action, an {@code IqlDefinition}
 * names an object the IQL Engine stores and later runs. Pure logic, no Minecraft.
 *
 * <p>{@code body} is the raw text after {@code AS} (a query for a view, a {@code { ... }} block for a
 * procedure, a statement or procedure name for a job), kept verbatim and re-parsed when the object runs,
 * so the catalog persists exactly what the player wrote. {@code triggerKind}/{@code triggerSpec} carry a
 * job's {@code EVERY <duration>} or {@code WHEN <condition>}; both are empty/NONE for views and procedures.
 */
public record IqlDefinition(Verb verb, ObjectType objectType, String name, String body,
                            TriggerKind triggerKind, String triggerSpec) {

    /** What the statement does to the catalog. */
    public enum Verb { CREATE, DROP, EXEC }

    /** The kind of saved object. {@code NONE} only for EXEC, which names a procedure. */
    public enum ObjectType implements IStableId {
        VIEW(0),
        PROCEDURE(1),
        JOB(2),
        NONE(3);

        private static final StableIds<ObjectType> IDS = StableIds.of(ObjectType.class);

        private final int id;

        ObjectType(final int id) {
            this.id = id;
        }

        @Override
        public int id() {
            return id;
        }

        /** The type that declares {@code id}; an id no type declares reads as {@link #NONE}. */
        public static ObjectType byId(final int id) {
            return IDS.byId(id, NONE);
        }
    }

    /** A job's trigger family: a fixed interval, a condition, or none (views/procedures). */
    public enum TriggerKind implements IStableId {
        NONE(0),
        EVERY(1),
        WHEN(2);

        private static final StableIds<TriggerKind> IDS = StableIds.of(TriggerKind.class);

        private final int id;

        TriggerKind(final int id) {
            this.id = id;
        }

        @Override
        public int id() {
            return id;
        }

        /** The trigger that declares {@code id}; an id no trigger declares reads as {@link #NONE}. */
        public static TriggerKind byId(final int id) {
            return IDS.byId(id, NONE);
        }
    }

    public IqlDefinition {
        if (name == null) {
            name = "";
        }
        if (body == null) {
            body = "";
        }
        if (triggerSpec == null) {
            triggerSpec = "";
        }
    }

    public static IqlDefinition create(final ObjectType type, final String name, final String body,
                                       final TriggerKind triggerKind, final String triggerSpec) {
        return new IqlDefinition(Verb.CREATE, type, name, body, triggerKind, triggerSpec);
    }

    public static IqlDefinition drop(final ObjectType type, final String name) {
        return new IqlDefinition(Verb.DROP, type, name, "", TriggerKind.NONE, "");
    }

    public static IqlDefinition exec(final String name) {
        return new IqlDefinition(Verb.EXEC, ObjectType.PROCEDURE, name, "", TriggerKind.NONE, "");
    }

    public boolean hasTrigger() {
        return triggerKind != TriggerKind.NONE;
    }
}
