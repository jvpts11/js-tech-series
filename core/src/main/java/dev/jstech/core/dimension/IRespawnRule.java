/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.dimension;

import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.portal.DimensionTransition;

/**
 * Where a player who died comes back, for the dimensions a mod says so of: a world where a bed outside a room of air
 * means nothing, a dungeon that sends its dead to its door. The game's own choice stands where no rule says otherwise.
 */
@FunctionalInterface
public interface IRespawnRule {

    /**
     * Where {@code player}, who died in {@code died}, comes back: somewhere of the rule's own, or nothing to leave it
     * to the next rule and at last to the game, whose choice is {@code planned}.
     */
    Optional<DimensionTransition> respawn(ServerPlayer player, ServerLevel died, DimensionTransition planned);
}
