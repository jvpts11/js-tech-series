/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.item;

import dev.jstech.core.jstechseries.tier.Era;
import net.minecraft.world.item.Item;

/**
 * A mining riser (computers/docs/design/teracoin.md): the small board with an x16 slot, its USB cable and the adapter's
 * gold edge, that carries a graphics card to one of a mining board's x1 slots. Each era's board takes its own era's
 * risers, and a riser carries a card of its era or an older one.
 */
public class MiningRiserItem extends Item {

    private final Era era;

    public MiningRiserItem(final Properties properties, final Era era) {
        super(properties);
        this.era = era;
    }

    /** The era of the riser: the board it fits, and the newest card it carries. */
    public Era era() {
        return era;
    }

    /** Whether a card of {@code cardEra} stands on this riser: one of its era or older. */
    public boolean carries(final Era cardEra) {
        return cardEra.isAtMost(era);
    }
}
