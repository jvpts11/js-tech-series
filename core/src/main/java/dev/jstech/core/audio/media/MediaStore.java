/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio.media;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;

/**
 * The recordings a server keeps, one file each, named by what is in it. A recording is written once however many
 * players bring it or however many disks hold it, and only a recording that can be read as one is kept: a file that
 * is not what its kind says is refused before it takes any room.
 *
 * <p>It lives with the world, so the recordings travel with the save and a world copied to another server keeps its
 * music. The world's own folder is handed in, which keeps this free of the game and testable anywhere.
 */
public final class MediaStore {

    private final Path root;
    private final Path incoming;
    /** What each recording is, read once from its file the first time anybody asks. */
    private final Map<String, MediaInfo> infos = new ConcurrentHashMap<>();

    private static final String INCOMING = "incoming";

    private static volatile @Nullable MediaStore current;

    /** The store in {@code root}, which is made when it is not there yet. */
    public MediaStore(final Path root) throws IOException {
        this.root = root;
        this.incoming = root.resolve(INCOMING);
        Files.createDirectories(incoming);
    }

    /** The store of the server that is running, when one is. */
    public static Optional<MediaStore> current() {
        return Optional.ofNullable(current);
    }

    /** Makes {@code store} the one the running server keeps its recordings in; null when the server stops. */
    public static void use(@Nullable final MediaStore store) {
        current = store;
    }

    /**
     * Keeps the recording in {@code content}, of kind {@code format}.
     *
     * @return the name it is kept under, the same one it had if it was already here
     * @throws IOException when it is not a recording of that kind, or it cannot be written
     */
    public MediaId put(final byte[] content, final String format) throws IOException {
        final MediaId id = MediaId.of(content, format);
        final Path target = path(id);
        if (!Files.exists(target)) {
            final MediaInfo info = MediaProbe.probe(id.format(), content);
            final Path temporary = Files.createTempFile(incoming, id.hash(), ".part");
            Files.write(temporary, content);
            move(temporary, target);
            infos.put(id.hash(), info);
        }
        return id;
    }

    /**
     * Keeps a recording that arrived into a file of the incoming folder, and has been checked against its name.
     *
     * @throws IOException when it is not a recording of its kind; the file is removed either way
     */
    public MediaId adopt(final Path arrived, final MediaId id) throws IOException {
        try {
            final Path target = path(id);
            if (!Files.exists(target)) {
                final MediaInfo info = MediaProbe.probe(id.format(), Files.readAllBytes(arrived));
                move(arrived, target);
                infos.put(id.hash(), info);
            }
            return id;
        } finally {
            Files.deleteIfExists(arrived);
        }
    }

    /** A fresh file in the incoming folder, for a recording on its way in. */
    public Path newIncoming(final MediaId id) throws IOException {
        return Files.createTempFile(incoming, id.hash(), ".part");
    }

    /** Whether the recording is kept here. */
    public boolean has(final MediaId id) {
        return Files.isRegularFile(path(id));
    }

    /** Where the recording's file is, whether or not it is there. */
    public Path path(final MediaId id) {
        return root.resolve(id.fileName());
    }

    /** The recording's bytes, from the start. */
    public InputStream open(final MediaId id) throws IOException {
        return Files.newInputStream(path(id));
    }

    /** What the recording is. */
    public MediaInfo info(final MediaId id) throws IOException {
        final MediaInfo known = infos.get(id.hash());
        if (known != null) {
            return known;
        }
        final MediaInfo read = MediaProbe.probe(id.format(), Files.readAllBytes(path(id)));
        infos.put(id.hash(), read);
        return read;
    }

    /** Clears out whatever a server that stopped mid-way left half-arrived. */
    public void sweepIncoming() throws IOException {
        try (Stream<Path> parts = Files.list(incoming)) {
            for (final Path part : parts.toList()) {
                Files.deleteIfExists(part);
            }
        }
    }

    /* Into place in one step where the file system allows, so nobody ever reads half a recording. */
    private static void move(final Path from, final Path to) throws IOException {
        try {
            Files.move(from, to, StandardCopyOption.ATOMIC_MOVE);
        } catch (final IOException notAtomic) {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
