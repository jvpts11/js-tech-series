/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.tool;

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
import org.jetbrains.annotations.Nullable;

/**
 * Every place the blocks a player builds with may come from, by id: the player's inventory first, then what each mod
 * declares, such as a network's storage. The building tools take from the first that has the block, the inventory
 * before the rest; a Multiblock Builder takes from the one chosen. Only the inventory's plain stacks are taken: a named
 * or changed stack is the player's to keep. A player in creative takes from the inventory without using it up.
 *
 * <pre>{@code
 * BuildSources.declare(ResourceLocation.fromNamespaceAndPath(MODID, "network"), new NetworkSource());
 * }</pre>
 */
public final class BuildSources {

    /** The player's own inventory. */
    public static final ResourceLocation INVENTORY = ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "inventory");
    /* Creative players hold as many of every block as a build may want. */
    private static final long CREATIVE_HOLDS = Integer.MAX_VALUE / 2;
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

    /** How many of {@code item} {@code player} can build with, from every place they may use now. */
    public static long available(final ServerPlayer player, final Item item) {
        long total = 0;
        for (final IBuildSource source : usable(player)) {
            total += source.count(player, item);
            if (total < 0) {
                return Long.MAX_VALUE;
            }
        }
        return total;
    }

    /**
     * Takes one {@code item} for {@code player} to place, from the first place they may use that has one, the
     * inventory first; the place it came from, which takes it back if it cannot stay, or null when none had one.
     */
    @Nullable
    public static IBuildSource takeOne(final ServerPlayer player, final Item item) {
        for (final IBuildSource source : usable(player)) {
            if (!source.take(player, item).isEmpty()) {
                return source;
            }
        }
        return null;
    }

    /* The places {@code player} may take from now, in order. */
    private static synchronized List<IBuildSource> usable(final Player player) {
        final List<IBuildSource> out = new ArrayList<>();
        for (final IBuildSource source : SOURCES.values()) {
            if (source.usable(player)) {
                out.add(source);
            }
        }
        return out;
    }

    /* A stack of {@code item} the inventory may give: one nobody named or changed. */
    private static boolean plain(final ItemStack stack, final Item item) {
        return stack.is(item) && stack.getComponentsPatch().isEmpty();
    }

    /** The player's inventory, which a player in creative takes from without using it up. */
    private static final class InventorySource implements IBuildSource {

        @Override
        public Text name() {
            return ToolTexts.YOUR_INVENTORY.text();
        }

        @Override
        public boolean usable(final Player player) {
            return true;
        }

        @Override
        public long count(final Player player, final Item item) {
            if (player.getAbilities().instabuild) {
                return CREATIVE_HOLDS;
            }
            long count = 0;
            for (final ItemStack stack : player.getInventory().items) {
                if (plain(stack, item)) {
                    count += stack.getCount();
                }
            }
            return count;
        }

        @Override
        public ItemStack take(final ServerPlayer player, final Item item) {
            if (player.getAbilities().instabuild) {
                return new ItemStack(item);
            }
            final Inventory inventory = player.getInventory();
            for (final ItemStack stack : inventory.items) {
                if (plain(stack, item)) {
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
