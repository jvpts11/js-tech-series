/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock.damage;

import dev.jstech.core.JsCore;
import dev.jstech.core.multiblock.damage.DamageStamp.Edge;
import dev.jstech.core.multiblock.damage.DamageStamp.Push;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;

/**
 * The kinds of damage a formed model can be struck with, by name: the Core's own, and any a mod declares from its
 * constructor. A mark of damage keeps its stamp's name, so a save or a player's game reads it back by this list.
 *
 * <pre>{@code
 * public static final ResourceLocation LASER = DamageStamps.declare(
 *         ResourceLocation.fromNamespaceAndPath("mymod", "laser"),
 *         new DamageStamp(0.3, 0.1, DamageStamp.Push.SAG, 0.08, false, DamageStamp.Edge.SCORCHED));
 * }</pre>
 */
public final class DamageStamps {

    private static final Map<ResourceLocation, DamageStamp> STAMPS = new ConcurrentHashMap<>();

    /** Struck from outside: a hole with its petals bent in and curled over it, bare metal torn along its rim. */
    public static final ResourceLocation IMPACT = core("impact",
            new DamageStamp(0.55, 0.35, Push.PETALS_IN, 0.55, true, Edge.TORN));
    /** Blown from inside: a wider, rougher hole with its petals bent out, soot round it. */
    public static final ResourceLocation EXPLOSION = core("explosion",
            new DamageStamp(0.7, 0.5, Push.PETALS_OUT, 0.75, false, Edge.SOOTED));
    /** A grain from space: a small hole in a shallow crater, a ring of bare metal round it. */
    public static final ResourceLocation MICROMETEOROID = core("micrometeoroid",
            new DamageStamp(0.13, 0.15, Push.CRATER, 0.06, false, Edge.SPALL));
    /** Burnt through: the rim melted and sagging in, char round it and the last of the glow along it. */
    public static final ResourceLocation BURN = core("burn",
            new DamageStamp(0.5, 0.25, Push.SAG, 0.12, false, Edge.SCORCHED));
    /** A collision: a dent with folds round it, nothing cut, a few scrapes. */
    public static final ResourceLocation COLLISION = core("collision",
            new DamageStamp(0, 0, Push.FOLDS, 0.6, false, Edge.SCUFFED));
    /** A blow that breaks nothing: a shallow dent, only seen. */
    public static final ResourceLocation DENT = core("dent",
            new DamageStamp(0, 0, Push.DENT, 0.25, false, Edge.SCUFFED));

    private DamageStamps() {
    }

    /**
     * Declares the stamp {@code stamp} by the name {@code id}, from a mod's constructor.
     *
     * @return {@code id}
     * @throws IllegalStateException when a stamp of that name is declared already
     */
    public static ResourceLocation declare(final ResourceLocation id, final DamageStamp stamp) {
        if (STAMPS.putIfAbsent(id, stamp) != null) {
            throw new IllegalStateException("a stamp of damage is declared as " + id + " already");
        }
        return id;
    }

    /** The stamp named {@code id}, if one is declared. */
    public static Optional<DamageStamp> get(final ResourceLocation id) {
        return Optional.ofNullable(STAMPS.get(id));
    }

    /** Every stamp declared, by name. */
    public static Map<ResourceLocation, DamageStamp> all() {
        return Map.copyOf(STAMPS);
    }

    private static ResourceLocation core(final String name, final DamageStamp stamp) {
        return declare(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, name), stamp);
    }
}
