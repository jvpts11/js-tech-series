/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.material;

import dev.jstech.core.heat.Thermal;
import dev.jstech.core.jstechseries.SeriesMaterials;
import dev.jstech.core.jstechseries.SeriesMinerals;
import dev.jstech.core.material.Form;
import dev.jstech.core.material.Forms;
import dev.jstech.core.material.HostRock;
import dev.jstech.core.material.HostRocks;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MaterialKind;
import dev.jstech.core.material.MaterialUses;
import dev.jstech.core.material.Mineral;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.industrial.JsIndustrial;
import dev.jstech.industrial.metallurgy.FurnaceReach;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/**
 * What J's Industrial uses of the series' shared catalogue, which the Core registers because it does: the five metals
 * of its first rung (iron, copper, gold, tin and bronze) in the forms its machines and its hand tools make, and their
 * scraps; the "X ore" of each of the four mined; the minerals of its first veins, an ore of each in every rock its
 * veins cross, and the dust of every byproduct they give, with an ingot for the byproducts a furnace melts. The rest of
 * the catalogue joins as its machines do.
 */
@TextHolder
public final class IndustrialMaterials {

    public static final TextKey SCRAP_NAME = TextKey.of("jsindustrial.form.scrap", "%s Scrap");
    /**
     * The scraps of a metal rolling and drawing leave, which an ordinary furnace melts back with a loss and an electric
     * arc furnace whole: an alloy's scraps are the alloy's.
     */
    public static final Form SCRAP = Forms.form(rl("scrap"), Form.Shape.ITEM, "scraps", SCRAP_NAME)
            .fits(MaterialKind.METAL).art(rl("item/scrap")).declare();
    /** The metals of the first rung. */
    public static final List<Material> METALS = List.of(SeriesMaterials.IRON, SeriesMaterials.COPPER,
            SeriesMaterials.GOLD, SeriesMaterials.TIN, SeriesMaterials.BRONZE);
    /** The forms the mod makes of each metal of its first rung: those its machines and its hand tools make. */
    public static final List<Form> FORMS = List.of(Forms.INGOT, Forms.NUGGET, Forms.BLOCK, Forms.DUST, Forms.PLATE,
            Forms.WIRE, SCRAP);
    /** The metals mined as themselves, each with its "X ore": the game's own for three of them. */
    public static final List<Material> MINED = List.of(SeriesMaterials.IRON, SeriesMaterials.COPPER,
            SeriesMaterials.GOLD, SeriesMaterials.TIN);

    static {
        for (final Material metal : METALS) {
            for (final Form form : FORMS) {
                MaterialUses.use(JsIndustrial.MODID, metal, form);
            }
        }
        for (final Material metal : MINED) {
            MaterialUses.ore(JsIndustrial.MODID, metal, metal == SeriesMaterials.GOLD
                    ? new HostRock[] {HostRocks.STONE, HostRocks.DEEPSLATE, HostRocks.NETHERRACK}
                    : new HostRock[] {HostRocks.STONE, HostRocks.DEEPSLATE});
        }
        for (final Material mineral : SeriesMinerals.all()) {
            final Mineral of = mineral.mineral().orElseThrow();
            MaterialUses.ore(JsIndustrial.MODID, mineral, of.rocks().toArray(HostRock[]::new));
        }
        for (final Material element : mineralElements()) {
            if (element.fits(Forms.DUST)) {
                MaterialUses.use(JsIndustrial.MODID, element, Forms.DUST);
            }
            if (meltsInAFurnace(element)) {
                MaterialUses.use(JsIndustrial.MODID, element, Forms.INGOT);
            }
        }
    }

    private IndustrialMaterials() {
    }

    /** Declares the uses, from the mod's constructor. */
    public static void declare() {
        // Loading the class declares them.
    }

    /** Every element the mod's minerals give, as their main element or a byproduct, in the order first met. */
    public static Set<Material> mineralElements() {
        final Set<Material> out = new LinkedHashSet<>();
        for (final Material mineral : SeriesMinerals.all()) {
            mineral.mineral().ifPresent(of -> of.contents().forEach(content -> out.add(content.material())));
        }
        return out;
    }

    /** Whether a furnace melts {@code material}'s dust into its ingot: a metal that melts no hotter than iron. */
    public static boolean meltsInAFurnace(final Material material) {
        return material.kind() == MaterialKind.METAL && material.value(Thermal.MELTING)
                .map(FurnaceReach::meltsDust).orElse(false);
    }

    private static ResourceLocation rl(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsIndustrial.MODID, path);
    }
}
