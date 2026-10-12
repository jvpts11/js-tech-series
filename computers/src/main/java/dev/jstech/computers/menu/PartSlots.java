/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.menu;

import dev.jstech.computers.blockentity.PartRefusals;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.util.function.BiFunction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

/** What the part slots of an assembly screen say when they keep a part out. */
final class PartSlots {

    private PartSlots() {
    }

    /**
     * Why {@code stack} stays out of {@code slot}, when the slot is one of a machine's part slots over {@code parts}:
     * a socket the board does not have first, then the rule the machine names; null for any other slot and for a part
     * that goes in.
     */
    @Nullable
    static Component refusal(final Slot slot, final IItemHandler parts, final ItemStack stack,
                             final BiFunction<Integer, ItemStack, TextKey> rule) {
        if (stack.isEmpty() || !(slot instanceof SlotItemHandler part) || part.getItemHandler() != parts
                || slot.mayPlace(stack)) {
            return null;
        }
        if (slot instanceof BoardSlot seat && !seat.onTheBoard()) {
            return GameText.component(seat.noneOpen() ? PartRefusals.NO_BOARD : PartRefusals.NOT_ON_THE_BOARD);
        }
        final TextKey why = rule.apply(part.getSlotIndex(), stack);
        return why == null ? null : GameText.component(why);
    }
}
