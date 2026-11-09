/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.tool;

import dev.jstech.core.text.Text;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Somewhere the blocks a player builds with come from: the player's inventory, or, from a mod that has one, a store
 * such as a network's. The building tools take from the inventory first and then from each source the player may
 * use, in the order declared; a Multiblock Builder takes from the one its player chose. Whether it is usable is asked
 * on either side, so a screen lists it; it is counted and taken from on the server, a block at a time as each is
 * placed, and the server tells a Multiblock Builder's display what it holds, so a source need not count on the
 * player's game.
 */
public interface IBuildSource {

    /** What a screen calls it. */
    Text name();

    /** Whether {@code player} may take blocks from it now. */
    boolean usable(Player player);

    /** How many of {@code item} it holds for {@code player}; none while they may not use it. */
    long count(Player player, Item item);

    /**
     * Takes one of {@code item} for {@code player}, as a stack to place; an empty stack when it has none, or while they
     * may not use it.
     */
    ItemStack take(ServerPlayer player, Item item);

    /**
     * Takes back what {@link #take} gave and was not placed, or a block taken out of the world; what it cannot take
     * goes to the player, so nothing is lost.
     */
    void giveBack(ServerPlayer player, ItemStack stack);
}
