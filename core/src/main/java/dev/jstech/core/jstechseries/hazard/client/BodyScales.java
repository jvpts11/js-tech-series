/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.hazard.client;

import dev.jstech.core.client.GameLocale;
import dev.jstech.core.client.gui.component.ScaleView;
import dev.jstech.core.hazard.BodyHeat;
import dev.jstech.core.hazard.BodyTemperature;
import dev.jstech.core.hazard.BodyTemperaturePayload;
import dev.jstech.core.hazard.HazardBand;
import dev.jstech.core.hazard.HazardKind;
import dev.jstech.core.jstechseries.hazard.HazardScreenTexts;
import dev.jstech.core.text.GameText;
import java.util.ArrayList;
import java.util.List;

/**
 * The scales of the body's screen: the core's warmth among its bands, moved by what the body is used to, with a
 * pointer where the core is; and a hazard built up against its bands, from nothing to past its last one.
 */
final class BodyScales {

    /* The core's scale, in degrees: from past severe hypothermia to past heatstroke. */
    private static final double LOWEST = 26;
    private static final double HIGHEST = 44;
    /* A hazard's scale runs past its last band by a quarter of where that band starts. */
    private static final double PAST_LAST = 1.25;
    private static final List<String> NUMERALS = List.of("I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X");

    private BodyScales() {
    }

    /**
     * The core's warmth on its scale: the bands of hypothermia below, the normal one, those of overheating above, each
     * moved toward the climate the body is used to, and the pointer at the core.
     */
    static ScaleView.Reading temperature(final BodyTemperaturePayload now, final BodyScreen.Colours c) {
        final double normal = celsius(BodyHeat.NORMAL);
        final double cold = normal + Math.min(0, now.acclimation());
        final double heat = normal + Math.max(0, now.acclimation());
        final double[] edges = {LOWEST, cold - BodyTemperature.SEVERE, cold - BodyTemperature.MODERATE,
                cold - BodyTemperature.MILD, heat + BodyTemperature.EXHAUSTION, heat + BodyTemperature.STROKE, HIGHEST};
        final int[] colours = {c.deepCold(), c.cold(), c.cool(), c.normal(), c.warm(), c.hot()};
        final List<ScaleView.Band> bands = new ArrayList<>(colours.length);
        final List<ScaleView.Mark> marks = new ArrayList<>(colours.length - 1);
        for (int k = 0; k < colours.length; k++) {
            bands.add(ScaleView.Band.of(share(edges[k]), share(edges[k + 1]), colours[k]));
            if (k > 0) {
                marks.add(new ScaleView.Mark(share(edges[k]), GameText.resolve(HazardScreenTexts.DEGREE_MARK.with(
                        GameLocale.numbers().decimal(Math.round(edges[k] * 10) / 10.0)))));
            }
        }
        return new ScaleView.Reading(share(celsius(now.coreKelvin())), bands, marks, c.pointer());
    }

    /**
     * {@code level} of {@code kind} built up, filling its scale from {@code low} to {@code high}, a mark at each of its
     * bands numbered from the first.
     */
    static ScaleView.Reading builtUp(final HazardKind kind, final double level, final int low, final int high) {
        final List<HazardBand> bands = kind.bands();
        final double most = bands.isEmpty() ? Math.max(1, level) : bands.getLast().from() * PAST_LAST;
        final List<ScaleView.Mark> marks = new ArrayList<>(bands.size());
        for (int k = 0; k < bands.size(); k++) {
            marks.add(new ScaleView.Mark(bands.get(k).from() / most, k < NUMERALS.size() ? NUMERALS.get(k) : ""));
        }
        return new ScaleView.Reading(level / most, List.of(new ScaleView.Band(0, 1, low, high)), marks, 0);
    }

    /** {@code kelvin} in degrees Celsius. */
    static double celsius(final double kelvin) {
        return kelvin - BodyHeat.FREEZING;
    }

    /* Where {@code degrees} stands on the core's scale. */
    private static double share(final double degrees) {
        return (degrees - LOWEST) / (HIGHEST - LOWEST);
    }
}
