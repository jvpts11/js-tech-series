/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.variation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RockRuleTest {

    /* The top of a plain, a face of a scarp, rock buried a few blocks down, and a block on the top of a scarp. */
    private static final RockPlace PLAIN_TOP = new RockPlace(0, 0, true, false, 70);
    private static final RockPlace SCARP_FACE = new RockPlace(3, 6, false, true, 67);
    private static final RockPlace BURIED = new RockPlace(5, 0, false, false, 65);
    private static final RockPlace SCARP_TOP = new RockPlace(0, 6, true, true, 70);

    @Test
    void fits_anywhereFitsEveryPlace() {
        for (final RockPlace place : new RockPlace[] {PLAIN_TOP, SCARP_FACE, BURIED, SCARP_TOP}) {
            assertTrue(RockRule.ANYWHERE.fits(place), place.toString());
        }
    }

    @Test
    void fits_readsHowTheRockLiesOpen() {
        final RockRule top = RockRule.ANYWHERE.exposed(RockRule.Exposure.TOP);
        final RockRule side = RockRule.ANYWHERE.exposed(RockRule.Exposure.SIDE);
        final RockRule buried = RockRule.ANYWHERE.exposed(RockRule.Exposure.BURIED);
        assertTrue(top.fits(PLAIN_TOP) && top.fits(SCARP_TOP));
        assertFalse(top.fits(SCARP_FACE) || top.fits(BURIED));
        assertTrue(side.fits(SCARP_FACE) && side.fits(SCARP_TOP));
        assertFalse(side.fits(PLAIN_TOP) || side.fits(BURIED));
        assertTrue(buried.fits(BURIED));
        assertFalse(buried.fits(PLAIN_TOP) || buried.fits(SCARP_FACE) || buried.fits(SCARP_TOP));
    }

    @Test
    void fits_keepsWithinItsDepthsSlopeAndHeights() {
        final RockRule upper = RockRule.ANYWHERE.depth(0, 3);
        assertTrue(upper.fits(PLAIN_TOP) && upper.fits(SCARP_FACE));
        assertFalse(upper.fits(BURIED));
        final RockRule steep = RockRule.ANYWHERE.slope(2);
        assertTrue(steep.fits(SCARP_FACE) && steep.fits(SCARP_TOP));
        assertFalse(steep.fits(PLAIN_TOP) || steep.fits(BURIED));
        final RockRule high = RockRule.ANYWHERE.heights(68, 80);
        assertTrue(high.fits(PLAIN_TOP));
        assertFalse(high.fits(SCARP_FACE) || high.fits(BURIED));
    }

    @Test
    void fits_holdsOnlyWhereEveryPartHolds() {
        // The faces of scarps: open on a side, on steep ground.
        final RockRule faces = RockRule.ANYWHERE.exposed(RockRule.Exposure.SIDE).slope(2);
        assertTrue(faces.fits(SCARP_FACE));
        assertFalse(faces.fits(new RockPlace(3, 1, false, true, 67)), "a cut on a plain is no scarp");
        assertFalse(faces.fits(new RockPlace(3, 6, false, false, 67)), "rock behind a scarp's face is not on it");
    }
}
