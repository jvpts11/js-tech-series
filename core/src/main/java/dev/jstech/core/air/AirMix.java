/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.air;

import com.mojang.serialization.Codec;
import dev.jstech.core.JsCore;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/**
 * What a body of air is made of: each gas, by its material's id, and the share of the air it makes up. Whether it can
 * be breathed is read from the partial pressure of its oxygen at the air's pressure, as a body breathes.
 *
 * <pre>{@code
 * "air": { "jscore:carbon_dioxide": 0.95, "jscore:nitrogen": 0.03, "jscore:argon": 0.02 }      // Mars
 * }</pre>
 *
 * @param shares each gas's share of the air, 0 to 1, by its material's id
 */
public record AirMix(Map<ResourceLocation, Double> shares) {

    /** The gases the Core reads in air, by their materials' ids. */
    public static final ResourceLocation OXYGEN = gas("oxygen");
    public static final ResourceLocation NITROGEN = gas("nitrogen");
    public static final ResourceLocation ARGON = gas("argon");
    public static final ResourceLocation CARBON_DIOXIDE = gas("carbon_dioxide");
    /** The overworld's air. */
    public static final AirMix EARTH = new AirMix(Map.of(NITROGEN, 0.78, OXYGEN, 0.21, ARGON, 0.01));
    /** No air at all. */
    public static final AirMix VACUUM = new AirMix(Map.of());
    public static final Codec<AirMix> CODEC = Codec.unboundedMap(ResourceLocation.CODEC, Codec.doubleRange(0, 1))
            .xmap(AirMix::new, AirMix::shares);

    public AirMix {
        shares = Map.copyOf(new LinkedHashMap<>(shares));
    }

    /** The share of the air {@code gas} makes up, 0 for none. */
    public double share(final ResourceLocation gas) {
        return this.shares.getOrDefault(gas, 0.0);
    }

    /** The partial pressure of {@code gas} in this air at {@code pressure} kilopascals. */
    public double partial(final ResourceLocation gas, final double pressure) {
        return AirMath.partial(share(gas), pressure);
    }

    /** Whether this air at {@code pressure} kilopascals can be breathed. */
    public boolean breathable(final double pressure) {
        return AirMath.breathable(share(OXYGEN), pressure);
    }

    private static ResourceLocation gas(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsCore.MODID, path);
    }
}
