/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.media;

import dev.jstech.computers.audio.MediaLibrary;
import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.client.os.MediaPlayerApp;
import dev.jstech.computers.operation.payload.ClientPayloadHandlers;
import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.computers.operation.payload.MediaFramePayload;
import dev.jstech.computers.operation.payload.MediaLibraryPayload;
import dev.jstech.computers.operation.payload.MediaRequestPayload;
import dev.jstech.computers.os.IOsHost;
import java.util.ArrayList;
import java.util.List;
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
            final byte[] png = MediaLibrary.frame(level, computer, payload.path());
            PacketDistributor.sendToPlayer(player, new MediaFramePayload(payload.hostPos(), payload.path(),
                    png == null || png.length > MediaFramePayload.MOST_BYTES ? NO_PICTURE : png));
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
