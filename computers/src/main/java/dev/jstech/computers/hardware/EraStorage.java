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

    /**
     * The bytes one thousandth of an item weighs on {@code era}'s disks, where an item weighs 1,000 of those units,
     * the same unit a millibucket of fluid weighs one of. A file of N bytes costs {@code ceil(N / bytesPerMbEq())} of
     * them.
     */
    public static long bytesPerMbEq(final Era era) {
        return mbPerItem(era) * 1024L * 1024L / 1000L;
    }
}
