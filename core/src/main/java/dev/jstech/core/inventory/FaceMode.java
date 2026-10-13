/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.inventory;

import dev.jstech.core.id.IStableName;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** What one face of a block lets through: nothing, what goes in, what comes out, or both. */
@TextHolder
public enum FaceMode implements IStableName {

    NONE("none", TextKey.of("jscore.face_mode.none", "Closed")),
    IN("in", TextKey.of("jscore.face_mode.in", "In")),
    OUT("out", TextKey.of("jscore.face_mode.out", "Out")),
    BOTH("both", TextKey.of("jscore.face_mode.both", "In and out"));

    private final String serializedName;
    private final TextKey name;

    FaceMode(final String serializedName, final TextKey name) {
        this.serializedName = serializedName;
        this.name = name;
    }

    @Override
    public String serializedName() {
        return this.serializedName;
    }

    /** What a player calls it. */
    public TextKey text() {
        return this.name;
    }

    /** Whether anything goes in through the face. */
    public boolean takesIn() {
        return this == IN || this == BOTH;
    }

    /** Whether anything comes out through the face. */
    public boolean givesOut() {
        return this == OUT || this == BOTH;
    }

    /** The next mode, as a button that goes round them would show it: closed, in, out, both, closed. */
    public FaceMode next() {
        return switch (this) {
            case NONE -> IN;
            case IN -> OUT;
            case OUT -> BOTH;
            case BOTH -> NONE;
        };
    }
}
