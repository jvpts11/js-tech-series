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
import org.jetbrains.annotations.ApiStatus;

/**
 * An expansion card that is no more than what every card is: its kind, its era, the slot it is made for and what it
 * draws. What an add-on declares for a card whose work is its own mod's, which finds it in a build by its kind; and
 * what a script declares, since a script brings no class of its own.
 *
 * @param kind     what the card is
 * @param era      the era it was made in
 * @param bus      the slot generation it is made for
 * @param tdpWatts what it draws, in watts
 */
@ApiStatus.Experimental
public record PlainCardSpec(ExpansionCardKind kind, Era era, PcieGeneration bus, int tdpWatts)
        implements IExpansionCardSpec {

    public PlainCardSpec {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(era, "era");
        Objects.requireNonNull(bus, "bus");
        if (tdpWatts < 0) {
            throw new IllegalArgumentException("tdpWatts must be >= 0; got " + tdpWatts);
        }
    }
}
