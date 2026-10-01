/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.inventory;

import java.util.Objects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;

/**
 * A stack of the game as an {@link ItemFilter} looks at it.
 *
 * @param stack the stack; its count does not matter
 */
public record StackSubject(ItemStack stack) implements IFilterSubject {

    public StackSubject {
        Objects.requireNonNull(stack, "stack");
    }

    /** The stack {@code stack} to filter. */
    public static StackSubject of(final ItemStack stack) {
        return new StackSubject(stack);
    }

    @Override
    public String itemId() {
        return BuiltInRegistries.ITEM.getKey(this.stack.getItem()).toString();
    }

    @Override
    public boolean hasTag(final String tag) {
        final ResourceLocation id = ResourceLocation.tryParse(tag);
        return id != null && this.stack.is(TagKey.create(Registries.ITEM, id));
    }

    @Override
    public boolean sameAs(final IFilterSubject other) {
        return other instanceof StackSubject subject && ItemStack.isSameItemSameComponents(this.stack, subject.stack);
    }
}
