/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.light;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The glowing parts of the Core's models drawn in the world, where they were last drawn: what draws a model reports
 * each one as it draws it ({@link #report}), and the glow and dynamic light read the last frame's. A part whose model
 * casts light lights the world around it as it moves, a lamp turning on a machine's arm.
 */
public final class KitGlows {

    /* The most parts a frame keeps, so a world full of them costs no more. */
    private static final int MOST = 512;

    /* The parts reported in the frame being drawn, and those of the last whole frame. */
    private static List<Glowing> filling = new ArrayList<>();
    private static volatile List<Glowing> last = List.of();

    private KitGlows() {
    }

    /**
     * Reports a glowing part drawn at {@code at} in {@code level}, which casts light of {@code light} (0 for none)
     * in {@code colour} ({@code 0xRRGGBB}).
     */
    public static void report(final Level level, final Vec3 at, final int light, final int colour) {
        if (level instanceof ClientLevel client && filling.size() < MOST) {
            filling.add(new Glowing(client, at, light, colour));
        }
    }

    /** The glowing parts of the last whole frame. */
    static List<Glowing> last() {
        return last;
    }

    /** Ends a frame: what it reported becomes the last. On the render thread. */
    static void endFrame() {
        last = List.copyOf(filling);
        filling = new ArrayList<>();
    }

    /**
     * A glowing part where it was drawn.
     *
     * @param level  the world it is in
     * @param at     where it is
     * @param light  the light it casts, 0 for none
     * @param colour its colour, {@code 0xRRGGBB}
     */
    record Glowing(ClientLevel level, Vec3 at, int light, int colour) {
    }
}
