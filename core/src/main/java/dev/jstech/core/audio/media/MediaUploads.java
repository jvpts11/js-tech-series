/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio.media;

import com.mojang.logging.LogUtils;
import dev.jstech.core.text.Text;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * The recordings players bring from their own computers. A player offers one first, saying what it is; the server
 * refuses it there if it would not keep it, takes it at once if it already keeps one by that name, and otherwise says
 * to send it. The pieces are written to a file of the store's incoming folder, and when the last has come and they are
 * the recording they were offered as, the store keeps it and whoever takes recordings for that purpose is handed it.
 *
 * <p>What a recording is brought for is named by a purpose, and a mod says who takes each one. The server's owner sets
 * how big a recording may be; a player sends the rest of theirs only when the last has finished.
 */
public final class MediaUploads {

    private static final Logger LOGGER = LogUtils.getLogger();
    /** The recordings one player may have on their way at once. */
    private static final int MOST_AT_ONCE = 2;

    private static final Map<String, IMediaUploadHandler> HANDLERS = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<Integer, Incoming>> INCOMING = new HashMap<>();

    private MediaUploads() {
    }

    /** Says who takes the recordings players bring for {@code purpose}. */
    public static void handle(final String purpose, final IMediaUploadHandler handler) {
        if (HANDLERS.putIfAbsent(purpose, handler) != null) {
            throw new IllegalStateException("recordings brought for " + purpose + " are already taken by another");
        }
    }

    /** A player offers a recording. */
    public static synchronized void offer(final ServerPlayer player, final MediaOfferPayload offer) {
        final Text refused = refusal(player, offer);
        if (refused != null) {
            reply(player, offer.token(), MediaOfferReplyPayload.REFUSED, refused);
            return;
        }
        final MediaStore store = MediaStore.current().orElseThrow();
        final IMediaUploadHandler handler = HANDLERS.get(offer.purpose());
        if (store.has(offer.media())) {
            // Somebody brought this very recording before: it is taken without a byte of it being sent again.
            try {
                final Text done = handler.received(player, offer.context(), offer.name(), offer.media(),
                        store.info(offer.media()));
                reply(player, offer.token(), MediaOfferReplyPayload.TAKEN, done);
            } catch (final IOException unreadable) {
                reply(player, offer.token(), MediaOfferReplyPayload.REFUSED, MediaTexts.UNREADABLE.text());
            }
            return;
        }
        try {
            final Path file = store.newIncoming(offer.media());
            final OutputStream out = Files.newOutputStream(file);
            final int pieces = MediaUploadPiecePayload.piecesFor(offer.media().bytes());
            INCOMING.computeIfAbsent(player.getUUID(), uuid -> new HashMap<>()).put(offer.token(),
                    new Incoming(offer, file, new MediaReceiver(offer.media(), pieces, out), out));
            reply(player, offer.token(), MediaOfferReplyPayload.SEND, Text.EMPTY);
        } catch (final IOException cannotWrite) {
            LOGGER.warn("Could not make room for a recording from {}: {}", player.getGameProfile().getName(),
                    cannotWrite.getMessage());
            reply(player, offer.token(), MediaOfferReplyPayload.REFUSED, MediaTexts.STORE_CLOSED.text());
        }
    }

    /** A piece of a recording a player was told to send. */
    public static synchronized void piece(final ServerPlayer player, final MediaUploadPiecePayload piece) {
        final Map<Integer, Incoming> theirs = INCOMING.get(player.getUUID());
        final Incoming incoming = theirs == null ? null : theirs.get(piece.token());
        if (incoming == null) {
            return;
        }
        try {
            incoming.receiver.accept(piece.index(), piece.data());
        } catch (final IOException broken) {
            end(player, theirs, incoming, false, MediaTexts.BROKEN.text());
            return;
        }
        if (!incoming.receiver.complete()) {
            return;
        }
        if (!incoming.receiver.verified()) {
            end(player, theirs, incoming, false, MediaTexts.BROKEN.text());
            return;
        }
        final Optional<MediaStore> store = MediaStore.current();
        if (store.isEmpty()) {
            end(player, theirs, incoming, false, MediaTexts.STORE_CLOSED.text());
            return;
        }
        final MediaOfferPayload offer = incoming.offer;
        try {
            final MediaId kept = store.get().adopt(incoming.file, offer.media());
            final Text done = HANDLERS.get(offer.purpose()).received(player, offer.context(), offer.name(), kept,
                    store.get().info(kept));
            end(player, theirs, incoming, true, done);
        } catch (final IOException unreadable) {
            end(player, theirs, incoming, false, MediaTexts.UNREADABLE.text());
        }
    }

    /** The player left: whatever they were sending is dropped. */
    public static synchronized void forget(final UUID player) {
        final Map<Integer, Incoming> theirs = INCOMING.remove(player);
        if (theirs != null) {
            theirs.values().forEach(Incoming::drop);
        }
    }

    /** Why this offer is refused before a byte of it is sent, or null when it is not. */
    @Nullable
    private static Text refusal(final ServerPlayer player, final MediaOfferPayload offer) {
        final IMediaUploadHandler handler = HANDLERS.get(offer.purpose());
        if (handler == null) {
            return MediaTexts.NO_TAKER.text();
        }
        if (MediaStore.current().isEmpty()) {
            return MediaTexts.STORE_CLOSED.text();
        }
        if (MediaBalance.maxFileBytes() <= 0) {
            return MediaTexts.UPLOADS_CLOSED.text();
        }
        if (offer.media().bytes() > MediaBalance.maxFileBytes()) {
            return MediaTexts.TOO_BIG.with(MediaBalance.maxFileMegabytes());
        }
        final Map<Integer, Incoming> theirs = INCOMING.get(player.getUUID());
        if (theirs != null && theirs.size() >= MOST_AT_ONCE) {
            return MediaTexts.BUSY.text();
        }
        return handler.refuse(player, offer.context(), offer.name(), offer.media());
    }

    private static void reply(final ServerPlayer player, final int token, final int verdict, final Text reason) {
        PacketDistributor.sendToPlayer(player,
                new MediaOfferReplyPayload(token, verdict, MediaBalance.uploadBytesPerTick(), reason));
    }

    private static void end(final ServerPlayer player, final Map<Integer, Incoming> theirs, final Incoming incoming,
                            final boolean ok, final Text message) {
        theirs.remove(incoming.offer.token());
        if (!ok) {
            incoming.drop();
        }
        PacketDistributor.sendToPlayer(player, new MediaUploadDonePayload(incoming.offer.token(), ok, message));
    }

    /** One recording on its way in, the file it is written to and what has come of it. */
    private record Incoming(MediaOfferPayload offer, Path file, MediaReceiver receiver, OutputStream out) {

        /* Given up on: the half of it that came is thrown away. */
        void drop() {
            try {
                out.close();
                Files.deleteIfExists(file);
            } catch (final IOException ignored) {
                // A file the server cannot remove now is swept out of the incoming folder on its next start.
            }
        }
    }
}
