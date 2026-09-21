/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.rack;

import dev.jstech.core.id.IStableId;
import dev.jstech.core.id.IStableName;
import dev.jstech.core.id.StableIds;
import dev.jstech.core.id.StableNames;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * How a RAID Controller aggregates the drives of its bay into one logical volume. Pure arithmetic
 * over drive capacities and counts, with no Minecraft types, so the capacity, redundancy and
 * throughput rules are unit tested on their own.
 *
 * <p>Without a controller the bay's drives stay independent volumes; that is the {@link #NONE}
 * case, and it is what every bay does by default. The controller item keeps the mode as its
 * {@link #serializedName()}; the firmware's storage page sends and shows it by {@link #id()}.
 */
public enum RaidMode implements IStableId, IStableName {

    /** No array: each drive is its own volume, as on any other computer. */
    NONE(0, "none", 1, 0),
    /** Striping: full capacity and faster storage operations, but losing any drive loses it all. */
    RAID0(1, "raid0", 2, 0),
    /** Mirror: the smallest drive's capacity, surviving down to a single drive. */
    RAID1(2, "raid1", 2, Integer.MAX_VALUE),
    /** Parity: capacity of all but one drive, surviving exactly one loss; needs three drives. */
    RAID5(3, "raid5", 3, 1);

    /** The storage-throughput bonus of a healthy stripe, in percent. */
    public static final int STRIPE_THROUGHPUT_BONUS_PERCENT = 25;

    private static final StableIds<RaidMode> IDS = StableIds.of(RaidMode.class);
    private static final StableNames<RaidMode> NAMES = StableNames.of(RaidMode.class);

    private final int id;
    private final String serializedName;
    private final int minDrives;
    private final int lossesTolerated;

    RaidMode(final int id, final String serializedName, final int minDrives, final int lossesTolerated) {
        this.id = id;
        this.serializedName = serializedName;
        this.minDrives = minDrives;
        this.lossesTolerated = lossesTolerated;
    }

    @Override
    public int id() {
        return id;
    }

    @Override
    public String serializedName() {
        return serializedName;
    }

    /** How many drives the mode needs before it forms an array at all. */
    public int minDrives() {
        return minDrives;
    }

    /**
     * How many drives the array can lose and still serve data. {@link #RAID1} mirrors every drive,
     * so it survives down to one, which is reported as {@code driveCount - 1} by
     * {@link #lossesTolerated(int)}; this raw value is the mode's own ceiling.
     */
    public int lossesTolerated(final int driveCount) {
        if (driveCount < minDrives) {
            return 0;
        }
        return Math.min(lossesTolerated, driveCount - 1);
    }

    /** Whether this mode forms an array over the given number of drives. */
    public boolean formsArray(final int driveCount) {
        return this != NONE && driveCount >= minDrives;
    }

    /**
     * The usable capacity of the array in items: the sum for a stripe, the smallest drive for a
     * mirror, and the sum minus one drive's worth for parity. Below the mode's minimum drive count
     * the array does not form and the capacity is zero, and the caller falls back to plain volumes.
     */
    public long usableCapacity(final List<Long> driveCapacities) {
        return usableCapacity(driveCapacities, driveCapacities.size());
    }

    /**
     * The usable capacity of an array formed with {@code memberCount} drives, of which
     * {@code driveCapacities} are the members still present. A degraded array keeps the size it
     * promised (losing a member costs redundancy, not capacity) so the parity reserve is charged
     * against the member count the array was built with, not against what is left in the bay.
     */
    public long usableCapacity(final List<Long> driveCapacities, final int memberCount) {
        if (driveCapacities.isEmpty() || !formsArray(Math.max(memberCount, driveCapacities.size()))) {
            return 0L;
        }
        long sum = 0L;
        long smallest = Long.MAX_VALUE;
        for (final long capacity : driveCapacities) {
            sum += capacity;
            smallest = Math.min(smallest, capacity);
        }
        return switch (this) {
            case RAID0 -> sum;
            case RAID1 -> smallest;
            /*
             * Parity costs one drive's worth. Charging the smallest member (rather than subtracting
             * it from the raw sum) is what makes a mixed array safe: the survivors always have room
             * for the whole volume when one member is pulled, so a degraded array never has to drop
             * data it promised to hold. On the usual array of identical drives the two are equal.
             */
            case RAID5 -> (Math.max(memberCount, driveCapacities.size()) - 1) * smallest;
            case NONE -> 0L;
        };
    }

    /** The storage-throughput multiplier in percent (100 = unchanged) for a healthy array. */
    public int throughputPercent() {
        return this == RAID0 ? 100 + STRIPE_THROUGHPUT_BONUS_PERCENT : 100;
    }

    /**
     * Whether an array that started with {@code originalDrives} members still serves data with
     * {@code presentDrives} of them left.
     */
    public boolean survives(final int originalDrives, final int presentDrives) {
        if (!formsArray(originalDrives)) {
            return false;
        }
        return originalDrives - presentDrives <= lossesTolerated(originalDrives);
    }

    /** Whether a member is missing and the mode can rebuild it once a replacement drive goes in. */
    public boolean rebuildable(final int originalDrives, final int presentDrives) {
        return presentDrives < originalDrives && survives(originalDrives, presentDrives)
                && this != RAID0;
    }

    /** The mode listed before this one on the firmware's storage page, staying on the first. */
    public RaidMode previous() {
        return switch (this) {
            case NONE, RAID0 -> NONE;
            case RAID1 -> RAID0;
            case RAID5 -> RAID1;
        };
    }

    /** The mode listed after this one on the firmware's storage page, staying on the last. */
    public RaidMode next() {
        return switch (this) {
            case NONE -> RAID0;
            case RAID0 -> RAID1;
            case RAID1, RAID5 -> RAID5;
        };
    }

    /** The mode that declares {@code id}, or null when none does. */
    @Nullable
    public static RaidMode find(final int id) {
        return IDS.find(id);
    }

    /** The mode that declares {@code name}; a name no mode declares reads as {@link #NONE}, independent volumes. */
    public static RaidMode byName(@Nullable final String name) {
        return NAMES.byName(name, NONE);
    }
}
