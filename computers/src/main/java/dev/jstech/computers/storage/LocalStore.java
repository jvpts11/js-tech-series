/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.storage;

import dev.jstech.computers.hardware.DiskSpec;
import dev.jstech.computers.hardware.EraStorage;
import dev.jstech.computers.os.disk.DiskPartitions;
import dev.jstech.computers.os.disk.ZfsMirrors;
import dev.jstech.computers.registry.HardwareRegistry;
import java.util.ArrayList;
import java.util.Comparator;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A computer's local storage as a capacity-bounded, component-preserving type → quantity store that lives
 * on its installed disks: each volume's items are its {@link StorageVolume}, so a computer's local storage is
 * literally the union of its volumes, a disk nothing has partitioned being one and a partitioned disk one for each
 * partition that holds data (computers/docs/design/storage-devices.md). A write touches one volume in place and
 * refreshes that volume's usage summary, whatever it holds.
 */
public final class LocalStore implements IWeightedStore {

    private final List<DiskPartitions.Volume> volumes;
    /* What each volume holds, in order: the stacks every reading below walks. */
    private final List<ItemStack> disks;
    /* The machine's disks themselves, the second member of a ZFS mirror among them though it is no volume. */
    private final List<ItemStack> drives;
    private final Runnable onChanged;
    private final boolean balanced;
    private final int roomPercent;

    /** What drives hold when nothing packs their items tighter, in hundredths of their nameplate room. */
    public static final int PLAIN_ROOM = 100;

    public LocalStore(final List<ItemStack> disks, final Runnable onChanged) {
        this(disks, onChanged, false);
    }

    /**
     * @param balanced when true, writes go to the emptiest disk first instead of filling disks in
     *                 order, what the Load Balancer service buys a server: no single drive fills up
     *                 while its neighbours sit half empty.
     */
    public LocalStore(final List<ItemStack> disks, final Runnable onChanged, final boolean balanced) {
        this(disks, onChanged, balanced, PLAIN_ROOM);
    }

    /**
     * @param roomPercent how many items the drives hold, in hundredths of their nameplate room: more than a hundred
     *                    when a service packs the items tighter, which is what the Data Compressor buys a server
     */
    public LocalStore(final List<ItemStack> disks, final Runnable onChanged, final boolean balanced,
                      final int roomPercent) {
        this.volumes = DiskPartitions.storeVolumes(disks);
        this.disks = this.volumes.stream().map(DiskPartitions.Volume::stack).toList();
        this.drives = List.copyOf(disks);
        this.onChanged = onChanged;
        this.balanced = balanced;
        this.roomPercent = roomPercent;
    }

    /** The disks in the order a write should visit them: by free space when balancing, else as given. */
    private List<ItemStack> writeOrder() {
        if (!balanced) {
            return disks;
        }
        // The free weight of each disk is read once, so the sort does not look every drive up per comparison.
        final List<DiskRoom> rooms = new ArrayList<>(disks.size());
        for (final ItemStack disk : disks) {
            rooms.add(new DiskRoom(disk, room(disk) * StorageKey.MB_EQ_PER_ITEM
                    - DriveVolumes.peek(disk).usedWeight()));
        }
        rooms.sort(Comparator.comparingLong(DiskRoom::free).reversed());
        final List<ItemStack> ordered = new ArrayList<>(rooms.size());
        for (final DiskRoom room : rooms) {
            ordered.add(room.disk());
        }
        return ordered;
    }

    /*
     * The room a volume has, in items: its partition's, or a whole disk's, less what bad sectors took of it, and
     * nothing of a dead one.
     */
    private static long diskCapacity(final ItemStack disk) {
        return DriveVolumes.usableCapacity(disk, DiskPartitions.capacityItems(disk));
    }

    /* The items a drive holds here: its room, packed tighter when a service compresses what it keeps. */
    private long room(final ItemStack disk) {
        return diskCapacity(disk) * roomPercent / PLAIN_ROOM;
    }

    /** What one item costs on that drive, in megabytes: its era's word size decides. */
    private static long megabytesPerItem(final ItemStack disk) {
        final DiskSpec spec = HardwareRegistry.disk(disk);
        return spec != null ? EraStorage.mbPerItem(spec.era()) : 0L;
    }

    /** The room these drives have, in items: less what bad sectors took of them, and nothing of a dead one. */
    public long capacity() {
        long total = 0L;
        for (final ItemStack disk : disks) {
            total += room(disk);
        }
        return total;
    }

    /**
     * The nameplate size of these drives together, in megabytes: what their labels say.
     *
     * <p>Asked of the drives rather than worked out from a count, because how many megabytes an item
     * takes is the drive's own business: a vintage disk spends one on it and a standard one 256, which
     * is why the same 2 000 items fill a 2 GB drive of one era and a 500 GB drive of another.
     */
    public long capacityMb() {
        long total = 0L;
        for (final ItemStack disk : disks) {
            total += diskCapacity(disk) * megabytesPerItem(disk);
        }
        return total;
    }

