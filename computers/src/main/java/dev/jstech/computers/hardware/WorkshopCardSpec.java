/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import dev.jstech.computers.workshop.WorkshopCard;
import java.util.Objects;

/**
 * A personal-use card: the crafting table, furnace, enchanting table or anvil of the Workshop on an expansion card.
 * One card of each kind serves every era from the Legacy on, so it seats in a PCI, AGP or PCI Express slot alike and
 * only an ISA board, the Vintage's, has nowhere to take it; what it does is the same everywhere, and only the
 * furnace's pace follows the computer's era.
 */
public record WorkshopCardSpec(WorkshopCard card, int tdpWatts) implements IExpansionCardSpec {

    public WorkshopCardSpec {
        Objects.requireNonNull(card, "card must not be null");
        if (tdpWatts < 0) {
            throw new IllegalArgumentException("tdpWatts must be >= 0; got " + tdpWatts);
        }
    }

    /** The bus it is listed under, the first it was made for. */
    @Override
    public PcieGeneration bus() {
        return PcieGeneration.PCI;
    }

    @Override
    public ExpansionCardKind kind() {
        return ExpansionCardKind.WORKSHOP;
    }

    @Override
    public boolean fits(final PcieGeneration slot) {
        return slot.busFamily() != ExpansionBus.ISA;
    }
}
