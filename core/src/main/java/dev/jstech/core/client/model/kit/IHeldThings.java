/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.model.kit;

import net.minecraft.world.item.ItemStack;

/**
 * What a model holds at its locators, drawn there as the item is drawn in an item frame and moving with the bone the
 * locator is on: the ingot on a press's plate, the tool in a robot's hand.
 *
 * @param <T> what the model is drawn for
 */
@FunctionalInterface
public interface IHeldThings<T> {

    /** Holds nothing. */
    static <T> IHeldThings<T> none() {
        return (thing, locator) -> ItemStack.EMPTY;
    }

    /** What {@code thing}'s model holds at the locator named {@code locator}; empty for nothing. */
    ItemStack at(T thing, String locator);
}
