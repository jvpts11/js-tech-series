/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.menu;

import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * A hardware slot usable only while its relative index is within the count the installed motherboard offers, so the
 * CPU/RAM/PCIe/disk bays appear and accept parts according to the board, and none of them do until a board is
 * installed (the limit is then 0). Shared by every computer-assembly menu so the board-gated slot behaves identically
 * everywhere. On a screen with two faces it shows on its own face only.
 */
final class BoardSlot extends SlotItemHandler {

    private final int relativeIndex;
    private final IntSupplier boardLimit;
    private final BooleanSupplier onFace;

    BoardSlot(final IItemHandler handler, final int index, final int x, final int y,
              final int relativeIndex, final IntSupplier boardLimit) {
        this(handler, index, x, y, relativeIndex, boardLimit, () -> true);
    }

    BoardSlot(final IItemHandler handler, final int index, final int x, final int y,
              final int relativeIndex, final IntSupplier boardLimit, final BooleanSupplier onFace) {
        super(handler, index, x, y);
        this.relativeIndex = relativeIndex;
        this.boardLimit = boardLimit;
        this.onFace = onFace;
    }

    @Override
    public boolean isActive() {
        /*
         * Within the board's slot count, or already holding a part, so a component is never
         * trapped behind a smaller board swapped in later.
         */
        return onFace.getAsBoolean() && (relativeIndex < boardLimit.getAsInt() || hasItem());
    }

    @Override
    public boolean mayPlace(final ItemStack stack) {
        return onTheBoard() && super.mayPlace(stack);
    }

    /** Whether the board seated has this slot: a board with fewer slots leaves the ones past its count unwired. */
    boolean onTheBoard() {
        return relativeIndex < boardLimit.getAsInt();
    }

    /** Whether no slot of this kind is open at all, as before any board is seated. */
    boolean noneOpen() {
        return boardLimit.getAsInt() == 0;
    }
}
