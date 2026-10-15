/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.energy;

import dev.jstech.core.JsCore;
import dev.jstech.core.grid.GridKind;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.util.DeclarationError;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Every energy the mods declare, FE first: what each is counted in, its classes of line and their grids. A mod declares
 * its own with {@link #builder}; one that only wants FE uses {@link #FE} and declares nothing.
 */
public final class Energies {

    private static final Map<ResourceLocation, Energy> BY_ID = new ConcurrentHashMap<>();
    private static final List<Energy> ALL = new CopyOnWriteArrayList<>();

    /** FE, the game's own energy, with no classes: its one grid is the Core's power grid. */
    public static final Energy FE = new Energy.Builder(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "fe"),
            CoreEnergy.FE_NAME).declareOn(GridKind.POWER);

    private Energies() {
    }

    /** Starts declaring the energy {@code id}, which players call {@code name}. */
    public static Energy.Builder builder(final ResourceLocation id, final TextKey name) {
        return new Energy.Builder(id, name);
    }

    /** Every energy declared, FE first. */
    public static List<Energy> all() {
        return List.copyOf(ALL);
    }

    /** The energy declared as {@code id}, or none. */
    public static Optional<Energy> byId(final ResourceLocation id) {
        return Optional.ofNullable(BY_ID.get(id));
    }

    /** The energy whose lines make {@code grid}, or none for a grid of no energy. */
    public static Optional<Energy> ofGrid(final GridKind grid) {
        for (final Energy energy : ALL) {
            if (energy.classOf(grid).isPresent()) {
                return Optional.of(energy);
            }
        }
        return Optional.empty();
    }

    /* Registers {@code energy} and declares its grids, or puts it on {@code only}, a grid declared already. */
    static Energy register(final Energy energy, final @Nullable GridKind only) {
        if (BY_ID.putIfAbsent(energy.id(), energy) != null) {
            throw DeclarationError.of(energy.id().getNamespace(), "the energy " + energy.id(), "is declared twice",
                    "declare it once, as a static final");
        }
        ALL.add(energy);
        energy.declareGrids(only);
        return energy;
    }
}
