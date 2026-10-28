/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.sky;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;

/**
 * Lets a mod draw a level's ground itself, as when its eye rises so far over the land that the blocks loaded are a
 * speck it draws better at its own scale: while any cover asks, the game draws none of the level's blocks and none of
 * its clouds. Entities, moving structures, particles and whatever else is drawn at the stages of the level's blocks
 * are drawn as ever.
 *
 * <p>On the way there, a mod can ask for the game's sight to reach down: the game draws only the blocks within its
 * sight's reach of the eye up or down, so from high over the land the ground falls out of sight long before it is far
 * off; while any asks, every loaded block the eye could see is drawn, however far below it is.
 */
public final class GroundCovers {

    private static final List<BooleanSupplier> COVERS = new CopyOnWriteArrayList<>();
    private static final List<BooleanSupplier> SIGHTS_DOWN = new CopyOnWriteArrayList<>();

    private GroundCovers() {
    }

    /** Adds {@code cover}, asked on the render thread every frame whether the level's ground is drawn by its mod. */
    public static void add(final BooleanSupplier cover) {
        COVERS.add(cover);
    }

    /** Adds {@code sight}, asked on the render thread every frame whether the game's sight reaches all the way down. */
    public static void reachDown(final BooleanSupplier sight) {
        SIGHTS_DOWN.add(sight);
    }

    /** Whether some cover is drawing the level's ground this frame. */
    public static boolean covered() {
        return any(COVERS);
    }

    /** Whether the game's sight reaches all the way down this frame. */
    public static boolean reachingDown() {
        return any(SIGHTS_DOWN);
    }

    private static boolean any(final List<BooleanSupplier> asked) {
        for (final BooleanSupplier one : asked) {
            if (one.getAsBoolean()) {
                return true;
            }
        }
        return false;
    }
}
