/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.audio.media;

import com.mojang.logging.LogUtils;
import dev.jstech.core.audio.media.MediaId;
import dev.jstech.core.audio.media.MediaPiecePayload;
import dev.jstech.core.audio.media.MediaReceiver;
import dev.jstech.core.audio.media.MediaWantPayload;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;

/**
 * The recordings this client has, kept on its own disk under their names, so a song heard once is never fetched
 * again, on this server or any other that keeps the same one. A recording it lacks is asked for, put together as its
 * pieces come and checked against its name before it is kept; whoever was waiting for it is told when it is there.
 *
 * <p>The cache keeps to a size: past it, the recordings heard longest ago make room.
 */
public final class MediaCache {

    private static final Logger LOGGER = LogUtils.getLogger();
    /** The most the cache holds before the recordings heard longest ago are thrown away. */
    private static final long LIMIT_BYTES = 512L * 1024 * 1024;
    private static final String FOLDER = "media-cache";

    private static final Map<String, Incoming> INCOMING = new HashMap<>();

    private MediaCache() {
    }

    /**
     * Hands {@code ready} the recording's file, at once when it is here, or when it has come; never when the server
     * has none by that name. Runs on the game's own thread.
     */
    public static void fetch(final MediaId media, final Consumer<Path> ready) {
        final Path kept = path(media);
        if (Files.isRegularFile(kept)) {
            touch(kept);
            ready.accept(kept);
            return;
        }
        final Incoming waiting = INCOMING.get(media.hash());
        if (waiting != null) {
            waiting.ready.add(ready);
            return;
        }
        try {
            Files.createDirectories(root());
            final Path part = Files.createTempFile(root(), media.hash(), ".part");
            final OutputStream out = Files.newOutputStream(part);
            final Incoming incoming = new Incoming(media, part, out,
                    new MediaReceiver(media, MediaPiecePayload.piecesFor(media.bytes()), out));
            incoming.ready.add(ready);
            INCOMING.put(media.hash(), incoming);
            PacketDistributor.sendToServer(new MediaWantPayload(media));
        } catch (final IOException cannotWrite) {
            LOGGER.warn("Could not make room for recording {}: {}", media.fileName(), cannotWrite.getMessage());
        }
    }

    /** Whether the recording is here already. */
    public static boolean has(final MediaId media) {
        return Files.isRegularFile(path(media));
    }

    /** Whether the recording is on its way, for a test. */
    public static boolean fetching(final MediaId media) {
        return INCOMING.containsKey(media.hash());
    }

    /** A piece of a recording this client asked for. */
    public static void onPiece(final MediaPiecePayload piece) {
        final Incoming incoming = INCOMING.get(piece.hash());
        if (incoming == null) {
            return;
        }
        try {
            incoming.receiver.accept(piece.index(), piece.data());
        } catch (final IOException broken) {
            LOGGER.warn("Recording {} arrived out of order: {}", incoming.media.fileName(), broken.getMessage());
            // Left registered, the dead transfer would swallow every later fetch of this recording as a waiter.
            INCOMING.remove(piece.hash());
            discard(incoming);
            return;
        }
        if (!incoming.receiver.complete()) {
            return;
        }
        INCOMING.remove(piece.hash());
        if (!incoming.receiver.verified()) {
            LOGGER.warn("Recording {} is not the bytes it was sent as", incoming.media.fileName());
            discard(incoming);
            return;
        }
        try {
            final Path kept = path(incoming.media);
            Files.move(incoming.part, kept, StandardCopyOption.REPLACE_EXISTING);
            incoming.ready.forEach(waiter -> waiter.accept(kept));
            keepToLimit();
        } catch (final IOException cannotKeep) {
            LOGGER.warn("Recording {} could not be kept: {}", incoming.media.fileName(), cannotKeep.getMessage());
            discard(incoming);
        }
    }

    /** The server has no recording by that name: nobody waits for it any more. */
    public static void onMissing(final String hash) {
        final Incoming incoming = INCOMING.remove(hash);
        if (incoming != null) {
            LOGGER.warn("The server has no recording {}", incoming.media.fileName());
            discard(incoming);
        }
    }

    /** The player left the server: whatever was on its way is dropped. */
    public static void clear() {
        INCOMING.values().forEach(MediaCache::discard);
        INCOMING.clear();
    }

    private static Path root() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("jstech").resolve(FOLDER);
    }

    private static Path path(final MediaId media) {
        return root().resolve(media.fileName());
    }

    /* Heard now, so it is the last to make room. */
    private static void touch(final Path file) {
        try {
            Files.setLastModifiedTime(file, FileTime.fromMillis(System.currentTimeMillis()));
        } catch (final IOException ignored) {
            // A recording whose time cannot be set is only let go a little sooner.
        }
    }

    /* Throws away the recordings heard longest ago until the cache is inside its size again. */
    private static void keepToLimit() {
        final List<Cached> kept = new ArrayList<>();
        long total = 0L;
        try (Stream<Path> files = Files.list(root())) {
            for (final Path file : files.filter(f -> !f.getFileName().toString().endsWith(".part")).toList()) {
                // Size and time are read once per file, so sorting does not go back to the disk on every comparison.
                final Cached cached = describe(file);
                if (cached != null) {
                    kept.add(cached);
                    total += cached.size();
                }
            }
        } catch (final IOException cannotList) {
            LOGGER.warn("Could not keep the recording cache to its size: {}", cannotList.getMessage());
            return;
        }
        kept.sort(Comparator.comparing(Cached::heard));
        for (final Cached oldest : kept) {
            if (total <= LIMIT_BYTES) {
                break;
            }
            try {
                Files.deleteIfExists(oldest.file());
                total -= oldest.size();
            } catch (final IOException cannotDelete) {
                // One file that will not go must not keep the others from making room.
                LOGGER.warn("Could not remove cached recording {}: {}", oldest.file().getFileName(),
                        cannotDelete.getMessage());
            }
        }
    }

    /* What the cache needs to know of a file, or null when it cannot be read (gone, or locked). */
    private static Cached describe(final Path file) {
        try {
            return new Cached(file, Files.size(file), Files.getLastModifiedTime(file));
        } catch (final IOException unreadable) {
            return null;
        }
    }

    private static void discard(final Incoming incoming) {
        try {
            incoming.out.close();
            Files.deleteIfExists(incoming.part);
        } catch (final IOException ignored) {
            // A part file left behind is only a few bytes of a cache folder the player may clear.
        }
    }

    /** A recording kept on disk, with its size and the time it was last heard, read once. */
    private record Cached(Path file, long size, FileTime heard) {
    }

    /** One recording on its way here, the file it is written to and who waits for it. */
    private record Incoming(MediaId media, Path part, OutputStream out, MediaReceiver receiver,
                            List<Consumer<Path>> ready) {

        Incoming(final MediaId media, final Path part, final OutputStream out, final MediaReceiver receiver) {
            this(media, part, out, receiver, new ArrayList<>());
        }
    }
}
