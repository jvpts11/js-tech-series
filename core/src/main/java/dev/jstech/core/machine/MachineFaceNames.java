/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.machine;

import dev.jstech.core.connect.RelativeFace;
import dev.jstech.core.inventory.FaceMode;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** The words for a machine's faces and what they do, as a sentence says them: "Items, back face: Out". */
@TextHolder
public final class MachineFaceNames {

    public static final TextKey FRONT = TextKey.of("jscore.machine.face.front", "front");
    public static final TextKey BACK = TextKey.of("jscore.machine.face.back", "back");
    public static final TextKey LEFT = TextKey.of("jscore.machine.face.left", "left");
    public static final TextKey RIGHT = TextKey.of("jscore.machine.face.right", "right");
    public static final TextKey TOP = TextKey.of("jscore.machine.face.top", "top");
    public static final TextKey BOTTOM = TextKey.of("jscore.machine.face.bottom", "bottom");
    public static final TextKey IN = TextKey.of("jscore.machine.face.in", "In");
    public static final TextKey OUT = TextKey.of("jscore.machine.face.out", "Out");
    public static final TextKey BOTH = TextKey.of("jscore.machine.face.both", "Both");
    public static final TextKey NONE = TextKey.of("jscore.machine.face.none", "Neither");

    private MachineFaceNames() {
    }

    /** A face's name. */
    public static TextKey of(final RelativeFace face) {
        return switch (face) {
            case FRONT -> FRONT;
            case BACK -> BACK;
            case LEFT -> LEFT;
            case RIGHT -> RIGHT;
            case TOP -> TOP;
            case BOTTOM -> BOTTOM;
        };
    }

    /** What a face does, in a word. */
    public static TextKey of(final FaceMode mode) {
        return switch (mode) {
            case IN -> IN;
            case OUT -> OUT;
            case BOTH -> BOTH;
            case NONE -> NONE;
        };
    }
}
