/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.engine;

import dev.jstech.computers.storage.StorageKey;
import org.jetbrains.annotations.Nullable;

/**
 * Something somebody wants made, as it reaches the network's engine.
 *
 * @param key              what to make
 * @param demand           how much of it
 * @param partial          whether making less than all of it, when the network cannot make it all, is still wanted
 * @param label            who asked, as the Operations log names them
 * @param onSettle         run when the work ends, however it ends; {@code null} when nobody waits on it
 * @param recipe           which of the network's recipes for it to use, by its place in the list a craft dialog
 *                         shows; {@link #ANY_RECIPE} lets the engine choose
 * @param preferMultiStage when the engine chooses and a multi-stage recipe and the single steps both make it,
 *                         whether the multi-stage recipe wins
 */
public record CraftRequest(StorageKey key, long demand, boolean partial, String label, @Nullable Runnable onSettle,
                           int recipe, boolean preferMultiStage) {

    /** The recipe index that leaves the choice to the engine. */
    public static final int ANY_RECIPE = -1;

    /**
     * The most one craft asks for. No step of its plan runs more often than this, so what a step needs, an amount
     * times its runs, stays within a long whatever asked for it: a player's screen, IQL, a program or a Gateway.
     */
    public static final long MOST_DEMAND = Integer.MAX_VALUE;

    public CraftRequest {
        demand = Math.min(demand, MOST_DEMAND);
    }

    /** A request the engine plans its own way, preferring a multi-stage recipe where there is one. */
    public static CraftRequest of(final StorageKey key, final long demand, final boolean partial, final String label,
                                  @Nullable final Runnable onSettle) {
        return new CraftRequest(key, demand, partial, label, onSettle, ANY_RECIPE, true);
    }

    /** The same request, made with the recipe at {@code index} of the network's list for the item. */
    public CraftRequest withRecipe(final int index) {
        return new CraftRequest(key, demand, partial, label, onSettle, index, preferMultiStage);
    }

    /** The same request, choosing whether a multi-stage recipe wins over the single steps that make the same. */
    public CraftRequest preferringMultiStage(final boolean prefer) {
        return new CraftRequest(key, demand, partial, label, onSettle, recipe, prefer);
    }
}
