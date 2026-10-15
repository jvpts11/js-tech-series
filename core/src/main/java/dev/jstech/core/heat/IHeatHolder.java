/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.heat;

import org.jetbrains.annotations.Nullable;

/**
 * A block entity that holds heat in a {@link HeatBody}: the level's heat carries heat between it and its neighbours,
 * and the Core offers its body on {@link HeatPorts#HEAT} on every face, with nothing for its mod to register. A block
 * entity that lets heat through only some faces, as a machine's Sides tab sets them, is also an {@code IFaceGate},
 * asked of the heat capability face by face.
 */
public interface IHeatHolder {

    /** The heat it holds, or none for a block of a kind that may hold heat but this one does not. */
    @Nullable HeatBody heatBody();
}
