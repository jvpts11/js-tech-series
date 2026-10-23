/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.tier;

import dev.jstech.core.JsCore;
import dev.jstech.core.progression.ProgressionAxes;
import dev.jstech.core.progression.ProgressionAxis;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

/**
 * The two axes the series counts by, the eras of hardware and the tiers of industry, as axes of the progression kit:
 * a player moves along them as along any other, and they are registered before any mod's own.
 */
@TextHolder
public final class SeriesAxes {

    /** The names of the two axes. */
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

    private SeriesAxes() {
    }

    /** Registers the two axes, once, from the Core's constructor, before any mod registers its own. */
    @ApiStatus.Internal
    public static void declare() {
        ProgressionAxes.register(HARDWARE_ERA);
        ProgressionAxes.register(INDUSTRIAL_TIER);
    }
}
