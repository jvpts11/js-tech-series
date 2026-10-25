/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.crafting;

import dev.jstech.computers.hardware.CraftingCardSpec;
import dev.jstech.computers.registry.ComputingComponents;
import dev.jstech.computers.registry.HardwareRegistry;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;

/**
 * The ROM of a Crafting Card: the bench recipes the computer crafts, kept on the card itself, so a card moved to
 * another computer brings them along. How many it keeps is its era's ({@link CraftingEras#romPerCard}).
 */
public final class CraftingCards {

    private CraftingCards() {
    }

    /** Whether {@code stack} is a Crafting Card, of this mod's or of any mod that declares one. */
    public static boolean isCard(final ItemStack stack) {
        return HardwareRegistry.card(stack) instanceof CraftingCardSpec;
    }

    /** The bench recipes the ROM of the card in {@code stack} keeps, in the order they were loaded. */
    public static List<CraftingPattern> rom(final ItemStack stack) {
        final List<CraftingPattern> held = stack.get(ComputingComponents.RECIPE_ROM.get());
        return held == null ? List.of() : held;
    }

    /** How many bench recipes the card in {@code stack} keeps at most, 0 for anything that is not a card. */
    public static int romSize(final ItemStack stack) {
        return HardwareRegistry.card(stack) instanceof CraftingCardSpec card ? card.romSize() : 0;
    }

    /**
     * Writes {@code pattern} into the ROM of the card in {@code stack}: false when it is full or keeps the same
     * recipe already. The stack is changed in place.
     */
    public static boolean load(final ItemStack stack, final CraftingPattern pattern) {
        final List<CraftingPattern> held = rom(stack);
        if (held.size() >= romSize(stack)) {
            return false;
        }
        for (final CraftingPattern existing : held) {
            if (existing.sameRecipe(pattern)) {
                return false;
            }
        }
        final List<CraftingPattern> next = new ArrayList<>(held);
        next.add(pattern);
        stack.set(ComputingComponents.RECIPE_ROM.get(), List.copyOf(next));
        return true;
    }

    /** Erases the recipe at {@code index} from the ROM of the card in {@code stack}; false when there is none. */
    public static boolean remove(final ItemStack stack, final int index) {
        final List<CraftingPattern> held = rom(stack);
        if (index < 0 || index >= held.size()) {
            return false;
        }
        final List<CraftingPattern> next = new ArrayList<>(held);
        next.remove(index);
        if (next.isEmpty()) {
            stack.remove(ComputingComponents.RECIPE_ROM.get());
        } else {
            stack.set(ComputingComponents.RECIPE_ROM.get(), List.copyOf(next));
        }
        return true;
    }
}
