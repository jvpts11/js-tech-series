/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.sky;

import dev.jstech.core.id.IStableName;
import java.util.List;

/**
 * The kind of a star a sky's sun is, which gives it its colour and how large it looks beside the overworld's sun at the
 * same distance: the hot blue giants, the Sun's own yellow dwarf, the small red dwarfs most worlds circle, a white
 * dwarf's point of light and a red giant filling the sky.
 */
public enum StarKind implements IStableName {
    O("o", 0x9BB0FF, 1.8),
    B("b", 0xAABFFF, 1.5),
    A("a", 0xCAD7FF, 1.3),
    F("f", 0xF8F7FF, 1.1),
    G("g", 0xFFF4EA, 1.0),
    K("k", 0xFFD2A1, 0.9),
    M("m", 0xFFA060, 0.6),
    WHITE_DWARF("white_dwarf", 0xE8EEFF, 0.2),
    RED_GIANT("red_giant", 0xFF7A40, 6.0);

    private static final List<StarKind> ROUND = List.of(O, B, A, F, G, K, M, WHITE_DWARF, RED_GIANT);
    private final String serializedName;
    private final int colour;
    private final double size;

    StarKind(final String serializedName, final int colour, final double size) {
        this.serializedName = serializedName;
        this.colour = colour;
        this.size = size;
    }

    /** Every kind, the hottest first. */
    public static List<StarKind> round() {
        return ROUND;
    }

    @Override
    public String serializedName() {
        return this.serializedName;
    }

    /** Its light's colour, {@code 0xRRGGBB}. */
    public int colour() {
        return this.colour;
    }

    /** How large it looks beside the overworld's sun at the same distance. */
    public double size() {
        return this.size;
    }
}
