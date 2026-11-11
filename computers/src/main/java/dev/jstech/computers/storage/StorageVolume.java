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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.DoubleSupplier;
import org.jetbrains.annotations.Nullable;

/**
 * The stored items of one drive, kept in the level's volume store rather than on the item: a mutable
 * key → quantity map with its data weight and item count carried alongside, so an insert or extract is
 * one map update however much the drive holds. The drive item itself carries only the volume's id and
 * a small usage summary, which is what keeps a full drive cheap to copy, compare, sync and save.
 *
 * <p>A volume also keeps what corruption did to it (computers/docs/design/storage.md): the items corrupted, out of
 * the contents and waiting for the Defrag, which still take their room; the system files corrupted, out of the disk's
 * file system and waiting the same way; the share of the disk lost to bad sectors; and whether the disk died.
 *
 * <p>And the snapshots btrfs and ZFS took of it (computers/docs/design/storage-devices.md), each the items and files it
 * held then: a snapshot shares what the volume still holds, so it takes room only for what changed since.
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
    /* The snapshots taken of it (computers/docs/design/storage-devices.md), oldest first. */
    private final List<VolumeSnapshot> snapshots = new ArrayList<>();
    /*
     * The most any snapshot keeps of each key, and the weight of what they keep that the contents no longer hold.
     * Snapshots share what they keep, so a key two of them hold weighs once, and a write moves that weight by its own
     * key alone instead of walking every snapshot.
     */
    private final Map<StorageKey, Long> keptMost = new HashMap<>();
    private long keptWeight;
    /* The snapshot btrfs starts from next, once one is set as its default; null when none is. */
    @Nullable
    private UUID startsFrom;

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
     * they are still on the disk, taking their room, until a Defrag gives them back or loses them. So does what the
     * snapshots keep that the contents no longer hold.
     */
    public long usedWeight() {
        return usedWeight + corruptedWeight + keptWeight;
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
        reweigh(key, now - amount, now);
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
        reweigh(key, have, have - taken);
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
        reweighAll();
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
        reweighAll();
        dirty.run();
    }

    /** An immutable copy of the contents, for code that reads a whole drive at once. */
    public ServerStorageContents snapshot() {
        return new ServerStorageContents(items);
    }

    /** Whether the volume holds nothing at all and nothing ever happened to it: it need not be kept. */
    public boolean blank() {
        return items.isEmpty() && corrupted.isEmpty() && corruptedFiles.files().isEmpty() && badSectors == 0 && !dead
                && snapshots.isEmpty();
    }

    /** The snapshots taken of the volume, oldest first. */
    public List<VolumeSnapshot> snapshots() {
        return Collections.unmodifiableList(snapshots);
    }

    /** What the snapshots keep that the contents no longer hold, which takes room on the disk as the contents do. */
    public long keptWeight() {
        return keptWeight;
    }

    /** What only {@code snapshot} keeps: what neither the contents nor another snapshot hold of it. */
    public long heldOnlyBy(final VolumeSnapshot snapshot) {
        long weight = 0L;
        for (final Map.Entry<StorageKey, Long> kept : snapshot.items().entrySet()) {
            long elsewhere = count(kept.getKey());
            for (final VolumeSnapshot other : snapshots) {
                if (!other.id().equals(snapshot.id())) {
                    elsewhere = Math.max(elsewhere, other.items().getOrDefault(kept.getKey(), 0L));
                }
            }
            weight += kept.getKey().weight(Math.max(0L, kept.getValue() - elsewhere));
        }
        return weight;
    }

    /** Keeps a snapshot, which weighs nothing until the contents move away from it. */
    public void keep(final VolumeSnapshot snapshot) {
        if (readOnly || dead) {
            return;
        }
        snapshots.add(snapshot);
        reweighAll();
        dirty.run();
    }

    /** Lets a snapshot go, and with it the weight only it kept. */
    public void forget(final UUID snapshot) {
        if (!readOnly && snapshots.removeIf(kept -> kept.id().equals(snapshot))) {
            if (snapshot.equals(startsFrom)) {
                startsFrom = null;
            }
            reweighAll();
            dirty.run();
        }
    }

    /**
     * Makes the contents what {@code snapshot} kept: its items come back, and what corruption had put aside goes, since
     * the partition is again as it was before any of it. The snapshot stays, as it does on btrfs and ZFS.
     */
    public void rollBack(final VolumeSnapshot snapshot) {
        if (readOnly || dead) {
            return;
        }
        corrupted.clear();
        corruptedWeight = 0L;
        corruptedFiles = FilesystemContents.EMPTY;
        replaceAll(snapshot.items());
    }

    /** The snapshot btrfs starts from next, set as its default, or null for none. */
    @Nullable
    public UUID startsFrom() {
        return startsFrom;
    }

    /** Sets the snapshot btrfs starts from next, or none. */
    public void startFrom(@Nullable final UUID snapshot) {
        if (!readOnly && !Objects.equals(startsFrom, snapshot)) {
            startsFrom = snapshot;
            dirty.run();
        }
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
        return Corruption.damage(corruptedWeight, usedWeight + corruptedWeight);
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
            final long now = items.merge(key, back, Long::sum);
            tally(key, back);
            reweigh(key, now - back, now);
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

    /** Puts back the snapshots that were saved, and the one btrfs was to start from. */
    void restoreSnapshots(final List<VolumeSnapshot> saved, @Nullable final UUID starts) {
        snapshots.clear();
        snapshots.addAll(saved);
        startsFrom = starts;
        reweighAll();
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
        reweigh(key, have, Math.max(0L, have - amount));
    }

    /* Moves the kept weight by one key's change in the contents, from {@code before} of it to {@code after}. */
    private void reweigh(final StorageKey key, final long before, final long after) {
        final Long most = keptMost.get(key);
        if (most != null) {
            keptWeight += key.weight(Math.max(0L, most - after)) - key.weight(Math.max(0L, most - before));
        }
    }

    /* Works the kept weight out afresh, after the snapshots or the whole contents changed. */
    private void reweighAll() {
        keptMost.clear();
        for (final VolumeSnapshot snapshot : snapshots) {
            for (final Map.Entry<StorageKey, Long> kept : snapshot.items().entrySet()) {
                keptMost.merge(kept.getKey(), kept.getValue(), Math::max);
            }
        }
        keptWeight = 0L;
        for (final Map.Entry<StorageKey, Long> kept : keptMost.entrySet()) {
            keptWeight += kept.getKey().weight(Math.max(0L, kept.getValue() - count(kept.getKey())));
        }
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
            // A dead disk holds nothing, its snapshots included.
            snapshots.clear();
            startsFrom = null;
            reweighAll();
            return;
        }
        badSectors = Math.max(badSectors, Corruption.badSectors(damage));
    }
}
