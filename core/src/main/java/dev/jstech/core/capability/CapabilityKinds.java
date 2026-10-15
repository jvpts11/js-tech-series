/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.capability;

import dev.jstech.core.util.DeclarationError;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.resources.ResourceLocation;

/** Every capability a mod declared through the Core, by id. */
public final class CapabilityKinds {

    private static final Map<ResourceLocation, CapabilityKind<?>> BY_ID = new ConcurrentHashMap<>();
    private static final List<CapabilityKind<?>> ALL = new CopyOnWriteArrayList<>();

    private CapabilityKinds() {
    }

    /**
     * Declares the capability {@code id}, handing over a {@code type}, for blocks, items and entities.
     *
     * @throws DeclarationError when a capability of that id was declared already
     */
    public static <T> CapabilityKind<T> declare(final ResourceLocation id, final Class<T> type) {
        final CapabilityKind<T> kind = new CapabilityKind<>(id, type);
        if (BY_ID.putIfAbsent(id, kind) != null) {
            throw DeclarationError.of(id.getNamespace(), "the capability " + id, "is declared twice",
                    "declare it once, as a static final, and use that field");
        }
        ALL.add(kind);
        return kind;
    }

    /** Every capability declared, in the order they were declared. */
    public static List<CapabilityKind<?>> all() {
        return List.copyOf(ALL);
    }
}
