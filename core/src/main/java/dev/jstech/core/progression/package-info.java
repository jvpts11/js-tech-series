/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */

/**
 * Progression for any mod: the axes a player advances along (each registered by the mod that counts by it; the series
 * registers its eras of hardware and tiers of industry), how far each player and each team has come along each, the
 * milestones a team passes and the knowledge it gains, and the gates a mod puts on what it offers.
 *
 * <p>An axis is a {@link ProgressionAxis} of steps ({@link IAxisStep}), registered once from the mod's constructor
 * with {@link ProgressionAxes}. {@link PlayerProgress} moves a player along it, only ever forward, kept with the world
 * and sent to the player, and posts an {@link AxisStepReachedEvent} once as they arrive; {@link ProgressionTriggers}
 * says what moves them the first time they make an item or place a block; and a data file gates something on a step
 * with a {@link ProgressionGate}. What a team reaches is {@link TeamProgress}: its furthest step along each axis, the
 * {@link Milestone}s it passed, declared in order with {@link Milestones}, and how much it knows of each
 * {@link Knowledge}, declared with {@link Knowledges}; a player who joins a team has all it has, and two teams that
 * merge keep everything either had. A recipe may ask for a piece of knowledge, and then runs only for an owner whose
 * team knows it ({@link KnowledgeGate}), while the recipe viewers and the manuals still show it, with a padlock.
 *
 * <p>How to use it, from zero, is in {@code core/docs/PROGRESSION.md}. An example that compiles is the test mod's own
 * era, axis, milestones and knowledge ({@code tests/src/main/java/dev/jstech/tests/TestProgression.java}).
 */
@ApiStatus.Experimental
package dev.jstech.core.progression;

import org.jetbrains.annotations.ApiStatus;
