/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.machine;

import dev.jstech.core.inventory.FaceMode;
import java.util.List;

/** The modes of a machine's face in the order of the Sides tab's knob: in, out, both, off. */
public final class FaceModes {

    private FaceModes() {
    }

    /** The modes in the knob's order, round from its first position. */
    public static List<FaceMode> round() {
        return List.of(FaceMode.IN, FaceMode.OUT, FaceMode.BOTH, FaceMode.NONE);
    }
}
