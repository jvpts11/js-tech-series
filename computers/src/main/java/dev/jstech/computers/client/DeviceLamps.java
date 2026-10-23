/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.core.model.ModelPose;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * The lamps of a device drawn as a model. A lamp is a lit part standing proud of a dark socket, shown or hidden and
 * never scaled, and a blinking one follows the level's clock, so every device in sight blinks in step.
 */
public final class DeviceLamps {

    /** Ticks a blinking lamp stays lit, and then dark: twice a second. */
    private static final int BLINK_TICKS = 5;

    private DeviceLamps() {
    }

    /**
     * Shows or hides a part of the model, in the pose of the one device being drawn: each device has a pose of its
     * own, worked out again every frame from its file, so a renderer sets the parts it owns every frame.
     */
    public static void show(final ModelPose pose, final String bone, final boolean visible) {
        final int index = pose.index(bone);
        if (index >= 0) {
            pose.setHidden(index, !visible);
        }
    }

    /** Whether a blinking lamp is lit at this moment of the level's clock. */
    public static boolean blinkLit(@Nullable final Level level) {
        return level == null || (level.getGameTime() / BLINK_TICKS) % 2 == 0;
    }

    /** The names of {@code count} numbered bones, {@code <prefix>0} on, made once rather than every frame. */
    static String[] numbered(final String prefix, final int count) {
        final String[] out = new String[count];
        for (int k = 0; k < count; k++) {
            out[k] = prefix + k;
        }
        return out;
    }
}
