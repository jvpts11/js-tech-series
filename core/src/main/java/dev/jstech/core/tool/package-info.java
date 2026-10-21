/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */

/**
 * Tools for any mod: what an item declared as a tool mines besides the block it breaks, what it builds, what it works
 * on, how it heats, how long it waits between uses and how long it charges; and the rules every tool follows alike.
 *
 * <p>A tool is an item declared with a {@link ToolSpec}: in each of its modes, or in all, it mines a
 * {@link MiningShape} (an area of the face it breaks into, or a vein) or builds by a {@link BuildMode} (a line, a
 * plane, a copy, a paste); it runs on a {@link ToolPower} (energy, a fuel, a gas) and heats as a {@link ToolHeat} says.
 * {@link Tools} keeps the rules: a tool works while it holds what it works on and has not overheated, and otherwise
 * does no more than a bare hand. {@link AreaMining} breaks the rest of an area or a vein, each block as the player
 * would break it, and {@link BuildingTools} places blocks taken from the player's supplies: the inventory first, then
 * each {@link IBuildSupply} a mod adds to {@link BuildSupplies}. A region a tool copies is kept in it as a
 * {@link CopiedRegion}. A mod's own item that acts on use asks {@link Tools#ready} first and tells {@link Tools#used}
 * after; a {@link ChargingItem} does both for an item held in use to charge and let go to act.
 *
 * <p>How to declare one, from zero, is in {@code core/docs/TOOLS.md}. An example that compiles is the test mod's tools
 * ({@code tests/src/main/java/dev/jstech/tests/TestTools.java}): a drill for an area and a vein, a pick that heats, a
 * chainsaw on fuel, a shovel on a gas, a railgun held to charge and a building wand.
 */
@ApiStatus.Experimental
package dev.jstech.core.tool;

import org.jetbrains.annotations.ApiStatus;
