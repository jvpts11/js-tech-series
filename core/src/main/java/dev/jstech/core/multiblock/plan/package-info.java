/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */

/**
 * Building by a plan: a structure laid out ahead, place by place, as a hangar keeps the rocket it builds, rather than
 * found by a pattern round a terminal. A plan ({@link BuildPlan}) says, for each of its places ({@link PlanSpot}),
 * the block wanted there, the part of the plan it belongs to, and which block of that part's formed model it merges as;
 * the blocks of other parts' models whose faces reach into it ({@link PlanReach}) shape it too while those parts are
 * formed.
 *
 * <p>Its owner, the block that keeps the plan, starts it ({@link PlannedBuild#start}): it counts what is in place
 * once, and from then on the world's index of structures tells it of each place that changes, so a block put down or
 * taken away changes its part's count by one, in the same short time however large the plan. A part forms by itself
 * once nothing of it is missing, its blocks merging into its formed model with no terminal of its own and never looking
 * for one; it comes apart as soon as one of its blocks goes. A block that keeps something of its own does not merge,
 * and is told as its part forms ({@link IPlanMember}). The counts are there for whatever shows or builds the plan, and
 * the owner draws the formed parts. The owner may hand the plan on ({@link PlannedBuild#handTo}), as a rocket takes its
 * own when it leaves the hangar.
 *
 * <p>A part's model is declared laid ({@code FormedShapes.declareLaid}) with its own blocks and the blocks of the parts
 * beside it its faces may reach into, and the data run writes its shapes as for a pattern's model.
 *
 * <p>How to use it is in {@code core/docs/MULTIBLOCKS.md}, under "Built by a plan". The game tests of the test mod
 * ({@code tests/src/main/java/dev/jstech/tests/gametest/core/BuildPlanGameTests.java}) are a working example, and
 * J's Space's Rocket Hangar builds its rockets this way.
 */
@ApiStatus.Experimental
package dev.jstech.core.multiblock.plan;

import org.jetbrains.annotations.ApiStatus;
