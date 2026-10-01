/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.content;

import dev.jstech.core.item.ItemMode;
import dev.jstech.core.item.ItemState;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

/**
 * Everything about one item that is not a block's, said once: what makes it, what it is called, how it looks, which
 * tab shows it, the components it starts with, and what it holds besides itself (its modes, energy, a fluid, stacks).
 * {@link #register()} registers it and keeps the declaration for the generator and the checks; the Core gives an
 * item that holds something the game's capability for it, keeps what it holds in its components and shows it in its
 * tooltip.
 * @param <I> the item's class
 */
public final class ItemBuilder<I extends Item> {

    private final ModContent content;
    private final String id;
    private final Function<Item.Properties, ? extends I> factory;
    private final List<ItemEntry.Default<?>> defaults = new ArrayList<>();
    private @Nullable String english;
    private IItemLook look = IItemLook.FLAT;
    private ContentTab.@Nullable Section section;
    private ItemState state = ItemState.NOTHING;

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

    /** A component every new one of it carries, and its value. */
    public <T> ItemBuilder<I> component(final Supplier<? extends DataComponentType<T>> type, final T value) {
        this.defaults.add(new ItemEntry.Default<>(Objects.requireNonNull(type, "type"),
                Objects.requireNonNull(value, "value")));
        return this;
    }

    /** The modes it switches between with the mode key, in the order the key goes through them; the first to start. */
    public ItemBuilder<I> modes(final ItemMode... modes) {
        if (modes.length < 2) {
            throw new IllegalArgumentException("an item with modes has two or more");
        }
        this.state = this.state.withModes(List.of(modes));
        return this;
    }

    /** It holds up to {@code capacity} FE, taking {@code in} and giving {@code out} at a time. */
    public ItemBuilder<I> holdsEnergy(final long capacity, final int in, final int out) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("an item that holds energy holds some");
        }
        this.state = this.state.withEnergy(capacity, in, out);
        return this;
    }

    /** It holds up to {@code capacity} millibuckets of one fluid. */
    public ItemBuilder<I> holdsFluid(final int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("an item that holds a fluid holds some");
        }
        this.state = this.state.withFluid(capacity);
        return this;
    }

    /** It holds {@code slots} stacks, up to {@value ItemState#MOST_SLOTS}. */
    public ItemBuilder<I> holdsItems(final int slots) {
        if (slots <= 0) {
            throw new IllegalArgumentException("an item that holds stacks holds one or more");
        }
        this.state = this.state.withSlots(slots);
        return this;
    }

    /**
     * Registers the item and keeps what was declared. An item that keeps anything inside stacks alone.
     *
     * @throws IllegalStateException when the item was given no name
     */
    public ItemEntry<I> register() {
        if (english == null) {
            throw new IllegalStateException(content.modid() + ":" + id + " needs a name");
        }
        final boolean alone = state.stacksAlone();
        final ItemEntry<I> entry = new ItemEntry<>(
                content.itemRegister().register(id, () -> factory.apply(alone ? new Item.Properties().stacksTo(1)
                        : new Item.Properties())).getId(),
                english, look, state, defaults);
        if (section != null) {
            section.add(entry);
        }
        content.declare(entry);
        return entry;
    }
}
