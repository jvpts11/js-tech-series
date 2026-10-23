/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.matter;

import dev.jstech.core.heat.HeatMath;
import dev.jstech.core.room.GasMath;

/**
 * What compressing a gas costs. Gas is counted in normal litres, its millibuckets at the open air's pressure and 0 °C,
 * where a mole fills 22.4 of them, and a vessel holds its volume times its pressure in them; pressing it from one
 * pressure to a higher one, cooled as it goes, takes the work of the gas law, {@code n R T ln(p2 / p1)}, divided by
 * the compressor's efficiency. The work grows with the ratio of the pressures, not with their difference: from 1 bar
 * to 200 costs about eight times what from 1 bar to 2 does, not a hundred, which is why real plants press their gas
 * to hundreds of bar.
 *
 * <pre>{@code
 * CompressionMath.work(1_000.0, 100.0, 20_000.0, 293.15, 0.7)   // a cubic metre of hydrogen to 200 bar: 0.82 MJ
 * }</pre>
 *
 * <p>Pure: no game types.
 */
public final class CompressionMath {

    private CompressionMath() {
    }

    /**
     * The energy, in joules, of compressing {@code normalLitres} of gas from {@code fromKpa} to {@code toKpa} at
     * {@code kelvin}, cooled to that temperature as it goes, by a compressor of {@code efficiency}, from above none to
     * the whole. Nothing for no gas or no rise in pressure.
     *
     * @throws IllegalArgumentException when a pressure or the temperature is not above none, or the efficiency is not
     *                                  above none and at most the whole
     */
    public static double work(final double normalLitres, final double fromKpa, final double toKpa,
                              final double kelvin, final double efficiency) {
        if (!(fromKpa > 0.0) || !(toKpa > 0.0) || !(kelvin > 0.0)) {
            throw new IllegalArgumentException("a gas has a pressure and a temperature above none");
        }
        if (!(efficiency > 0.0 && efficiency <= 1.0)) {
            throw new IllegalArgumentException("a compressor's efficiency is above none and at most the whole, not "
                    + efficiency);
        }
        if (!(normalLitres > 0.0) || toKpa <= fromKpa) {
            return 0.0;
        }
        final double moles = normalLitres / HeatMath.LITRES_PER_MOLE;
        return moles * GasMath.GAS_CONSTANT * kelvin * Math.log(toKpa / fromKpa) / efficiency;
    }

    /** The normal litres a vessel of {@code volumeLitres} holds at {@code kpa}: its volume times its pressure. */
    public static double held(final double volumeLitres, final double kpa) {
        return Math.max(0.0, volumeLitres) * Math.max(0.0, kpa) / GasMath.ATMOSPHERE;
    }
}
