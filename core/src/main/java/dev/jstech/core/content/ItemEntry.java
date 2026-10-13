/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.content;

import dev.jstech.core.item.ItemState;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * A declared item that is not a block's: the item itself, reached like any registered item, and what was said about
 * it where it was declared.
 * @param <I> the item's class
 */
public final class ItemEntry<I extends Item> extends DeferredItem<I> {

    private final String english;
    private final IItemLook look;
    private final ItemState state;
    private final List<Default<?>> defaults;

    ItemEntry(final ResourceLocation id, final String english, final IItemLook look, final ItemState state,
              final List<Default<?>> defaults) {
        super(ResourceKey.create(Registries.ITEM, id));
        this.english = english;
        this.look = look;
        this.state = state;
        this.defaults = List.copyOf(defaults);
    }

    /** What the item is called in English. */
    public String english() {
        return english;
    }

    public IItemLook look() {
        return look;
    }

    /** What the item holds besides itself, as it was declared. */
    public ItemState state() {
        return state;
    }

    /** The components every new one of the item carries, as they were declared. */
    public List<Default<?>> defaults() {
        return defaults;
    }

    /**
     * A component a new item carries from the start, and its value.
     *
     * @param type  the component
     * @param value what it carries
     * @param <T>   what the component holds
     */
    public record Default<T>(Supplier<? extends DataComponentType<T>> type, T value) {

        /** Sets the component on a patch of an item's components. */
        public void applyTo(final DataComponentPatch.Builder patch) {
            patch.set(this.type.get(), this.value);
        }
    }
}
