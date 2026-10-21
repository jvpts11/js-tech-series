/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.variant;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Which variants are made: every kind a mod declared, for every material of its family found, but the mods, the
 * materials and the kinds the settings leave out, each once. A variant's id is the declaring mod's, named after the
 * material and the kind: {@code othermod:oak_frame} for the game's oak, {@code othermod:create_limestone_frame} for a
 * stone another mod brings, {@code othermod:tin_casing} for a metal found by its common tag.
 *
 * <p>Pure: it is handed ids as text.
 */
public final class VariantPlan {

    /** The namespaces whose materials go by their bare name: the game's own and the common tags'. */
    private static final Set<String> BARE = Set.of("minecraft", "c");
    private static final String PLANKS = "planks";

    private VariantPlan() {
    }

    /**
     * The variants to make of {@code kinds} for {@code found}, in the order the kinds were declared and the materials
     * found, leaving out what {@code skipped} says.
     */
    public static List<Planned> of(final List<Kind> kinds, final List<Found> found, final Skipped skipped) {
        final List<Planned> out = new ArrayList<>();
        if (!skipped.enabled()) {
            return out;
        }
        final Set<String> ids = new HashSet<>();
        for (final Kind kind : kinds) {
            if (skipped.kinds().contains(kind.id())) {
                continue;
            }
            for (final Found material : found) {
                if (material.family() != kind.family() || skipped.materials().contains(material.material())
                        || skipped.mods().contains(namespaceOf(material.source()))
                        || skipped.mods().contains(namespaceOf(material.material()))) {
                    continue;
                }
                final String id = idOf(kind.id(), material.material());
                if (ids.add(id)) {
                    out.add(new Planned(kind.id(), material.material(), material.source(), id));
                }
            }
        }
        return out;
    }

    /** The id of the variant of kind {@code kind} ({@code mod:frame}) made of {@code material} ({@code mod:oak}). */
    public static String idOf(final String kind, final String material) {
        final String namespace = namespaceOf(material);
        final String path = pathOf(material);
        final String prefix = BARE.contains(namespace) ? path + "_" : namespace + "_" + path + "_";
        return namespaceOf(kind) + ":" + prefix + pathOf(kind);
    }

    /** The wood a block of planks is of, by its id's path: {@code oak_planks} and {@code planks_oak} are of oak. */
    public static String woodOf(final String planks) {
        final String suffix = "_" + PLANKS;
        final String prefix = PLANKS + "_";
        if (planks.endsWith(suffix) && planks.length() > suffix.length()) {
            return planks.substring(0, planks.length() - suffix.length());
        }
        if (planks.startsWith(prefix) && planks.length() > prefix.length()) {
            return planks.substring(prefix.length());
        }
        return planks;
    }

    /** The words of an id's path read as a name, for what is written in English: {@code dark_oak} as Dark Oak. */
    public static String englishOf(final String path) {
        final StringBuilder out = new StringBuilder();
        for (final String word : path.split("[_/]")) {
            if (word.isEmpty()) {
                continue;
            }
            if (!out.isEmpty()) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return out.isEmpty() ? path : out.toString();
    }

    private static String namespaceOf(final String id) {
        final int colon = id.indexOf(':');
        return colon < 0 ? "minecraft" : id.substring(0, colon);
    }

    private static String pathOf(final String id) {
        final int colon = id.indexOf(':');
        return colon < 0 ? id : id.substring(colon + 1);
    }

    /**
     * A kind of variant a mod declared.
     *
     * @param id     its id, as {@code othermod:frame}
     * @param family the family of materials it is made for
     */
    public record Kind(String id, VariantFamily family) {

        public Kind {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(family, "family");
        }
    }

    /**
     * A material found by the common tags.
     *
     * @param family   its family
     * @param material its id, as {@code minecraft:oak}, {@code create:limestone} or {@code c:tin}
     * @param source   the id of the block its variants are made from and drawn with
     */
    public record Found(VariantFamily family, String material, String source) {

        public Found {
            Objects.requireNonNull(family, "family");
            Objects.requireNonNull(material, "material");
            Objects.requireNonNull(source, "source");
        }
    }

    /**
     * What the settings leave out.
     *
     * @param enabled   whether variants are made at all
     * @param mods      the mods, by id, whose materials have no variants
     * @param materials the materials, by id, that have none
     * @param kinds     the kinds, by id, that are not made
     */
    public record Skipped(boolean enabled, Set<String> mods, Set<String> materials, Set<String> kinds) {

        /** Nothing left out. */
        public static final Skipped NONE = new Skipped(true, Set.of(), Set.of(), Set.of());

        public Skipped {
            mods = Set.copyOf(mods);
            materials = Set.copyOf(materials);
            kinds = Set.copyOf(kinds);
        }
    }

    /**
     * A variant to make.
     *
     * @param kind     the id of its kind
     * @param material the id of its material
     * @param source   the id of the block it is made from
     * @param id       its own id
     */
    public record Planned(String kind, String material, String source, String id) {
    }
}
