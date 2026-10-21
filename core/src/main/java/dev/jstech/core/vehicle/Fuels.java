/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.vehicle;

import dev.jstech.core.JsCore;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;

/**
 * The fuels engines burn, by id: declared in code by their mods, and written by data packs as files under
 * {@code data/<namespace>/jstech/fuels/}, a file winning over a declaration of its id.
 */
public final class Fuels {

    private static final VehicleCatalogue<Fuel> CATALOGUE = new VehicleCatalogue<>(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "fuels"), "jstech/fuels", Fuel.CODEC);

    private Fuels() {
    }

    /** Declares the fuel {@code id}, as it is while no data pack says otherwise; and gives the id. */
    public static ResourceLocation declare(final ResourceLocation id, final Fuel fuel) {
        CATALOGUE.declare(id, fuel);
        return id;
    }

    /** The fuel {@code id} on the side {@code level} is on, or none. */
    public static Optional<Fuel> get(final Level level, final ResourceLocation id) {
        return CATALOGUE.get(level, id);
    }

    /** Every fuel on the side {@code level} is on, by id. */
    public static Map<ResourceLocation, Fuel> all(final Level level) {
        return CATALOGUE.all(level);
    }

    /** Which of {@code fuels} the liquid {@code fluid} is, or none. */
    public static Optional<ResourceLocation> liquidAmong(final Level level, final Iterable<ResourceLocation> fuels,
                                                         final Fluid fluid) {
        for (final ResourceLocation id : fuels) {
            final Optional<Fuel> fuel = get(level, id);
            if (fuel.isPresent() && fuel.get().liquid().isPresent() && fluid.is(fuel.get().liquid().get())) {
                return Optional.of(id);
            }
        }
        return Optional.empty();
    }

    /** Which of {@code fuels} the gas of the material {@code material} is, or none. */
    public static Optional<ResourceLocation> gasAmong(final Level level, final Iterable<ResourceLocation> fuels,
                                                      final ResourceLocation material) {
        for (final ResourceLocation id : fuels) {
            final Optional<Fuel> fuel = get(level, id);
            if (fuel.isPresent() && fuel.get().gases().contains(material)) {
                return Optional.of(id);
            }
        }
        return Optional.empty();
    }
}
