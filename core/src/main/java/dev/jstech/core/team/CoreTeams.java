/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.team;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Codec;
import dev.jstech.core.JsCore;
import dev.jstech.core.log.CoreLogs;
import dev.jstech.core.state.CoreState;
import dev.jstech.core.state.CoreStates;
import dev.jstech.core.state.PlayerState;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.GameProfileCache;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;

/**
 * The one place the Core asks which team a player is on. The answer comes from a single {@link ITeamSource}: the
 * game's scoreboard teams until a mod hands over another, which it does once, while the game loads. A mod that keeps
 * something for a team (a state kept per team, a team's machines) asks here and never the source itself, so it keeps
 * working whichever source is in use.
 */
public final class CoreTeams {

    private static final Logger LOGGER = CoreLogs.TEAMS.logger();
    /** What the id of a player's team of their own starts with. */
    private static final String SOLO = "player:";
    /*
     * The name each player was last known by as they joined, kept with the world: the game's own cache of names lets
     * the oldest go once it is full, and a team found by name would be lost with it while its player is away.
     */
    private static final PlayerState<String> KNOWN_NAMES = CoreState.builder(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "known_names"), Codec.STRING, "").player();

    private static volatile ITeamSource source = ScoreboardTeams.INSTANCE;

    private CoreTeams() {
    }

    /** Keeps the names of the players who join, once, from the Core's constructor. */
    @ApiStatus.Internal
    public static void listen() {
        CoreStates.register(KNOWN_NAMES);
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                KNOWN_NAMES.set(player.server, player.getUUID(), player.getScoreboardName());
            }
        });
    }

    /** The source in use. */
    public static ITeamSource source() {
        return source;
    }

    /** Hands the Core another source of teams, from a mod that brings teams of its own. */
    public static void use(final ITeamSource next) {
        final ITeamSource previous = source;
        source = Objects.requireNonNull(next, "next");
        if (previous != ScoreboardTeams.INSTANCE && previous != next) {
            LOGGER.warn("Teams were coming from {} and now come from {}; only one source of teams is used",
                    previous.getClass().getName(), next.getClass().getName());
        }
    }

    /** The team {@code player} is on; a player on no team is on their own, {@link #solo(UUID)}. */
    public static String teamOf(final MinecraftServer server, final UUID player) {
        return source.teamOf(server, player);
    }

    /** The id of {@code player}'s team of their own, the team of a player on no other. */
    public static String solo(final UUID player) {
        return SOLO + player;
    }

    /** The player a team of their own is, from its id; none for any other team. */
    public static Optional<UUID> soloPlayer(final String team) {
        if (!team.startsWith(SOLO)) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(team.substring(SOLO.length())));
        } catch (final IllegalArgumentException notAnId) {
            return Optional.empty();
        }
    }

    /** The players on {@code team}, as far as the source in use can say. */
    public static List<UUID> membersOf(final MinecraftServer server, final String team) {
        return source.membersOf(server, team);
    }

    /**
     * The name {@code team} is shown by to players, as the source in use names it; none for a player's team of their
     * own, whose name is the player's, or when the source cannot say.
     */
    public static Optional<String> teamName(final MinecraftServer server, final String team) {
        return soloPlayer(team).isPresent() ? Optional.empty() : source.teamName(server, team);
    }

    /** The name the server last knew {@code player} by, online or not; none when it never knew one. */
    public static Optional<String> nameOf(final MinecraftServer server, final UUID player) {
        final ServerPlayer online = server.getPlayerList().getPlayer(player);
        if (online != null) {
            return Optional.of(online.getScoreboardName());
        }
        final GameProfileCache profiles = server.getProfileCache();
        final Optional<String> cached = profiles == null ? Optional.empty() : profiles.get(player)
                .map(GameProfile::getName);
        if (cached.isPresent()) {
            return cached;
        }
        final String known = KNOWN_NAMES.get(server, player);
        return known.isEmpty() ? Optional.empty() : Optional.of(known);
    }
}
