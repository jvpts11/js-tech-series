/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.overhead;

import dev.jstech.core.cable.CableType;
import java.util.List;

/**
 * The insulators of a block, which overhead wires are hung from: a pole's, a tower's, a wall's feedthrough, or a
 * machine's own where a line ends at it. A block offers it through {@link OverheadLines#ANCHORS}; a mod gives its
 * poles, towers and insulators.
 */
public interface IWireAnchors {

    /** Its insulators, numbered by their place in the list; the number never changes for a block of one kind. */
    List<WireAnchor> anchors();

    /** Whether the insulator numbered {@code anchor} takes a wire of {@code line}; every line unless said. */
    default boolean takes(final int anchor, final CableType line) {
        return true;
    }
}
