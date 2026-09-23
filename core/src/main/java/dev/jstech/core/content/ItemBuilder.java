/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.content;

import java.util.function.Function;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

/**
 * Everything about one item that is not a block's, said once: what makes it, what it is called, how it looks and
 * which tab shows it. {@link #register()} registers it and keeps the declaration for the generator and the checks.
 *
 * @param <I> the item's class
 */
public final class ItemBuilder<I extends Item> {

    private final ModContent content;
    private final String id;
    private final Function<Item.Properties, ? extends I> factory;
    private @Nullable String english;
    private IItemLook look = IItemLook.FLAT;
    private ContentTab.@Nullable Section section;

    ItemBuilder(final ModContent content, final String id, final Function<Item.Properties, ? extends I> factory) {
        this.content = content;
        this.id = id;
        this.factory = factory;
    }

    /** What the item is called in English. */
    public ItemBuilder<I> named(final String name) {
        this.english = name;
        return this;
    }

    /** How the item looks; without this it is a flat sprite of its own texture. */
    public ItemBuilder<I> look(final IItemLook itemLook) {
        this.look = itemLook;
        return this;
    }

    /** The section of a tab it is shown in. */
    public ItemBuilder<I> tab(final ContentTab.Section inSection) {
        this.section = inSection;
        return this;
    }

    /**
     * Registers the item and keeps what was declared.
     *
     * @throws IllegalStateException when the item was given no name
     */
    public ItemEntry<I> register() {
        if (english == null) {
            throw new IllegalStateException(content.modid() + ":" + id + " needs a name");
        }
        final ItemEntry<I> entry = new ItemEntry<>(
                content.itemRegister().register(id, () -> factory.apply(new Item.Properties())).getId(),
                english, look);
        if (section != null) {
            section.add(entry);
        }
        content.declare(entry);
        return entry;
    }
}
