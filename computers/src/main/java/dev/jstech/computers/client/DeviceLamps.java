/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;

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
     * Shows or hides a part of the model. The baked model is shared by every device drawn with it, and by its item,
     * so a renderer sets every part it owns on every frame.
     */
    public static void show(final BakedGeoModel model, final String bone, final boolean visible) {
        model.getBone(bone).ifPresent(b -> b.setHidden(!visible));
    }

    /** Whether a blinking lamp is lit at this moment of the level's clock. */
    public static boolean blinkLit(@Nullable final Level level) {
        return level == null || (level.getGameTime() / BLINK_TICKS) % 2 == 0;
    }
}
