/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.menu;

import java.util.function.BooleanSupplier;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * A part's slot shown on one face of an assembly screen only (computers/docs/design/temperature.md): a board's on the
 * board's face, a fan seat on the cooling face while the case has that seat open. It still takes a part sent to it
 * with a shift-click while the other face shows, which only saves turning the screen.
 */
final class FacedSlot extends SlotItemHandler {

    private final BooleanSupplier shown;

    FacedSlot(final IItemHandler handler, final int index, final int x, final int y, final BooleanSupplier shown) {
        super(handler, index, x, y);
        this.shown = shown;
    }

    @Override
    public boolean isActive() {
        return shown.getAsBoolean();
    }
}
