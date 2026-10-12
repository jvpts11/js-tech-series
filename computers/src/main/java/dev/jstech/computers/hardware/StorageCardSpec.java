/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import dev.jstech.core.tier.HardwareEra;
import java.util.Objects;

/**
 * A storage card of a server: the Vaultis Array, a RAID controller that joins the drives of the server it sits in into
 * one volume, or the Vaultis Cache, which answers the reads of those drives faster. Each era makes one of each for its
 * own slot. Anywhere but a server's they sit in their slot and do nothing, as a card does in a machine that has no use
 * for it.
 *
 * @param kind           {@link ExpansionCardKind#RAID_CONTROLLER} or {@link ExpansionCardKind#READ_CACHE}
 * @param era            the age of machine it was made for
 * @param bus            the slot it takes
 * @param tdpWatts       what it draws
 * @param readCutPercent how much of its drives' read latency a cache takes away; 0 on a RAID controller
 */
public record StorageCardSpec(ExpansionCardKind kind, HardwareEra era, PcieGeneration bus, int tdpWatts,
                              int readCutPercent) implements IExpansionCardSpec {

    public StorageCardSpec {
        Objects.requireNonNull(kind, "kind must not be null");
        Objects.requireNonNull(era, "era must not be null");
        Objects.requireNonNull(bus, "bus must not be null");
        if (kind != ExpansionCardKind.RAID_CONTROLLER && kind != ExpansionCardKind.READ_CACHE) {
            throw new IllegalArgumentException("a storage card is a RAID controller or a read cache; got " + kind);
        }
        if (tdpWatts < 0) {
            throw new IllegalArgumentException("tdpWatts must be >= 0; got " + tdpWatts);
        }
        if (readCutPercent < 0 || readCutPercent >= 100) {
            throw new IllegalArgumentException("readCutPercent must be within 0..99; got " + readCutPercent);
        }
        if (kind == ExpansionCardKind.RAID_CONTROLLER && readCutPercent != 0) {
            throw new IllegalArgumentException("a RAID controller cuts no read latency; got " + readCutPercent);
        }
    }

    /** A RAID controller of {@code era}, on its {@code bus}. */
    public static StorageCardSpec array(final HardwareEra era, final PcieGeneration bus, final int tdpWatts) {
        return new StorageCardSpec(ExpansionCardKind.RAID_CONTROLLER, era, bus, tdpWatts, 0);
    }

    /** A read cache of {@code era}, on its {@code bus}, taking {@code readCutPercent} of its drives' read latency. */
    public static StorageCardSpec cache(final HardwareEra era, final PcieGeneration bus, final int tdpWatts,
                                        final int readCutPercent) {
        return new StorageCardSpec(ExpansionCardKind.READ_CACHE, era, bus, tdpWatts, readCutPercent);
    }
}
