/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.machine;

import dev.jstech.core.hazard.PollutionKind;
import dev.jstech.core.hazard.RadiationKind;
import java.util.Objects;

/**
 * What a machine of J's Industrial gives off while it works, through J's Core's hazards: a pollution put on the chunk
 * it stands in, so much a second, or a radiation it gives off as a source, which a Lead Shielding in its upgrade slots
 * holds in (industrial/docs/design/machines.md).
 */
public sealed interface Emission permits Emission.Pollutes, Emission.Radiates {

    /** {@code perSecond} of {@code kind}, in its unit, put on its chunk each second it works. */
    static Emission pollutes(final PollutionKind kind, final double perSecond) {
        return new Pollutes(kind, perSecond);
    }

    /** {@code strength} of {@code kind}, in its unit, given off while it works. */
    static Emission radiates(final RadiationKind kind, final double strength) {
        return new Radiates(kind, strength);
    }

    /**
     * A pollution.
     *
     * @param kind      the pollution
     * @param perSecond how much it puts on its chunk each second it works, in the pollution's unit
     */
    record Pollutes(PollutionKind kind, double perSecond) implements Emission {

        public Pollutes {
            Objects.requireNonNull(kind, "kind");
            if (!(perSecond > 0.0) || Double.isInfinite(perSecond)) {
                throw new IllegalArgumentException("a machine pollutes by a finite amount above nothing; got "
                        + perSecond);
            }
        }
    }

    /**
     * A radiation.
     *
     * @param kind     the radiation
     * @param strength how much it gives off while it works, in the radiation's unit
     */
    record Radiates(RadiationKind kind, double strength) implements Emission {

        public Radiates {
            Objects.requireNonNull(kind, "kind");
            if (!(strength > 0.0) || Double.isInfinite(strength)) {
                throw new IllegalArgumentException("a machine radiates a finite strength above nothing; got "
                        + strength);
            }
        }
    }
}
