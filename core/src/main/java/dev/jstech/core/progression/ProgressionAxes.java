/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.progression;

import dev.jstech.core.JsCore;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.core.tier.IndustrialTier;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Every axis a player advances along, by id: the two the series counts by, the eras of hardware and the tiers of
 * industry, and any a mod adds while the game loads. An axis is registered once, from its mod's constructor.
 */
@TextHolder
public final class ProgressionAxes {

    /** The names of the two axes of the series. */
    public static final TextKey HARDWARE_ERA_NAME = TextKey.of("jscore.axis.hardware_era", "Hardware era");
    public static final TextKey INDUSTRIAL_TIER_NAME = TextKey.of("jscore.axis.industrial_tier", "Industrial tier");

    /** The eras of hardware, from the Vintage to the Singularity. */
    public static final ProgressionAxis<HardwareEra> HARDWARE_ERA = ProgressionAxis.of(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "hardware_era"), HARDWARE_ERA_NAME,
            List.of(HardwareEra.values()));
    /** The tiers of industry, from T0 to T9. */
    public static final ProgressionAxis<IndustrialTier> INDUSTRIAL_TIER = ProgressionAxis.of(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "industrial_tier"), INDUSTRIAL_TIER_NAME,
            List.of(IndustrialTier.values()));

    private static final Map<ResourceLocation, ProgressionAxis<?>> BY_ID =
            Collections.synchronizedMap(new LinkedHashMap<>());

    static {
        register(HARDWARE_ERA);
        register(INDUSTRIAL_TIER);
    }

    private ProgressionAxes() {
    }

    /**
     * Registers {@code axis}, from the constructor of the mod that adds it.
     *
     * @throws IllegalStateException when an axis of the same id is registered already
     */
    public static void register(final ProgressionAxis<?> axis) {
        if (BY_ID.putIfAbsent(axis.id(), axis) != null) {
            throw new IllegalStateException("the progression axis " + axis.id() + " is registered twice");
        }
    }

    /** The axis registered as {@code id}, or null when there is none. */
    public static @Nullable ProgressionAxis<?> byId(final ResourceLocation id) {
        return BY_ID.get(id);
    }

    /** Every axis registered, the series' own first. */
    public static Collection<ProgressionAxis<?>> all() {
        synchronized (BY_ID) {
            return List.copyOf(BY_ID.values());
        }
    }
}
