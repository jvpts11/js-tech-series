/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.network;

import dev.jstech.core.util.DeclarationError;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

/**
 * The state tables of every mod ({@link StateTable}), declared while the game loads and closed once it has loaded, so
 * a query saved yesterday reads the same tables today. A table under a name another already has stops the load.
 */
public final class StateTables {

    private static final Map<ResourceLocation, StateTable> TABLES = new LinkedHashMap<>();
    private static boolean frozen;

    private StateTables() {
    }

    /**
     * Declares {@code table}, from a mod's constructor or the class that holds its tables, before the game has loaded.
     *
     * @return the table, to keep in a constant
     */
    public static synchronized StateTable declare(final StateTable table) {
        Objects.requireNonNull(table, "table");
        if (frozen) {
            throw DeclarationError.of(table.id().getNamespace(), "the state table " + table.id(),
                    "is declared after the game has loaded",
                    "declare it from the mod's constructor, or from a class the constructor loads");
        }
        final StateTable before = TABLES.putIfAbsent(table.id(), table);
        if (before != null && before != table) {
            throw DeclarationError.of(table.id().getNamespace(), "the state table " + table.id(),
                    "is declared twice", "give one of the two another name");
        }
        return table;
    }

    /** The table named {@code id}, if a mod declared one. */
    public static synchronized Optional<StateTable> get(final ResourceLocation id) {
        return Optional.ofNullable(TABLES.get(id));
    }

    /** Every table, in the order the mods declared them. */
    public static synchronized List<StateTable> all() {
        return List.copyOf(TABLES.values());
    }

    /** Closes the list once the game has loaded; from the Core. */
    @ApiStatus.Internal
    public static synchronized void freeze() {
        frozen = true;
    }
}
