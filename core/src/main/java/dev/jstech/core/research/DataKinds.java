/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.research;

import dev.jstech.core.util.DeclarationError;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * The kinds of data of every mod, an open list: any mod adds its own, and a discovery that asks for a kind no installed
 * mod declares does not exist.
 */
public final class DataKinds {

    private static final Map<ResourceLocation, DataKind> DECLARED = new LinkedHashMap<>();

    private DataKinds() {
    }

    /**
     * Declares {@code kind}, from a mod's constructor or the class that holds it.
     *
     * @return the kind, to keep in a constant
     */
    public static synchronized DataKind declare(final DataKind kind) {
        Objects.requireNonNull(kind, "kind");
        if (DECLARED.putIfAbsent(kind.id(), kind) != null) {
            throw DeclarationError.of(kind.id().getNamespace(), "the kind of data " + kind.id(), "is declared twice",
                    "give one of the two another id");
        }
        return kind;
    }

    /** The kind of {@code id}, if one is declared. */
    public static synchronized Optional<DataKind> byId(final ResourceLocation id) {
        return Optional.ofNullable(DECLARED.get(id));
    }

    /** Every kind, in the order the mods declared them. */
    public static synchronized List<DataKind> all() {
        return List.copyOf(DECLARED.values());
    }

    /** The kind of data {@code stack} is, when it is data. */
    public static Optional<DataKind> of(final ItemStack stack) {
        return stack.getItem() instanceof IResearchData data ? Optional.of(data.dataKind()) : Optional.empty();
    }
}
