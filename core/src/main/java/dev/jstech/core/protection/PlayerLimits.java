/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.protection;

import com.mojang.serialization.Codec;
import dev.jstech.core.JsCore;
import dev.jstech.core.state.CoreState;
import dev.jstech.core.state.CoreStates;
import dev.jstech.core.state.PlayerState;
import dev.jstech.core.team.CorePermissions;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.util.DeclarationError;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntSupplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.ApiStatus;

/**
 * How many of a kind of thing one player may have at once (robots, moving structures, large edits in progress), so
 * nobody can bring a server down on purpose. Each kit declares its limit with what sets it, takes from it as a player
 * gets one and gives back as it goes; the counts are kept with the world, per player. Whoever holds
 * {@link CorePermissions#PASS_LIMITS} goes past every limit.
 *
 * <pre>{@code
 * Limit CRANES = PlayerLimits.declare(id("cranes"), CRANES_NAME, Balance::cranesPerPlayer);
 * if (!PlayerLimits.take(server, owner, CRANES)) {
 *     return; // at the limit
 * }
 * }</pre>
 */
@TextHolder
public final class PlayerLimits {

    /* How many the player has, how many they may have, and of what. */
    public static final TextKey REACHED = TextKey.of("jscore.limit.reached", "You have %s of %s %s allowed");
    public static final TextKey ROBOTS_NAME = TextKey.of("jscore.limit.robots", "robots");

    /** What each player has of each kind, by the kind's id. */
    private static final PlayerState<Map<String, Integer>> COUNTS = CoreState.builder(
                    ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "player_limits"),
                    Codec.unboundedMap(Codec.STRING, Codec.INT), Map.<String, Integer>of())
            .player();
    private static final Map<ResourceLocation, Limit> DECLARED = new ConcurrentHashMap<>();

    /** The robots one player may have. */
    public static final Limit ROBOTS = declare(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "robots"),
            ROBOTS_NAME, ProtectionBalance::robotsPerPlayer);

    private PlayerLimits() {
    }

    /**
     * A limit.
     *
     * @param id   its name
     * @param name what it counts, as a player reads it
     * @param most how many one player may have, read each time
     */
    public record Limit(ResourceLocation id, TextKey name, IntSupplier most) {

        public Limit {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(most, "most");
        }
    }

    /**
     * Declares a limit, from a kit's class or a mod's constructor.
     *
     * @throws DeclarationError when one of that id is declared already
     */
    public static Limit declare(final ResourceLocation id, final TextKey name, final IntSupplier most) {
        final Limit limit = new Limit(id, name, most);
        if (DECLARED.putIfAbsent(id, limit) != null) {
            throw DeclarationError.of(id.getNamespace(), "the limit " + id, "is declared twice",
                    "declare each limit once");
        }
        return limit;
    }

    /** Every limit declared. */
    public static List<Limit> declared() {
        return List.copyOf(DECLARED.values());
    }

    /** How many of {@code limit}'s kind {@code player} has. */
    public static int count(final MinecraftServer server, final UUID player, final Limit limit) {
        return COUNTS.get(server, player).getOrDefault(limit.id().toString(), 0);
    }

    /**
     * Takes one of {@code limit} for {@code player}, when they have fewer than it allows or may go past it.
     *
     * @return whether they may have it; one they may not have is not counted
     */
    public static boolean take(final MinecraftServer server, final UUID player, final Limit limit) {
        final int now = count(server, player, limit);
        if (now >= limit.most().getAsInt() && !CorePermissions.has(player, CorePermissions.PASS_LIMITS)) {
            return false;
        }
        put(server, player, limit, now + 1);
        return true;
    }

    /** What a player at {@code limit} is told: how many they have, how many they may have, and of what. */
    public static Text reached(final MinecraftServer server, final UUID player, final Limit limit) {
        return REACHED.with(count(server, player, limit), limit.most().getAsInt(), limit.name());
    }

    /** Gives back one of {@code limit} {@code player} had, as the thing goes. */
    public static void giveBack(final MinecraftServer server, final UUID player, final Limit limit) {
        put(server, player, limit, Math.max(0, count(server, player, limit) - 1));
    }

    /** Registers the counts kept with the world, once, from the Core's constructor. */
    @ApiStatus.Internal
    public static void register() {
        CoreStates.register(COUNTS);
    }

    private static void put(final MinecraftServer server, final UUID player, final Limit limit, final int count) {
        COUNTS.update(server, player, counts -> {
            final Map<String, Integer> next = new HashMap<>(counts);
            if (count == 0) {
                next.remove(limit.id().toString());
            } else {
                next.put(limit.id().toString(), count);
            }
            return Map.copyOf(next);
        });
    }
}
