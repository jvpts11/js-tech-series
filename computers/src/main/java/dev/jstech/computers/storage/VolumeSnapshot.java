/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.storage;

import dev.jstech.computers.os.fs.FilesystemContents;
import java.util.Map;
import java.util.UUID;

/**
 * A partition kept as it was when a snapshot was taken (computers/docs/design/storage-devices.md): the items it held
 * and its files. btrfs and ZFS take one at once, sharing everything with the partition until it changes, so a snapshot
 * weighs only what changed since; rolling back makes the partition hold it again.
 *
 * @param id      what tells it from every other snapshot, for the save
 * @param number  the number it is listed and chosen by, as btrfs numbers its subvolumes
 * @param name    the name it was taken under: a path for btrfs, a dataset and a name after the at sign for ZFS
 * @param takenAt the world's time of day when it was taken, in ticks
 * @param items   the items the partition held
 * @param files   the files it held
 */
public record VolumeSnapshot(UUID id, int number, String name, long takenAt, Map<StorageKey, Long> items,
                             FilesystemContents files) {

    public VolumeSnapshot {
        items = Map.copyOf(items);
        files = files == null ? FilesystemContents.EMPTY : files;
    }

    /** The data weight of the items it keeps, as if nothing were shared. */
    public long itemsWeight() {
        long weight = 0L;
        for (final Map.Entry<StorageKey, Long> entry : this.items.entrySet()) {
            weight += entry.getKey().weight(entry.getValue());
        }
        return weight;
    }
}
