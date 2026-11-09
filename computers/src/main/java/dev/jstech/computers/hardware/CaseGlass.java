/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import dev.jstech.core.gui.Tube;
import java.util.Objects;

/**
 * The glass of a computer that is its own screen, as an all-in-one is (computers/docs/design/quince.md): its face
 * shows the screen as a monitor's does, and a click on the glass sits the player down at it as a click on a monitor
 * does.
 *
 * @param area  where the glass is on the front, in sixty-fourths of the face from its top left as it is seen
 * @param depth how far behind the block's front face the glass lies, in sixty-fourths of a block
 * @param tube  the tube the picture is seen through: a white phosphor for a black-and-white screen, colour for others
 */
public record CaseGlass(FrontArea area, int depth, Tube tube) {

    public CaseGlass {
        Objects.requireNonNull(area, "area");
        Objects.requireNonNull(tube, "tube");
        if (depth < 0) {
            throw new IllegalArgumentException("a glass lies on or behind the face, not in front of it");
        }
    }
}
