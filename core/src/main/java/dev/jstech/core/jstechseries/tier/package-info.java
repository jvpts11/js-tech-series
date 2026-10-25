/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */

/**
 * The two ladders the series counts progress by: the hardware eras, Vintage to Singularity, the generation of the
 * computers a player builds; and the industrial tiers, T0 to T9, how far a player's industry has come. They are two
 * ladders on purpose, and nothing derives one from the other: a player can run an Advanced computer on a T2 industry.
 *
 * <p>Each is an enum of steps: {@link Era} and {@link IndustrialTier}. A step has a level counted from 0
 * without gaps, which is also its stable id, so saves, packets and menu data carry it; a name data files write it by
 * ({@code legacy}, {@code t3}); and the name a player reads ("Legacy era", "T3"). Both are steps of an axis
 * ({@link IAxisStep}), so a player moves along them as along any other axis of the progression kit. An era also says
 * what its hardware is: the word size of its processors, what an item costs on its disks, and the colour its screens
 * are remembered by. A block state keeps an era as its level, in an {@code IntegerProperty}, and reads it back with
 * {@code HardwareEra.fromLevel}, so the enum stays free of the game and is tested on its own. The eras an add-on
 * brings, after the last or between two, are in the registry of eras ({@link Eras}).
 *
 * <p>How the two ladders are used is in {@code core/docs/PROGRESSION.md}. Examples that compile are the test mod's
 * era put after Advanced ({@code tests/src/main/java/dev/jstech/tests/TestProgression.java}) and its kind of Operation
 * that T1 can run ({@code tests/src/main/java/dev/jstech/tests/TestNetworkMembers.java}).
 */
@ApiStatus.Experimental
package dev.jstech.core.jstechseries.tier;

import dev.jstech.core.progression.IAxisStep;
import org.jetbrains.annotations.ApiStatus;
