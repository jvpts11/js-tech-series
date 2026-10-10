/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.audio.media;

import dev.jstech.core.audio.media.MediaId;
import dev.jstech.core.audio.media.MediaOfferPayload;
import dev.jstech.core.audio.media.MediaOfferReplyPayload;
import dev.jstech.core.audio.media.MediaTexts;
import dev.jstech.core.audio.media.MediaUploadDonePayload;
import dev.jstech.core.audio.media.MediaUploadPiecePayload;
import dev.jstech.core.network.transfer.Pieces;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextBounds;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Brings a recording from the player's own computer to the server: it is read off the game's thread, named by its
 * hash and offered; once the server says to send it, it goes a few pieces a tick at the pace the server asked for,
 * and whoever brought it is told how far it has got and how it ended.
 */
public final class MediaUploader {

    /** The largest file the server's own setting can ever allow, so nothing bigger is worth reading into memory. */
    private static final int MOST_FILE_MEGABYTES = 1024;
    private static final long BYTES_PER_MEGABYTE = 1024L * 1024L;

    private static final Map<Integer, Outgoing> OUTGOING = new HashMap<>();

    private static int nextToken;
    private static int generation;

    private MediaUploader() {
    }

    /** Hears how an upload goes. Every call comes on the game's own thread. */
    public interface IListener {

        /** So many of its bytes have been sent. */
        void progress(long sent, long total);

        /**
         * It ended: kept and taken, or not.
         *
         * @param message what taking it did, or why it was not kept
         */
        void finished(boolean ok, Text message);
    }

    /**
     * Brings the file at {@code file} to the server for {@code purpose}.
     *
     * @param context what the server's side needs to know to do with it
     */
    public static void upload(final Path file, final String purpose, final String context, final IListener listener) {
        final String name = file.getFileName().toString();
        final int dot = name.lastIndexOf('.');
        final String format = dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
        final int started = generation;
        CompletableFuture.supplyAsync(() -> read(file), Util.ioPool()).handleAsync((loaded, failure) -> {
            if (failure != null || loaded == null) {
                // Anything that went wrong while reading, an out-of-memory error included, still ends the upload
                // for whoever waits on it.
                listener.finished(false, Text.literal(name));
                return null;
            }
            if (loaded.tooBig()) {
                listener.finished(false, MediaTexts.TOO_BIG.with(MOST_FILE_MEGABYTES));
                return null;
            }
            if (started != generation || Minecraft.getInstance().getConnection() == null) {
                // The player left the server while the file was being read: it is not offered to the next one.
                listener.finished(false, Text.literal(name));
                return null;
            }
            final byte[] bytes = loaded.bytes();
            final MediaId media;
            try {
                media = MediaId.of(bytes, format);
            } catch (final IllegalArgumentException notAKind) {
                listener.finished(false, Text.literal(name));
                return null;
            }
            final int token = ++nextToken;
            OUTGOING.put(token, new Outgoing(bytes, listener));
            PacketDistributor.sendToServer(new MediaOfferPayload(token, media,
                    TextBounds.clip(name, MediaOfferPayload.MAX_NAME), purpose,
                    TextBounds.clip(context, MediaOfferPayload.MAX_CONTEXT)));
            return null;
        }, Minecraft.getInstance());
    }

    /** The server answered an offer. */
    public static void onReply(final MediaOfferReplyPayload reply) {
        final Outgoing outgoing = OUTGOING.get(reply.token());
        if (outgoing == null) {
            return;
        }
        if (reply.verdict() == MediaOfferReplyPayload.SEND) {
            outgoing.bytesPerTick = Math.max(1, reply.bytesPerTick());
            outgoing.sending = true;
            return;
        }
        OUTGOING.remove(reply.token());
        outgoing.listener.finished(reply.verdict() == MediaOfferReplyPayload.TAKEN, reply.reason());
    }

    /** The server has the last piece and says how it went. */
    public static void onDone(final MediaUploadDonePayload done) {
        final Outgoing outgoing = OUTGOING.remove(done.token());
        if (outgoing != null) {
            outgoing.listener.finished(done.ok(), done.message());
        }
    }

    /** Sends this tick's share of every upload the server said to send. */
    public static void tick() {
        for (final Map.Entry<Integer, Outgoing> entry : OUTGOING.entrySet()) {
            final Outgoing outgoing = entry.getValue();
            if (!outgoing.sending || outgoing.index >= outgoing.pieces) {
                continue;
            }
            int budget = outgoing.bytesPerTick;
            while (budget > 0 && outgoing.index < outgoing.pieces) {
                final int start = (int) Pieces.start(outgoing.index, MediaUploadPiecePayload.PIECE_BYTES);
                final int length = Pieces.length(outgoing.index, outgoing.bytes.length,
                        MediaUploadPiecePayload.PIECE_BYTES);
                final byte[] piece = new byte[length];
                System.arraycopy(outgoing.bytes, start, piece, 0, piece.length);
                PacketDistributor.sendToServer(new MediaUploadPiecePayload(entry.getKey(), outgoing.index, piece));
                outgoing.index++;
                budget -= Math.max(1, piece.length);
                outgoing.listener.progress(Math.min((long) outgoing.index * MediaUploadPiecePayload.PIECE_BYTES,
                        outgoing.bytes.length), outgoing.bytes.length);
            }
        }
    }

    /** Whether any upload is under way, for a test. */
    public static boolean busy() {
        return !OUTGOING.isEmpty();
    }

    /** The player left the server: nothing more is sent. */
    public static void clear() {
        OUTGOING.clear();
        // A file still being read finds the number changed and is dropped instead of offered to the next server.
        generation++;
    }

    /* Reads the file, or says it is too big to; null when it cannot be read. */
    private static Loaded read(final Path file) {
        try {
            if (Files.size(file) > MOST_FILE_MEGABYTES * BYTES_PER_MEGABYTE) {
                return new Loaded(null, true);
            }
            return new Loaded(Files.readAllBytes(file), false);
        } catch (final IOException unreadable) {
            return null;
        }
    }

    /** What reading a file came to: its bytes, or that it is larger than any server can be set to take. */
    private record Loaded(byte[] bytes, boolean tooBig) {
    }

    /** One recording on its way to the server. */
    private static final class Outgoing {

        private final byte[] bytes;
        private final IListener listener;
        private final int pieces;
        private int index;
        private int bytesPerTick;
        private boolean sending;

        Outgoing(final byte[] bytes, final IListener listener) {
            this.bytes = bytes;
            this.listener = listener;
            this.pieces = MediaUploadPiecePayload.piecesFor(bytes.length);
        }
    }
}
