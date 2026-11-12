/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.media;

import dev.jstech.computers.audio.MediaLibrary;
import dev.jstech.computers.audio.SongFiles;
import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.client.os.MediaPlayerApp;
import dev.jstech.computers.operation.payload.ClientPayloadHandlers;
import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.computers.operation.payload.MediaFramePayload;
import dev.jstech.computers.operation.payload.MediaLibraryPayload;
import dev.jstech.computers.operation.payload.MediaRequestPayload;
import dev.jstech.computers.os.IOsHost;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.minecraft.Util;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * The Media Player's questions to its computer: what it can play, and the pictures of a sequence, one at a time. Its
 * songs play through the computer's own player, as Soundfoundry's do, so they need nothing here.
 */
public final class MediaPayloads {

    private static final byte[] NO_PICTURE = new byte[0];
    /* The most pictures a player has made at once: what one Media Player asks for before the first comes back. */
    private static final int MOST_MAKING = 4;
    /* The most a player's pictures wait their turn; past that, the asks are no Media Player's. */
    private static final int MOST_WAITING = 64;
    /* How many pictures each player has a worker making, and those waiting; touched only on the server thread. */
    private static final Map<UUID, Integer> MAKING = new HashMap<>();
    private static final Map<UUID, Deque<Runnable>> WAITING = new HashMap<>();

    private MediaPayloads() {
    }

    /** Registers the payloads this class handles. */
    public static void register(final PayloadRegistrar registrar) {
        ComputerAccess.accept(registrar, MediaRequestPayload.TYPE, MediaRequestPayload.STREAM_CODEC,
                ComputerAccess.machine(MediaRequestPayload::hostPos), MediaPayloads::handle);
        registrar.playToClient(MediaLibraryPayload.TYPE, MediaLibraryPayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread((library, player) -> MediaPlayerApp.accept(library)));
        registrar.playToClient(MediaFramePayload.TYPE, MediaFramePayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread((frame, player) -> MediaPlayerApp.accept(frame)));
    }

    private static void handle(final MediaRequestPayload payload, final ServerPlayer player, final ServerLevel level) {
        if (!(level.getBlockEntity(payload.hostPos()) instanceof IOsHost computer)
                || !MediaLibrary.playerOn(computer)) {
            return;
        }
        if (payload.what() == MediaRequestPayload.FRAME) {
            askFrame(payload, player, level, computer);
            return;
        }
        final List<MediaLibrary.Song> songs = new ArrayList<>();
        for (final MediaLibrary.Song song : MediaLibrary.songs(level, computer)) {
            if (fits(song.path()) && fits(song.title())) {
                songs.add(song);
            }
        }
        final List<MediaLibrary.Sequence> sequences = new ArrayList<>();
        for (final MediaLibrary.Sequence sequence : MediaLibrary.sequences(level, computer)) {
            sequences.add(travelling(sequence));
        }
        final MediaLibrary.Sequence asked = payload.path().isEmpty() ? MediaLibrary.Sequence.NONE
                : travelling(MediaLibrary.sequence(level, computer, payload.path()));
        final boolean sound = computer instanceof AbstractComputerBlockEntity machine && machine.playsRecordings();
        PacketDistributor.sendToPlayer(player, new MediaLibraryPayload(payload.hostPos(), songs, sequences, asked,
                sound));
    }

    /*
     * A picture is read from its disk here, on the server thread, and made on a worker, so a large camera picture
     * never holds the tick up. A player has no more of them made at once than a Media Player asks for; the rest wait
     * their turn, and each goes back from the server thread.
     */
    private static void askFrame(final MediaRequestPayload payload, final ServerPlayer player, final ServerLevel level,
                                 final IOsHost computer) {
        final String content = SongFiles.content(level, computer, payload.path()).orElse(null);
        final UUID who = player.getUUID();
        final MinecraftServer server = level.getServer();
        final Runnable make = () -> CompletableFuture
                .supplyAsync(() -> MediaLibrary.made(payload.path(), content), Util.backgroundExecutor())
                .whenComplete((png, failed) -> server.execute(() -> {
                    final byte[] sent = failed != null || png == null || png.length > MediaFramePayload.MOST_BYTES
                            ? NO_PICTURE : png;
                    PacketDistributor.sendToPlayer(player, new MediaFramePayload(payload.hostPos(), payload.path(),
                            sent));
                    madeOne(who);
                }));
        if (MAKING.getOrDefault(who, 0) < MOST_MAKING) {
            start(who, make);
        } else {
            final Deque<Runnable> waiting = WAITING.computeIfAbsent(who, any -> new ArrayDeque<>());
            if (waiting.size() < MOST_WAITING) {
                waiting.addLast(make);
            }
        }
    }

    private static void start(final UUID who, final Runnable make) {
        MAKING.merge(who, 1, Integer::sum);
        make.run();
    }

    /* One of a player's pictures went back: the next waiting one, if any, is made. */
    private static void madeOne(final UUID who) {
        MAKING.computeIfPresent(who, (any, count) -> count <= 1 ? null : count - 1);
        final Deque<Runnable> waiting = WAITING.get(who);
        if (waiting == null) {
            return;
        }
        final Runnable next = waiting.pollFirst();
        if (waiting.isEmpty()) {
            WAITING.remove(who);
        }
        if (next != null) {
            start(who, next);
        }
    }

    /* A sequence with only the pictures whose paths travel, its folder's too. */
    private static MediaLibrary.Sequence travelling(final MediaLibrary.Sequence sequence) {
        if (!fits(sequence.folder())) {
            return MediaLibrary.Sequence.NONE;
        }
        return new MediaLibrary.Sequence(sequence.folder(), sequence.frames().stream().filter(MediaPayloads::fits)
                .toList());
    }

    private static boolean fits(final String text) {
        return text.length() <= MediaLibraryPayload.MOST_TEXT;
    }
}
