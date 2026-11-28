/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.registry;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.api.ComputersRegisterEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * This mod's own parts, kept as each is declared onto the mod's content, and declared again, part by part, through
 * the register event the way an add-on declares its own: an item made here is a part only because the event says so.
 */
public final class OwnHardware {

    private static final List<Entry> ENTRIES = new ArrayList<>();
    /* The parts that look just as another does, by item: a board that differs from another only in what it seats. */
    private static final Map<ResourceLocation, ResourceLocation> DRAWN_AS = new ConcurrentHashMap<>();

    private OwnHardware() {
    }

    /** Keeps that the part {@code id} looks just as the part {@code like} does, drawn by its models. */
    public static void drawnAs(final String id, final String like) {
        DRAWN_AS.put(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, id),
                ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, like));
    }

    /** The part whose models draw {@code item}: the item itself, unless it looks just as another does. */
    public static ResourceLocation modelOf(final ResourceLocation item) {
        return DRAWN_AS.getOrDefault(item, item);
    }

    /**
     * Keeps the part this mod declares under {@code id}, to be declared through the event: {@code declare} is the
     * event's call for its kind, given the item.
     */
    public static synchronized void add(final String id, final BiConsumer<ComputersRegisterEvent, Item> declare) {
        ENTRIES.add(new Entry(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, id), declare));
    }

    /** Declares every part kept, through {@code event}; the mod's listener on the event. */
    public static synchronized void declare(final ComputersRegisterEvent event) {
        for (final Entry entry : ENTRIES) {
            final Item item = BuiltInRegistries.ITEM.get(entry.id());
            if (item == Items.AIR) {
                throw new IllegalStateException("the part " + entry.id() + " was kept but never registered");
            }
            entry.declare().accept(event, item);
        }
    }

    /** The ids of the parts kept, in the order the mod declared them: the catalogue's own order. */
    public static synchronized List<ResourceLocation> ids() {
        return ENTRIES.stream().map(Entry::id).toList();
    }

    private record Entry(ResourceLocation id, BiConsumer<ComputersRegisterEvent, Item> declare) {
    }
}
