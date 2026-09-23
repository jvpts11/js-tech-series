/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.content;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import org.jetbrains.annotations.Nullable;

/**
 * Everything about one block, said once: what it is made from, what it is called, how it and its item look, what
 * it drops, which tab shows it, which tool mines it. {@link #register()} registers it and keeps the declaration for
 * the generator and the checks.
 *
 * @param <B> the block's class
 */
public final class BlockBuilder<B extends Block> {

    private final ModContent content;
    private final String id;
    private final Function<BlockBehaviour.Properties, ? extends B> factory;
    private final List<TagKey<Block>> tags = new ArrayList<>();
    private final List<ResourceLocation> recipeTypes = new ArrayList<>();
    private BlockBehaviour.Properties properties = BlockBehaviour.Properties.of();
    private @Nullable String english;
    private @Nullable IBlockLook look;
    private @Nullable BiFunction<B, Item.Properties, ? extends Item> itemFactory;
    private @Nullable IItemLook itemLook;
    private @Nullable Drops drops;
    private ContentTab.@Nullable Section section;

    BlockBuilder(final ModContent content, final String id, final Function<BlockBehaviour.Properties, ? extends B> factory) {
        this.content = content;
        this.id = id;
        this.factory = factory;
    }

    /** Sets the block's properties, starting from the plain ones. */
    public BlockBuilder<B> properties(final UnaryOperator<BlockBehaviour.Properties> change) {
        this.properties = change.apply(properties);
        return this;
    }

    /** What the block, and its item, are called in English. */
    public BlockBuilder<B> named(final String name) {
        this.english = name;
        return this;
    }

    public BlockBuilder<B> look(final IBlockLook blockLook) {
        this.look = blockLook;
        return this;
    }

    /** A look whose files are named after the block's id, such as {@code IBlockLook::orientable}. */
    public BlockBuilder<B> look(final Function<String, IBlockLook> byId) {
        return look(byId.apply(id));
    }

    /** Gives the block a plain item of its own. */
    public BlockBuilder<B> item() {
        return item(BlockItem::new);
    }

    /** Gives the block an item made this way. */
    public BlockBuilder<B> item(final BiFunction<B, Item.Properties, ? extends Item> made) {
        this.itemFactory = made;
        return this;
    }

    /** How the block's item looks; without this it shows the block's model. */
    public BlockBuilder<B> itemLook(final IItemLook look) {
        this.itemLook = look;
        return this;
    }

    /** What its loot table gives; without this a block with an item drops it, and one without drops nothing. */
    public BlockBuilder<B> drops(final Drops what) {
        this.drops = what;
        return this;
    }

    /** The section of a tab its item is shown in. */
    public BlockBuilder<B> tab(final ContentTab.Section inSection) {
        this.section = inSection;
        return this;
    }

    /** A block tag it belongs to, such as {@code minecraft:mineable/pickaxe} for the tool that mines it. */
    public BlockBuilder<B> tag(final TagKey<Block> tag) {
        tags.add(tag);
        return this;
    }

    /** A recipe type this block is the machine for, so a network knows where such a recipe runs. */
    public BlockBuilder<B> machineFor(final ResourceLocation recipeType) {
        recipeTypes.add(recipeType);
        return this;
    }

    /**
     * Registers the block, and its item if it has one, and keeps what was declared.
     *
     * @throws IllegalStateException when the declaration misses what every block needs (a name and a look), or asks
     *                               for something only an item can have
     */
    public BlockEntry<B> register() {
        if (english == null || look == null) {
            throw new IllegalStateException(content.modid() + ":" + id + " needs a name and a look");
        }
        final Drops dropped = drops != null ? drops : itemFactory != null ? Drops.SELF : Drops.NONE;
        if (itemFactory == null && (dropped == Drops.SELF || section != null || itemLook != null)) {
            throw new IllegalStateException(content.modid() + ":" + id + " has no item to drop, show or draw");
        }
        final BlockBehaviour.Properties madeWith = properties;
        final DeferredBlock<B> block = content.blockRegister().register(id, () -> factory.apply(madeWith));
        final BiFunction<B, Item.Properties, ? extends Item> itemMade = itemFactory;
        final DeferredItem<Item> item = itemMade == null ? null
                : content.itemRegister().register(id, () -> itemMade.apply(block.get(), new Item.Properties()));
        final BlockEntry<B> entry = new BlockEntry<>(block.getId(), english, look, item,
                item == null ? null : itemLook != null ? itemLook : IItemLook.OF_BLOCK, dropped, tags, recipeTypes);
        if (section != null) {
            section.add(entry);
        }
        content.declare(entry);
        return entry;
    }
}
