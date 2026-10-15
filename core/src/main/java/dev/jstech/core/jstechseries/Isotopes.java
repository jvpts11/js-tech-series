/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries;

import dev.jstech.core.JsCore;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MaterialProperties;
import dev.jstech.core.material.Materials;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/**
 * The isotopes the series' mods work with, each a material of its own tied to its element by its mass number: the
 * fuels and targets of the reactor and the accelerators, the sources of radiation, deuterium, tritium and helium-3 for
 * fusion; and, for each superheavy element of the known table, the neutron-rich isotope of the island of stability,
 * near 184 neutrons, that is stable and can be used. Any mod adds the isotopes it needs with
 * {@code Materials.isotope(...)}.
 */
@TextHolder
public final class Isotopes {

    public static final TextKey DEUTERIUM_NAME = TextKey.of("jscore.material.deuterium", "Deuterium");
    public static final TextKey TRITIUM_NAME = TextKey.of("jscore.material.tritium", "Tritium");

    /** The neutrons of the island of stability. */
    public static final int ISLAND_NEUTRONS = 184;

    public static final Material DEUTERIUM = Materials.isotope(id("deuterium"), Elements.of(Element.HYDROGEN), 2,
            DEUTERIUM_NAME).with(MaterialProperties.SYMBOL, "D").declare();
    public static final Material TRITIUM = Materials.isotope(id("tritium"), Elements.of(Element.HYDROGEN), 3,
            TRITIUM_NAME).with(MaterialProperties.SYMBOL, "T").declare();
    public static final Material HELIUM_3 = isotope(Element.HELIUM, 3);
    public static final Material CALCIUM_48 = isotope(Element.CALCIUM, 48);
    public static final Material COBALT_60 = isotope(Element.COBALT, 60);
    public static final Material ZINC_70 = isotope(Element.ZINC, 70);
    public static final Material STRONTIUM_90 = isotope(Element.STRONTIUM, 90);
    public static final Material THULIUM_170 = isotope(Element.THULIUM, 170);
    public static final Material IRIDIUM_192 = isotope(Element.IRIDIUM, 192);
    public static final Material URANIUM_235 = isotope(Element.URANIUM, 235);
    public static final Material URANIUM_238 = isotope(Element.URANIUM, 238);
    public static final Material NEPTUNIUM_237 = isotope(Element.NEPTUNIUM, 237);
    public static final Material PLUTONIUM_238 = isotope(Element.PLUTONIUM, 238);
    public static final Material PLUTONIUM_239 = isotope(Element.PLUTONIUM, 239);
    public static final Material AMERICIUM_241 = isotope(Element.AMERICIUM, 241);
    public static final Material CURIUM_244 = isotope(Element.CURIUM, 244);
    public static final Material BERKELIUM_249 = isotope(Element.BERKELIUM, 249);
    public static final Material CALIFORNIUM_252 = isotope(Element.CALIFORNIUM, 252);

    private static final Map<Element, Material> STABLE = new EnumMap<>(Element.class);

    static {
        for (final Element element : Element.values()) {
            if (element.number() >= Element.RUTHERFORDIUM.number()) {
                STABLE.put(element, isotope(element, element.number() + ISLAND_NEUTRONS));
            }
        }
    }

    private Isotopes() {
    }

    /** Declares the isotopes, from the Core's constructor. */
    public static void declare() {
        // Loading the class declares them.
    }

    /**
     * The stable, neutron-rich isotope of a superheavy element of the known table, rutherfordium to oganesson: what
     * can be made once the element's first, short-lived synthesis has been studied.
     */
    public static Optional<Material> stable(final Element element) {
        return Optional.ofNullable(STABLE.get(element));
    }

    /** Every stable superheavy isotope, by element. */
    public static Map<Element, Material> stable() {
        return Collections.unmodifiableMap(STABLE);
    }

    /** Every isotope declared of {@code element}, by any mod. */
    public static List<Material> of(final Material element) {
        final List<Material> out = new ArrayList<>();
        for (final Material material : Materials.all()) {
            if (material.isotope().map(isotope -> isotope.element() == element).orElse(false)) {
                out.add(material);
            }
        }
        return out;
    }

    private static Material isotope(final Element element, final int massNumber) {
        return Materials.isotope(id(element.path() + "_" + massNumber), Elements.of(element), massNumber).declare();
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsCore.MODID, path);
    }
}
