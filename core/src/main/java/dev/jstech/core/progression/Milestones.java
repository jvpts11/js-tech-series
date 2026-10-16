/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.progression;

import dev.jstech.core.util.DeclarationError;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/**
 * The milestones of every mod, as ordered sets: each mod declares its firsts in the order a team reaches them, while
 * the game loads, and any mod reads them. Each milestone is declared as knowledge too ({@link Knowledges}). Milestones
 * sit beside the axes, never mixed with them.
 *
 * <pre>{@code
 * public static final List<Milestone> FIRSTS = Milestones.declare(
 *         ResourceLocation.fromNamespaceAndPath("jsspace", "firsts"), List.of(
 *                 new Milestone(ResourceLocation.fromNamespaceAndPath("jsspace", "suborbital"), MyTexts.SUBORBITAL),
 *                 new Milestone(ResourceLocation.fromNamespaceAndPath("jsspace", "orbit"), MyTexts.ORBIT)));
 * }</pre>
 */
public final class Milestones {

    private static final Map<ResourceLocation, List<Milestone>> SETS = new LinkedHashMap<>();
    private static final Map<ResourceLocation, Milestone> BY_ID = new LinkedHashMap<>();

    private Milestones() {
    }

    /**
     * Declares the set {@code set}, its milestones in the order a team reaches them, while the game loads.
     *
     * @return the milestones, to keep in a constant
     */
    public static synchronized List<Milestone> declare(final ResourceLocation set, final List<Milestone> milestones) {
        Objects.requireNonNull(set, "set");
        if (milestones.isEmpty() || SETS.containsKey(set)) {
            throw DeclarationError.of(set.getNamespace(), "the milestones " + set,
                    milestones.isEmpty() ? "are none" : "are declared twice",
                    "declare each set once, with its milestones in the order a team reaches them");
        }
        final List<Milestone> kept = List.copyOf(milestones);
        for (final Milestone milestone : kept) {
            if (BY_ID.putIfAbsent(milestone.id(), milestone) != null) {
                throw DeclarationError.of(set.getNamespace(), "the milestone " + milestone.id(),
                        "is declared twice", "give one of the two another id");
            }
            Knowledges.declare(milestone.knowledge());
        }
        SETS.put(set, kept);
        return kept;
    }

    /** The milestones of {@code set}, in their order; none for a set not declared. */
    public static synchronized List<Milestone> of(final ResourceLocation set) {
        return SETS.getOrDefault(set, List.of());
    }

    /** The milestone of {@code id}, if one is declared. */
    public static synchronized Optional<Milestone> byId(final ResourceLocation id) {
        return Optional.ofNullable(BY_ID.get(id));
    }

    /** The ids of every set, in the order the mods declared them. */
    public static synchronized List<ResourceLocation> sets() {
        return List.copyOf(SETS.keySet());
    }
}
