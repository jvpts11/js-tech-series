/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.api.planner;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

/**
 * One step of a craft's plan, as a planner's extensions see it: a recipe run a number of times.
 *
 * @param item     what the step makes, by the item's id
 * @param runs     how many times its recipe runs
 * @param made     how many of the item those runs make
 * @param machine  whether a machine runs it, rather than a computer at a bench
 * @param estimate how long the planner reckons it takes, in ticks
 */
@ApiStatus.Experimental
public record PlanStep(ResourceLocation item, long runs, long made, boolean machine, long estimate) {
}
