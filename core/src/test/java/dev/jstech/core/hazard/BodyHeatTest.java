/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.hazard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BodyHeatTest {

    private static final double SEA_LEVEL = 101.325;
    private static final double STANDING = 100;
    private static final double WALKING = 200;
    private static final double COLD_EDGE = 308.15;
    private static final double WARM_EDGE = 310.65;
    private static final double HOUR = 3_600;
    /* A whole outfit of leather: about one clo over the whole body, keeping the wind out. */
    private static final BodyHeat.Clothes LEATHER = new BodyHeat.Clothes(0.162, 0.97, 0);

    @Test
    void step_keepsABareBodyStandingInMildStillAirInItsNormalBand() {
        final double core = after(air(14), BodyHeat.Clothes.NONE, STANDING, 4 * HOUR);

        assertTrue(core > COLD_EDGE && core < WARM_EDGE, "a bare body in a mild plain shivers a little: " + c(core));
    }

    @Test
    void step_coolsABareBodyInSnowAndWindPastItsNormalBand() {
        final double core = after(windy(-10, 5), BodyHeat.Clothes.NONE, STANDING, 2 * HOUR);

        assertTrue(core < COLD_EDGE, "two hours bare in the snow and the wind bring hypothermia: " + c(core));
    }

    @Test
    void step_keepsTheSameBodyWarmInLeather() {
        final double core = after(windy(-10, 5), LEATHER, STANDING, 4 * HOUR);

        assertTrue(core > COLD_EDGE, "leather that keeps the wind out keeps the core in its band: " + c(core));
    }

    @Test
    void step_chillsFarFasterInIceWaterThanInAirAsCold() {
        final BodyHeat.Place water = new BodyHeat.Place(276, SEA_LEVEL, 0, 0, 276, true, 276, false);
        final double inWater = after(water, BodyHeat.Clothes.NONE, STANDING, 600);
        final double inAir = after(air(3), BodyHeat.Clothes.NONE, STANDING, 600);

        assertTrue(inWater < inAir - 0.5, "ten minutes in water near freezing: " + c(inWater) + " against "
                + c(inAir) + " in air as cold");
    }

    @Test
    void step_letsTheSunWarmABodyInTheOpen() {
        final BodyHeat.Place shade = air(14);
        final BodyHeat.Place sun = new BodyHeat.Place(shade.airKelvin(), SEA_LEVEL, 0, 1_000, shade.radiantKelvin(),
                false, 0, false);

        final double inShade = BodyHeat.step(BodyHeat.NORMAL, shade, BodyHeat.Clothes.NONE, STANDING, 1).lost();
        final double inSun = BodyHeat.step(BodyHeat.NORMAL, sun, BodyHeat.Clothes.NONE, STANDING, 1).lost();

        assertTrue(inSun < inShade - 100, "the noon sun gives a bare body some hundred watts: " + inShade + " W lost "
                + "in the shade, " + inSun + " W in the sun");
    }

    @Test
    void step_holdsAWalkerInADesertNoonNearNormalBySweating() {
        final BodyHeat.Place desert = new BodyHeat.Place(299.15, SEA_LEVEL, 0, 1_000, 299.15, false, 0, false);

        final double core = after(desert, BodyHeat.Clothes.NONE, WALKING, 2 * HOUR);

        assertTrue(core > BodyHeat.NORMAL && core < 311.15, "sweat holds a walker under 38 °C: " + c(core));
    }

    @Test
    void step_carriesHeatOnlyByRadiationInAVacuum() {
        final BodyHeat.Place space = new BodyHeat.Place(3, 0, 0, 0, 3, false, 0, false);

        final BodyHeat.Balance balance = BodyHeat.step(BodyHeat.NORMAL, space, BodyHeat.Clothes.NONE, STANDING, 1);

        // Through the tissue, half open at the normal core, and radiation's film alone: no air, no sweat.
        assertEquals(BodyHeat.AREA * (BodyHeat.NORMAL - 3) / (0.065 + 1 / 4.6), balance.lost(), 1.0);
    }

    @Test
    void step_letsTheWindPressThroughClothesThatDoNotKeepItOut() {
        final BodyHeat.Clothes open = new BodyHeat.Clothes(0.15, 0, 0);
        final BodyHeat.Clothes shut = new BodyHeat.Clothes(0.15, 1, 0);

        final double letsOutOpen = BodyHeat.step(BodyHeat.NORMAL, windy(0, 10), open, STANDING, 1).letsOut();
        final double letsOutShut = BodyHeat.step(BodyHeat.NORMAL, windy(0, 10), shut, STANDING, 1).letsOut();

        assertEquals(1 / 0.075, letsOutOpen, 1.0e-9, "a gale takes half of what the clothes hold");
        assertEquals(1 / 0.15, letsOutShut, 1.0e-9, "and none of what windproof clothes hold");
    }

    @Test
    void step_wetsClothesInTheRainToAThirdOfTheirWarmth() {
        final BodyHeat.Place rain = new BodyHeat.Place(283.15, SEA_LEVEL, 0, 0, 283.15, false, 0, true);

        final BodyHeat.Balance balance = BodyHeat.step(BodyHeat.NORMAL, rain, new BodyHeat.Clothes(0.15, 0, 0),
                STANDING, 1);

        assertTrue(balance.wet());
        assertEquals(1 / (0.15 * 0.3), balance.letsOut(), 1.0e-9);
    }

    @Test
    void perMinute_isTheWattsOverTheHeatACoreHolds() {
        assertEquals(0.1, BodyHeat.perMinute(0.1 * BodyHeat.HEAT_CAPACITY / 60), 1.0e-12);
    }

    /* The core of a body that started at the normal one after {@code seconds} in {@code place}. */
    private static double after(final BodyHeat.Place place, final BodyHeat.Clothes clothes, final double work,
                                final double seconds) {
        double core = BodyHeat.NORMAL;
        for (int s = 0; s < seconds; s++) {
            core = BodyHeat.step(core, place, clothes, work, 1).coreKelvin();
        }
        return core;
    }

    /* Still air at {@code celsius}, no sun, and what is around as warm as the air. */
    private static BodyHeat.Place air(final double celsius) {
        return windy(celsius, 0);
    }

    private static BodyHeat.Place windy(final double celsius, final double wind) {
        final double kelvin = celsius + 273.15;
        return new BodyHeat.Place(kelvin, SEA_LEVEL, wind, 0, kelvin, false, 0, false);
    }

    private static String c(final double kelvin) {
        return String.format("%.2f °C", kelvin - 273.15);
    }
}
