/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.dimension;

import dev.jstech.core.JsCore;
import dev.jstech.core.hazard.HazardKind;
import dev.jstech.core.hazard.Hazards;
import dev.jstech.core.hazard.IHazardEffect;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * The hazards of a place's air and climate, kinds of the hazard kit: air that cannot be breathed, heat that burns,
 * cold that freezes, air stale with carbon dioxide and air with too much oxygen. Each builds up a second at a time
 * while the player stands in it and is gone the moment they leave; a suit spares its wearer by cancelling the
 * {@link DimensionHazardEvent}, as before.
 */
@TextHolder
public final class DimensionHazards {

    public static final TextKey SECONDS = TextKey.of("jscore.hazard.seconds", "s");
    public static final TextKey AIR_NAME = TextKey.of("jscore.hazard.unbreathable", "Air that cannot be breathed");
    public static final TextKey AIR_BAND = TextKey.of("jscore.hazard.unbreathable.band", "Choking");
    public static final TextKey HEAT_NAME = TextKey.of("jscore.hazard.scorching", "Scorching heat");
    public static final TextKey HEAT_BAND = TextKey.of("jscore.hazard.scorching.band", "Burning");
    public static final TextKey COLD_NAME = TextKey.of("jscore.hazard.freezing", "Freezing cold");
    public static final TextKey COLD_BAND = TextKey.of("jscore.hazard.freezing.band", "Freezing");
    public static final TextKey CARBON_NAME = TextKey.of("jscore.hazard.stale_air", "Stale air");
    public static final TextKey CARBON_BAND = TextKey.of("jscore.hazard.stale_air.band", "A headache");
    public static final TextKey OXYGEN_NAME = TextKey.of("jscore.hazard.rich_air", "Too much oxygen");
    public static final TextKey OXYGEN_BAND = TextKey.of("jscore.hazard.rich_air.band", "Poisoned slowly");
    private static final Map<DimensionHazardEvent.Hazard, HazardKind> KINDS =
            new EnumMap<>(DimensionHazardEvent.Hazard.class);

    public static final HazardKind AIR = kind(DimensionHazardEvent.Hazard.AIR, "unbreathable", AIR_NAME, AIR_BAND);
    public static final HazardKind HEAT = kind(DimensionHazardEvent.Hazard.HEAT, "scorching", HEAT_NAME, HEAT_BAND);
    public static final HazardKind COLD = kind(DimensionHazardEvent.Hazard.COLD, "freezing", COLD_NAME, COLD_BAND);
    public static final HazardKind CARBON_DIOXIDE = kind(DimensionHazardEvent.Hazard.CARBON_DIOXIDE, "stale_air",
            CARBON_NAME, CARBON_BAND);
    public static final HazardKind OXYGEN = kind(DimensionHazardEvent.Hazard.OXYGEN, "rich_air", OXYGEN_NAME,
            OXYGEN_BAND);

    private DimensionHazards() {
    }

    /** Declares the kinds, from the Core's constructor. */
    public static void declare() {
        // Loading the class declares them.
    }

    /** The kind of {@code hazard}. */
    public static HazardKind of(final DimensionHazardEvent.Hazard hazard) {
        return KINDS.get(hazard);
    }

    private static HazardKind kind(final DimensionHazardEvent.Hazard hazard, final String path, final TextKey name,
                                   final TextKey band) {
        final HazardKind kind = Hazards.declare(HazardKind.builder(
                        ResourceLocation.fromNamespaceAndPath(JsCore.MODID, path), name, SECONDS)
                .band(0, band, new IHazardEffect() {
                    @Override
                    public void second(final LivingEntity body, final HazardKind self, final double level) {
                        if (body instanceof Player player && hazard == DimensionHazardEvent.Hazard.HEAT) {
                            DimensionEffects.ignite(player);
                        }
                    }

                    @Override
                    public void tick(final LivingEntity body, final HazardKind self, final double level) {
                        if (body instanceof Player player) {
                            DimensionEffects.suffer(player, hazard);
                        }
                    }
                })
                .recoversShare(1).build());
        kind.addSource((body, level) -> body instanceof Player player
                && DimensionEffects.weighed(player).contains(hazard) ? 1 : 0);
        KINDS.put(hazard, kind);
        return kind;
    }
}
