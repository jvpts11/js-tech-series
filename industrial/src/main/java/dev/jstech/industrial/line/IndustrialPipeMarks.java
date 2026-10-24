/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.line;

import com.google.common.base.Suppliers;
import dev.jstech.core.fluid.PipeMarks;
import dev.jstech.core.jstechseries.Element;
import dev.jstech.core.jstechseries.Elements;
import dev.jstech.core.jstechseries.Isotopes;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MatterState;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.industrial.JsIndustrial;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;

/**
 * The marks of what J's Industrial's pipes carry, over J's Core's ({@link PipeMarks}), as its pipes' table gives them
 * (industrial/docs/design/fluids-and-gases.md). A pipe has to be made for acids (PVC, PVDF), molten metal (the
 * refractory launder), molten salt (Hastelloy), ultra-pure water (PVDF) and the corrosive gases, chlorine and fluorine
 * (the stainless gas pipe), to carry them at all; hydrogen wears through the steel not made to stand it, and a slurry
 * abrades a pipe, each pipe in its own time.
 */
@TextHolder
public final class IndustrialPipeMarks {

    public static final TextKey ACIDS_NAME = TextKey.of("tag.fluid.jsindustrial.acids", "Acids");
    public static final TextKey MOLTEN_METAL_NAME = TextKey.of("tag.fluid.jsindustrial.molten_metal", "Molten Metal");
    public static final TextKey MOLTEN_SALT_NAME = TextKey.of("tag.fluid.jsindustrial.molten_salt", "Molten Salt");
    public static final TextKey ULTRA_PURE_WATER_NAME = TextKey.of("tag.fluid.jsindustrial.ultra_pure_water",
            "Ultra-Pure Water");
    public static final TextKey CORROSIVE_GASES_NAME = TextKey.of("tag.fluid.jsindustrial.corrosive_gases",
            "Corrosive Gases");
    public static final TextKey HYDROGEN_NAME = TextKey.of("tag.fluid.jsindustrial.hydrogen", "Hydrogen");
    public static final TextKey ABRASIVE_NAME = TextKey.of("tag.fluid.jsindustrial.abrasive", "Abrasive Slurries");
    /** Acids, which only a pipe made for them stands. */
    public static final TagKey<Fluid> ACIDS = PipeMarks.madeFor(tag("acids"), material -> false);
    /** Molten metal, which only a refractory pipe takes. */
    public static final TagKey<Fluid> MOLTEN_METAL = PipeMarks.madeFor(tag("molten_metal"), material -> false);
    /** Molten salt, which only a nickel alloy stands. */
    public static final TagKey<Fluid> MOLTEN_SALT = PipeMarks.madeFor(tag("molten_salt"), material -> false);
    /** Ultra-pure water, which only a pipe that does not spoil it carries. */
    public static final TagKey<Fluid> ULTRA_PURE_WATER = PipeMarks.madeFor(tag("ultra_pure_water"),
            material -> false);
    /** The gases that eat metal, which only a stainless gas pipe stands: chlorine and fluorine. */
    public static final TagKey<Fluid> CORROSIVE_GASES = PipeMarks.madeFor(tag("corrosive_gases"),
            material -> MarkedMaterials.CORROSIVE_GASES.get().contains(material));
    /** Hydrogen and its isotopes, which embrittle plain steel. */
    public static final TagKey<Fluid> HYDROGEN = PipeMarks.wears(tag("hydrogen"),
            material -> MarkedMaterials.HYDROGENS.get().contains(material));
    /** A slurry, whose solids abrade the pipe it runs in. */
    public static final TagKey<Fluid> ABRASIVE = PipeMarks.wears(tag("abrasive"),
            material -> material.takes(MatterState.SLURRY));

    private IndustrialPipeMarks() {
    }

    /** Declares the marks, from the mod's constructor, before any pipe is. */
    public static void declare() {
        // Loading the class declares them.
    }

    private static TagKey<Fluid> tag(final String path) {
        return TagKey.create(Registries.FLUID, ResourceLocation.fromNamespaceAndPath(JsIndustrial.MODID, path));
    }

    /* The materials of the marks, found once the catalogue is there, never while the marks are declared. */
    private static final class MarkedMaterials {

        static final Supplier<Set<Material>> CORROSIVE_GASES = Suppliers.memoize(() -> Set.of(
                Elements.of(Element.CHLORINE), Elements.of(Element.FLUORINE)));
        static final Supplier<Set<Material>> HYDROGENS = Suppliers.memoize(() -> Set.of(
                Elements.of(Element.HYDROGEN), Isotopes.DEUTERIUM, Isotopes.TRITIUM));

        private MarkedMaterials() {
        }
    }
}
