/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.hazard;

import java.util.EnumSet;
import java.util.Set;

/**
 * What a gas does to whoever is in a cloud of it, once let out: the hazards of a gas, read off its hazard classes.
 * Every mod that lets a gas out (a leak, a vent, an atmosphere) reads them here, so a cloud of chlorine poisons and
 * feeds a fire wherever it comes from.
 *
 * <p>Pure: no game types.
 */
public enum GasHazard {
    /** A spark or a flame sets it alight; with enough of it, it explodes. */
    FLAMMABLE,
    /** It hurts and poisons whoever breathes it. */
    TOXIC,
    /** It takes the place of the air: in a closed room, whoever breathes it suffocates. */
    ASPHYXIANT,
    /** It makes what burns burn harder. */
    OXIDISING,
    /** It hurts on contact, and eats metal. */
    CORROSIVE,
    /** It irradiates whoever is in it. */
    RADIOACTIVE;

    /** The hazards of a gas of {@code classes}: a harmful gas is as toxic in a cloud as an acutely toxic one. */
    public static Set<GasHazard> of(final Set<HazardClass> classes) {
        final Set<GasHazard> out = EnumSet.noneOf(GasHazard.class);
        for (final HazardClass hazard : classes) {
            switch (hazard) {
                case FLAMMABLE, EXPLOSIVE -> out.add(FLAMMABLE);
                case ACUTELY_TOXIC, HARMFUL -> out.add(TOXIC);
                case ASPHYXIANT -> out.add(ASPHYXIANT);
                case OXIDISING -> out.add(OXIDISING);
                case CORROSIVE -> out.add(CORROSIVE);
                case RADIOACTIVE -> out.add(RADIOACTIVE);
                case HEALTH_HAZARD, ENVIRONMENTAL -> {
                }
            }
        }
        return out;
    }
}
