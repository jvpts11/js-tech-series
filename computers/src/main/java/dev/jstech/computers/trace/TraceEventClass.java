/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.trace;

import dev.jstech.core.id.IStableId;
import dev.jstech.core.id.StableIds;
import org.jetbrains.annotations.Nullable;

/**
 * What the IQL Server Profiler can see happen on a network, each in one of the five groups a trace picks from:
 * statements, Operations, locks, plans and buses. The name a profiler shows for each is its own, written as one word
 * the way a profiler writes its event classes.
 */
public enum TraceEventClass implements IStableId {

    STATEMENT_STARTING(1, "StatementStarting", Group.STATEMENTS),
    STATEMENT_COMPLETED(2, "StatementCompleted", Group.STATEMENTS),
    OPERATION_CREATED(3, "OperationCreated", Group.OPERATIONS),
    OPERATION_SETTLED(4, "OperationSettled", Group.OPERATIONS),
    LOCK_ACQUIRED(5, "LockAcquired", Group.LOCKS),
    PLAN_CHOSEN(6, "PlanChosen", Group.PLANS),
    BUS_MOVED(7, "BusMoved", Group.BUSES);

    private static final StableIds<TraceEventClass> IDS = StableIds.of(TraceEventClass.class);

    private final int id;
    private final String eventName;
    private final Group group;

    TraceEventClass(final int id, final String eventName, final Group group) {
        this.id = id;
        this.eventName = eventName;
        this.group = group;
    }

    /** The groups a trace picks its events from, each a bit of a trace's mask. */
    public enum Group {
        STATEMENTS(1),
        OPERATIONS(2),
        LOCKS(4),
        PLANS(8),
        BUSES(16);

        /** Every group at once. */
        public static final int ALL = 31;

        private final int bit;

        Group(final int bit) {
            this.bit = bit;
        }

        /** The bit this group sets in a trace's mask. */
        public int bit() {
            return bit;
        }

        /** Whether {@code mask} picks this group. */
        public boolean in(final int mask) {
            return (mask & bit) != 0;
        }
    }

    /** The class numbered {@code id}, or null for none. */
    @Nullable
    public static TraceEventClass byId(final int id) {
        return IDS.find(id);
    }

    @Override
    public int id() {
        return id;
    }

    /** What a profiler calls it. */
    public String eventName() {
        return eventName;
    }

    /** The group a trace picks it with. */
    public Group group() {
        return group;
    }
}
