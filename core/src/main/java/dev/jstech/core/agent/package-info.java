/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */

/**
 * What a mod tells an agent playing the game through the Core's MCP server of its own things, beyond what the game
 * shows of every block and entity: a machine working, a computer and its job, a ship's flight with its throttle, its
 * autopilot and the world below. An agent reads the game as the player sees it, and a mod's state drawn on a screen
 * or a HUD is only pixels to it unless the mod tells it.
 *
 * <p>A mod declares each reading once with {@link AgentReadings}: {@code block} for its blocks, {@code entity} for
 * its entities, {@code structure} for the moving structures it flies, each an {@link IBlockReading},
 * {@link IEntityReading} or {@link IStructureReading} that tells an {@link AgentReport} its named numbers, words,
 * flags and parts, and tells nothing of what is not its own. It declares them from its constructor, or from its
 * client's setup for what only the player's game knows, as what a HUD shows. The MCP server adds what they tell under
 * the mod's id, as {@code readings}, wherever the agent meets the thing: the block or the entity the player looks at,
 * the vehicle ridden and the moving structure stood on, in {@code player_state}. A reading that fails is left out and
 * said in the log, so a mod's mistake never costs the agent the rest of its answer.
 *
 * <p>How to use it is in {@code core/docs/DEVELOPING.md}, under the MCP server. An example that compiles is J's
 * Space's reading of a ship's flight
 * ({@code space/src/main/java/dev/jstech/space/client/ship/ShipAgentReadings.java}).
 */
@ApiStatus.Experimental
package dev.jstech.core.agent;

import org.jetbrains.annotations.ApiStatus;
