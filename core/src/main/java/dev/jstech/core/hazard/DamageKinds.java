/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.hazard;

import dev.jstech.core.JsCore;
import dev.jstech.core.text.TextKey;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;

/**
 * The kinds of harm any mod declares, the Core's first: radiation, electrical, cryogenic, acid, heat, a blast and
 * pressure, which are damage; and what is breathed, what poisons the touch and what blinds, which hazards are
 * protected from. One language for hazards and damage alike: a hazard says the kind that protects from it, a worn
 * piece what share of each kind it stops.
 */
public final class DamageKinds {

    private static final Map<ResourceLocation, DamageKind> DECLARED = new LinkedHashMap<>();

    public static final DamageKind RADIATION = declare("radiation", HazardTexts.RADIATION);
    public static final DamageKind ELECTRICAL = declare("electrical", HazardTexts.ELECTRICAL);
    public static final DamageKind CRYOGENIC = declare("cryogenic", HazardTexts.CRYOGENIC);
    public static final DamageKind ACID = declare("acid", HazardTexts.ACID);
    public static final DamageKind HEAT = declare("heat", HazardTexts.HEAT);
    public static final DamageKind BLAST = declare("blast", HazardTexts.BLAST);
    public static final DamageKind PRESSURE = declare("pressure", HazardTexts.PRESSURE);
    /** What is breathed: dusts, smoke, a gas; what masks and respirators stop. */
    public static final DamageKind INHALED = declare("inhaled", HazardTexts.INHALED);
    /** What poisons the bare hand; what gloves stop. */
    public static final DamageKind TOXIC = declare("toxic", HazardTexts.TOXIC);
    /** What is too bright to look at; what a dark visor stops. */
    public static final DamageKind BLINDING = declare("blinding", HazardTexts.BLINDING);

    /**
     * The Core's damage of the cryogenic kind, beside the game's freezing: the burn of what is far too cold, a
     * cryogenic liquid or the metal it chills, for a mod to deal ({@link #source}).
     */
    public static final ResourceKey<DamageType> COLD_BURN = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "cold_burn"));
    /** The Core's damage of the pressure kind: what a difference of pressure does to a body, for a mod to deal. */
    public static final ResourceKey<DamageType> PRESSURE_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "pressure"));

    private DamageKinds() {
    }

    /** Damage of {@code type} in {@code level}, from nothing in particular. */
    public static DamageSource source(final Level level, final ResourceKey<DamageType> type) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(type));
    }

    /** Declares {@code kind}, from a mod's constructor; its name is said beside it, in a class of texts. */
    public static DamageKind declare(final DamageKind kind) {
        synchronized (DECLARED) {
            if (DECLARED.putIfAbsent(kind.id(), kind) != null) {
                throw new IllegalStateException("the damage kind " + kind.id() + " is declared twice");
            }
        }
        return kind;
    }

    /** The kind {@code id}, if one is declared. */
    public static Optional<DamageKind> find(final ResourceLocation id) {
        synchronized (DECLARED) {
            return Optional.ofNullable(DECLARED.get(id));
        }
    }

    /** Every kind declared, in the order they were. */
    public static Collection<DamageKind> all() {
        synchronized (DECLARED) {
            return List.copyOf(DECLARED.values());
        }
    }

    /** The kind {@code source} is of: the first kind whose damage types hold its type. */
    public static Optional<DamageKind> of(final DamageSource source) {
        for (final DamageKind kind : all()) {
            if (source.is(kind.types())) {
                return Optional.of(kind);
            }
        }
        return Optional.empty();
    }

    private static DamageKind declare(final String path, final TextKey name) {
        return declare(new DamageKind(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, path), name));
    }
}
