/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.tool;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;

/**
 * Somewhere a building tool takes the blocks it places from, besides the player's inventory: J's Computers' network
 * storage, reached through what links the player to it. A mod adds one with {@link BuildSupplies#add}; the tools ask
 * the inventory first, then each supply in the order they were added.
 */
public interface IBuildSupply {

    /** How many of {@code item} it can give {@code player} now. */
    long available(ServerPlayer player, Item item);

    /** Takes up to {@code count} of {@code item} for {@code player}; says how many it took, or would take. */
    long take(ServerPlayer player, Item item, long count, boolean simulate);
}
