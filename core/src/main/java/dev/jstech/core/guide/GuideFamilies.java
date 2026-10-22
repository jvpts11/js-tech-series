/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Families of items only a mod's code knows, which an entry of a manual written in Markdown is the page of by name:
 * every form of every material some installed mod uses, every part a mod adds to a machine. A mod registers each under
 * an id of its namespace while the game loads, and an entry names it in its header,
 * {@code covers_family: mymod:kiln_parts}; the items are asked for as the manuals are read, so a part added later is
 * the page of the entry with nothing more to write.
 *
 * <pre>{@code
 * GuideFamilies.register("mymod:kiln_parts", () -> MyContent.KILN_PARTS.stream()
 *         .map(part -> BuiltInRegistries.ITEM.getKey(part.asItem()).toString()).toList());
 * }</pre>
 */
public final class GuideFamilies {

    private static final Map<String, Supplier<List<String>>> FAMILIES = new ConcurrentHashMap<>();
    /* What went wrong goes to the JDK's own log, which needs no library, so the class stays pure. */
    private static final System.Logger LOGGER = System.getLogger(GuideFamilies.class.getName());

    private GuideFamilies() {
    }

    /** Registers the family {@code id}, the ids of its items, {@code namespace:path}, answered when asked. */
    public static void register(final String id, final Supplier<List<String>> items) {
        Objects.requireNonNull(items, "items");
        if (FAMILIES.putIfAbsent(Objects.requireNonNull(id, "id"), items) != null) {
            throw new IllegalStateException("the manuals' family " + id + " is registered twice");
        }
    }

    /** The ids of the items of the family {@code id}, answered now; none when no mod registered it, or it failed. */
    public static List<String> itemsOf(final String id) {
        final Supplier<List<String>> items = FAMILIES.get(id);
        if (items == null) {
            LOGGER.log(System.Logger.Level.WARNING, "No mod registered the manuals' family " + id);
            return List.of();
        }
        try {
            return List.copyOf(items.get());
        } catch (final RuntimeException e) {
            LOGGER.log(System.Logger.Level.WARNING, "The manuals' family " + id + " could not be read", e);
            return List.of();
        }
    }

    /** The id of every family registered. */
    public static Set<String> ids() {
        return Set.copyOf(FAMILIES.keySet());
    }
}
