/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.persistor;

import dev.jstech.core.id.IStableName;
import java.util.List;

/** The shape of the area a Space Persistor keeps loaded round its own chunk, as its keys pick it. */
public enum PersistorShape implements IStableName {

    /** A square of chunks, {@code 2r + 1} a side. */
    SQUARE("square"),
    /** The chunks whose middles lie within its radius, a round area. */
    CIRCLE("circle"),
    /** A line of chunks {@code 2r + 1} long, across the way the Persistor faces. */
    LINE("line"),
    /** Its own chunk only, and whatever chunks are clicked on its map. */
    FREE("free");

    private final String serializedName;

    PersistorShape(final String serializedName) {
        this.serializedName = serializedName;
    }

    /** The shapes in the order of their keys on the Persistor's face. */
    public static List<PersistorShape> round() {
        return List.of(SQUARE, CIRCLE, LINE, FREE);
    }

    @Override
    public String serializedName() {
        return this.serializedName;
    }
}
