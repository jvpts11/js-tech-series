/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */

/**
 * States of matter that are neither solid nor liquid, for any mod: gases, supercritical fluids, slurries, plasma and
 * exotic matter, each kept in tanks of its own, offered by a capability of its own and moved along lines of its own,
 * so a gas line never finds a slurry. Solids are items, and liquids the game's fluids.
 *
 * <p>An amount of matter is a {@link MatterStack}: a material of the catalogue and how much of it, in millibuckets at
 * the pressure of the open air. A block keeps a {@link MatterTank}, which holds one material at a time, its volume
 * times its pressure, saves it with its block entity, and offers it as an {@link IMatterHandler} by the capability
 * of its state in {@link MatterCapabilities}; a {@link FaceMatterHandler} gives it out through one face, filled only
 * through a face that lets things in, with what its filter lets pass, and emptied only through one that lets things
 * out. A {@link ContainmentTank} holds plasma or exotic matter only while its block pays its upkeep each tick: without
 * it plasma dissipates, and exotic matter stays sealed in. {@link MatterRelease} lets matter out where it is lost,
 * and never exotic matter. A cable on the grid of a state is that state's line: each dimension's {@link LevelMatter},
 * found through {@link MatterGrids}, moves it after the machines have ticked, shared among its ends as fluids are,
 * between the Core's stores and the stores of other mods, whose chemicals and gas fluids {@link MatterBridges} links
 * to the catalogue's materials, by id.
 *
 * <p>How matter is kept and moved, from zero, is in {@code core/docs/ENERGY_AND_RESOURCES.md}. Examples that compile
 * are the test mod's tanks of gas and slurry ({@code tests/src/main/java/dev/jstech/tests/TestMatterEnds.java}) and
 * its gas and slurry lines ({@code tests/src/main/java/dev/jstech/tests/TestCableTypes.java}), and the Core's own
 * containment of exotic matter
 * ({@code core/src/main/java/dev/jstech/core/jstechseries/containment/ExoticContainment.java}).
 */
@ApiStatus.Experimental
package dev.jstech.core.matter;

import org.jetbrains.annotations.ApiStatus;
