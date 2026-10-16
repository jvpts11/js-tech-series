/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.registry;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;

/**
 * The namespaces whose data the game takes as stable: their registry entries and their dimensions open a world with
 * no warning about experimental settings. The game marks as experimental everything not from its own pack, and every
 * mod's jar is a pack of its own; a mod whose data is not an experiment says so here, from its constructor, for its
 * own namespace only. Another mod's dimension still warns, as it should.
 */
public final class StableNamespaces {

    private static final Set<String> STABLE = ConcurrentHashMap.newKeySet();

    private StableNamespaces() {
    }

    /** Says the data of {@code namespace} is stable, from the constructor of the mod it is. */
    public static void declare(final String namespace) {
        STABLE.add(Objects.requireNonNull(namespace, "namespace"));
    }

    /** Whether what {@code id} names is of a namespace declared stable. */
    public static boolean isStable(final ResourceLocation id) {
        return STABLE.contains(id.getNamespace());
    }
}
