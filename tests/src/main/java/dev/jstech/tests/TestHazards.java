/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests;

import dev.jstech.core.hazard.DamageKinds;
import dev.jstech.core.hazard.HazardEffects;
import dev.jstech.core.hazard.HazardKind;
import dev.jstech.core.hazard.Hazards;
import dev.jstech.core.hazard.Withering;
import dev.jstech.core.jstechseries.hazard.Dissonance;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * A hazard of the test mod's own, declared the way another mod declares one: it builds up from what a test exposes a
 * body to, a band at 5 that weakens and one at 20 that hurts, a unit a second recovered and twice that under
 * regeneration, heat protection taking its share, creatures suffering it, and a map that steps up at 1 and 10. With
 * it, a contamination of the land at the places a test names.
 */
@TextHolder
public final class TestHazards {

    public static final TextKey NAME = TextKey.of("jstests.hazard.test", "Test hazard");
    public static final TextKey SHORT = TextKey.of("jstests.hazard.test.short", "TST");
    public static final TextKey UNIT = TextKey.of("jstests.hazard.test.unit", "u");
    public static final TextKey ITCHY = TextKey.of("jstests.hazard.test.itchy", "Itchy");
    public static final TextKey SORE = TextKey.of("jstests.hazard.test.sore", "Sore");
    /* What each body is exposed to, a second, and the places the land is contaminated at. */
    private static final Map<UUID, Double> EXPOSED = new ConcurrentHashMap<>();
    private static final Set<BlockPos> CONTAMINATED = ConcurrentHashMap.newKeySet();
    /* The bodies a test stood in a bubble of normal laws. */
    private static final Set<UUID> SHIELDED = ConcurrentHashMap.newKeySet();

    public static final HazardKind KIND = Hazards.declare(HazardKind.builder(
                    ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "test_hazard"), NAME, UNIT)
            .band(5, ITCHY, HazardEffects.effect(MobEffects.WEAKNESS, 0))
            .band(20, SORE, HazardEffects.damage(1))
            .recovers(1).protectedBy(DamageKinds.HEAT).affectsMobs().shortName(SHORT).mapped(1, 10).build());

    static {
        KIND.addSource((body, level) -> EXPOSED.getOrDefault(body.getUUID(), 0.0));
        // Regeneration stands for a medicine: twice as fast while it lasts.
        KIND.addRecovery((body, level) -> body.hasEffect(MobEffects.REGENERATION) ? 1 : 0);
        // Twice what plants bear wherever a test contaminated, nothing elsewhere.
        Withering.declare((level, pos) -> CONTAMINATED.contains(pos) ? 2 : 0);
        Dissonance.shield(body -> SHIELDED.contains(body.getUUID()));
    }

    private TestHazards() {
    }

    /** Declares the hazard and the contamination, from the test mod's constructor. */
    public static void declare() {
        // Loading the class declares them.
    }

    /** Exposes {@code body} to {@code perSecond} of the hazard; 0 takes it away. */
    public static void expose(final LivingEntity body, final double perSecond) {
        if (perSecond <= 0) {
            EXPOSED.remove(body.getUUID());
        } else {
            EXPOSED.put(body.getUUID(), perSecond);
        }
    }

    /** Stands {@code body} in a bubble of normal laws, or takes it out. */
    public static void shield(final LivingEntity body, final boolean shielded) {
        if (shielded) {
            SHIELDED.add(body.getUUID());
        } else {
            SHIELDED.remove(body.getUUID());
        }
    }

    /** Contaminates the land at {@code pos}, or cleans it. */
    public static void contaminate(final BlockPos pos, final boolean contaminated) {
        if (contaminated) {
            CONTAMINATED.add(pos.immutable());
        } else {
            CONTAMINATED.remove(pos);
        }
    }
}
