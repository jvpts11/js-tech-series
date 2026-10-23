/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.hazard;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Which pictograms a label shows, from the substance's hazard classes and how it is held. Each class calls for its
 * pictogram; the exclamation mark gives way to the skull, as the GHS says, since a substance toxic enough for the
 * skull needs no warning that it is harmful; and the gas cylinder is no class of the substance but of its container:
 * it shows for a gas held above the open air's pressure, a cylinder of hydrogen, never for hydrogen in a room.
 *
 * <pre>{@code
 * GhsPictograms.of(Set.of(OXIDISING, ACUTELY_TOXIC, HARMFUL, ENVIRONMENTAL), true)   // chlorine in a cylinder
 * // [FLAME_OVER_CIRCLE, GAS_CYLINDER, SKULL, ENVIRONMENT]
 * }</pre>
 *
 * <p>Pure: no game types.
 */
public final class GhsPictograms {

    private GhsPictograms() {
    }

    /**
     * The pictograms a substance of {@code classes} shows, in the order of their codes; with the gas cylinder when
     * {@code underPressure}, a gas held above the open air's pressure.
     */
    public static List<GhsPictogram> of(final Set<HazardClass> classes, final boolean underPressure) {
        final Set<GhsPictogram> shown = EnumSet.noneOf(GhsPictogram.class);
        for (final HazardClass hazard : classes) {
            hazard.pictogram().ifPresent(shown::add);
        }
        if (underPressure) {
            shown.add(GhsPictogram.GAS_CYLINDER);
        }
        if (shown.contains(GhsPictogram.SKULL)) {
            shown.remove(GhsPictogram.EXCLAMATION_MARK);
        }
        return List.copyOf(new ArrayList<>(shown));
    }

    /** The hazards of {@code classes} a label marks outside the GHS: an asphyxiant gas, radioactivity. */
    public static List<HazardClass> beyond(final Set<HazardClass> classes) {
        final List<HazardClass> out = new ArrayList<>();
        for (final HazardClass hazard : HazardClass.values()) {
            if (classes.contains(hazard) && hazard.pictogram().isEmpty()) {
                out.add(hazard);
            }
        }
        return out;
    }
}
