/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.hazard.GasHazard;
import dev.jstech.core.hazard.GhsPictogram;
import dev.jstech.core.hazard.HazardClass;
import dev.jstech.core.hazard.MaterialHazards;
import dev.jstech.core.jstechseries.Element;
import dev.jstech.core.jstechseries.Elements;
import dev.jstech.core.jstechseries.SeriesMinerals;
import dev.jstech.core.jstechseries.SharedCatalogue;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MaterialProperties;
import dev.jstech.core.material.MaterialValues;
import dev.jstech.core.material.Mineral;
import dev.jstech.core.material.MineralFamilies;
import dev.jstech.core.room.GasMath;
import dev.jstech.tests.JsTests;
import java.util.List;
import java.util.Set;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The hazards of the catalogue's materials as real data: the classes of a real safety data sheet, the GHS pictograms
 * they call for, the gas cylinder only for a gas held under pressure, what a cloud of a gas does, and the tooltip's
 * line of them; and the catalogue's minerals, made of what their formulas say, their traces counted as the series
 * counts them.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class MaterialHazardsGameTests {

    private static final String ARENA = "empty";
    private static final double CLOSE = 0.002;

    private MaterialHazardsGameTests() {
    }

    @GameTest(template = ARENA)
    public static void chlorine_showsTheLabelOfARealCylinder(final GameTestHelper helper) {
        final Material chlorine = Elements.of(Element.CHLORINE);
        helper.assertTrue(MaterialHazards.pictograms(chlorine).equals(List.of(GhsPictogram.FLAME_OVER_CIRCLE,
                GhsPictogram.SKULL, GhsPictogram.ENVIRONMENT)), "oxidising, toxic, hazardous to the environment: "
                + MaterialHazards.pictograms(chlorine));
        helper.assertTrue(MaterialHazards.pictograms(chlorine, 2 * GasMath.ATMOSPHERE)
                .contains(GhsPictogram.GAS_CYLINDER), "and the gas cylinder in a cylinder");
        helper.assertTrue(!MaterialHazards.pictograms(Elements.of(Element.IRON), 2 * GasMath.ATMOSPHERE)
                .contains(GhsPictogram.GAS_CYLINDER), "never for what is no gas");
        helper.assertTrue(MaterialHazards.gasHazards(chlorine).equals(Set.of(GasHazard.OXIDISING, GasHazard.TOXIC)),
                "a cloud of it poisons and feeds a fire: " + MaterialHazards.gasHazards(chlorine));
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void gases_carryTheHazardsOfTheirClouds(final GameTestHelper helper) {
        helper.assertTrue(MaterialHazards.gasHazards(Elements.of(Element.HYDROGEN)).equals(Set.of(
                GasHazard.FLAMMABLE)), "hydrogen burns");
        helper.assertTrue(MaterialHazards.gasHazards(Elements.of(Element.NITROGEN)).equals(Set.of(
                GasHazard.ASPHYXIANT)), "nitrogen suffocates in a closed room");
        helper.assertTrue(MaterialHazards.gasHazards(SharedCatalogue.CARBON_DIOXIDE).equals(Set.of(
                GasHazard.ASPHYXIANT)), "and carbon dioxide");
        helper.assertTrue(MaterialHazards.pictograms(SharedCatalogue.AMMONIA).equals(List.of(GhsPictogram.CORROSION,
                GhsPictogram.SKULL, GhsPictogram.ENVIRONMENT)), "ammonia's label shows no flame");
        helper.assertTrue(MaterialHazards.of(Elements.of(Element.RADON)).contains(HazardClass.RADIOACTIVE)
                && MaterialHazards.pictograms(Elements.of(Element.RADON)).isEmpty(),
                "radon is radioactive, which is no GHS pictogram");
        helper.assertTrue(MaterialHazards.of(Elements.of(Element.GOLD)).isEmpty(), "gold is harmless");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void tooltip_listsThePictogramsAndTheHazardsBeyondThem(final GameTestHelper helper) {
        final String line = MaterialHazards.HAZARDS.read(MaterialValues.value(Elements.of(Element.CHLORINE),
                MaterialHazards.HAZARDS, false).orElseThrow()).english();
        helper.assertTrue(line.equals("Oxidising, Toxic, Hazardous to the environment"), "chlorine's line: " + line);
        final String radon = MaterialHazards.HAZARDS.read(MaterialValues.value(Elements.of(Element.RADON),
                MaterialHazards.HAZARDS, false).orElseThrow()).english();
        helper.assertTrue(radon.equals("Asphyxiant, Radioactive"), "radon's line: " + radon);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void minerals_areMadeOfWhatTheirFormulasSay(final GameTestHelper helper) {
        final Mineral chalcopyrite = SeriesMinerals.CHALCOPYRITE.mineral().orElseThrow();
        // Its traces of gold, silver, selenium and tellurium take 0.17 of it, counted a thousand times over.
        helper.assertTrue(chalcopyrite.family() == MineralFamilies.SULFIDE
                && chalcopyrite.main().material() == Elements.of(Element.COPPER)
                && Math.abs(chalcopyrite.main().share() - 0.3463 * 0.83) < CLOSE
                && Math.abs(chalcopyrite.byproducts().getFirst().share() - 0.3494 * 0.83) < CLOSE,
                "chalcopyrite is a third copper and a third sulfur: " + chalcopyrite.contents());
        helper.assertTrue(chalcopyrite.byproducts().size() == 5
                && chalcopyrite.contains(Elements.of(Element.GOLD)), "with its traces");
        helper.assertTrue(SeriesMinerals.CHALCOPYRITE.value(MaterialProperties.FORMULA).orElseThrow().equals("CuFeS2")
                && MaterialProperties.FORMULA.read("CuFeS2").english().equals("CuFeS₂"),
                "and its formula, read with its counts small");
        final Mineral electrum = SeriesMinerals.ELECTRUM.mineral().orElseThrow();
        helper.assertTrue(electrum.main().share() == 0.75 && electrum.byproducts().getFirst().share() == 0.25,
                "a natural alloy says its parts");
        helper.assertTrue(Math.abs(SeriesMinerals.HEMATITE.mineral().orElseThrow().main().share() - 0.6994) < CLOSE,
                "hematite is seven tenths iron");
        helper.assertTrue(Elements.of(Element.IRON).value(MaterialProperties.ATOMIC_WEIGHT).orElseThrow() == 55.845,
                "every element carries its atomic weight");
        helper.succeed();
    }
}
