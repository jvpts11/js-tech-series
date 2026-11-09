/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.chain;

import dev.jstech.computers.JsComputers;
import dev.jstech.core.jstechseries.Element;
import dev.jstech.core.jstechseries.Elements;
import dev.jstech.core.jstechseries.SeriesAlloys;
import dev.jstech.core.jstechseries.SeriesChemicals;
import dev.jstech.core.jstechseries.SeriesCompounds;
import dev.jstech.core.jstechseries.SeriesMinerals;
import dev.jstech.core.material.Form;
import dev.jstech.core.material.Forms;
import dev.jstech.core.material.HostRock;
import dev.jstech.core.material.HostRocks;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MaterialLiquids;
import dev.jstech.core.material.MaterialUses;
import java.util.List;

/**
 * What J's Computers' own chain uses of J's Core's catalogue (computers/docs/design/survival.md), which the Core
 * registers because it does: each element's ore in the rocks of its era's veins and the forms the chain asks for, the
 * chain's compounds and alloys in the forms their icons show, fluorspar for its fluorine, and the chain's liquids. No
 * other form of these exists unless another mod asks for it. Declared in the order the chain meets them, so the
 * registries and the creative tab list them the same every time.
 */
public final class ChainMaterials {

    /* The ores of the first two eras, near the surface and deep, in the stone and the deepslate. */
    private static final HostRock[] SHALLOW = {HostRocks.STONE, HostRocks.DEEPSLATE};
    /* The ores of the later eras, only in the deepslate. */
    private static final HostRock[] DEEP = {HostRocks.DEEPSLATE};

    /** The elements the chain mines as their own ore; iron, copper and gold keep the game's. */
    public static final List<Element> MINED = List.of(Element.TIN, Element.LEAD, Element.ZINC, Element.NICKEL,
            Element.ALUMINIUM, Element.TANTALUM, Element.CHROMIUM, Element.MANGANESE, Element.MERCURY, Element.COBALT,
            Element.SILVER, Element.LITHIUM, Element.ZIRCONIUM, Element.TUNGSTEN, Element.MOLYBDENUM);
    /** The chain's liquids, at room temperature: its acids, solvents and mixtures, and mercury. */
    public static final List<Material> LIQUIDS = List.of(SeriesChemicals.COAL_TAR, SeriesChemicals.PHENOL,
            SeriesChemicals.METHANOL, SeriesChemicals.HYDROFLUORIC_ACID, SeriesChemicals.SULFURIC_ACID,
            SeriesChemicals.FERRIC_CHLORIDE, SeriesChemicals.PHOTORESIST, SeriesChemicals.ULTRAPURE_WATER,
            SeriesChemicals.LIQUID_CRYSTAL, SeriesChemicals.BATTERY_ELECTROLYTE, Elements.of(Element.MERCURY));
    /**
     * The rocks fluorspar's ore lies in, its mineral's own, in the order the rocks were declared: the mineral keeps
     * them as a set, whose order would change from one start to the next and the ore blocks' with it.
     */
    public static final List<HostRock> FLUORSPAR_ROCKS = HostRocks.all().stream()
            .filter(SeriesMinerals.FLUORSPAR.mineral().orElseThrow().rocks()::contains).toList();

