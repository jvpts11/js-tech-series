/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries;

import dev.jstech.core.heat.Thermal;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * What the elements do with heat, as data: each one's density, specific heat and thermal conductivity near room
 * temperature, how well its surface radiates, and where it melts and boils at the open air's pressure; and, for the
 * liquids that are kept and boiled, what a kilogram takes to boil, what a mole weighs and how dense the liquid is. The
 * elements of every day are here; one that is not is taken as its kind is on the whole.
 */
final class ElementHeat {

    private static final Map<Element, Thermal> HEAT = new EnumMap<>(Element.class);
    /* Joules in a kilojoule, grams in a kilogram. */
    private static final double KILO = 1_000.0;

    static {
        gas(Element.HYDROGEN, 0.0899, 14_304, 0.1805, 14.01, 20.28);
        gas(Element.HELIUM, 0.1786, 5_193, 0.1513, 0.95, 4.22);
        put(Element.LITHIUM, 534, 3_582, 84.8, 0.2, 453.69, 1_615);
        put(Element.BERYLLIUM, 1_850, 1_825, 200, 0.2, 1_560, 2_742);
        put(Element.BORON, 2_340, 1_026, 27.4, 0.8, 2_349, 4_200);
        put(Element.CARBON, 2_267, 709, 140, 0.85, 3_823, 4_098);
        gas(Element.NITROGEN, 1.251, 1_040, 0.025_83, 63.15, 77.36);
        gas(Element.OXYGEN, 1.429, 918, 0.026_58, 54.36, 90.20);
        gas(Element.FLUORINE, 1.696, 824, 0.0277, 53.53, 85.03);
        gas(Element.NEON, 0.9002, 1_030, 0.0491, 24.56, 27.07);
        put(Element.SODIUM, 968, 1_228, 142, 0.1, 370.87, 1_156);
        put(Element.MAGNESIUM, 1_738, 1_023, 156, 0.1, 923, 1_363);
        put(Element.ALUMINIUM, 2_700, 897, 237, 0.1, 933.47, 2_792);
        put(Element.SILICON, 2_329, 705, 149, 0.6, 1_687, 3_538);
        put(Element.PHOSPHORUS, 1_823, 769, 0.236, 0.9, 317.3, 553.6);
        put(Element.SULFUR, 2_070, 710, 0.205, 0.9, 388.36, 717.87);
        gas(Element.CHLORINE, 3.2, 479, 0.0089, 171.6, 239.11);
        gas(Element.ARGON, 1.784, 520, 0.017_72, 83.8, 87.3);
        put(Element.POTASSIUM, 862, 757, 102.5, 0.1, 336.53, 1_032);
        put(Element.CALCIUM, 1_550, 647, 201, 0.2, 1_115, 1_757);
        put(Element.SCANDIUM, 2_985, 568, 15.8, 0.3, 1_814, 3_109);
        put(Element.TITANIUM, 4_506, 523, 21.9, 0.3, 1_941, 3_560);
        put(Element.VANADIUM, 6_110, 489, 30.7, 0.3, 2_183, 3_680);
        put(Element.CHROMIUM, 7_190, 449, 93.9, 0.3, 2_180, 2_944);
        put(Element.MANGANESE, 7_210, 479, 7.81, 0.3, 1_519, 2_334);
        put(Element.IRON, 7_874, 449, 80.4, 0.3, 1_811, 3_134);
        put(Element.COBALT, 8_900, 421, 100, 0.3, 1_768, 3_200);
        put(Element.NICKEL, 8_908, 444, 90.9, 0.2, 1_728, 3_186);
        put(Element.COPPER, 8_960, 385, 401, 0.05, 1_357.77, 2_835);
        put(Element.ZINC, 7_140, 388, 116, 0.1, 692.68, 1_180);
        put(Element.GALLIUM, 5_910, 371, 40.6, 0.2, 302.91, 2_477);
        put(Element.GERMANIUM, 5_323, 320, 60.2, 0.5, 1_211.4, 3_106);
        put(Element.SELENIUM, 4_810, 321, 0.519, 0.6, 494, 958);
        put(Element.BROMINE, 3_102.8, 474, 0.122, 0.9, 265.8, 332.0);
        gas(Element.KRYPTON, 3.749, 248, 0.009_43, 115.79, 119.93);
        put(Element.RUBIDIUM, 1_532, 363, 58.2, 0.1, 312.46, 961);
        put(Element.STRONTIUM, 2_640, 301, 35.4, 0.2, 1_050, 1_655);
        put(Element.YTTRIUM, 4_472, 298, 17.2, 0.3, 1_799, 3_609);
        put(Element.ZIRCONIUM, 6_520, 278, 22.6, 0.3, 2_128, 4_682);
        put(Element.NIOBIUM, 8_570, 265, 53.7, 0.3, 2_750, 5_017);
        put(Element.MOLYBDENUM, 10_280, 251, 138, 0.2, 2_896, 4_912);
        put(Element.RUTHENIUM, 12_450, 238, 117, 0.3, 2_607, 4_423);
        put(Element.RHODIUM, 12_410, 243, 150, 0.1, 2_237, 3_968);
        put(Element.PALLADIUM, 12_023, 246, 71.8, 0.1, 1_828.05, 3_236);
        put(Element.SILVER, 10_490, 235, 429, 0.02, 1_234.93, 2_435);
        put(Element.CADMIUM, 8_650, 232, 96.6, 0.1, 594.22, 1_040);
        put(Element.INDIUM, 7_310, 233, 81.8, 0.1, 429.75, 2_345);
        put(Element.TIN, 7_287, 228, 66.8, 0.1, 505.08, 2_875);
        put(Element.ANTIMONY, 6_697, 207, 24.4, 0.3, 903.78, 1_860);
        put(Element.TELLURIUM, 6_240, 202, 2.35, 0.5, 722.66, 1_261);
        put(Element.IODINE, 4_933, 214, 0.449, 0.9, 386.85, 457.4);
        gas(Element.XENON, 5.894, 158, 0.005_65, 161.4, 165.03);
        put(Element.CAESIUM, 1_930, 242, 35.9, 0.1, 301.59, 944);
        put(Element.BARIUM, 3_510, 204, 18.4, 0.3, 1_000, 2_170);
        put(Element.LANTHANUM, 6_162, 195, 13.4, 0.3, 1_193, 3_737);
        put(Element.CERIUM, 6_770, 192, 11.3, 0.3, 1_068, 3_716);
        put(Element.NEODYMIUM, 7_010, 190, 16.5, 0.3, 1_297, 3_347);
        put(Element.HAFNIUM, 13_310, 144, 23.0, 0.3, 2_506, 4_876);
        put(Element.TANTALUM, 16_690, 140, 57.5, 0.3, 3_290, 5_731);
        put(Element.TUNGSTEN, 19_250, 132, 173, 0.1, 3_695, 5_828);
        put(Element.RHENIUM, 21_020, 137, 48.0, 0.2, 3_459, 5_869);
        put(Element.OSMIUM, 22_590, 130, 87.6, 0.2, 3_306, 5_285);
        put(Element.IRIDIUM, 22_560, 131, 147, 0.2, 2_719, 4_701);
        put(Element.PLATINUM, 21_450, 133, 71.6, 0.1, 2_041.4, 4_098);
        put(Element.GOLD, 19_300, 129, 318, 0.02, 1_337.33, 3_129);
        put(Element.MERCURY, 13_534, 140, 8.30, 0.1, 234.32, 629.88);
        put(Element.THALLIUM, 11_850, 129, 46.1, 0.3, 577, 1_746);
        put(Element.LEAD, 11_340, 129, 35.3, 0.4, 600.61, 2_022);
        put(Element.BISMUTH, 9_780, 122, 7.97, 0.3, 544.7, 1_837);
        gas(Element.RADON, 9.73, 94, 0.003_61, 202, 211.5);
        put(Element.RADIUM, 5_500, 94, 18.6, 0.3, 973, 2_010);
        put(Element.THORIUM, 11_700, 113, 54.0, 0.3, 2_023, 5_061);
        put(Element.URANIUM, 19_100, 116, 27.5, 0.3, 1_405.3, 4_404);
        put(Element.PLUTONIUM, 19_816, 130, 6.74, 0.3, 912.5, 3_505);
        // The liquids that are kept and boiled: the gases liquefied, and the two elements liquid in the open air.
        boils(Element.HYDROGEN, 446, 2.016, 70.85);
        boils(Element.HELIUM, 20.7, 4.003, 125);
        boils(Element.NITROGEN, 199.2, 28.014, 806.1);
        boils(Element.OXYGEN, 213.1, 31.998, 1_141);
        boils(Element.FLUORINE, 166, 37.997, 1_505);
        boils(Element.NEON, 85.8, 20.18, 1_207);
        boils(Element.CHLORINE, 287.8, 70.90, 1_562.5);
        boils(Element.ARGON, 161.1, 39.948, 1_395.4);
        boils(Element.KRYPTON, 108.4, 83.798, 2_413);
        boils(Element.XENON, 96.3, 131.293, 2_942);
        boils(Element.RADON, 81.5, 222, 4_400);
        boils(Element.BROMINE, 187, 159.808, 3_102.8);
        boils(Element.MERCURY, 295, 200.59, 13_534);
    }

    private ElementHeat() {
    }

    /** What {@code element} does with heat, if it is one of the elements given here. */
    static Optional<Thermal> of(final Element element) {
        return Optional.ofNullable(HEAT.get(element));
    }

    private static void put(final Element element, final double density, final double specificHeat,
                            final double conductivity, final double emissivity, final double melting,
                            final double boiling) {
        HEAT.put(element, new Thermal(density, specificHeat, conductivity, emissivity, melting, boiling));
    }

    /*
     * How its liquid boils: the kilojoules a kilogram takes, the grams a mole of its gas's molecules weighs (two atoms
     * of nitrogen to a molecule), and the kilograms a cubic metre of the liquid weighs.
     */
    private static void boils(final Element element, final double kiloJoules, final double grams,
                              final double liquid) {
        HEAT.computeIfPresent(element, (key, heat) -> heat.boils(kiloJoules * KILO, grams / KILO, liquid));
    }

    /* A gas at the open air's pressure, which radiates nothing to speak of. */
    private static void gas(final Element element, final double density, final double specificHeat,
                            final double conductivity, final double melting, final double boiling) {
        put(element, density, specificHeat, conductivity, 0.0, melting, boiling);
    }
}