    /** How many of those megabytes are spoken for, counted the same way, drive by drive, and less when packed. */
    public long usedMb() {
        long total = 0L;
        for (final ItemStack disk : disks) {
            total += DriveVolumes.peek(disk).usedWeight() * megabytesPerItem(disk)
                    / StorageKey.MB_EQ_PER_ITEM;
        }
        return total * PLAIN_ROOM / roomPercent;
    }

    /**
     * What {@code items} would cost on these drives, in megabytes, at what they charge for one.
     *
     * <p>For the parts of a machine that hold a share of the whole rather than whole drives: a logical
     * volume over an array, or the slice of its disks a personal computer publishes to the network.
     */
    public long megabytesFor(final long items) {
        final long all = capacity();
        return all <= 0L ? 0L : items * capacityMb() / all;
    }

    public long capacityWeight() {
        return capacity() * StorageKey.MB_EQ_PER_ITEM;
    }

    public long usedWeight() {
        long total = 0L;
        for (final ItemStack disk : disks) {
            total += DriveVolumes.peek(disk).usedWeight();
        }
        return total;
    }

    public long freeWeight() {
        return Math.max(0L, capacityWeight() - usedWeight());
    }

    public long used() {
        return usedWeight() / StorageKey.MB_EQ_PER_ITEM;
    }

    public long free() {
        return freeWeight() / StorageKey.MB_EQ_PER_ITEM;
    }

    public Map<StorageKey, Long> view() {
        final Map<StorageKey, Long> merged = new LinkedHashMap<>();
        for (final ItemStack disk : disks) {
            DriveVolumes.peek(disk).items().forEach((key, count) -> merged.merge(key, count, Long::sum));
        }
        return merged;
    }

    /*
     * The owner-local view/insert/extract above is always the full contents, and the slider never blocks
     * the owner at their own machine. The public/private split below is a read-only classification the
     * network sees, computed per disk from its public-share permille; it never moves items.
     */

    public int diskCount() {
        return disks.size();
    }

    /**
     * Read-only access to what the volumes hold, in slot order: a disk nothing has partitioned (or an empty slot) is
     * one, and a partitioned disk one for each partition that holds data.
     */
    public List<ItemStack> disks() {
        return Collections.unmodifiableList(disks);
    }

    /** The public-share permille of one disk (the private default when out of range or not a disk). */
    public int diskPublicPermille(final int index) {
        return index >= 0 && index < disks.size() ? DriveVolumes.publicPermille(disks.get(index)) : 0;
    }

    /**
     * Sets one volume's public share, written back with its disk; false for an index past them or an empty slot, where
     * nothing is set.
     */
    public boolean setPublicPermille(final int index, final int permille) {
        if (index < 0 || index >= disks.size() || !DriveVolumes.isDisk(disks.get(index))) {
            return false;
        }
        DriveVolumes.setPublicPermille(disks.get(index), permille);
        volumes.get(index).commit();
        return true;
    }

    /** The used data weight stored on one disk. */
    public long diskUsedWeight(final int index) {
        return index >= 0 && index < disks.size() ? DriveVolumes.peek(disks.get(index)).usedWeight() : 0L;
    }

    /** The capacity data weight of one disk. */
    public long diskCapacityWeight(final int index) {
        return index >= 0 && index < disks.size()
                ? diskCapacity(disks.get(index)) * StorageKey.MB_EQ_PER_ITEM : 0L;
    }

    /**
     * How many items this computer has published to the network, counting each disk's own share.
     *
     * <p>A disk kept entirely private adds nothing: the network's capacity is what the network may
     * actually fill, not what the machine happens to have installed.
     */
    public long publicCapacity() {
        long sum = 0L;
        for (int i = 0; i < disks.size(); i++) {
            sum += diskCapacityWeight(i) / StorageKey.MB_EQ_PER_ITEM * diskPublicPermille(i) / 1000L;
        }
        return sum;
    }

    /** One disk's public view: what of it the network may read, and the only part of it corruption can reach. */
    public static Map<StorageKey, Long> publicViewOf(final ItemStack disk) {
        return DiskStorageView.publicView(DriveVolumes.peek(disk).items(),
                diskCapacity(disk) * StorageKey.MB_EQ_PER_ITEM, DriveVolumes.publicPermille(disk));
    }

    /** The union of every disk's public view, what the network may read from this computer. */
    public Map<StorageKey, Long> publicView() {
        final Map<StorageKey, Long> merged = new LinkedHashMap<>();
        for (final ItemStack disk : disks) {
            final long capacityWeight = diskCapacity(disk) * StorageKey.MB_EQ_PER_ITEM;
            DiskStorageView.publicView(DriveVolumes.peek(disk).items(), capacityWeight,
                            DriveVolumes.publicPermille(disk))
                    .forEach((key, count) -> merged.merge(key, count, Long::sum));
        }
        return merged;
    }

