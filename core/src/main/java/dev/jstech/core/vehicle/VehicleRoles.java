/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.vehicle;

import dev.jstech.core.JsCore;
import dev.jstech.core.text.TextKey;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;

/**
 * What whoever sits in a seat does from it: the Core's driver and passenger, and the roles a mod adds, a crane's
 * operator or a lookout, which its own code reads from the vehicle ({@code vehicle.seatedAs(role)}). A role is
 * declared in code, once, since players read its name.
 */
public final class VehicleRoles {

    /** Drives the vehicle. */
    public static final ResourceLocation DRIVER = ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "driver");
    /** Rides along. */
    public static final ResourceLocation PASSENGER = ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "passenger");

    private static final Map<ResourceLocation, Role> ROLES = new ConcurrentHashMap<>(Map.of(
            DRIVER, new Role(VehicleTexts.DRIVER, true), PASSENGER, new Role(VehicleTexts.PASSENGER, false)));

    private VehicleRoles() {
    }

    /** Declares the role {@code id}, called {@code name}, driving the vehicle or not; and gives the id. */
    public static ResourceLocation declare(final ResourceLocation id, final TextKey name, final boolean drives) {
        ROLES.put(Objects.requireNonNull(id, "id"), new Role(Objects.requireNonNull(name, "name"), drives));
        return id;
    }

    /** The role {@code id}, or a passenger's for one never declared. */
    public static Role of(final ResourceLocation id) {
        return ROLES.getOrDefault(id, ROLES.get(PASSENGER));
    }

    /**
     * A role.
     *
     * @param name   what players call it
     * @param drives whether whoever holds it drives the vehicle
     */
    public record Role(TextKey name, boolean drives) {
    }
}
