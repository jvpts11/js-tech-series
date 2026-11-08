/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.storage;

import dev.jstech.computers.os.fs.FilesystemContents;
import dev.jstech.computers.os.fs.StoredFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.DoubleSupplier;

/**
 * The stored items of one drive, kept in the level's volume store rather than on the item: a mutable
 * key → quantity map with its data weight and item count carried alongside, so an insert or extract is
 * one map update however much the drive holds. The drive item itself carries only the volume's id and
 * a small usage summary, which is what keeps a full drive cheap to copy, compare, sync and save.
 *
 * <p>A volume also keeps what corruption did to it (computers/docs/design/storage.md): the items corrupted, out of
 * the contents and waiting for the Defrag, which still take their room; the system files corrupted, out of the disk's
 * file system and waiting the same way; the share of the disk lost to bad sectors; and whether the disk died.
 */
public final class StorageVolume {

    /** A blank volume that refuses writes: what a drive without one, or a client, sees. */
    public static final StorageVolume EMPTY = new StorageVolume(new UUID(0L, 0L), () -> { }, true);

    private final UUID id;
    private final Runnable dirty;
    private final boolean readOnly;
    private final Map<StorageKey, Long> items = new LinkedHashMap<>();
    private final Map<StorageKey, Long> corrupted = new LinkedHashMap<>();
    private long usedWeight;
    /*
     * Running totals per kind, kept alongside the map so the usage summary never walks the contents:
     * a drive holding thousands of types is written to on every insert, and that walk was the cost the
     * volume store exists to avoid.
     */
    private long itemUnits;
    private long fluidUnits;
    private long chemicalUnits;
    private long corruptedWeight;
    private FilesystemContents corruptedFiles = FilesystemContents.EMPTY;
    private int badSectors;
    private boolean dead;

    StorageVolume(final UUID id, final Runnable dirty, final boolean readOnly) {
        this.id = id;
        this.dirty = dirty;
        this.readOnly = readOnly;
    }

    public UUID id() {
        return id;
    }

