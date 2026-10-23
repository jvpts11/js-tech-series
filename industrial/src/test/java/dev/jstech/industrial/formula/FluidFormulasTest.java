/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.formula;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;

class FluidFormulasTest {

    @Test
    void tanks_holdTheirInnerVolumeAndGasTimesItsPressure() {
        assertEquals(27L, FluidFormulas.innerBlocks(5, 5, 5));
        assertEquals(27_000L, FluidFormulas.liquidTank(FluidFormulas.innerBlocks(5, 5, 5)));
        assertEquals(540_000L, FluidFormulas.gasTank(27L, 20.0));
        assertEquals(0L, FluidFormulas.innerBlocks(2, 9, 9));
    }

    @Test
    void run_carriesItsWorstPipesFlow() {
        assertEquals(250L, FluidFormulas.run(1_000L, 250L, 4_000L));
        assertEquals(0L, FluidFormulas.run());
    }

    @Test
    void oil_isTwoHundredMillibucketsABlock() {
        assertEquals(2_000_000L, FluidFormulas.oil(10_000L));
    }

    @Test
    void airShares_countTheRareGasesAThousandTimesAndAddUpToTheWhole() {
        final Map<String, Double> shares = FluidFormulas.airShares();
        assertEquals(1.0, shares.values().stream().mapToDouble(Double::doubleValue).sum(), 1e-12);
        assertEquals(0.780_8 / 0.209_5, shares.get("nitrogen") / shares.get("oxygen"), 1e-9);
        assertEquals(18.18e-3 / 0.209_5, shares.get("neon") / shares.get("oxygen"), 1e-9);
        assertEquals("nitrogen", shares.keySet().iterator().next());
    }
}
