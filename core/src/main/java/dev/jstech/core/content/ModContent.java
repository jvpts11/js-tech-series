/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.content;

import dev.jstech.core.audio.SoundKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * What one mod puts in the game, declared in one place: its blocks, its items, the block entities its blocks make
 * and its creative tabs.
 *
 * <p>A block or an item is declared once, with everything about it, and nothing else lists it again: the
 * generator writes its block state, models, name, loot and tags from the declaration, the tab shows it from the
 * declaration, and the checks read the declarations to prove that every registered thing has all of those. Each mod
 * keeps one of these, made before any declaration and handed its event bus by {@link #register(IEventBus)}.
 */
public final class ModContent {

    private final String modid;
    private final DeferredRegister.Blocks blocks;
    private final DeferredRegister.Items items;
    private final DeferredRegister<BlockEntityType<?>> blockEntities;
    private final DeferredRegister<CreativeModeTab> tabs;
    private final DeferredRegister<SoundEvent> sounds;
    private final List<BlockEntry<?>> declaredBlocks = new ArrayList<>();
    private final List<ItemEntry<?>> declaredItems = new ArrayList<>();
    private final List<ContentTab> declaredTabs = new ArrayList<>();
    private final List<SoundKey> declaredSounds = new ArrayList<>();

    /* Every mod's content, by mod id, in the order the mods made theirs. */
    private static final Map<String, ModContent> BY_MOD = Collections.synchronizedMap(new LinkedHashMap<>());

    public ModContent(final String modid) {
        this.modid = modid;
        this.blocks = DeferredRegister.createBlocks(modid);
        this.items = DeferredRegister.createItems(modid);
        this.blockEntities = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, modid);
        this.tabs = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, modid);
        this.sounds = DeferredRegister.create(Registries.SOUND_EVENT, modid);
        if (BY_MOD.putIfAbsent(modid, this) != null) {
            throw new IllegalStateException("the content of " + modid + " is declared twice");
        }
    }

    /** Every mod's content. */
    public static Collection<ModContent> all() {
        synchronized (BY_MOD) {
            return List.copyOf(BY_MOD.values());
        }
    }

    /**
     * That mod's content.
     *
     * @throws IllegalStateException when the mod declared none
     */
    public static ModContent of(final String modid) {
        final ModContent content = BY_MOD.get(modid);
        if (content == null) {
            throw new IllegalStateException(modid + " declared no content");
        }
        return content;
    }

    public String modid() {
        return modid;
    }

    /** Starts declaring a block made from its properties. */
    public <B extends Block> BlockBuilder<B> block(final String id,
                                                  final Function<BlockBehaviour.Properties, ? extends B> factory) {
        return new BlockBuilder<>(this, id, factory);
    }

    /** Starts declaring an item that is not a block's, made from its properties. */
    public <I extends Item> ItemBuilder<I> item(final String id, final Function<Item.Properties, ? extends I> factory) {
        return new ItemBuilder<>(this, id, factory);
    }

    /**
     * Registers the block entity made by those blocks. Every block that makes it is named here, so a variant of a
     * block (another era of the same machine) cannot be left out of its entity's valid blocks.
     */
    public <T extends BlockEntity> DeferredHolder<BlockEntityType<?>, BlockEntityType<T>> blockEntity(
            final String id, final BlockEntityType.BlockEntitySupplier<? extends T> factory,
            final DeferredBlock<?>... madeBy) {
        final List<DeferredBlock<?>> makers = List.of(madeBy);
        return blockEntities.register(id, () -> BlockEntityType.Builder.<T>of(factory,
                makers.stream().map(DeferredBlock::get).toArray(Block[]::new)).build(null));
    }

    /** Declares a creative tab, called that in English, showing that icon. */
    public ContentTab tab(final String id, final String englishTitle, final Supplier<? extends ItemLike> icon) {
        final ContentTab tab = new ContentTab(modid, id, englishTitle, icon);
        tabs.register(id, tab::build);
        declaredTabs.add(tab);
        return tab;
    }

    /** Starts declaring a sound, known by that path under the mod's namespace ({@code computer/power_on}). */
    public SoundBuilder sound(final String path) {
        return new SoundBuilder(this, path);
    }

    /** The blocks declared so far, in declaration order. */
    public List<BlockEntry<?>> declaredBlocks() {
        return Collections.unmodifiableList(declaredBlocks);
    }

    /** The items declared so far that are not blocks', in declaration order. */
    public List<ItemEntry<?>> declaredItems() {
        return Collections.unmodifiableList(declaredItems);
    }

    public List<ContentTab> declaredTabs() {
        return Collections.unmodifiableList(declaredTabs);
    }

    /** The sounds declared so far, in declaration order. */
    public List<SoundKey> declaredSounds() {
        return Collections.unmodifiableList(declaredSounds);
    }

    /** Hands the registrations to the mod's event bus; call it once, after every declaration class has loaded. */
    public void register(final IEventBus modEventBus) {
        Arrays.asList(blocks, items, blockEntities, tabs, sounds).forEach(register -> register.register(modEventBus));
    }

    DeferredRegister.Blocks blockRegister() {
        return blocks;
    }

    DeferredRegister.Items itemRegister() {
        return items;
    }

    void declare(final BlockEntry<?> entry) {
        declaredBlocks.add(entry);
    }

    void declare(final ItemEntry<?> entry) {
        declaredItems.add(entry);
    }

    DeferredRegister<SoundEvent> soundRegister() {
        return sounds;
    }

    void declare(final SoundKey key) {
        declaredSounds.add(key);
    }
}
