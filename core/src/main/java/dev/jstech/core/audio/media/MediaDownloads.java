/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio.media;

import com.mojang.logging.LogUtils;
import dev.jstech.core.network.transfer.Pieces;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;

/**
 * The recordings on their way from the server to its players, a queue for each player, sent in pieces a tick at a
 * time and no faster than the server's owner lets them: a player who walks into a room of music is not handed a
 * hundred megabytes at once, and the other players on the server do not feel it.
 */
public final class MediaDownloads {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Map<UUID, Deque<Outgoing>> QUEUES = new HashMap<>();

    private MediaDownloads() {
    }

    /** A player asked for a recording: it goes on the end of their queue, unless it is already on it. */
    public static synchronized void request(final ServerPlayer player, final MediaId media) {
        final Optional<MediaStore> store = MediaStore.current();
        if (store.isEmpty() || !store.get().has(media)) {
            PacketDistributor.sendToPlayer(player, new MediaMissingPayload(media.hash()));
            return;
        }
        store.get().used(media);
        final Deque<Outgoing> queue = QUEUES.computeIfAbsent(player.getUUID(), uuid -> new ArrayDeque<>());
        for (final Outgoing waiting : queue) {
            if (waiting.media.equals(media)) {
                return;
            }
        }
        queue.addLast(new Outgoing(media));
    }

    /** Sends each player this tick's share of what they are waiting for. */
    public static synchronized void tick(final MinecraftServer server) {
        if (QUEUES.isEmpty()) {
            return;
        }
        final Optional<MediaStore> store = MediaStore.current();
        QUEUES.entrySet().removeIf(entry -> {
            final ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null || store.isEmpty()) {
                return true;
            }
            send(player, store.get(), entry.getValue());
            return entry.getValue().isEmpty();
        });
    }

    /** The player left: nothing more is sent them. */
    public static synchronized void forget(final UUID player) {
        QUEUES.remove(player);
    }

    /** Whether anything is on its way to that player, for a test. */
    public static synchronized boolean sending(final UUID player) {
        final Deque<Outgoing> queue = QUEUES.get(player);
        return queue != null && !queue.isEmpty();
    }

    private static void send(final ServerPlayer player, final MediaStore store, final Deque<Outgoing> queue) {
        int budget = MediaBalance.downloadBytesPerTick();
        while (budget > 0 && !queue.isEmpty()) {
            final Outgoing next = queue.peekFirst();
            try {
                final byte[] piece = next.read(store);
                PacketDistributor.sendToPlayer(player,
                        new MediaPiecePayload(next.media.hash(), next.index, next.count, piece));
                budget -= Math.max(1, piece.length);
                next.index++;
            } catch (final IOException unreadable) {
                LOGGER.warn("Could not read recording {} for {}: {}", next.media.fileName(),
                        player.getGameProfile().getName(), unreadable.getMessage());
                PacketDistributor.sendToPlayer(player, new MediaMissingPayload(next.media.hash()));
                next.index = next.count;
            }
            if (next.index >= next.count) {
                queue.removeFirst();
            }
        }
    }

    /** One recording on its way to one player, and the next piece of it to send. */
    private static final class Outgoing {

        private final MediaId media;
        private final int count;
        private int index;

        Outgoing(final MediaId media) {
            this.media = media;
            this.count = MediaPiecePayload.piecesFor(media.bytes());
        }

        /* The next piece, read from the recording's file where it starts. */
        byte[] read(final MediaStore store) throws IOException {
            final long start = Pieces.start(index, MediaPiecePayload.PIECE_BYTES);
            final int length = Pieces.length(index, media.bytes(), MediaPiecePayload.PIECE_BYTES);
            final byte[] piece = new byte[length];
            try (RandomAccessFile file = new RandomAccessFile(store.path(media).toFile(), "r")) {
                file.seek(start);
                file.readFully(piece);
            }
            return piece;
        }
    }
}
