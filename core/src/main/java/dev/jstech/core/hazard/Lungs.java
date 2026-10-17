/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.hazard;

import dev.jstech.core.JsCore;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * What dusts do to the lungs, the real diseases of miners and builders: coal dust builds up black lung, silica
 * silicosis, asbestos fibres asbestosis, each a lasting harm in percent of the lungs, that heals slowly, and
 * asbestosis hardly at all. A cough first, then less breath, then a struggle for it, then the lungs failing. Masks and
 * respirators protect, by what they stop of what is breathed. Dust is breathed breaking the blocks of the tags
 * {@code jscore:dust/coal}, {@code jscore:dust/silica} and {@code jscore:dust/asbestos}, and wherever a machine of
 * any mod puts it in the air ({@link #breathe}).
 */
@TextHolder
public final class Lungs {

    public static final TextKey PERCENT = TextKey.of("jscore.hazard.percent", "%");
    public static final TextKey COAL_NAME = TextKey.of("jscore.hazard.black_lung", "Coal dust");
    public static final TextKey SILICA_NAME = TextKey.of("jscore.hazard.silicosis", "Silica");
    public static final TextKey ASBESTOS_NAME = TextKey.of("jscore.hazard.asbestosis", "Asbestos");
    public static final TextKey COUGH = TextKey.of("jscore.hazard.lungs.cough", "A slight cough");
    public static final TextKey SHORT_OF_BREATH = TextKey.of("jscore.hazard.lungs.short", "Short of breath");
    public static final TextKey STRUGGLING = TextKey.of("jscore.hazard.lungs.struggling", "Struggling to breathe");
    public static final TextKey FAILING = TextKey.of("jscore.hazard.lungs.failing", "The lungs are failing");
    public static final TextKey COAL_TREATMENT = TextKey.of("jscore.hazard.black_lung.treatment",
            "Black lung has no cure: clean air lets it heal a little, and a mask keeps it from coming back");
    public static final TextKey SILICA_TREATMENT = TextKey.of("jscore.hazard.silicosis.treatment",
            "Silicosis has no cure: it heals slowly in clean air, and wet cutting and a respirator prevent it");
    public static final TextKey ASBESTOS_TREATMENT = TextKey.of("jscore.hazard.asbestosis.treatment",
            "Asbestosis hardly heals at all: never breathe the fibres, and wear a respirator near them");
    /** What the lungs failing kills as, its own death message. */
    public static final ResourceKey<DamageType> FAILURE = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "lung_failure"));
    /** The blocks whose breaking puts coal dust in the air, silica, and asbestos fibres. */
    public static final TagKey<Block> COAL_DUSTY = tag("coal");
    public static final TagKey<Block> SILICA_DUSTY = tag("silica");
    public static final TagKey<Block> ASBESTOS_DUSTY = tag("asbestos");
    /* How much of the lungs one block broken harms, in percent, before a mask. */
    private static final double COAL_PER_BLOCK = 0.05;
    private static final double SILICA_PER_BLOCK = 0.01;
    private static final double ASBESTOS_PER_BLOCK = 0.2;
    /* What has been breathed since each body's last second, by kind, to be taken in at its next. */
    private static final Map<UUID, double[]> BREATHED = new ConcurrentHashMap<>();
    private static final int COAL = 0;
    private static final int SILICA = 1;
    private static final int ASBESTOS = 2;

    public static final HazardKind BLACK_LUNG = kind("black_lung", COAL_NAME, COAL_TREATMENT, 0.01, COAL);
    public static final HazardKind SILICOSIS = kind("silicosis", SILICA_NAME, SILICA_TREATMENT, 0.005, SILICA);
    public static final HazardKind ASBESTOSIS = kind("asbestosis", ASBESTOS_NAME, ASBESTOS_TREATMENT, 0.0002,
            ASBESTOS);

    private Lungs() {
    }

    /** Hands the game's bus what breathes the dust of broken blocks, once, from the Core's constructor. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(BlockEvent.BreakEvent.class, event -> {
            if (event.getLevel().isClientSide() || !HazardBalance.lungs()) {
                return;
            }
            if (event.getState().is(COAL_DUSTY)) {
                breathe(event.getPlayer(), BLACK_LUNG, COAL_PER_BLOCK);
            }
            if (event.getState().is(SILICA_DUSTY)) {
                breathe(event.getPlayer(), SILICOSIS, SILICA_PER_BLOCK);
            }
            if (event.getState().is(ASBESTOS_DUSTY)) {
                breathe(event.getPlayer(), ASBESTOSIS, ASBESTOS_PER_BLOCK);
            }
        });
    }

    /**
     * {@code body} breathes {@code percent} of {@code disease}'s dust, as a machine of a mod puts it in the air round
     * them: taken in at their next second, what they wear for what is breathed taking its share off.
     */
    public static void breathe(final LivingEntity body, final HazardKind disease, final double percent) {
        final int index = disease == BLACK_LUNG ? COAL : disease == SILICOSIS ? SILICA : ASBESTOS;
        BREATHED.computeIfAbsent(body.getUUID(), id -> new double[3])[index] += percent;
    }

    /** How much of {@code body}'s lungs is harmed in all, in percent: every dust's harm added up. */
    public static double harmed(final LivingEntity body) {
        return Math.min(100, Hazards.level(body, BLACK_LUNG) + Hazards.level(body, SILICOSIS)
                + Hazards.level(body, ASBESTOSIS));
    }

    /** The lungs' kinds, coal dust's first. */
    public static List<HazardKind> kinds() {
        return List.of(BLACK_LUNG, SILICOSIS, ASBESTOSIS);
    }

    private static HazardKind kind(final String path, final TextKey name, final TextKey treatment,
                                   final double heals, final int index) {
        final HazardKind kind = Hazards.declare(HazardKind.builder(
                        ResourceLocation.fromNamespaceAndPath(JsCore.MODID, path), name, PERCENT)
                .band(10, COUGH, HazardEffects.effect(MobEffects.MOVEMENT_SLOWDOWN, 0))
                .band(30, SHORT_OF_BREATH, HazardEffects.effect(MobEffects.MOVEMENT_SLOWDOWN, 0),
                        HazardEffects.effect(MobEffects.WEAKNESS, 0))
                .band(60, STRUGGLING, HazardEffects.effect(MobEffects.MOVEMENT_SLOWDOWN, 1),
                        HazardEffects.effect(MobEffects.WEAKNESS, 0), HazardEffects.effect(MobEffects.DIG_SLOWDOWN, 0))
                .band(90, FAILING, HazardEffects.effect(MobEffects.MOVEMENT_SLOWDOWN, 1),
                        HazardEffects.effect(MobEffects.WEAKNESS, 1), HazardEffects.damage(0.5F))
                .recovers(heals).protectedBy(DamageKinds.INHALED).treatedBy(treatment).deadlyAs(FAILURE)
                .enabledWhen(HazardBalance::lungs).upTo(100).build());
        kind.addSource((body, level) -> {
            final double[] breathed = BREATHED.get(body.getUUID());
            if (breathed == null || breathed[index] <= 0) {
                return 0;
            }
            final double taken = breathed[index];
            breathed[index] = 0;
            return taken;
        });
        return kind;
    }

    private static TagKey<Block> tag(final String dust) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "dust/" + dust));
    }
}
