/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.storage.StorageKey;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

/**
 * What a player means by the name they typed.
 *
 * <p>A registry id is what a machine calls a thing; nobody types one at a prompt when they can help it. This
 * turns {@code cobblestone}, {@code minecraft:cobblestone} and {@code "oak log"} alike into the things they
 * could stand for, in the order a person would expect: the one thing named exactly, then the things whose name
 * begins with it, then the things whose name has it somewhere. What the network is holding comes first among
 * equals, since a name typed at a network terminal nearly always means something that is in there.
 *
 * <p>Several answers is an answer. A name that fits more than one thing is not a failure to be guessed at: the
 * caller shows them and asks which, which is the whole reason this does not return one key.
 */
public final class ItemNames {

    /** How many things a name may fit before the list is cut short rather than filling the glass. */
    public static final int MOST_MATCHES = 8;

    private ItemNames() {
    }

    /**
     * What that name could stand for, best first.
     *
     * @param typed  what the player wrote, with or without a namespace, in any case
     * @param stored what the network is holding, which is what a name most likely means at a terminal
     */
    public static List<StorageKey> matching(final String typed, final Map<StorageKey, Long> stored) {
        final String wanted = typed == null ? "" : typed.trim().toLowerCase(Locale.ROOT);
        if (wanted.isEmpty()) {
            return List.of();
        }
        /*
         * A name written the way a machine writes one is that thing and nothing else, so it never opens a
         * question. Everything else is a search, and what is in the network is searched before the registry.
         */
        final StorageKey exact = exact(wanted, stored);
        if (exact != null) {
            return List.of(exact);
        }
        final Map<StorageKey, Integer> found = new LinkedHashMap<>();
        for (final StorageKey key : stored.keySet()) {
            rank(key, wanted, found);
        }
        /*
         * The registry is only read when the network is holding nothing of that name, which is the one case
         * where it has to be: asking to craft something there is none of. Reading it is reading every item
         * there is, so a name that the network can answer never pays for that.
         */
        if (found.isEmpty()) {
            for (final Item item : BuiltInRegistries.ITEM) {
                rank(StorageKey.of(item), wanted, found);
            }
        }
        final List<StorageKey> out = new ArrayList<>(found.keySet());
        out.sort((left, right) -> Integer.compare(found.get(left), found.get(right)));
        return out.size() > MOST_MATCHES ? List.copyOf(out.subList(0, MOST_MATCHES)) : List.copyOf(out);
    }

    /**
     * The one thing that name is, written as a machine writes one, or null when it is a name to search for.
     *
     * <p>A name with a namespace is a registry id and answers for every kind of thing a network holds; one
     * without is taken as vanilla's when an item answers to it, which is what {@code iron_ingot} means.
     */
    private static StorageKey exact(final String wanted, final Map<StorageKey, Long> stored) {
        if (wanted.contains(":")) {
            for (final StorageKey key : stored.keySet()) {
                if (key.registryId().toString().equals(wanted)) {
                    return key;
                }
            }
            final ResourceLocation id = ResourceLocation.tryParse(wanted);
            return id == null ? null : BuiltInRegistries.ITEM.getOptional(id).map(StorageKey::of).orElse(null);
        }
        return StorageKey.byName(wanted);
    }

    /** Scores how well a key answers to the name, and remembers the best score it has had. */
    private static void rank(final StorageKey key, final String wanted, final Map<StorageKey, Integer> found) {
        final String display = key.displayName().getString().toLowerCase(Locale.ROOT);
        final String path = key.registryId().getPath();
        final int score;
        if (display.equals(wanted) || path.equals(wanted)) {
            score = 0;
        } else if (display.startsWith(wanted) || path.startsWith(wanted)) {
            score = 1;
        } else if (display.contains(wanted) || path.contains(wanted)) {
            score = 2;
        } else {
            return;
        }
        found.merge(key, score, Math::min);
    }
}
