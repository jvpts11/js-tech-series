/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.crafting;

import dev.jstech.computers.storage.StorageKey;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Turns a craft request into the plan the craft runs: the full quantity when the network can make it, else
 * (when the requester allows a partial) as much as it can. Pure CPU over immutable inputs (the patterns,
 * the machines and a stock snapshot) so it runs the same on the main thread or on a virtual thread.
 */
public final class CraftPlanning {

    private CraftPlanning() {
    }

    /** A feasible plan and the quantity it makes ({@code target} is the request, or less for a partial). */
    public record Planned(CraftPlanner.Plan plan, long target) {
    }

    /**
     * A plan and the way it is to be carried out, as an engine with a planner of its own decides both.
     *
     * @param planned the plan, or null when there is none to run
     * @param routing where its raw materials come from and how much of it runs at once
     */
    public record Routed(@Nullable Planned planned, CraftRouting routing) {
    }

    /**
     * Plans {@code demand} of {@code key}. Returns null when nothing on the network makes it, or when the
     * full quantity cannot be made and {@code partial} is off (or nothing at all can be made).
     */
    @Nullable
    public static Planned plan(final StorageKey key, final long demand, final boolean partial,
                               final List<CraftingPattern> patterns, final List<ProcessingPattern> machines,
                               final Map<StorageKey, Long> stock) {
        long target = demand;
        CraftPlanner.Plan plan = CraftPlanner.plan(key, target, patterns, machines, stock);
        if (plan.steps().isEmpty()) {
            return null; // no pattern on the network produces this item
        }
        if (!plan.feasible()) {
            if (!partial) {
                return null;
            }
            target = CraftPlanner.maxFeasible(key, demand, patterns, machines, stock);
            if (target <= 0) {
                return null;
            }
            plan = CraftPlanner.plan(key, target, patterns, machines, stock);
        }
        return new Planned(plan, target);
    }

    /**
     * {@code patterns} as a plan reads them where the player picked how {@code key} itself is made: {@code bench} the
     * only bench pattern making it, and none at all where a {@code machine} was picked. All of them where neither was.
     */
    public static List<CraftingPattern> benchesFor(final StorageKey key, @Nullable final CraftingPattern bench,
                                                   @Nullable final ProcessingPattern machine,
                                                   final List<CraftingPattern> patterns) {
        if (bench == null && machine == null) {
            return patterns;
        }
        final List<CraftingPattern> out = new ArrayList<>();
        if (bench != null) {
            out.add(bench);
        }
        for (final CraftingPattern pattern : patterns) {
            if (!key.equals(StorageKey.of(pattern.result()))) {
                out.add(pattern);
            }
        }
        return out;
    }

    /** The same for the machine patterns: {@code machine} the only one making {@code key}, none where a bench was. */
    public static List<ProcessingPattern> machinesFor(final StorageKey key, @Nullable final CraftingPattern bench,
                                                      @Nullable final ProcessingPattern machine,
                                                      final List<ProcessingPattern> machines) {
        if (bench == null && machine == null) {
            return machines;
        }
        final List<ProcessingPattern> out = new ArrayList<>();
        if (machine != null) {
            out.add(machine);
        }
        for (final ProcessingPattern other : machines) {
            final ProcessingPattern.ProcessingOutput made = other.primaryOutput();
            if (made == null || !key.equals(made.key())) {
                out.add(other);
            }
        }
        return out;
    }
}
