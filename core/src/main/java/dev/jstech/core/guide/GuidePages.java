/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Pages chosen by the stack rather than by the item. Most items are the page of one entry, which the entry says by
 * covering them; but some items stand for different things from stack to stack, such as an installation disc that
 * holds one system or another. A mod says here, for such an item, which entry a given stack is the page of, and the
 * manual key over that stack opens it, in the book that holds it.
 *
 * <pre>{@code
 * GuidePages.byStack(INSTALL_DISC.get(), stack -> "jsc:install_" + systemOn(stack));
 * }</pre>
 *
 * <p>A stack the mod answers nothing for, with an empty id, opens its item's own page when it has one.
 */
public final class GuidePages {

    private static final Map<Item, Function<ItemStack, String>> BY_STACK = new ConcurrentHashMap<>();

    private GuidePages() {
    }

    /** Says which entry a stack of that item is the page of, or empty for the item's own page. */
    public static void byStack(final Item item, final Function<ItemStack, String> entryOf) {
        Objects.requireNonNull(entryOf, "entryOf");
        if (BY_STACK.putIfAbsent(Objects.requireNonNull(item, "item"), entryOf) != null) {
            throw new IllegalStateException(item + " already has its stacks' pages chosen");
        }
    }

    /** The entry a stack is the page of, as its item's mod chose; empty when it chose none. */
    public static String entryOf(final ItemStack stack) {
        final Function<ItemStack, String> chooser = BY_STACK.get(stack.getItem());
        if (chooser == null) {
            return "";
        }
        final String entry = chooser.apply(stack);
        return entry == null ? "" : entry;
    }
}
