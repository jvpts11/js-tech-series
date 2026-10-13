/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.engine;

import dev.jstech.computers.crafting.CraftPlanner;
import dev.jstech.computers.crafting.CraftingPattern;
import dev.jstech.computers.crafting.ProcessingPattern;
import dev.jstech.computers.storage.StorageKey;
import java.util.List;
import java.util.Map;

/**
 * How an engine works out a craft's plan before anything is made: what a craft dialog, the Craft Planner and the
 * Network Interactor's catalog show.
 */
public interface ICraftPlanning {

    /** The planner the Operations core itself plans with: bench and machine patterns, the first that fits wins. */
    ICraftPlanning REFERENCE = new ICraftPlanning() {
        @Override
        public CraftPlanner.Plan plan(final StorageKey key, final long quantity, final List<CraftingPattern> patterns,
                                      final List<ProcessingPattern> machines, final Map<StorageKey, Long> stock) {
            return CraftPlanner.plan(key, quantity, patterns, machines, stock);
        }

        @Override
        public long maxFeasible(final StorageKey key, final long quantity, final List<CraftingPattern> patterns,
                                final List<ProcessingPattern> machines, final Map<StorageKey, Long> stock) {
            return CraftPlanner.maxFeasible(key, quantity, patterns, machines, stock);
        }
    };

    /** The plan for {@code quantity} of {@code key} from these patterns and this stock. */
    CraftPlanner.Plan plan(StorageKey key, long quantity, List<CraftingPattern> patterns,
                           List<ProcessingPattern> machines, Map<StorageKey, Long> stock);

    /** The most of {@code key}, up to {@code quantity}, these patterns and this stock can make. */
    long maxFeasible(StorageKey key, long quantity, List<CraftingPattern> patterns, List<ProcessingPattern> machines,
                     Map<StorageKey, Long> stock);

    /** The plan with bench patterns alone. */
    default CraftPlanner.Plan plan(final StorageKey key, final long quantity, final List<CraftingPattern> patterns,
                                   final Map<StorageKey, Long> stock) {
        return plan(key, quantity, patterns, List.of(), stock);
    }
}
