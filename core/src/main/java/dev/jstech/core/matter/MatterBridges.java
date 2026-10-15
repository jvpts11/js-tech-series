/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.matter;

import dev.jstech.core.material.Material;
import dev.jstech.core.util.DeclarationError;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;

/**
 * Which material another mod's chemical or gas fluid is: hydrogen is the Core's hydrogen whether it sits in a Core
 * tank, in a chemical mod's tank or in a tank of a mod that keeps its gases as fluids, so a line carries it from one to
 * the other. A chemical or a fluid with no link still moves, under its own name, between the stores of its own mod.
 *
 * <p>A link names the chemical or the fluid by id alone, so it is declared whether or not that mod is installed: with
 * the mod absent the id names nothing and the link sleeps.
 *
 * <pre>{@code
 * MatterBridges.chemical(hydrogen, ResourceLocation.fromNamespaceAndPath("mekanism", "hydrogen"));
 * }</pre>
 */
public final class MatterBridges {

    private static final Map<ResourceLocation, Material> MATERIAL_OF_CHEMICAL = new ConcurrentHashMap<>();
    private static final Map<Material, ResourceLocation> CHEMICAL_OF_MATERIAL = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, Material> MATERIAL_OF_FLUID = new ConcurrentHashMap<>();
    private static final Map<Material, ResourceLocation> FLUID_OF_MATERIAL = new ConcurrentHashMap<>();

    private MatterBridges() {
    }

    /**
     * Declares that the chemical {@code chemical} of a chemical mod is {@code material}.
     *
     * @throws DeclarationError when either is linked already
     */
    public static void chemical(final Material material, final ResourceLocation chemical) {
        link(material, chemical, MATERIAL_OF_CHEMICAL, CHEMICAL_OF_MATERIAL, "chemical");
    }

    /**
     * Declares that the fluid {@code fluid}, a gas another mod keeps as a fluid, is {@code material}.
     *
     * @throws DeclarationError when either is linked already
     */
    public static void fluid(final Material material, final ResourceLocation fluid) {
        link(material, fluid, MATERIAL_OF_FLUID, FLUID_OF_MATERIAL, "fluid");
    }

    /** The material the chemical {@code chemical} is, or none. */
    public static Optional<Material> materialOfChemical(final ResourceLocation chemical) {
        return Optional.ofNullable(MATERIAL_OF_CHEMICAL.get(chemical));
    }

    /** The chemical {@code material} is in a chemical mod, or none. */
    public static Optional<ResourceLocation> chemicalOf(final Material material) {
        return Optional.ofNullable(CHEMICAL_OF_MATERIAL.get(material));
    }

    /** The material the fluid {@code fluid} is, or none. */
    public static Optional<Material> materialOfFluid(final ResourceLocation fluid) {
        return Optional.ofNullable(MATERIAL_OF_FLUID.get(fluid));
    }

    /** The fluid {@code material} is kept as by another mod, or none. */
    public static Optional<ResourceLocation> fluidOf(final Material material) {
        return Optional.ofNullable(FLUID_OF_MATERIAL.get(material));
    }

    private static void link(final Material material, final ResourceLocation other,
                             final Map<ResourceLocation, Material> toMaterial,
                             final Map<Material, ResourceLocation> fromMaterial, final String what) {
        Objects.requireNonNull(material, "material");
        Objects.requireNonNull(other, "other");
        if (toMaterial.putIfAbsent(other, material) != null) {
            throw DeclarationError.of(other.getNamespace(), "the " + what + " " + other,
                    "is linked to a material twice", "link it once");
        }
        if (fromMaterial.putIfAbsent(material, other) != null) {
            toMaterial.remove(other);
            throw DeclarationError.of(material.id().getNamespace(), "the material " + material.id(),
                    "is linked to a second " + what + ", " + other, "link each material to one " + what);
        }
    }
}
