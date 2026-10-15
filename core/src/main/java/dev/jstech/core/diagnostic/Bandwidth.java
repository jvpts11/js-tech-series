/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.diagnostic;

import dev.jstech.core.format.Unit;
import dev.jstech.core.format.UnitFormatter;
import dev.jstech.core.log.CoreLogs;
import io.netty.channel.ChannelPipeline;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import net.minecraft.network.Connection;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;

/**
 * What the server sends each player over the network, counted as it is sent: by kind (a mod's payloads by their id,
 * the game's packets by theirs), by mod, and by the block entity whose updates took it, with the bytes of each second
 * of the last minute for a graph. {@code /jstech bandwidth} reads it for everyone or for one player.
 *
 * <p>The bytes are those the game writes each packet as, before the connection compresses them. A player playing in
 * their own game is not counted, as nothing they are sent is written as bytes; whoever joins it over the network is.
 */
public final class Bandwidth {

    /** The bars a graph is drawn with, the lowest first. */
    public static final String BARS = "▁▂▃▄▅▆▇█";
    /** How many seconds each bar of a graph stands for. */
    public static final int SECONDS_PER_BAR = 2;

    private static final Logger LOGGER = CoreLogs.DIAGNOSTICS.logger();
    private static final UnitFormatter FORMAT = new UnitFormatter(Locale.ROOT);
    private static final BandwidthLedger EVERYONE = new BandwidthLedger(Bandwidth::second);
    private static final Map<UUID, BandwidthLedger> PLAYERS = new ConcurrentHashMap<>();

    private Bandwidth() {
    }

    /** What the server sent everyone. */
    public static BandwidthLedger everyone() {
        return EVERYONE;
    }

    /** What the server sent the player {@code player} while they are on, or none when it is not counted. */
    public static Optional<BandwidthLedger> of(final UUID player) {
        return Optional.ofNullable(PLAYERS.get(player));
    }

    /** How many of the players on are counted. */
    public static int countedPlayers() {
        return PLAYERS.size();
    }

    /** A rate as the server writes it: {@code 12.4KB/s}. */
    public static String rate(final long bytesPerSecond) {
        return FORMAT.compact(bytesPerSecond, Unit.BYTES_PER_SECOND);
    }

    /**
     * A minute's bytes as a line of bars, each the sum of {@value #SECONDS_PER_BAR} seconds, as tall as its share of
     * the tallest; a minute with nothing sent is a line of the lowest bar.
     */
    public static String graph(final long[] seconds) {
        final int bars = (seconds.length + SECONDS_PER_BAR - 1) / SECONDS_PER_BAR;
        final long[] sums = new long[bars];
        long most = 0L;
        for (int i = 0; i < seconds.length; i++) {
            sums[i / SECONDS_PER_BAR] += seconds[i];
        }
        for (final long sum : sums) {
            most = Math.max(most, sum);
        }
        final StringBuilder out = new StringBuilder(bars);
        final int top = BARS.length() - 1;
        for (final long sum : sums) {
            out.append(BARS.charAt(most == 0L ? 0 : (int) (sum * top / most)));
        }
        return out.toString();
    }

    /** Starts counting what is sent to {@code player}, as they join; nothing when their connection is not counted. */
    public static void watch(final ServerPlayer player) {
        final Connection connection = player.connection.getConnection();
        if (connection.isMemoryConnection() || connection.channel() == null) {
            return;
        }
        if (!watch(connection.channel().pipeline(), player.getUUID(), () -> player.level().dimension())) {
            LOGGER.debug("What is sent to {} is not counted: their connection has no encoder", player.getUUID());
        }
    }

    /**
     * Starts counting what goes out through {@code pipeline} as what is sent to {@code player}, whose updates of block
     * entities are in {@code dimension}: the counters go either side of the pipeline's encoder.
     *
     * @return whether it is counted: false when the pipeline has no encoder, or is counted already
     */
    public static boolean watch(final ChannelPipeline pipeline, final UUID player,
                                final Supplier<ResourceKey<Level>> dimension) {
        final BandwidthLedger ledger = new BandwidthLedger(Bandwidth::second);
        if (!BandwidthHandlers.attach(pipeline, ledger, EVERYONE, dimension)) {
            return false;
        }
        PLAYERS.put(player, ledger);
        return true;
    }

    /** Stops keeping what was sent to {@code player}, as they leave. */
    public static void forget(final UUID player) {
        PLAYERS.remove(player);
    }

    /** Hands the game's bus the players joining and leaving, once, from the Core's constructor. */
    @ApiStatus.Internal
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                watch(player);
            }
        });
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedOutEvent.class,
                event -> forget(event.getEntity().getUUID()));
    }

    private static long second() {
        return TimeUnit.NANOSECONDS.toSeconds(System.nanoTime());
    }
}
