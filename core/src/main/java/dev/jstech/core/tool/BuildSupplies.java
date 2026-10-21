/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.tool;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Where building tools take the blocks they place from: the player's inventory first, then every supply a mod added,
 * in the order they were added. A player in creative takes nothing, as a block placed by hand costs nothing there.
 */
public final class BuildSupplies {

    private static final List<IBuildSupply> SUPPLIES = new CopyOnWriteArrayList<>();
    /* The player's own inventory, asked before any supply. */
    private static final IBuildSupply INVENTORY = new Inventory();

    private BuildSupplies() {
    }

    /** Adds {@code supply}, from the constructor of the mod that brings it. */
    public static void add(final IBuildSupply supply) {
        SUPPLIES.add(supply);
    }

    /** How many of {@code item} {@code player} can place: as many as wanted in creative. */
    public static long available(final ServerPlayer player, final Item item) {
        if (player.getAbilities().instabuild) {
            return Long.MAX_VALUE;
        }
        long total = INVENTORY.available(player, item);
        for (final IBuildSupply supply : SUPPLIES) {
            total += supply.available(player, item);
            if (total < 0) {
                return Long.MAX_VALUE;
            }
        }
        return total;
    }

    /** Takes one {@code item} for {@code player} to place, wherever it is first found; whether there was one. */
    public static boolean takeOne(final ServerPlayer player, final Item item) {
        if (player.getAbilities().instabuild) {
            return true;
        }
        if (INVENTORY.take(player, item, 1, false) == 1) {
            return true;
        }
        for (final IBuildSupply supply : SUPPLIES) {
            if (supply.take(player, item, 1, false) == 1) {
                return true;
            }
        }
        return false;
    }

    /** Gives back one {@code item} taken for a block that could not stay: into the inventory, else at the feet. */
    public static void giveBack(final ServerPlayer player, final Item item) {
        if (!player.getAbilities().instabuild) {
            final ItemStack back = new ItemStack(item);
            if (!player.getInventory().add(back)) {
                player.drop(back, false);
            }
        }
    }

    /* The stacks of the player's inventory, the plain ones of an item: a named or changed stack is kept. */
    private static final class Inventory implements IBuildSupply {

        @Override
        public long available(final ServerPlayer player, final Item item) {
            long count = 0;
            for (final ItemStack stack : player.getInventory().items) {
                if (plain(stack, item)) {
                    count += stack.getCount();
                }
            }
            return count;
        }

        @Override
        public long take(final ServerPlayer player, final Item item, final long count, final boolean simulate) {
            long taken = 0;
            for (final ItemStack stack : player.getInventory().items) {
                if (taken >= count) {
                    break;
                }
                if (plain(stack, item)) {
                    final int now = (int) Math.min(stack.getCount(), count - taken);
                    if (!simulate) {
                        stack.shrink(now);
                    }
                    taken += now;
                }
            }
            return taken;
        }

        private static boolean plain(final ItemStack stack, final Item item) {
            return stack.is(item) && stack.getComponentsPatch().isEmpty();
        }
    }
}
