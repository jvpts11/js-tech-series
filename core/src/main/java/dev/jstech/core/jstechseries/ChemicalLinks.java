/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries;

import dev.jstech.core.material.Material;
import dev.jstech.core.matter.MatterBridges;
import net.minecraft.resources.ResourceLocation;

/**
 * The materials of the catalogue that a chemical mod also has, linked by the chemical's id, so its gas and the Core's
 * are one: hydrogen made by one mod's electrolyser fills another's tank. A link names an id alone and sleeps while that
 * mod is absent.
 */
public final class ChemicalLinks {

    /* The namespace of Mekanism's chemicals. */
    private static final String MEKANISM = "mekanism";

    private ChemicalLinks() {
    }

    /** Declares the links, from the Core's constructor, after the catalogue. */
    public static void declare() {
        link(Elements.of(Element.HYDROGEN), MEKANISM, "hydrogen");
        link(Elements.of(Element.OXYGEN), MEKANISM, "oxygen");
        link(Elements.of(Element.CHLORINE), MEKANISM, "chlorine");
        link(Isotopes.DEUTERIUM, MEKANISM, "deuterium");
        link(Isotopes.TRITIUM, MEKANISM, "tritium");
    }

    private static void link(final Material material, final String namespace, final String chemical) {
        MatterBridges.chemical(material, ResourceLocation.fromNamespaceAndPath(namespace, chemical));
    }
}
