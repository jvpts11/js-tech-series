/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core Test Kit.
 */
package dev.jstech.testkit.gametest;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

/**
 * Players for a GameTest that the server counts as online.
 *
 * <p>The game's own mock player logs in, and a player who logs in is greeted by every mod in the run, often with data
 * a test's connection was never set up to carry, which fails the test and leaves the player in the world. A player
 * from {@link #join} is only placed where the server looks a player up by id: enough for advancements, for whoever
 * works a machine, for teams and for the Core's progress. A player from {@link #connected} logs in over a connection
 * that carries every mod's payloads, for a test of what a player is sent. Either is taken out again by
 * {@link #leave}, however the test ends.
 *
 * <pre>{@code
 * final ServerPlayer player = GameTestPlayers.join(helper, "tester");
 * try {
 *     ...
 * } finally {
 *     GameTestPlayers.leave(player);
 * }
 * }</pre>
 */
public final class GameTestPlayers {

    private GameTestPlayers() {
    }

    /**
     * A new player of that name, logged in over a connection that carries every mod's payloads, as a player's game
     * with the same mods would. They stand in the world at its spawn, as a player who logs in does, until
     * {@link #leave}.
     */
    public static ServerPlayer connected(final GameTestHelper helper, final String name) {
        final ServerLevel level = helper.getLevel();
        final CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(),
                name), false);
        final ServerPlayer player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(),
                cookie.clientInformation());
        final Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        NetworkRegistry.configureMockConnection(connection);
        level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        return player;
    }

    /** A new player of that name, with an id of their own, online in the test's level. */
    public static ServerPlayer join(final GameTestHelper helper, final String name) {
        return join(helper.getLevel(), UUID.randomUUID(), name);
    }

    /** The player with that id and name, online in {@code level}: the same player across a test's steps. */
    public static ServerPlayer join(final ServerLevel level, final UUID id, final String name) {
        final ServerPlayer player = new ServerPlayer(level.getServer(), level, new GameProfile(id, name),
                ClientInformation.createDefault());
        lookup(level.getServer().getPlayerList()).put(player.getUUID(), player);
        return player;
    }

    /** Takes the player out of the server again: out of its lookup, or logged out when they logged in. */
    public static void leave(final ServerPlayer player) {
        final PlayerList players = player.server.getPlayerList();
        if (players.getPlayers().contains(player)) {
            players.remove(player);
        } else {
            lookup(players).remove(player.getUUID());
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<UUID, ServerPlayer> lookup(final PlayerList players) {
        try {
            final Field byId = PlayerList.class.getDeclaredField("playersByUUID");
            byId.setAccessible(true);
            return (Map<UUID, ServerPlayer>) byId.get(players);
        } catch (final ReflectiveOperationException missing) {
            throw new IllegalStateException("the server's player lookup is not where it was", missing);
        }
    }
}
