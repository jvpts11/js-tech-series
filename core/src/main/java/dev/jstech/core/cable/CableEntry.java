/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.cable;

import dev.jstech.core.content.ItemEntry;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * A cable a mod declared: the cable, once registered, and the item that lays it, both under the declaration's id.
 */
public final class CableEntry implements ItemLike, Supplier<CableType> {

    private final DeferredHolder<CableType, CableType> type;
    private final ItemEntry<CableItem> item;

    public CableEntry(final DeferredHolder<CableType, CableType> type, final ItemEntry<CableItem> item) {
        this.type = Objects.requireNonNull(type, "type");
        this.item = Objects.requireNonNull(item, "item");
    }

    /** The cable. */
    @Override
    public CableType get() {
        return this.type.get();
    }

    /** The item that lays it. */
    @Override
    public Item asItem() {
        return this.item.get();
    }

    /** The item's declaration. */
    public ItemEntry<CableItem> item() {
        return this.item;
    }

    /** The id the cable and its item are registered under. */
    public ResourceLocation id() {
        return this.type.getId();
    }

    /** {@code count} of the item that lays it. */
    public ItemStack stack(final int count) {
        return new ItemStack(asItem(), count);
    }
}
