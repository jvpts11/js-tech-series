/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.mechanical;

import org.jetbrains.annotations.Nullable;

/**
 * A block entity that holds a part of a network of mechanical power without being one itself, as a machine with a
 * motor or a dynamo does: the Core offers its part on {@link MechanicalPorts#MECHANICAL}, with nothing for its mod to
 * register.
 */
public interface IMechanicalHolder {

    /** The part it holds, or none for one of a kind that may turn but does not. */
    @Nullable IMechanicalPart mechanicalPart();
}
