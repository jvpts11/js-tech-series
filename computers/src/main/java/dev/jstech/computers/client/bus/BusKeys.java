/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.bus;

import dev.jstech.computers.bus.BusScript;
import dev.jstech.computers.storage.StorageKey;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Names and pictures for what a bus's settings and log write by id: an item, a fluid or a chemical by its text id, or
 * an item by its registry id. What is not there any more is shown by the id it was written with.
 */
final class BusKeys {

    private BusKeys() {
    }

    /** The name of what a text id ({@code item|minecraft:iron_ore}) names. */
    static Component name(final String keyId) {
        final StorageKey key = StorageKey.byId(keyId);
        return key == null ? Component.literal(BusScript.shortId(keyId)) : key.displayName();
    }

    /** The name of the item a registry id ({@code minecraft:iron_ore}) names, or a tag ({@code #c:ores}) as it is. */
    static Component itemName(final String id) {
        if (id.startsWith("#")) {
            return Component.literal(id);
        }
        final StorageKey key = StorageKey.byName(id);
        return key == null ? Component.literal(BusScript.shortId(id)) : key.displayName();
    }

    /** What to draw for what a text id names: the item, a fluid's bucket, or nothing. */
    static ItemStack icon(final String keyId) {
        final StorageKey key = StorageKey.byId(keyId);
        if (key == null) {
            return ItemStack.EMPTY;
        }
        if (key.isItem()) {
            return key.stack(1);
        }
        return key.isFluid() ? new ItemStack(key.fluidPrototype().getFluid().getBucket()) : ItemStack.EMPTY;
    }
}
