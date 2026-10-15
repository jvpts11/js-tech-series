/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.material;

import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.util.DeclarationError;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.jetbrains.annotations.ApiStatus;

/**
 * Things more than one mod uses that are no material: generic components (a servomotor, an LED, an antenna), items
 * with no game of their own. Each is declared once, in a catalogue any mod adds to, and exists in the game only when an
 * installed mod says it uses it; the Core registers the union, each named as declared and drawn with the texture it
 * names. Each mod that makes one gives its own recipe.
 *
 * <pre>{@code
 * public static final CatalogueItems.Entry LED = CatalogueItems.declare(rl("led"), LED_NAME, rl("item/led"));
 * public static final Supplier<Item> MY_LED = CatalogueItems.use(MODID, LED);
 * }</pre>
 */
public final class CatalogueItems {

    private static final Map<ResourceLocation, Entry> BY_ID = new ConcurrentHashMap<>();
    private static final List<Entry> ALL = new CopyOnWriteArrayList<>();
    private static final List<Use> USES = new CopyOnWriteArrayList<>();

    private static volatile List<Entry> registered = List.of();
    private static volatile boolean closed;

    private CatalogueItems() {
    }

    /**
     * Declares the entry {@code id}, called {@code name}, drawn with the texture {@code art}
     * ({@code mymod:item/led} for {@code assets/mymod/textures/item/led.png}).
     *
     * @throws DeclarationError when an entry of that id is declared already
     */
    public static Entry declare(final ResourceLocation id, final TextKey name, final ResourceLocation art) {
        final Entry entry = new Entry(id, name, art);
        if (BY_ID.putIfAbsent(id, entry) != null) {
            throw DeclarationError.of(id.getNamespace(), "the catalogue entry " + id, "is declared twice",
                    "declare it once, as a static final");
        }
        ALL.add(entry);
        return entry;
    }

    /**
     * Declares that the mod {@code modid} uses {@code entry}, and gives its item.
     *
     * @throws DeclarationError when the game has already registered its things
     */
    public static Supplier<Item> use(final String modid, final Entry entry) {
        if (closed) {
            throw DeclarationError.of(modid, "the use of " + entry.id(), "comes after the game registered its things",
                    "declare uses from the mod's constructor");
        }
        USES.add(new Use(Objects.requireNonNull(modid, "modid"), Objects.requireNonNull(entry, "entry")));
        return DeferredItem.createItem(entry.id());
    }

    /** Every entry declared, in the order they were. */
    public static List<Entry> all() {
        return List.copyOf(ALL);
    }

    /** The entries some installed mod uses, which the Core registered, in the order they were declared. */
    public static List<Entry> registered() {
        return registered;
    }

    /** The items of the entries the Core registered. */
    public static List<Item> items() {
        final List<Item> out = new ArrayList<>();
        for (final Entry entry : registered) {
            out.add(BuiltInRegistries.ITEM.get(entry.id()));
        }
        return out;
    }

    /** The mods that declared a use. */
    public static Set<String> users() {
        final Set<String> out = new LinkedHashSet<>();
        USES.forEach(use -> out.add(use.modid()));
        return out;
    }

    /** Hands the mod's bus the registering of the union, once, from the Core's constructor. */
    @ApiStatus.Internal
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterEvent.class, CatalogueItems::onRegister);
    }

    private static void onRegister(final RegisterEvent event) {
        event.register(Registries.ITEM, helper -> {
            closed = true;
            final Set<Entry> used = new LinkedHashSet<>();
            USES.forEach(use -> used.add(use.entry()));
            final List<Entry> out = new ArrayList<>();
            for (final Entry entry : ALL) {
                if (used.contains(entry)) {
                    out.add(entry);
                    helper.register(entry.id(), new CatalogueItem(new Item.Properties(), entry));
                }
            }
            registered = List.copyOf(out);
        });
    }

    /**
     * An entry of the catalogue.
     *
     * @param id   its id
     * @param name what it is called
     * @param art  the texture it is drawn with
     */
    public record Entry(ResourceLocation id, TextKey name, ResourceLocation art) {

        public Entry {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(art, "art");
        }
    }

    /** The item of an entry, named as declared. */
    public static final class CatalogueItem extends Item {

        private final Entry entry;

        public CatalogueItem(final Properties properties, final Entry entry) {
            super(properties);
            this.entry = entry;
        }

        /** The entry it is. */
        public Entry entry() {
            return this.entry;
        }

        @Override
        public Component getName(final ItemStack stack) {
            return GameText.component(this.entry.name());
        }
    }

    /* One mod's use of one entry. */
    private record Use(String modid, Entry entry) {
    }
}
