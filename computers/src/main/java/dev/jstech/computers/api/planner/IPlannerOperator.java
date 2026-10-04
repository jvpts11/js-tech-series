/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.api.planner;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

/**
 * A hint another mod adds to the dialect of an engine that takes extensions: words a statement may end with, and
 * a value after them when the hint takes one ({@code PREFER COMPUTER 'Bench A'}). When a statement uses it, every
 * plan the planner weighs for that statement goes past it.
 */
@ApiStatus.Experimental
public interface IPlannerOperator {

    /** The hint's id. */
    ResourceLocation id();

    /** The words that call it, in capitals, separated by single spaces. */
    String keyword();

    /** Whether a value follows the words: a name in quotes, a number or a single word. */
    boolean takesValue();

    /** What the hint does, in a sentence, as the list of hints shows it. */
    Component description();

    /** Changes one plan the way the hint asks, with the value written after it (empty when it takes none). */
    void apply(String value, PlanCandidate candidate);
}