    static {
        for (final Element element : MINED) {
            final boolean deep = element == Element.ZIRCONIUM || element == Element.TUNGSTEN
                    || element == Element.MOLYBDENUM;
            MaterialUses.ore(JsComputers.MODID, Elements.of(element), deep ? DEEP : SHALLOW);
        }
        MaterialUses.ore(JsComputers.MODID, SeriesMinerals.FLUORSPAR, FLUORSPAR_ROCKS.toArray(HostRock[]::new));
        uses(SeriesMinerals.FLUORSPAR, Forms.DUST, Forms.CRYSTAL);
        uses(Element.IRON, Forms.CRUSHED, Forms.INGOT, Forms.NUGGET, Forms.DUST);
        uses(Element.COPPER, Forms.CRUSHED, Forms.INGOT, Forms.NUGGET, Forms.PLATE, Forms.SHEET, Forms.FOIL,
                Forms.WIRE, Forms.ROD);
        uses(Element.GOLD, Forms.CRUSHED, Forms.INGOT, Forms.NUGGET);
        uses(Element.TIN, Forms.CRUSHED, Forms.INGOT, Forms.NUGGET);
        uses(Element.LEAD, Forms.CRUSHED, Forms.INGOT);
        uses(Element.ZINC, Forms.CRUSHED, Forms.INGOT, Forms.DUST);
        uses(Element.NICKEL, Forms.CRUSHED, Forms.INGOT, Forms.FOIL);
        uses(Element.ALUMINIUM, Forms.CRUSHED, Forms.INGOT, Forms.NUGGET, Forms.PLATE, Forms.SHEET, Forms.FOIL);
        uses(Element.TANTALUM, Forms.CRUSHED, Forms.INGOT, Forms.DUST);
        uses(Element.MANGANESE, Forms.CRUSHED, Forms.INGOT, Forms.DUST);
        uses(Element.CHROMIUM, Forms.CRUSHED, Forms.INGOT, Forms.NUGGET);
        uses(Element.MERCURY, Forms.CRUSHED);
        uses(Element.COBALT, Forms.CRUSHED, Forms.INGOT, Forms.NUGGET, Forms.DUST);
        uses(Element.SILVER, Forms.CRUSHED, Forms.INGOT, Forms.DUST, Forms.FOIL);
        uses(Element.LITHIUM, Forms.CRUSHED);
        uses(Element.ZIRCONIUM, Forms.CRUSHED);
        uses(Element.TUNGSTEN, Forms.CRUSHED, Forms.INGOT, Forms.NUGGET);
        uses(Element.MOLYBDENUM, Forms.CRUSHED, Forms.INGOT, Forms.PLATE);
        uses(Element.GALLIUM, Forms.DUST, Forms.INGOT);
        uses(Element.INDIUM, Forms.DUST, Forms.INGOT);
        uses(Element.HAFNIUM, Forms.DUST, Forms.INGOT, Forms.NUGGET);
        uses(Element.RUTHENIUM, Forms.DUST, Forms.INGOT, Forms.NUGGET);
        uses(Element.GERMANIUM, Forms.DUST);
        uses(Element.ARSENIC, Forms.DUST);
        uses(Element.SULFUR, Forms.DUST);
        uses(Element.BORON, Forms.DUST);
        uses(Element.PHOSPHORUS, Forms.DUST);
        uses(Element.SILICON, Forms.BOULE, Forms.WAFER);
        uses(SeriesCompounds.ALUMINA, Forms.DUST);
        uses(SeriesCompounds.CAUSTIC_SODA, Forms.DUST);
        uses(SeriesCompounds.SALT, Forms.DUST);
        uses(SeriesCompounds.ZINC_SULFIDE, Forms.DUST);
        uses(SeriesCompounds.LITHIUM_HYDROXIDE, Forms.DUST);
        uses(SeriesCompounds.LITHIUM_COBALT_OXIDE, Forms.DUST);
        uses(SeriesCompounds.CALCIUM_CARBIDE, Forms.LUMP);
        uses(SeriesCompounds.COKE, Forms.LUMP);
        uses(SeriesCompounds.METALLURGICAL_SILICON, Forms.LUMP);
        uses(SeriesCompounds.POLYSILICON, Forms.ROD);
        uses(SeriesCompounds.CULTURED_QUARTZ, Forms.CRYSTAL);
        uses(SeriesCompounds.GALLIUM_ARSENIDE, Forms.BOULE, Forms.WAFER);
        uses(SeriesCompounds.ALGAINP, Forms.BOULE, Forms.WAFER);
        uses(SeriesCompounds.GALLIUM_NITRIDE, Forms.BOULE, Forms.WAFER);
        uses(SeriesCompounds.CALCIUM_FLUORIDE, Forms.BOULE, Forms.LENS);
        uses(SeriesCompounds.WOOD_PULP, Forms.DUST);
        // The resins as the thick boards their icons show, the plastics as the thin sheets the recipes ask for.
        uses(SeriesCompounds.PHENOLIC_RESIN, Forms.PLATE);
        uses(SeriesCompounds.EPOXY_RESIN, Forms.PLATE);
        uses(SeriesCompounds.POLYCARBONATE, Forms.SHEET);
        uses(SeriesCompounds.PVC, Forms.SHEET);
        uses(SeriesAlloys.STEEL, Forms.INGOT, Forms.PLATE, Forms.SHEET, Forms.ROD);
        uses(SeriesAlloys.STAINLESS_STEEL, Forms.INGOT, Forms.PLATE);
        uses(SeriesAlloys.SOLDER, Forms.WIRE);
        uses(SeriesAlloys.LEAD_FREE_SOLDER, Forms.WIRE, Forms.NUGGET);
        uses(SeriesAlloys.PERMALLOY, Forms.INGOT, Forms.FOIL);
        uses(SeriesAlloys.INDIUM_TIN_OXIDE, Forms.INGOT, Forms.FOIL);
        uses(SeriesAlloys.REFINED_COPPER, Forms.INGOT, Forms.NUGGET);
        for (final Material liquid : LIQUIDS) {
            MaterialLiquids.use(JsComputers.MODID, liquid, MaterialLiquids.ROOM);
        }
    }

    private ChainMaterials() {
    }

    /** Declares the uses, from the mod's constructor. */
    public static void declare() {
        // Loading the class declares them.
    }

    private static void uses(final Element element, final Form... forms) {
        uses(Elements.of(element), forms);
    }

    private static void uses(final Material material, final Form... forms) {
        for (final Form form : forms) {
            MaterialUses.use(JsComputers.MODID, material, form);
        }
    }
}
