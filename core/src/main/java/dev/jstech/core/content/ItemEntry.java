/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.content;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * A declared item that is not a block's: the item itself, reached like any registered item, and what was said about
 * it where it was declared.
 *
 * @param <I> the item's class
 */
public final class ItemEntry<I extends Item> extends DeferredItem<I> {

    private final String english;
    private final IItemLook look;

    ItemEntry(final ResourceLocation id, final String english, final IItemLook look) {
        super(ResourceKey.create(Registries.ITEM, id));
        this.english = english;
        this.look = look;
    }

    /** What the item is called in English. */
    public String english() {
        return english;
    }

    public IItemLook look() {
        return look;
    }
}
