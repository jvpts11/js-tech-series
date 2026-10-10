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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;

/**
 * The recordings a server keeps, one file each, named by what is in it. A recording is written once however many
 * players bring it or however many disks hold it, and only a recording that can be read as one is kept: a file that
 * is not what its kind says is refused before it takes any room.
 *
 * <p>It lives with the world, so the recordings travel with the save and a world copied to another server keeps its
 * music. The world's own folder and what keeps the ledger are handed in, which keeps this free of the game and testable
 * anywhere.
 */
public final class MediaStore {

    private final Path root;
    private final Path incoming;
    /** What each recording is, read once from its file the first time anybody asks. */
    private final Map<String, MediaInfo> infos = new ConcurrentHashMap<>();
    /** Who brought each recording and when it was last used. */
    private final MediaLedger ledger;
    private final IMediaLedgerKeeper keeper;
    private final LongSupplier clock;

    private static final String INCOMING = "incoming";
    /** The file the ledger was kept in, beside the recordings, before it was kept with the world. */
    private static final String LEDGER = "ledger.txt";

    private static volatile @Nullable MediaStore current;

    /** The store in {@code root}, which is made when it is not there yet, its ledger kept by {@code keeper}. */
    MediaStore(final Path root, final IMediaLedgerKeeper keeper) throws IOException {
        this(root, System::currentTimeMillis, keeper);
    }

    /**
     * The same, telling the time by {@code clock}, in milliseconds since the epoch.
     *
     * <p>A ledger never kept by {@code keeper} is read from the text file the store kept it in before, when there is
     * one, and kept at the next {@link #flush()}. That file goes once the keeper has the ledger.
     */
    MediaStore(final Path root, final LongSupplier clock, final IMediaLedgerKeeper keeper) throws IOException {
        this.root = root;
        this.incoming = root.resolve(INCOMING);
        this.keeper = keeper;
        this.clock = clock;
        Files.createDirectories(incoming);
        final Path textLedger = root.resolve(LEDGER);
        final List<MediaLedger.Entry> kept = keeper.load();
        if (kept != null) {
            this.ledger = MediaLedger.of(kept);
            // A text ledger still here was read in an earlier run, and the world was saved with it since.
            Files.deleteIfExists(textLedger);
        } else if (Files.isRegularFile(textLedger)) {
            this.ledger = MediaLedger.read(Files.readString(textLedger, StandardCharsets.UTF_8));
            this.ledger.markChanged();
        } else {
            this.ledger = new MediaLedger();
        }
        ledger.reconcile(held(), clock.getAsLong());
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
        if (!has(id)) {
            final MediaInfo info = MediaProbe.probe(id.format(), content);
            final Path temporary = Files.createTempFile(incoming, id.hash(), ".part");
            Files.write(temporary, content);
            move(temporary, target);
            infos.put(id.hash(), info);
        }
        used(id);
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
            if (!has(id)) {
                final MediaInfo info = MediaProbe.probe(id.format(), Files.readAllBytes(arrived));
                move(arrived, target);
                infos.put(id.hash(), info);
            }
            used(id);
            return id;
        } finally {
            Files.deleteIfExists(arrived);
        }
    }

    /** Something made use of the recording now: it was played, fetched, put on a disk or offered. */
    public void used(final MediaId id) {
        ledger.used(id, clock.getAsLong());
    }

    /** A player brought the recording; it counts towards their share only when nobody had brought it before. */
    public void broughtBy(final MediaId id, final UUID player) {
        ledger.brought(id, player, clock.getAsLong());
    }

    /** How many bytes of the store the recordings a player brought first take. */
    public long broughtBytes(final UUID player) {
        return ledger.broughtBytes(player);
    }

    /** How many recordings the store keeps, and how many bytes they take together. */
    public Held size() {
        long bytes = 0L;
        final List<MediaLedger.Entry> entries = ledger.entries();
        for (final MediaLedger.Entry entry : entries) {
            bytes += entry.media().bytes();
        }
        return new Held(entries.size(), bytes);
    }

    /**
     * Takes out every recording nothing has used for {@code unusedMillis}, leaving those some mod still needs
     * ({@link MediaKeepers}), and gives each player back the share their recordings took. A disk that still names
     * one of them finds it gone, and says so when it is played.
     *
     * @return how many recordings went and the bytes they took
     */
    public Held prune(final long unusedMillis, final Set<MediaId> kept) throws IOException {
        int count = 0;
        long bytes = 0L;
        for (final MediaId media : ledger.unusedSince(clock.getAsLong() - Math.max(0L, unusedMillis), kept)) {
            Files.deleteIfExists(path(media));
            infos.remove(media.hash());
            ledger.forget(media);
            count++;
            bytes += media.bytes();
        }
        flush();
        return new Held(count, bytes);
    }

    /**
     * Hands the ledger to its keeper when something in it changed; whatever calls this does so now and then, and
     * before the world is saved for the last time.
     */
    public void flush() {
        if (ledger.dirty()) {
            keeper.keep(ledger.taken());
        }
    }

    /**
     * A number of recordings and the bytes they take.
     *
     * @param count how many
     * @param bytes how many bytes together
     */
    public record Held(int count, long bytes) {
    }

    /** A fresh file in the incoming folder, for a recording on its way in. */
    public Path newIncoming(final MediaId id) throws IOException {
        return Files.createTempFile(incoming, id.hash(), ".part");
    }

    /**
     * Whether the recording is kept here: a file under its name with as many bytes as it says. A name with the right
     * hash and another size is not this recording, however it came to be written.
     */
    public boolean has(final MediaId id) {
        final Path file = path(id);
        try {
            return Files.isRegularFile(file) && Files.size(file) == id.bytes();
        } catch (final IOException unreadable) {
            return false;
        }
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

    /* The recordings the folder holds, by the names their files have; anything else in it is left alone. */
    private List<MediaId> held() throws IOException {
        final List<MediaId> held = new ArrayList<>();
        try (Stream<Path> files = Files.list(root)) {
            for (final Path file : files.filter(Files::isRegularFile).toList()) {
                final String name = file.getFileName().toString();
                final int dot = name.indexOf('.');
                if (dot <= 0 || name.equals(LEDGER)) {
                    continue;
                }
                try {
                    held.add(new MediaId(name.substring(0, dot), name.substring(dot + 1), Files.size(file)));
                } catch (final IllegalArgumentException notARecording) {
                    // Not named the way a recording is: nothing of the store's.
                }
            }
        }
        return held;
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
