/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import org.jetbrains.annotations.ApiStatus;

/**
 * What every expansion card exposes to a {@link ComputerBuild}, regardless of what the card does: the bus generation
 * it needs (to check it fits the board), its power draw, and its {@link ExpansionCardKind}.
 *
 * <p>Open: this mod's own cards each have a spec of their own, and an add-on brings a card of a kind of its own the
 * same way, or declares a plain one ({@link PlainCardSpec}), and finds it in a build by its kind.
 */
@ApiStatus.Experimental
public interface IExpansionCardSpec {

    PcieGeneration bus();

    int tdpWatts();

    ExpansionCardKind kind();

    /** Whether the card seats in a board whose slots are {@code slot}: by default, a slot of its own bus family. */
    default boolean fits(final PcieGeneration slot) {
        return bus().compatibleWith(slot);
    }

    /**
     * How many machines the card lets its computer read and command on the industrial control cable: a field bus
     * card's; none for every other card. A computer with control ports keeps links on that cable, and its machines
     * take one each.
     */
    default int controlPorts() {
        return 0;
    }
}
