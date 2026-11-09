/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import dev.jstech.core.jstechseries.tier.Era;

import java.util.Objects;

/**
 * A Network Anchor Card: while the computer it sits in runs, it keeps the chunks round that computer loaded, under the
 * name of whoever works the computer and within their quota of loaded chunks, so the machines there go on working
 * with nobody near (computers/docs/design/network.md).
 *
 * @param era      the age of machine it was made for
 * @param bus      the slot it takes
 * @param tdpWatts what it draws
 * @param reach    how many chunks it keeps on each side of the computer's own: 0 for that chunk alone, 1 for the 3 by
 *                 3 round it, 2 for the 5 by 5
 */
public record AnchorCardSpec(Era era, PcieGeneration bus, int tdpWatts, int reach) implements IExpansionCardSpec {

    public AnchorCardSpec {
        Objects.requireNonNull(era, "era must not be null");
        Objects.requireNonNull(bus, "bus must not be null");
        if (tdpWatts < 0) {
            throw new IllegalArgumentException("tdpWatts must be >= 0; got " + tdpWatts);
        }
        if (reach < 0) {
            throw new IllegalArgumentException("reach must be >= 0; got " + reach);
        }
    }

    @Override
    public ExpansionCardKind kind() {
        return ExpansionCardKind.ANCHOR;
    }

    /** How many chunks a side of the square it keeps is: 1, 3 or 5. */
    public int side() {
        return 2 * reach + 1;
    }

    /** How many chunks it keeps loaded in all. */
    public int chunks() {
        return side() * side();
    }
}
