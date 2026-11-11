/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.EraTable;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.util.Sizes;

/**
 * What an item costs on the disks of each era. A wider word makes a bigger record: an item (or a bucket of fluid,
 * which weighs the same) takes 1 MB on a Vintage disk, 16 MB on a Legacy one and 256 MB from the Transition on, the
 * word size of each era's processors (16, 32 and 64 bits). Older hardware therefore packs more into the same
 * megabytes, which is how a 20 MB Vintage drive holds anything at all, and a 500 GB Standard one holds 2,000 items.
 * An era an add-on brings costs what the era before it does; the formula is the mod's own.
 */
public final class EraStorage {

    private static final EraTable<Integer> BITS = EraTable.of(16)
            .from(HardwareEra.LEGACY, 32)
            .from(HardwareEra.TRANSITION, 64);
    private static final EraTable<Long> MB_PER_ITEM = EraTable.of(1L)
            .from(HardwareEra.LEGACY, 16L)
            .from(HardwareEra.TRANSITION, 256L);
    private static final long BYTES_PER_MB = 1024L * 1024L;
    /* An item weighs 1,000 units on a disk, as a bucket of fluid weighs 1,000 millibuckets. */
    private static final long WEIGHT_PER_ITEM = 1000L;

    private EraStorage() {
    }

    /** The word size of {@code era}'s processors, in bits; what an item costs on its disks follows from it. */
    public static int bits(final Era era) {
        return BITS.get(era);
    }

    /** The megabytes one item takes on a disk of {@code era}. */
    public static long mbPerItem(final Era era) {
        return MB_PER_ITEM.get(era);
    }

    /**
     * How many items a size in megabytes costs on {@code era}'s disks, rounded up: a system image, say.
     *
     * <p>Rounded up without the addition the obvious form uses, because that addition is itself an amount that can
     * run past the end: a size near the largest number there is would round up to a negative one, and something
     * enormous would then cost nothing to store.
     */
    public static long itemsFor(final Era era, final long mb) {
        return Sizes.ceilDiv(mb, mbPerItem(era));
    }

    /** The bytes one item takes on a disk of {@code era}. */
    public static long bytesPerItem(final Era era) {
        return mbPerItem(era) * BYTES_PER_MB;
    }

    /**
     * What {@code bytes} weigh on {@code era}'s disks, in thousandths of an item (the unit a millibucket of fluid
     * weighs one of), rounded up, and exactly: a file as big as an item weighs an item, 1,000 of them.
     *
     * <p>Worked out in whole items and what is left over, so no size a long holds runs past the end on the way.
     */
    public static long weightOfBytes(final Era era, final long bytes) {
        if (bytes <= 0L) {
            return 0L;
        }
        final long perItem = bytesPerItem(era);
        return bytes / perItem * WEIGHT_PER_ITEM + Sizes.ceilDiv(bytes % perItem * WEIGHT_PER_ITEM, perItem);
    }

    /** The bytes {@code weight} thousandths of an item come to on {@code era}'s disks, rounded down. */
    public static long bytesOfWeight(final Era era, final long weight) {
        if (weight <= 0L) {
            return 0L;
        }
        final long perItem = bytesPerItem(era);
        return weight / WEIGHT_PER_ITEM * perItem + weight % WEIGHT_PER_ITEM * perItem / WEIGHT_PER_ITEM;
    }

    /**
     * The weight a medium of {@code capacityBytes} holds on the terms of {@code era}, the era of its format, rounded
     * down: what files of those bytes together may weigh on it.
     */
    public static long weightHeldBy(final Era era, final long capacityBytes) {
        if (capacityBytes <= 0L) {
            return 0L;
        }
        final long perItem = bytesPerItem(era);
        return capacityBytes / perItem * WEIGHT_PER_ITEM + capacityBytes % perItem * WEIGHT_PER_ITEM / perItem;
    }
}
