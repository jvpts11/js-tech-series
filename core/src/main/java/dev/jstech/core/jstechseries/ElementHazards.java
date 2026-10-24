/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries;

import dev.jstech.core.hazard.HazardClass;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The hazard classes of the elements, as real safety data sheets give them for each element in the form it is
 * handled in: hydrogen a flammable gas, chlorine an oxidising and toxic one, arsenic toxic, the noble gases asphyxiant,
 * sodium burning in water. Every element with no stable isotope, technetium, promethium and those from polonium on, is
 * radioactive besides. A data pack changes any of them as it does any material's values.
 *
 * <p>Pure: no game types.
 */
final class ElementHazards {

    private static final Map<Element, Set<HazardClass>> CLASSES = new EnumMap<>(Element.class);
    /*
     * The gaseous elements' limits in the air, as the NIOSH pocket guide gives them: hydrogen burns from 4% to 75% of
     * the air; fluorine is immediately dangerous from 25 ppm, chlorine from 10.
     */
    private static final Map<Element, GasData> GASES = new EnumMap<>(Map.of(
            Element.HYDROGEN, new GasData(0.04, 0.75, Double.NaN),
            Element.FLUORINE, new GasData(Double.NaN, Double.NaN, 25),
            Element.CHLORINE, new GasData(Double.NaN, Double.NaN, 10)));
    /* The first element from which none has a stable isotope: polonium. */
    private static final int FIRST_UNSTABLE = 84;

    static {
        put(Element.HYDROGEN, HazardClass.FLAMMABLE);
        put(Element.HELIUM, HazardClass.ASPHYXIANT);
        put(Element.LITHIUM, HazardClass.FLAMMABLE, HazardClass.CORROSIVE);
        put(Element.BERYLLIUM, HazardClass.ACUTELY_TOXIC, HazardClass.HEALTH_HAZARD, HazardClass.HARMFUL);
        put(Element.NITROGEN, HazardClass.ASPHYXIANT);
        put(Element.OXYGEN, HazardClass.OXIDISING);
        put(Element.FLUORINE, HazardClass.OXIDISING, HazardClass.ACUTELY_TOXIC, HazardClass.CORROSIVE);
        put(Element.NEON, HazardClass.ASPHYXIANT);
        put(Element.SODIUM, HazardClass.FLAMMABLE, HazardClass.CORROSIVE);
        put(Element.MAGNESIUM, HazardClass.FLAMMABLE);
        put(Element.ALUMINIUM, HazardClass.FLAMMABLE);
        put(Element.PHOSPHORUS, HazardClass.FLAMMABLE, HazardClass.ACUTELY_TOXIC, HazardClass.CORROSIVE,
                HazardClass.ENVIRONMENTAL);
        put(Element.SULFUR, HazardClass.HARMFUL);
        put(Element.CHLORINE, HazardClass.OXIDISING, HazardClass.ACUTELY_TOXIC, HazardClass.HARMFUL,
                HazardClass.ENVIRONMENTAL);
        put(Element.ARGON, HazardClass.ASPHYXIANT);
        put(Element.POTASSIUM, HazardClass.FLAMMABLE, HazardClass.CORROSIVE);
        put(Element.CALCIUM, HazardClass.FLAMMABLE);
        put(Element.TITANIUM, HazardClass.FLAMMABLE);
        put(Element.COBALT, HazardClass.HEALTH_HAZARD, HazardClass.HARMFUL);
        put(Element.NICKEL, HazardClass.HEALTH_HAZARD, HazardClass.HARMFUL);
        put(Element.ZINC, HazardClass.FLAMMABLE, HazardClass.ENVIRONMENTAL);
        put(Element.GALLIUM, HazardClass.CORROSIVE);
        put(Element.ARSENIC, HazardClass.ACUTELY_TOXIC, HazardClass.ENVIRONMENTAL);
        put(Element.SELENIUM, HazardClass.ACUTELY_TOXIC, HazardClass.HEALTH_HAZARD);
        put(Element.BROMINE, HazardClass.ACUTELY_TOXIC, HazardClass.CORROSIVE, HazardClass.ENVIRONMENTAL);
        put(Element.KRYPTON, HazardClass.ASPHYXIANT);
        put(Element.RUBIDIUM, HazardClass.FLAMMABLE, HazardClass.CORROSIVE);
        put(Element.ZIRCONIUM, HazardClass.FLAMMABLE);
        put(Element.CADMIUM, HazardClass.ACUTELY_TOXIC, HazardClass.HEALTH_HAZARD, HazardClass.ENVIRONMENTAL);
        put(Element.ANTIMONY, HazardClass.HARMFUL);
        put(Element.IODINE, HazardClass.HARMFUL, HazardClass.HEALTH_HAZARD, HazardClass.ENVIRONMENTAL);
        put(Element.XENON, HazardClass.ASPHYXIANT);
        put(Element.CAESIUM, HazardClass.FLAMMABLE, HazardClass.CORROSIVE);
        put(Element.BARIUM, HazardClass.FLAMMABLE);
        put(Element.HAFNIUM, HazardClass.FLAMMABLE);
        put(Element.MERCURY, HazardClass.ACUTELY_TOXIC, HazardClass.HEALTH_HAZARD, HazardClass.ENVIRONMENTAL);
        put(Element.THALLIUM, HazardClass.ACUTELY_TOXIC, HazardClass.HEALTH_HAZARD, HazardClass.ENVIRONMENTAL);
        put(Element.LEAD, HazardClass.HEALTH_HAZARD, HazardClass.ENVIRONMENTAL);
        put(Element.RADON, HazardClass.ASPHYXIANT);
        put(Element.URANIUM, HazardClass.ACUTELY_TOXIC, HazardClass.HEALTH_HAZARD, HazardClass.ENVIRONMENTAL);
        for (final Element element : Element.values()) {
            if (element == Element.TECHNETIUM || element == Element.PROMETHIUM
                    || element.number() >= FIRST_UNSTABLE) {
                CLASSES.computeIfAbsent(element, unused -> EnumSet.noneOf(HazardClass.class))
                        .add(HazardClass.RADIOACTIVE);
            }
        }
    }

    private ElementHazards() {
    }

    /** The hazard classes of {@code element}, in the order the classes are declared; none for a harmless one. */
    static List<HazardClass> of(final Element element) {
        final Set<HazardClass> classes = CLASSES.get(element);
        return classes == null ? List.of() : List.copyOf(classes);
    }

    /** What a cloud of {@code element}'s gas does at what share of the air, as safety data gives it; none unknown. */
    static Optional<GasData> gas(final Element element) {
        return Optional.ofNullable(GASES.get(element));
    }

    private static void put(final Element element, final HazardClass... classes) {
        CLASSES.computeIfAbsent(element, unused -> EnumSet.noneOf(HazardClass.class)).addAll(List.of(classes));
    }

    /**
     * The limits of a gas in the air, each unknown as NaN.
     *
     * @param lower its lower explosive limit, a share of the air
     * @param upper its upper explosive limit
     * @param idlh  the parts per million of it immediately dangerous to life and health
     */
    record GasData(double lower, double upper, double idlh) {
    }
}
