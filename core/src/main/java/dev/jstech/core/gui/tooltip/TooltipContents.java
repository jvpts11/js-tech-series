/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui.tooltip;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

/**
 * What an item that holds others holds, in a tooltip: each kind once with all it holds of it, the kind it holds most
 * of first, in the slots of the game's bundle; when there are more kinds than slots, the last slot says how many more.
 *
 * @param shown   the kinds shown, the most first, each with its whole count
 * @param more    how many kinds more there are
 * @param columns how many slots a row has
 */
public record TooltipContents(List<ItemStack> shown, int more, int columns) implements TooltipComponent {

    /** The slots of a row and the rows a tooltip shows at most. */
    public static final int COLUMNS = 6;
    public static final int ROWS = 3;

    public TooltipContents {
        final List<ItemStack> copies = new ArrayList<>();
        shown.forEach(stack -> copies.add(stack.copy()));
        shown = List.copyOf(copies);
        if (columns < 1 || more < 0) {
            throw new IllegalArgumentException("A tooltip's contents need a slot to a row and no fewer than none "
                    + "more, not " + columns + " and " + more);
        }
    }

    /**
     * What {@code held} holds, in rows of {@link #COLUMNS} and at most {@link #ROWS} rows; empty stacks are left out,
     * and a stack's count may be over its usual most.
     */
    public static TooltipContents of(final Iterable<ItemStack> held) {
        final List<ItemStack> kinds = kinds(held);
        final int slots = COLUMNS * ROWS;
        if (kinds.size() <= slots) {
            // A few kinds take a short row, two slots at the least, as the game's bundle does.
            return new TooltipContents(kinds, 0, Math.min(COLUMNS, Math.max(2, kinds.size())));
        }
        return new TooltipContents(kinds.subList(0, slots - 1), kinds.size() - (slots - 1), COLUMNS);
    }

    /**
     * The kinds {@code held} holds, each once with all it holds of it, the kind it holds most of first; empty stacks
     * are left out.
     */
    public static List<ItemStack> kinds(final Iterable<ItemStack> held) {
        final List<ItemStack> kinds = new ArrayList<>();
        for (final ItemStack stack : held) {
            if (stack.isEmpty()) {
                continue;
            }
            ItemStack same = null;
            for (final ItemStack kind : kinds) {
                if (ItemStack.isSameItemSameComponents(kind, stack)) {
                    same = kind;
                    break;
                }
            }
            if (same == null) {
                kinds.add(stack.copy());
            } else {
                same.grow(stack.getCount());
            }
        }
        kinds.sort(Comparator.comparingInt(ItemStack::getCount).reversed());
        return kinds;
    }

    /** Whether it shows nothing. */
    public boolean isEmpty() {
        return shown.isEmpty();
    }

    /** The kinds shown, copies. */
    @Override
    public List<ItemStack> shown() {
        final List<ItemStack> copies = new ArrayList<>();
        shown.forEach(stack -> copies.add(stack.copy()));
        return copies;
    }
}
