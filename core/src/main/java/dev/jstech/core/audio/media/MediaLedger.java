/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio.media;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

/**
 * What a server knows of each recording it keeps beyond its bytes: who brought it first, and when anything last made
 * use of it. It is what lets each player's share of the store be counted, and what the store's owner goes by to clear
 * out recordings nobody has used for a long time.
 *
 * <p>A recording's use is written down to the hour at most, so a song played over and over does not make the ledger
 * be kept again every time. The ledger is kept with the world, by whatever its store is handed to keep it. A world from
 * before that kept it as text beside the recordings, a line to a recording, which is still read once.
 *
 * <p>This class is pure and carries no Minecraft dependency.
 */
final class MediaLedger {

    private final Map<String, Entry> entries = new HashMap<>();
    private boolean dirty;

    /** What the text ledger of before started with, so a file that is something else is not read as one. */
    static final String HEADER = "JSMEDIA1";
    /** How much time has to pass before a recording's use is written down again. */
    static final long GRAIN_MILLIS = 60L * 60L * 1000L;
    private static final String NOBODY = "-";

    /**
     * One recording.
     *
     * @param media     the recording
     * @param lastUsed  when anything last made use of it, in milliseconds since the epoch
     * @param broughtBy who brought it first, or null for a recording nobody brought (a catalogue's)
     */
    record Entry(MediaId media, long lastUsed, @Nullable UUID broughtBy) {
    }

    /** A ledger holding {@code kept}, as it was last kept. */
    static MediaLedger of(final Collection<Entry> kept) {
        final MediaLedger ledger = new MediaLedger();
        for (final Entry entry : kept) {
            ledger.entries.put(entry.media().fileName(), entry);
        }
        return ledger;
    }

    /** A recording was made use of at {@code now}; one the ledger does not know yet is written down. */
    synchronized void used(final MediaId media, final long now) {
        final Entry entry = entries.get(media.fileName());
        if (entry == null) {
            entries.put(media.fileName(), new Entry(media, now, null));
            dirty = true;
        } else if (now - entry.lastUsed() >= GRAIN_MILLIS) {
            entries.put(media.fileName(), new Entry(media, now, entry.broughtBy()));
            dirty = true;
        }
    }

    /** A player brought a recording at {@code now}; it stays theirs only when nobody had brought it before. */
    synchronized void brought(final MediaId media, final UUID player, final long now) {
        final Entry entry = entries.get(media.fileName());
        if (entry == null || entry.broughtBy() == null) {
            entries.put(media.fileName(), new Entry(media, now, player));
            dirty = true;
        } else {
            used(media, now);
        }
    }

    /** How many bytes of the recordings kept were brought first by that player. */
    synchronized long broughtBytes(final UUID player) {
        long total = 0L;
        for (final Entry entry : entries.values()) {
            if (player.equals(entry.broughtBy())) {
                total += entry.media().bytes();
            }
        }
        return total;
    }

    /** Every recording the ledger knows. */
    synchronized List<Entry> entries() {
        return List.copyOf(entries.values());
    }

    /** The recordings nothing has used since {@code cutoff}, leaving out those in {@code kept}. */
    synchronized List<MediaId> unusedSince(final long cutoff, final Set<MediaId> kept) {
        final List<MediaId> unused = new ArrayList<>();
        for (final Entry entry : entries.values()) {
            if (entry.lastUsed() < cutoff && !kept.contains(entry.media())) {
                unused.add(entry.media());
            }
        }
        return unused;
    }

    /** The recording is gone from the store. */
    synchronized void forget(final MediaId media) {
        if (entries.remove(media.fileName()) != null) {
            dirty = true;
        }
    }

    /**
     * Puts the ledger in step with the recordings the store holds: one it did not know is written down as used at
     * {@code now}, so a store kept before there was a ledger loses nothing at once, and one it knew that is gone is
     * forgotten.
     */
    synchronized void reconcile(final Collection<MediaId> held, final long now) {
        final Map<String, MediaId> byName = new HashMap<>();
        for (final MediaId media : held) {
            byName.put(media.fileName(), media);
            if (!entries.containsKey(media.fileName())) {
                entries.put(media.fileName(), new Entry(media, now, null));
                dirty = true;
            }
        }
        if (entries.keySet().removeIf(name -> !byName.containsKey(name))) {
            dirty = true;
        }
    }

    /** Whether something changed since the ledger was last kept. */
    synchronized boolean dirty() {
        return dirty;
    }

    /** Marks the ledger as changed, for one read from somewhere it is no longer kept. */
    synchronized void markChanged() {
        dirty = true;
    }

    /** Every entry, by the name of its recording, which the ledger is then taken to have been kept as. */
    synchronized List<Entry> taken() {
        final List<String> names = new ArrayList<>(entries.keySet());
        names.sort(null);
        final List<Entry> out = new ArrayList<>(names.size());
        for (final String name : names) {
            out.add(entries.get(name));
        }
        dirty = false;
        return out;
    }

    /**
     * A ledger as the text of before had it; a line that cannot be read is left out, and text that is no ledger reads
     * empty.
     */
    static MediaLedger read(final String text) {
        final MediaLedger ledger = new MediaLedger();
        final String[] lines = text.split("\n");
        if (lines.length == 0 || !HEADER.equals(lines[0].strip())) {
            return ledger;
        }
        for (int i = 1; i < lines.length; i++) {
            final Entry entry = entryOf(lines[i].strip());
            if (entry != null) {
                ledger.entries.put(entry.media().fileName(), entry);
            }
        }
        return ledger;
    }

    @Nullable
    private static Entry entryOf(final String line) {
        final String[] parts = line.split(" ");
        if (parts.length != 4) {
            return null;
        }
        final int dot = parts[0].indexOf('.');
        try {
            final MediaId media = new MediaId(parts[0].substring(0, Math.max(0, dot)),
                    dot < 0 ? "" : parts[0].substring(dot + 1), Long.parseLong(parts[1]));
            final UUID broughtBy = NOBODY.equals(parts[3]) ? null : UUID.fromString(parts[3]);
            return new Entry(media, Long.parseLong(parts[2]), broughtBy);
        } catch (final IllegalArgumentException malformed) {
            return null;
        }
    }
}
