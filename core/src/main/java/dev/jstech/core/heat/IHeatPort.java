/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.heat;

/**
 * A block's heat as its neighbours and heat lines meet it, through one of its faces: how hot it is, how much heat it
 * holds a kelvin, how well that face conducts, and heat put in or taken out. Offered on the {@link HeatPorts#HEAT}
 * capability; the Core's {@link HeatBody} is one.
 */
public interface IHeatPort {

    /** How hot it is, in kelvin. */
    double temperature();

    /** How many joules it holds for each kelvin. */
    double capacity();

    /** How well the face conducts, in watts a metre a kelvin, as its material does. */
    double conductivity();

    /** Puts {@code joules} of heat in, or takes them out for a negative amount. */
    void exchange(double joules);
}
