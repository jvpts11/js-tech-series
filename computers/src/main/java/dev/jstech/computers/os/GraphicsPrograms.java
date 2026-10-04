/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;

/**
 * The programs whose windows draw graphics and hold video memory while they are open: a paint program, an image
 * viewer, a game. Every other window holds only RAM.
 */
public final class GraphicsPrograms {

    private static final Set<ResourceLocation> GRAPHICAL = ConcurrentHashMap.newKeySet();

    private GraphicsPrograms() {
    }

    /** Marks {@code program} as one whose windows hold video memory. */
    public static void register(final ResourceLocation program) {
        GRAPHICAL.add(program);
    }

    /** Whether a window opened under {@code key} is a graphics program's. */
    public static boolean isGraphical(final String key) {
        final ProgramSpec spec = WindowKeys.program(key);
        return spec != null && GRAPHICAL.contains(spec.id());
    }
}
