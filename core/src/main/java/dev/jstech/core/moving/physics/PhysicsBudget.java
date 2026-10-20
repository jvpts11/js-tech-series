/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving.physics;

import dev.jstech.core.moving.StructureBalance;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerLevel;

/**
 * How much of this tick the physics of a dimension's structures has taken, against how much it may
 * ({@link StructureBalance#physicsMillis()}): past it, a structure waits a tick, as it is, and goes the next whatever
 * the budget, so a great many structures slow down together rather than slow the server. On the server's thread.
 */
final class PhysicsBudget {

    private long tick = Long.MIN_VALUE;
    private long spent;

    private static final Map<ServerLevel, PhysicsBudget> LEVELS = new WeakHashMap<>();
    private static final double NANOS_PER_MILLI = 1_000_000.0;

    /** The budget of {@code level}, its spending begun anew each tick. */
    static PhysicsBudget of(final ServerLevel level) {
        final PhysicsBudget budget = LEVELS.computeIfAbsent(level, ignored -> new PhysicsBudget());
        final long now = level.getGameTime();
        if (budget.tick != now) {
            budget.tick = now;
            budget.spent = 0;
        }
        return budget;
    }

    /** Whether this tick's budget is spent. */
    boolean spent() {
        return spent >= StructureBalance.physicsMillis() * NANOS_PER_MILLI;
    }

    /** Counts {@code nanos} more spent this tick. */
    void spend(final long nanos) {
        spent += Math.max(0, nanos);
    }
}
