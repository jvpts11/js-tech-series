/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.room;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GasMathTest {

    private static final double CLOSE = 1e-6;

    @Test
    void pressure_followsTheAmountAndTheVolumeAsAnIdealGas() {
        final double atmosphere = GasMath.moles(GasMath.ATMOSPHERE, 1, GasMath.TEMPERED);
        assertEquals(41.57, atmosphere, 0.01, "a block of air at one atmosphere holds about 41.6 moles");
        assertEquals(GasMath.ATMOSPHERE, GasMath.pressure(atmosphere, 1, GasMath.TEMPERED), CLOSE);
        assertEquals(GasMath.ATMOSPHERE / 2, GasMath.pressure(atmosphere, 2, GasMath.TEMPERED), CLOSE,
                "the same gas in twice the room at half the pressure");
        assertEquals(0, GasMath.pressure(10, 0, GasMath.TEMPERED), CLOSE, "nothing in no room");
    }

    @Test
    void rate_emptiesASmallRoomThroughAHoleInSecondsAndThroughACrackInMinutes() {
        final double hole = GasMath.rate(1, GasMath.FLOW, 8);
        final double crack = GasMath.rate(0.01, GasMath.FLOW, 8);
        assertTrue(GasMath.kept(hole, 5) < 0.1, "a hole of a block: most of it gone in five seconds");
        assertTrue(GasMath.kept(crack, 60) > 0.7 && GasMath.kept(crack, 600) < 0.1,
                "a crack: most of it still there after a minute, gone after ten");
    }

    @Test
    void between_movesGasUntilEachCubicMetreHoldsTheSame() {
        final double moved = GasMath.between(100, 1, 0, 3, 1, GasMath.FLOW, 1_000);
        assertEquals(75, moved, 1e-3, "in the end three quarters go to the room three times as large");
        assertEquals(0, GasMath.between(10, 1, 30, 3, 1, GasMath.FLOW, 5), CLOSE, "nothing moves between equals");
        assertTrue(GasMath.between(0, 1, 30, 3, 1, GasMath.FLOW, 1) < 0, "and it moves back the other way");
    }

    @Test
    void flooding_pushesWaterInAsFastAsTheDepthPushesIt() {
        final double tenMetres = GasMath.underWater(GasMath.ATMOSPHERE, 10) - GasMath.ATMOSPHERE;
        assertEquals(98.07, tenMetres, 0.01, "ten metres of water weigh about an atmosphere");
        assertEquals(0.6 * Math.sqrt(2 * GasMath.GRAVITY * 10), GasMath.flooding(1, tenMetres), 1e-6);
        assertEquals(0, GasMath.flooding(1, -5), CLOSE, "air pushed harder than the water keeps it out");
    }

    @Test
    void bursts_whenTheDifferenceIsMoreThanTheBlockIsRatedTo() {
        assertTrue(GasMath.bursts(120, 101, 250));
        assertFalse(GasMath.bursts(120, 101, 200));
        assertTrue(GasMath.bursts(50, 101, 0), "a weak window bursts outwards into a vacuum too");
    }

    @Test
    void air_doesWhatItsGasesDo() {
        assertFalse(GasMath.headache(0.004), "the International Space Station's 0.4% is fine");
        assertTrue(GasMath.headache(0.02) && !GasMath.stifling(0.02), "2%: a headache");
        assertTrue(GasMath.stifling(0.06), "above 5%: danger");
        assertTrue(GasMath.poisons(101) && !GasMath.poisons(21), "pure oxygen at full pressure poisons");
        assertTrue(GasMath.burns(21) && !GasMath.burns(5), "no oxygen, no fire");
        assertTrue(GasMath.inferno(1.0, 34), "pure oxygen at a third of an atmosphere: an inferno");
        assertFalse(GasMath.inferno(0.21, 21), "the overworld's air: a fire");
    }
}
