/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.content;

import dev.jstech.core.item.ItemState;
import dev.jstech.core.tool.ToolSpec;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
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
    private final List<TagKey<Item>> tags;
    private final Optional<ToolSpec> tool;

    ItemEntry(final ResourceLocation id, final String english, final IItemLook look, final ItemState state,
              final List<Default<?>> defaults, final List<TagKey<Item>> tags, final Optional<ToolSpec> tool) {
        super(ResourceKey.create(Registries.ITEM, id));
        this.english = english;
        this.look = look;
        this.state = state;
        this.defaults = List.copyOf(defaults);
        this.tags = List.copyOf(tags);
        this.tool = tool;
    }

    /** What the item does as a tool, as it was declared; none for an item that is no tool of the Core's. */
    public Optional<ToolSpec> tool() {
        return tool;
    }

    /** The item tags it was declared in, besides those of its tab. */
    public List<TagKey<Item>> declaredTags() {
        return tags;
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
