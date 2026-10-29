/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.life;

import dev.jstech.core.client.light.DynamicLightsClient;
import dev.jstech.core.client.light.IDynamicLightSource;
import dev.jstech.space.api.SuitModule;
import dev.jstech.space.life.LifeSupport;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * The lamps on a suit's helmet, lighting the way a little ahead of whoever wears them, on every player's game.
 */
public final class SuitLamps {

    /* How far ahead of the eyes the light stands, in blocks, and its warm white. */
    private static final double AHEAD = 1.5;
    private static final int WARM_WHITE = 0xFFF4E0;

    private SuitLamps() {
    }

    /** Hands the Core's moving lights the lamps of every helmet, once, from the mod's client setup. */
    public static void listen() {
        DynamicLightsClient.provide(level -> {
            final List<IDynamicLightSource.Light> lights = new ArrayList<>();
            for (final Player player : level.players()) {
                for (final SuitModule module : LifeSupport.modules(level, player.getItemBySlot(EquipmentSlot.HEAD))) {
                    if (module.kind() == SuitModule.Kind.LIGHT) {
                        final Vec3 ahead = player.getEyePosition().add(player.getLookAngle().scale(AHEAD));
                        lights.add(new IDynamicLightSource.Light(ahead, (int) module.amount(), WARM_WHITE));
                        break;
                    }
                }
            }
            return lights;
        });
    }
}
