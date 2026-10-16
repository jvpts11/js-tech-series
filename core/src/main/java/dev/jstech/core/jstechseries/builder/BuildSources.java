/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.builder;

import dev.jstech.core.JsCore;
import dev.jstech.core.text.Text;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Every place a Multiblock Builder may take its blocks from, by id: the player's inventory, the Core's own, and what
 * other mods declare, such as a network's storage. A player in creative takes from the inventory without using it up.
 */
public final class BuildSources {

    /** The player's own inventory. */
    public static final ResourceLocation INVENTORY = ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "inventory");
    /* Creative players hold as many of every block as a build may want. */
    private static final int CREATIVE_HOLDS = Integer.MAX_VALUE / 2;
    private static final Map<ResourceLocation, IBuildSource> SOURCES = new LinkedHashMap<>();

    static {
        SOURCES.put(INVENTORY, new InventorySource());
    }

    private BuildSources() {
    }

    /**
     * Declares a place to take the blocks from, from a mod's constructor.
     *
     * @throws IllegalStateException when one of that id is declared already
     */
    public static synchronized void declare(final ResourceLocation id, final IBuildSource source) {
        Objects.requireNonNull(source, "source");
        if (SOURCES.putIfAbsent(id, source) != null) {
            throw new IllegalStateException("a place to build from is declared twice: " + id);
        }
    }

    /** The place of that id, or the player's inventory when there is none of it. */
    public static synchronized IBuildSource get(final ResourceLocation id) {
        return SOURCES.getOrDefault(id, SOURCES.get(INVENTORY));
    }

    /** Every place's id, the inventory first, in the order declared. */
    public static synchronized List<ResourceLocation> ids() {
        return new ArrayList<>(SOURCES.keySet());
    }

    /** The player's inventory, which a player in creative takes from without using it up. */
    private static final class InventorySource implements IBuildSource {

        @Override
        public Text name() {
            return BuilderTexts.INVENTORY.text();
        }

        @Override
        public boolean usable(final Player player) {
            return true;
        }

        @Override
        public int count(final Player player, final Item item) {
            return player.getAbilities().instabuild ? CREATIVE_HOLDS : player.getInventory().countItem(item);
        }

        @Override
        public ItemStack take(final ServerPlayer player, final Item item) {
            if (player.getAbilities().instabuild) {
                return new ItemStack(item);
            }
            final Inventory inventory = player.getInventory();
            for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                final ItemStack stack = inventory.getItem(slot);
                if (stack.is(item)) {
                    return stack.split(1);
                }
            }
            return ItemStack.EMPTY;
        }

        @Override
        public void giveBack(final ServerPlayer player, final ItemStack stack) {
            if (!stack.isEmpty() && !player.getAbilities().instabuild && !player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        }
    }
}