    /** The contents, read-only and live: a snapshot must be taken to keep it across a write. */
    public Map<StorageKey, Long> items() {
        return Collections.unmodifiableMap(items);
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    /** How many distinct keys the volume holds. */
    public int types() {
        return items.size();
    }

    /** How many units (items, or millibuckets) the volume holds in all. */
    public long total() {
        return itemUnits + fluidUnits + chemicalUnits;
    }

    /** How many items the volume holds, by the piece. */
    public long itemUnits() {
        return itemUnits;
    }

    /** How much fluid the volume holds, in millibuckets. */
    public long fluidUnits() {
        return fluidUnits;
    }

    /** How much chemical the volume holds, in millibuckets. */
    public long chemicalUnits() {
        return chemicalUnits;
    }

    /**
     * The data weight of everything stored, in the same units a disk's capacity is measured in. Corrupted items count:
     * they are still on the disk, taking their room, until a Defrag gives them back or loses them.
     */
    public long usedWeight() {
        return usedWeight + corruptedWeight;
    }

    private void tally(final StorageKey key, final long delta) {
        usedWeight += key.weight(delta);
        // Gases and the other states of matter are counted with the chemicals, in millibuckets of other matter.
        switch (key.kind()) {
            case ITEM -> itemUnits += delta;
            case FLUID -> fluidUnits += delta;
            default -> chemicalUnits += delta;
        }
    }

    public long count(final StorageKey key) {
        return items.getOrDefault(key, 0L);
    }

    /** Adds {@code amount} of {@code key}; returns the key's new count. The caller checks the room. */
    public long add(final StorageKey key, final long amount) {
        if (readOnly || amount <= 0L) {
            return count(key);
        }
        final long now = items.merge(key, amount, Long::sum);
        tally(key, amount);
        dirty.run();
        return now;
    }

    /** Takes up to {@code amount} of {@code key}; returns how many came out. */
    public long take(final StorageKey key, final long amount) {
        if (readOnly || amount <= 0L) {
            return 0L;
        }
        final long have = count(key);
        if (have <= 0L) {
            return 0L;
        }
        final long taken = Math.min(have, amount);
        if (taken == have) {
            items.remove(key);
        } else {
            items.put(key, have - taken);
        }
        tally(key, -taken);
        dirty.run();
        return taken;
    }

    /** Empties the volume. */
    public void clear() {
        if (readOnly || items.isEmpty()) {
            return;
        }
        items.clear();
        resetTallies();
        dirty.run();
    }

    private void resetTallies() {
        usedWeight = 0L;
        itemUnits = 0L;
        fluidUnits = 0L;
        chemicalUnits = 0L;
    }

    /** Replaces the whole contents; entries that are not positive are dropped. */
    public void replaceAll(final Map<StorageKey, Long> contents) {
        if (readOnly) {
            return;
        }
        items.clear();
        resetTallies();
        for (final Map.Entry<StorageKey, Long> entry : contents.entrySet()) {
            final Long count = entry.getValue();
            if (count != null && count > 0L) {
                items.put(entry.getKey(), count);
                tally(entry.getKey(), count);
            }
        }
        dirty.run();
    }

    /** An immutable copy of the contents, for code that reads a whole drive at once. */
    public ServerStorageContents snapshot() {
        return new ServerStorageContents(items);
    }

    /** Whether the volume holds nothing at all and nothing ever happened to it: it need not be kept. */
    public boolean blank() {
        return items.isEmpty() && corrupted.isEmpty() && corruptedFiles.files().isEmpty() && badSectors == 0 && !dead;
    }

    /** The items corrupted, out of the contents and waiting for the Defrag, read-only and live. */
    public Map<StorageKey, Long> corrupted() {
        return Collections.unmodifiableMap(corrupted);
    }

    /** What the corrupted items weigh, which they still take of the disk. */
    public long corruptedWeight() {
        return corruptedWeight;
    }

    /** The system files corrupted, out of the disk's file system and waiting for the Defrag. */
    public FilesystemContents corruptedFiles() {
        return corruptedFiles;
    }

    /** How much of what the disk stores is corrupted, in permille. */
    public int damage() {
        return Corruption.damage(corruptedWeight, usedWeight());
    }

    /** How much of the disk's capacity is lost to bad sectors, in permille. */
    public int badSectors() {
        return badSectors;
    }

    /** Whether the disk died of its corruption: it holds nothing and takes nothing. */
    public boolean dead() {
        return dead;
    }

    /**
     * Corrupts {@code share} of each key {@code exposed} holds (never more than the volume holds of it): those items
     * leave the contents for the corrupted ones. The damage that leaves can hurt the disk itself: past half, bad
     * sectors take the capacity it goes over half by, and past nine tenths the disk dies.
     *
     * @param exposed what corruption can reach, which for a Personal Computer is its public part alone
     * @return the weight corrupted
     */
    public long corrupt(final Map<StorageKey, Long> exposed, final double share, final DoubleSupplier random) {
        if (readOnly || dead) {
            return 0L;
        }
        long weight = 0L;
        for (final Map.Entry<StorageKey, Long> entry : new ArrayList<>(exposed.entrySet())) {
            final StorageKey key = entry.getKey();
            final long reached = Math.min(count(key), entry.getValue() == null ? 0L : entry.getValue());
            final long hit = Corruption.share(reached, share, random);
            if (hit <= 0L) {
                continue;
            }
            removeIntact(key, hit);
            corrupted.merge(key, hit, Long::sum);
            corruptedWeight += key.weight(hit);
            weight += key.weight(hit);
        }
        if (weight > 0L) {
            hurt();
            dirty.run();
        }
        return weight;
    }

    /** Puts a corrupted system file aside, out of the disk's file system, to wait for the Defrag. */
    public void corruptFile(final StoredFile file) {
        if (readOnly || dead) {
            return;
        }
        corruptedFiles = corruptedFiles.with(file);
        dirty.run();
    }

    /** Gives back {@code amount} of a corrupted key: it is part of the contents again. Returns how many came back. */
    public long recover(final StorageKey key, final long amount) {
        final long back = settleCorrupted(key, amount);
        if (back > 0L) {
            items.merge(key, back, Long::sum);
            tally(key, back);
            dirty.run();
        }
        return back;
    }

    /** Loses {@code amount} of a corrupted key for good. Returns how many were lost. */
    public long lose(final StorageKey key, final long amount) {
        final long gone = settleCorrupted(key, amount);
        if (gone > 0L) {
            dirty.run();
        }
        return gone;
    }

    /** Takes a corrupted system file off the list, whether it is given back to the disk or lost. */
    public void settleFile(final String path) {
        if (corruptedFiles.files().containsKey(path)) {
            corruptedFiles = corruptedFiles.without(path);
            dirty.run();
        }
    }

    /** Sets the share of the disk lost to bad sectors, in permille: a pass mends some of them. */
    public void badSectors(final int permille) {
        final int now = Math.max(0, Math.min(Corruption.WHOLE, permille));
        if (!readOnly && now != badSectors) {
            badSectors = now;
            dirty.run();
        }
    }

    /** Puts back what was saved of the corruption: the corrupted items and files, the bad sectors, a death. */
    void restore(final Map<StorageKey, Long> wasCorrupted, final FilesystemContents files, final int bad,
                 final boolean died) {
        corrupted.clear();
        corruptedWeight = 0L;
        for (final Map.Entry<StorageKey, Long> entry : wasCorrupted.entrySet()) {
            if (entry.getValue() != null && entry.getValue() > 0L) {
                corrupted.put(entry.getKey(), entry.getValue());
                corruptedWeight += entry.getKey().weight(entry.getValue());
            }
        }
        corruptedFiles = files == null ? FilesystemContents.EMPTY : files;
        badSectors = Math.max(0, Math.min(Corruption.WHOLE, bad));
        dead = died;
    }

    /* Takes up to {@code amount} of a key off the corrupted ones; returns how many came off. */
    private long settleCorrupted(final StorageKey key, final long amount) {
        final long have = corrupted.getOrDefault(key, 0L);
        if (readOnly || amount <= 0L || have <= 0L) {
            return 0L;
        }
        final long off = Math.min(have, amount);
        if (off == have) {
            corrupted.remove(key);
        } else {
            corrupted.put(key, have - off);
        }
        corruptedWeight -= key.weight(off);
        return off;
    }

    /* Takes {@code amount} of a key out of the contents, which hold at least that much. */
    private void removeIntact(final StorageKey key, final long amount) {
        final long have = count(key);
        if (amount >= have) {
            items.remove(key);
        } else {
            items.put(key, have - amount);
        }
        tally(key, -amount);
    }

    /*
     * What the damage now does to the disk itself: bad sectors past half (they never shrink here, only a pass mends
     * them), and past nine tenths the end of it, taking all it held.
     */
    private void hurt() {
        final int damage = damage();
        if (Corruption.dies(damage)) {
            items.clear();
            corrupted.clear();
            resetTallies();
            corruptedWeight = 0L;
            corruptedFiles = FilesystemContents.EMPTY;
            badSectors = Corruption.WHOLE;
            dead = true;
            return;
        }
        badSectors = Math.max(badSectors, Corruption.badSectors(damage));
    }
}
