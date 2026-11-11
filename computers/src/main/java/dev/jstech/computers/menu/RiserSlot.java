/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.menu;

import java.util.function.BooleanSupplier;
import java.util.function.IntPredicate;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * A Mining Computer's riser on its assembly screen (computers/docs/design/teracoin.md): it takes a riser of the rig's
 * era, shown on the board's face, and keeps a riser a card stands on, which comes off only once its card is taken off
 * in the world.
 */
final class RiserSlot extends SlotItemHandler {

    private final BooleanSupplier shown;
    private final IntPredicate carries;

    RiserSlot(final IItemHandler risers, final int index, final int x, final int y, final BooleanSupplier shown,
              final IntPredicate carries) {
        super(risers, index, x, y);
        this.shown = shown;
        this.carries = carries;
    }

    @Override
    public boolean isActive() {
        return shown.getAsBoolean();
    }

    @Override
    public boolean mayPickup(final Player player) {
        return !carries.test(getSlotIndex()) && super.mayPickup(player);
    }
}
