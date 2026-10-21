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

/**
 * The parts vehicles are made of, by id: declared in code by their mods, and written by data packs as files under
 * {@code data/<namespace>/jstech/vehicle_parts/}, a file winning over a declaration of its id.
 *
 * <pre>{@code
 * public static final ResourceLocation DIESEL = rl("diesel_engine");
 * VehicleParts.declare(DIESEL, VehiclePartDef.engine("item.mymod.diesel_engine", 180,
 *         new VehiclePartDef.Engine(90_000, 800, 4500, 0.3, false, List.of(MyFuels.DIESEL), Optional.empty())));
 * }</pre>
 */
public final class VehicleParts {

    private static final VehicleCatalogue<VehiclePartDef> CATALOGUE = new VehicleCatalogue<>(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "vehicle_parts"), "jstech/vehicle_parts",
            VehiclePartDef.CODEC);

    private VehicleParts() {
    }

    /** Declares the part {@code id}, as it is while no data pack says otherwise; and gives the id. */
    public static ResourceLocation declare(final ResourceLocation id, final VehiclePartDef part) {
        CATALOGUE.declare(id, part);
        return id;
    }

    /** The part {@code id} on the side {@code level} is on, or none. */
    public static Optional<VehiclePartDef> get(final Level level, final ResourceLocation id) {
        return CATALOGUE.get(level, id);
    }

    /** The part {@code id} wherever it is known, for what has no level to ask by. */
    public static Optional<VehiclePartDef> anywhere(final ResourceLocation id) {
        return CATALOGUE.anywhere(id);
    }

    /** Every part on the side {@code level} is on, by id. */
    public static Map<ResourceLocation, VehiclePartDef> all(final Level level) {
        return CATALOGUE.all(level);
    }

    /** Every part the player's game has, by id. */
    public static Map<ResourceLocation, VehiclePartDef> allOnClient() {
        return CATALOGUE.allOnClient();
    }

    /** Every part declared in code, by id. */
    public static Map<ResourceLocation, VehiclePartDef> declared() {
        return CATALOGUE.declared();
    }
}
