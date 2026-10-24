/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.progression;

import dev.jstech.core.team.CoreTeams;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * The knowledge gate, written once for every mod: something that asks for a piece of knowledge runs only for an owner
 * whose team knows it. A recipe is locked for running, never for seeing: the recipe viewers and the manuals always show
 * it, with a lock while the player's team does not know what it needs.
 */
public final class KnowledgeGate {

    /** The player at the crafting grid the game is working out a result for, on the thread doing it. */
    private static final ThreadLocal<Player> CRAFTING = new ThreadLocal<>();
    /* What opens knowledge to every team, whatever they know. */
    private static final List<IKnowledgeOpener> OPENERS = new CopyOnWriteArrayList<>();

    private KnowledgeGate() {
    }

    /**
     * Has {@code opener} say which knowledge is open to every team, whatever they know, from a mod's constructor: a
     * mod whose setting turns its research off opens what its discoveries lock, and grants nothing.
     */
    public static void openWhen(final IKnowledgeOpener opener) {
        OPENERS.add(Objects.requireNonNull(opener, "opener"));
    }

    /** Whether the knowledge {@code id} is open to everyone on {@code server}, or on this player's game with none. */
    public static boolean opened(final @Nullable MinecraftServer server, final ResourceLocation id) {
        for (final IKnowledgeOpener opener : OPENERS) {
            if (opener.open(server, id)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Runs {@code work} with {@code player} as the one at the crafting grid, whose team a crafting recipe that asks
     * for knowledge asks: the game's crafting table and inventory grid say who it is as they work out a result.
     */
    @ApiStatus.Internal
    public static void craftingAs(final Player player, final Runnable work) {
        final Player before = CRAFTING.get();
        CRAFTING.set(player);
        try {
            work.run();
        } finally {
            if (before == null) {
                CRAFTING.remove();
            } else {
                CRAFTING.set(before);
            }
        }
    }

    /**
     * Whether a crafting recipe that asks for {@code knowledge} matches on {@code level} now: one that asks for none
     * always; one that asks for some only for a player at a grid whose team knows it, so a crafter block or any grid
     * that names no player crafts nothing locked, as a machine of no owner runs nothing locked; and anything a mod
     * opened to everyone ({@link #openWhen}). On a player's own game it is as the server last said of the player's
     * team.
     */
    public static boolean craftable(final Level level, final Optional<ResourceLocation> knowledge) {
        if (knowledge.isEmpty()) {
            return true;
        }
        if (level.isClientSide()) {
            return openHere(knowledge);
        }
        final Player player = CRAFTING.get();
        final MinecraftServer server = level.getServer();
        return server != null && (opened(server, knowledge.get())
                || player != null && allows(server, player.getUUID(), knowledge));
    }

    /**
     * Whether a machine owned by {@code owner} may run what asks for {@code knowledge}: anything that asks for none,
     * and what asks for some only when the owner's team knows it whole or a mod opened it to everyone. A machine of no
     * owner runs nothing locked.
     */
    public static boolean allows(final MinecraftServer server, final @Nullable UUID owner,
                                 final Optional<ResourceLocation> knowledge) {
        if (knowledge.isEmpty() || opened(server, knowledge.get())) {
            return true;
        }
        return owner != null && TeamProgress.knows(server, CoreTeams.teamOf(server, owner), knowledge.get());
    }

    /** Whether what asks for {@code knowledge} is open to this client's player's team, as the server last said. */
    public static boolean openHere(final Optional<ResourceLocation> knowledge) {
        return knowledge.isEmpty() || opened(null, knowledge.get()) || TeamProgress.knowsHere(knowledge.get());
    }

    /**
     * What the recipes that ask for the knowledge {@code id} make, on the side {@code level} is on, one stack a recipe
     * in no set order: what knowing it opens, as a research screen tells the player. It reads every recipe, so a
     * screen asks once and keeps the answer.
     */
    public static List<ItemStack> opens(final Level level, final ResourceLocation id) {
        final Optional<ResourceLocation> asked = Optional.of(id);
        final List<ItemStack> made = new ArrayList<>();
        for (final RecipeHolder<?> holder : level.getRecipeManager().getRecipes()) {
            if (holder.value() instanceof IKnowledgeGated gated && gated.knowledge().equals(asked)) {
                final ItemStack result = holder.value().getResultItem(level.registryAccess());
                if (!result.isEmpty()) {
                    made.add(result.copy());
                }
            }
        }
        return made;
    }
}
