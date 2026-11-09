/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import dev.jstech.core.jstechseries.network.DataLine;
import dev.jstech.core.jstechseries.network.DataLink;
import dev.jstech.core.jstechseries.tier.Era;
import java.util.Objects;
import org.jetbrains.annotations.Nullable;

/**
 * A WAN Interface Card: what one port of a WAN Gateway Computer takes the long-distance line by, the Modem Card's
 * telephone line in the Vintage up to the Optical Transport Card's dark fibre in the Advanced. Like a cable's port it
 * takes the line of its era and of every earlier one, never a later one (computers/docs/design/network.md).
 *
 * @param era      the era of the line it was made for
 * @param bus      the slot it takes
 * @param tdpWatts what it draws
 */
public record WanCardSpec(Era era, PcieGeneration bus, int tdpWatts) implements IExpansionCardSpec {

    public WanCardSpec {
        Objects.requireNonNull(era, "era must not be null");
        Objects.requireNonNull(bus, "bus must not be null");
        if (tdpWatts < 0) {
            throw new IllegalArgumentException("tdpWatts must be >= 0; got " + tdpWatts);
        }
        // The fields are set only once this constructor ends, so the line is made from the parameter here.
        if (!new DataLink(DataLine.LONG_DISTANCE, era).exists()) {
            throw new IllegalArgumentException("no long-distance line is made in " + era);
        }
    }

    @Override
    public ExpansionCardKind kind() {
        return ExpansionCardKind.WAN_INTERFACE;
    }

    /** The line of its own era, the newest it takes. */
    public DataLink line() {
        return new DataLink(DataLine.LONG_DISTANCE, era);
    }

    /** How many items a tick its own line carries. */
    public long itemsPerTick() {
        return line().throughput();
    }

    /** Whether it takes a cable of {@code link}: the long-distance line, of its era or an earlier one. */
    public boolean takes(final @Nullable DataLink link) {
        return link != null && link.line() == DataLine.LONG_DISTANCE && era.isAtLeast(link.era());
    }
}
