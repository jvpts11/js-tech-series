/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.content;

import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import org.jetbrains.annotations.Nullable;

/**
 * A declared block: the block itself, reached like any registered block, and everything said about it where it was
 * declared, from which its files are generated and against which it is checked.
 *
 * @param <B> the block's class
 */
public final class BlockEntry<B extends Block> extends DeferredBlock<B> {

    private final String english;
    private final IBlockLook look;
    private final @Nullable DeferredItem<Item> item;
    private final @Nullable IItemLook itemLook;
    private final Drops drops;
    private final List<TagKey<Block>> tags;
    private final List<ResourceLocation> recipeTypes;

    BlockEntry(final ResourceLocation id, final String english, final IBlockLook look,
               final @Nullable DeferredItem<Item> item, final @Nullable IItemLook itemLook, final Drops drops,
               final List<TagKey<Block>> tags, final List<ResourceLocation> recipeTypes) {
        super(ResourceKey.create(Registries.BLOCK, id));
        this.english = english;
        this.look = look;
        this.item = item;
        this.itemLook = itemLook;
        this.drops = drops;
        this.tags = List.copyOf(tags);
        this.recipeTypes = List.copyOf(recipeTypes);
    }

    /** The block's item. */
    public Item item() {
        if (item == null) {
            throw new IllegalStateException(getId() + " was declared without an item");
        }
        return item.get();
    }

    public boolean hasItem() {
        return item != null;
    }

    /** What the block is called in English. */
    public String english() {
        return english;
    }

    public IBlockLook look() {
        return look;
    }

    /** How the block's item looks; {@code null} when it has none. */
    public @Nullable IItemLook itemLook() {
        return itemLook;
    }

    public Drops drops() {
        return drops;
    }

    /** The block tags it was declared into, such as the one saying which tool mines it. */
    public List<TagKey<Block>> declaredTags() {
        return tags;
    }

    /** The recipe types this block is the machine for. */
    public List<ResourceLocation> recipeTypes() {
        return recipeTypes;
    }
}