    /** The total public weight across every disk. */
    public long publicWeight() {
        long total = 0L;
        for (final ItemStack disk : disks) {
            final long capacityWeight = diskCapacity(disk) * StorageKey.MB_EQ_PER_ITEM;
            total += DiskStorageView.publicWeight(DriveVolumes.peek(disk).items(), capacityWeight,
                    DriveVolumes.publicPermille(disk));
        }
        return total;
    }

    /** The union of every disk's private view, owner-only, never offered to the network. */
    public Map<StorageKey, Long> privateView() {
        final Map<StorageKey, Long> merged = new LinkedHashMap<>();
        for (final ItemStack disk : disks) {
            final long capacityWeight = diskCapacity(disk) * StorageKey.MB_EQ_PER_ITEM;
            DiskStorageView.privateView(DriveVolumes.peek(disk).items(), capacityWeight,
                            DriveVolumes.publicPermille(disk))
                    .forEach((key, count) -> merged.merge(key, count, Long::sum));
        }
        return merged;
    }

    /**
     * Extracts up to {@code amount} of {@code key} but only from the public share of each disk, so a network pull can never reach a private item. Used by the network SELECT path; the owner-local {@link #extract} is unaffected.
     */
    public long extractPublic(final StorageKey key, final long amount) {
        if (amount <= 0L) {
            return 0L;
        }
        long taken = 0L;
        for (final ItemStack disk : disks) {
            if (taken >= amount) {
                break;
            }
            final StorageVolume volume = DriveVolumes.peek(disk);
            healOnRead(disk, volume, key);
            if (volume.isEmpty()) {
                continue;
            }
            final long capacityWeight = diskCapacity(disk) * StorageKey.MB_EQ_PER_ITEM;
            final long publicHave = DiskStorageView.publicView(volume.items(), capacityWeight,
                    DriveVolumes.publicPermille(disk)).getOrDefault(key, 0L);
            if (publicHave <= 0L) {
                continue;
            }
            final long got = volume.take(key, Math.min(amount - taken, publicHave));
            if (got > 0L) {
                refreshed(disk, volume);
                taken += got;
            }
        }
        if (taken > 0L) {
            onChanged.run();
        }
        return taken;
    }

    public long count(final StorageKey key) {
        long total = 0L;
        for (final ItemStack disk : disks) {
            total += DriveVolumes.peek(disk).count(key);
        }
        return total;
    }

    public long insert(final StorageKey key, final long amount) {
        if (amount <= 0L) {
            return 0L;
        }
        final long unitWeight = key.weight(1L); // 1000 for an item, 1 per mB of fluid
        long remaining = amount;
        for (final ItemStack disk : writeOrder()) {
            if (remaining <= 0L) {
                break;
            }
            final long capacityWeight = room(disk) * StorageKey.MB_EQ_PER_ITEM;
            if (capacityWeight <= 0L) {
                continue;
            }
            final StorageVolume volume = DriveVolumes.of(disk);
            if (volume == StorageVolume.EMPTY) {
                // Off the server thread the volume is the read-only blank one; writing to it would lose the items.
                continue;
            }
            final long roomNative = (capacityWeight - volume.usedWeight()) / unitWeight;
            if (roomNative <= 0L) {
                continue;
            }
            final long put = Math.min(remaining, roomNative);
            volume.add(key, put);
            refreshed(disk, volume);
            remaining -= put;
        }
        final long stored = amount - remaining;
        if (stored > 0L) {
            onChanged.run();
        }
        return stored;
    }

    public long extract(final StorageKey key, final long amount) {
        if (amount <= 0L) {
            return 0L;
        }
        long taken = 0L;
        for (final ItemStack disk : disks) {
            if (taken >= amount) {
                break;
            }
            final StorageVolume volume = DriveVolumes.peek(disk);
            healOnRead(disk, volume, key);
            final long got = volume.take(key, amount - taken);
            if (got > 0L) {
                refreshed(disk, volume);
                taken += got;
            }
        }
        if (taken > 0L) {
            onChanged.run();
        }
        return taken;
    }

    /*
     * Reading a key corruption damaged on a ZFS mirror repairs it from the other disk first, as ZFS does when it reads
     * a damaged part (computers/docs/design/storage-devices.md); on anything else the damage stays for the Defrag.
     */
    private void healOnRead(final ItemStack disk, final StorageVolume volume, final StorageKey key) {
        if (volume.corrupted().containsKey(key) && ZfsMirrors.heal(disk, drives, key) > 0L) {
            refreshed(disk, volume);
        }
    }

    /* Refreshes a volume's usage summary and writes it back with its disk, since a partition's view is a copy. */
    private void refreshed(final ItemStack disk, final StorageVolume volume) {
        DriveVolumes.refreshUsage(disk, volume);
        for (final DiskPartitions.Volume each : volumes) {
            if (each.stack() == disk) {
                each.commit();
                return;
            }
        }
    }

    private record DiskRoom(ItemStack disk, long free) {
    }
}
