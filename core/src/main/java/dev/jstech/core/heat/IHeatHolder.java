/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.heat;

/**
 * A block entity that holds heat in a {@link HeatBody}: the level's heat carries heat between it and its neighbours,
 * and the Core offers its body on {@link HeatPorts#HEAT} on every face, with nothing for its mod to register.
 */
public interface IHeatHolder {

    /** The heat it holds. */
    HeatBody heatBody();
}
