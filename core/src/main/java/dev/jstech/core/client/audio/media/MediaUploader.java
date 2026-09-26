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
import dev.jstech.core.audio.media.MediaUploadDonePayload;
import dev.jstech.core.audio.media.MediaUploadPiecePayload;
import dev.jstech.core.text.Text;
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

    private static final Map<Integer, Outgoing> OUTGOING = new HashMap<>();

    private static int nextToken;

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
        CompletableFuture.supplyAsync(() -> {
            try {
                return Files.readAllBytes(file);
            } catch (final IOException unreadable) {
                return null;
            }
        }, Util.ioPool()).thenAcceptAsync(bytes -> {
            if (bytes == null) {
                listener.finished(false, Text.literal(name));
                return;
            }
            final MediaId media;
            try {
                media = MediaId.of(bytes, format);
            } catch (final IllegalArgumentException notAKind) {
                listener.finished(false, Text.literal(name));
                return;
            }
            final int token = ++nextToken;
            OUTGOING.put(token, new Outgoing(bytes, listener));
            PacketDistributor.sendToServer(new MediaOfferPayload(token, media, clip(name, MediaOfferPayload.MAX_NAME),
                    purpose, clip(context, MediaOfferPayload.MAX_CONTEXT)));
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
                final int start = outgoing.index * MediaUploadPiecePayload.PIECE_BYTES;
                final int length = Math.min(MediaUploadPiecePayload.PIECE_BYTES, outgoing.bytes.length - start);
                final byte[] piece = new byte[Math.max(0, length)];
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
    }

    private static String clip(final String text, final int max) {
        return text.length() <= max ? text : text.substring(0, max);
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
