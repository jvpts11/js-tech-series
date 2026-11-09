/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.JsCore;
import dev.jstech.core.hazard.GasLimits;
import dev.jstech.core.hazard.HazardClass;
import dev.jstech.core.hazard.MaterialHazards;
import dev.jstech.core.jstechseries.Element;
import dev.jstech.core.jstechseries.Elements;
import dev.jstech.core.jstechseries.SeriesAlloys;
import dev.jstech.core.jstechseries.SeriesChemicals;
import dev.jstech.core.jstechseries.SeriesCompounds;
import dev.jstech.core.jstechseries.SeriesMinerals;
import dev.jstech.core.jstechseries.SharedCatalogue;
import dev.jstech.core.material.Form;
import dev.jstech.core.material.Forms;
import dev.jstech.core.material.HostRocks;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MaterialKind;
import dev.jstech.core.material.MaterialProperties;
import dev.jstech.core.material.Mineral;
import dev.jstech.core.material.MineralFamilies;
import dev.jstech.core.material.OreLook;
import dev.jstech.tests.JsTests;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * What J's Core's catalogue holds for the electronics chain (core/docs/design/materials.md): the crushed and lump
 * forms, the polymers and the crystals grown as rods, fluorspar and lithium's ore, the compounds, alloys, liquids and
 * gases with their formulas and hazards, the metals each in a hue of its own and the gases in their cylinders'
 * colours, and the batteries among the components.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ElectronicsMaterialsGameTests {

    private static final String ARENA = "empty";
    /* Fluorine's share of fluorite's mass, CaF2, by the atomic weights. */
    private static final double FLUORINE_IN_FLUORSPAR = 0.4867;
    private static final double SHARE_TOLERANCE = 0.001;

    private ElectronicsMaterialsGameTests() {
    }

    @GameTest(template = ARENA)
    public static void forms_crushedLumpPolymersAndRodsFitTheirKinds(final GameTestHelper helper) {
        helper.assertTrue(Forms.CRUSHED.oreColoured() && Forms.RAW.oreColoured() && !Forms.LUMP.oreColoured()
                && !Forms.INGOT.oreColoured(), "raw and crushed ore are drawn in the ore's colour, the rest not");
        helper.assertTrue(Forms.CRUSHED.art().equals(Optional.of(ResourceLocation.fromNamespaceAndPath(JsCore.MODID,
                "item/form/crushed"))), "crushed ore is drawn from the Core's own picture");
        helper.assertTrue(fits(SeriesCompounds.PVC, Forms.PLATE, Forms.SHEET, Forms.DUST)
                && !SeriesCompounds.PVC.fits(Forms.INGOT) && !SeriesCompounds.PVC.fits(Forms.LUMP),
                "a polymer comes as plate, sheet and dust, never as an ingot or in lumps");
        helper.assertTrue(SeriesCompounds.POLYSILICON.fits(Forms.ROD) && SeriesCompounds.COKE.fits(Forms.LUMP)
                && SeriesCompounds.METALLURGICAL_SILICON.fits(Forms.LUMP), "a crystal grows as a rod, coke in lumps");
        helper.assertTrue(fits(SeriesMinerals.FLUORSPAR, Forms.ORE, Forms.RAW, Forms.CRUSHED, Forms.DUST,
                Forms.CRYSTAL), "fluorspar is mined, crushed, ground and comes as crystals");
        helper.assertTrue(Elements.of(Element.TIN).fits(Forms.CRUSHED) && Elements.of(Element.MERCURY)
                .fits(Forms.CRUSHED), "anything with an ore can be crushed, mercury's cinnabar too");
        final long ours = Forms.all().stream().filter(form -> form.id().getNamespace().equals(JsCore.MODID)).count();
        helper.assertValueEqual(ours, 23L, "the Core's forms");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void compounds_carryTheirKindFormulaAndHazards(final GameTestHelper helper) {
        helper.assertTrue(SeriesCompounds.all().size() == 20 && SeriesAlloys.all().size() == 7
                && SeriesChemicals.all().size() == 12, "twenty compounds, seven alloys, twelve liquids and gases");
        final Set<ResourceLocation> ids = new HashSet<>();
        final List<Material> all = new ArrayList<>(SeriesCompounds.all());
        all.addAll(SeriesAlloys.all());
        all.addAll(SeriesChemicals.all());
        for (final Material material : all) {
            helper.assertTrue(ids.add(material.id()) && material.colour() != 0, material.id() + " stands alone");
        }
        helper.assertTrue(formula(SeriesCompounds.CAUSTIC_SODA).equals("NaOH")
                && MaterialHazards.of(SeriesCompounds.CAUSTIC_SODA).equals(Set.of(HazardClass.CORROSIVE)),
                "caustic soda is NaOH, and corrosive");
        helper.assertTrue(SeriesCompounds.COKE.kind() == MaterialKind.NONMETAL_SOLID
                && MaterialHazards.of(SeriesCompounds.COKE).isEmpty(), "coke is carbon, and harmless");
        helper.assertTrue(SeriesCompounds.PVC.kind() == MaterialKind.POLYMER
                && formula(SeriesCompounds.PVC).equals("(C₂H₃Cl)ₙ"), "PVC is a polymer of its repeating unit");
        helper.assertTrue(SeriesCompounds.EPOXY_RESIN.value(MaterialProperties.FORMULA).isEmpty()
                && SeriesChemicals.COAL_TAR.value(MaterialProperties.FORMULA).isEmpty(),
                "a mixture has no one formula");
        helper.assertTrue(SeriesChemicals.METHANOL.kind() == MaterialKind.LIQUID
                && MaterialHazards.of(SeriesChemicals.METHANOL).equals(Set.of(HazardClass.FLAMMABLE,
                HazardClass.ACUTELY_TOXIC, HazardClass.HEALTH_HAZARD)), "methanol burns, poisons and harms");
        helper.assertTrue(SeriesChemicals.HYDROGEN_CHLORIDE.kind() == MaterialKind.GAS
                && SeriesChemicals.ACETYLENE.kind() == MaterialKind.GAS, "hydrogen chloride and acetylene are gases");
        helper.assertTrue(SeriesChemicals.HYDROGEN_CHLORIDE.value(GasLimits.IDLH).orElse(0.0) == 50.0
                && SeriesChemicals.ACETYLENE.value(GasLimits.LOWER_EXPLOSIVE).orElse(0.0) == 0.025,
                "with their real limits");
        helper.assertTrue(MaterialHazards.of(SeriesAlloys.SOLDER).contains(HazardClass.HEALTH_HAZARD)
                && MaterialHazards.of(SeriesAlloys.LEAD_FREE_SOLDER).isEmpty(), "only the leaded solder harms");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void fluorspar_isAHalideOfFluorineAndLithiumHasItsOre(final GameTestHelper helper) {
        final Mineral fluorspar = SeriesMinerals.FLUORSPAR.mineral().orElseThrow();
        helper.assertTrue(fluorspar.family() == MineralFamilies.HALIDE
                && fluorspar.main().material() == Elements.of(Element.FLUORINE)
                && Math.abs(fluorspar.main().share() - FLUORINE_IN_FLUORSPAR) < SHARE_TOLERANCE,
                "fluorspar is a halide mined for its fluorine, by its formula; got " + fluorspar.main().share());
        helper.assertTrue(fluorspar.rocks().equals(Set.of(HostRocks.STONE, HostRocks.DEEPSLATE, HostRocks.GRANITE,
                HostRocks.ANDESITE, HostRocks.TUFF)), "in five rocks of the Overworld");
        helper.assertTrue(!SeriesMinerals.all().contains(SeriesMinerals.FLUORSPAR)
                && !Elements.hasGenericOre(Element.FLUORINE), "not J's Industrial's vein, and fluorine has no ore");
        final OreLook spodumene = Elements.of(Element.LITHIUM).oreLook();
        helper.assertTrue(Elements.hasGenericOre(Element.LITHIUM) && spodumene.pattern() == OreLook.Pattern.DIAMOND
                && spodumene.speckColour(0) == 0xC8A0D8 && spodumene.sheen().orElse(0) == 0xF0E0F8,
                "lithium's ore is drawn as spodumene");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void colours_metalsTakeHuesOfTheirOwnAndGasesTheirCylinders(final GameTestHelper helper) {
        final Map<Element, Integer> metals = Map.ofEntries(Map.entry(Element.TIN, 0xD2E2DA),
                Map.entry(Element.SILVER, 0xF2F2FA), Map.entry(Element.ALUMINIUM, 0xA6CFEA),
                Map.entry(Element.ZINC, 0x8FB0B8), Map.entry(Element.LEAD, 0x66607A),
                Map.entry(Element.NICKEL, 0xC4C88E), Map.entry(Element.TANTALUM, 0x8466C8),
                Map.entry(Element.INDIUM, 0x4E4AA0), Map.entry(Element.CHROMIUM, 0x6FA858),
                Map.entry(Element.MANGANESE, 0xD68AAE), Map.entry(Element.COBALT, 0x3858D8),
                Map.entry(Element.TUNGSTEN, 0x3E4148), Map.entry(Element.MOLYBDENUM, 0x9A9EA8),
                Map.entry(Element.GALLIUM, 0x6CD4CC), Map.entry(Element.HAFNIUM, 0x8C7A64),
                Map.entry(Element.RUTHENIUM, 0xB85A5A));
        metals.forEach((element, colour) -> helper.assertValueEqual(Elements.of(element).colour(), colour,
                element + "'s colour"));
        helper.assertValueEqual(new HashSet<>(metals.values()).size(), metals.size(), "no two metals alike");
        final Map<Element, Integer> gases = Map.of(Element.HYDROGEN, 0xC8302A, Element.OXYGEN, 0xF4F4F4,
                Element.NITROGEN, 0x2A2A2A, Element.ARGON, 0x2E6B3A, Element.KRYPTON, 0x6CC46C,
                Element.CHLORINE, 0xD8C83A);
        gases.forEach((element, colour) -> helper.assertValueEqual(Elements.of(element).colour(), colour,
                element + "'s cylinder"));
        helper.assertTrue(SeriesChemicals.HYDROGEN_CHLORIDE.colour() == 0xC8902A
                && SeriesChemicals.ACETYLENE.colour() == 0x7A2430, "hydrogen chloride's and acetylene's cylinders");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void components_takeTheBatteriesWithTheirPictures(final GameTestHelper helper) {
        helper.assertTrue(SharedCatalogue.components().contains(SharedCatalogue.LEAD_ACID_BATTERY)
                && SharedCatalogue.components().contains(SharedCatalogue.LITHIUM_ION_CELL),
                "the lead-acid battery and the lithium-ion cell are components of the catalogue");
        helper.assertTrue(SharedCatalogue.LITHIUM_ION_CELL.art().equals(ResourceLocation.fromNamespaceAndPath(
                JsCore.MODID, "item/component/lithium_ion_cell")), "drawn from the Core's own picture");
        helper.succeed();
    }

    private static boolean fits(final Material material, final Form... forms) {
        for (final Form form : forms) {
            if (!material.fits(form)) {
                return false;
            }
        }
        return true;
    }

    private static String formula(final Material material) {
        return material.value(MaterialProperties.FORMULA).orElse("");
    }
}
