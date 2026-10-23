/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.chemistry;

/**
 * The reactions of J's Industrial's chemistry, each as it runs in a real plant: its equation, the temperature and the
 * pressure it runs between, and its catalyst. A chemical machine runs any of them its limits reach; the list grows
 * with the plants.
 *
 * <p>Pure: no game types.
 */
public final class Reactions {

    /**
     * Ammonia from the air's nitrogen and hydrogen, over iron, at 400 to 500 °C and 150 to 250 bar, as Haber and
     * Bosch ran it in 1913: the first reaction of industrial chemistry.
     */
    public static final Conditions HABER_BOSCH = new Conditions(673.15, 773.15, 15_000.0, 25_000.0, "jscore:iron");
    public static final Reaction AMMONIA = Reaction.builder()
            .in("jscore:nitrogen", "N2", 1, Reaction.Phase.FLUID).in("jscore:hydrogen", "H2", 3, Reaction.Phase.FLUID)
            .out("jscore:ammonia", "NH3", 2, Reaction.Phase.FLUID).build();

    private Reactions() {
    }

    /**
     * What a reaction runs at: between two temperatures and two pressures, over its catalyst.
     *
     * @param leastKelvin the coolest it runs at
     * @param mostKelvin  the hottest
     * @param leastKpa    the lowest pressure it runs at, in kilopascals
     * @param mostKpa     the highest
     * @param catalyst    the material its catalyst is of, by its id: {@code "jscore:iron"}
     */
    public record Conditions(double leastKelvin, double mostKelvin, double leastKpa, double mostKpa,
                             String catalyst) {

        public Conditions {
            if (!(leastKelvin > 0.0 && mostKelvin >= leastKelvin && leastKpa > 0.0 && mostKpa >= leastKpa)) {
                throw new IllegalArgumentException("a reaction runs between a coolest and a hottest, a lowest and a"
                        + " highest pressure, each above none");
            }
        }
    }
}
