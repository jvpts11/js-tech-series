/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.builder;

import dev.jstech.core.text.Text;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Where a Multiblock Builder takes the blocks it places from: the player's inventory, or, from a mod that has one, a
 * store such as a network's. Counted on either side, so the builder's display says what it holds; taken from on the
 * server only, one block at a time, as each is placed.
 */
public interface IBuildSource {

    /** What the builder's display calls it. */
    Text name();

    /** Whether {@code player} may take blocks from it now. */
    boolean usable(Player player);

    /** How many of {@code item} it holds for {@code player}. */
    int count(Player player, Item item);

    /** Takes one of {@code item} for {@code player}, as a stack to place; an empty stack when it has none. */
    ItemStack take(ServerPlayer player, Item item);

    /** Takes back what {@link #take} gave and was not placed. */
    void giveBack(ServerPlayer player, ItemStack stack);
}
