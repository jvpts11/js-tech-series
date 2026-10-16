/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.light;

import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;

/**
 * Something on the player's game that lights the way as it moves, besides what is held, burns and glows: a vehicle's
 * headlights, a moving structure's lamps. Asked each tick; what it gives is drawn on this game only, as the rest of
 * dynamic light is. Handed to {@link DynamicLightsClient#provide}.
 *
 * <pre>{@code
 * DynamicLightsClient.provide(level -> Cars.near(level).stream()
 *         .map(car -> new IDynamicLightSource.Light(car.front(), 14, 0xFFF4E0)).toList());
 * }</pre>
 */
@FunctionalInterface
public interface IDynamicLightSource {

    /** The lights it gives in {@code level} now. */
    List<Light> lights(ClientLevel level);

    /**
     * A light that moves.
     *
     * @param at     where it is
     * @param level  its level, from 1 to 15
     * @param colour its colour, {@code 0xRRGGBB}; white for none
     */
    record Light(Vec3 at, int level, int colour) {
    }
}
