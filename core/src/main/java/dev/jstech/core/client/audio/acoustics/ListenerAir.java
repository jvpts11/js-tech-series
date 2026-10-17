/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.audio.acoustics;

import dev.jstech.core.audio.acoustics.ListenerAirPayload;
import dev.jstech.core.audio.acoustics.SoundMedium;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.material.FogType;

/**
 * The air round the player's head as the server last told it, and what sound travels through there: that air, or
 * water when the player's head is under it. Until the server says otherwise, the air at sea level.
 */
final class ListenerAir {

    private static final ListenerAirPayload SEA_LEVEL = new ListenerAirPayload(
            (float) SoundMedium.SEA_LEVEL.pressure(), (float) SoundMedium.SEA_LEVEL.temperature(), 0F, 0F);

    private static volatile ListenerAirPayload air = SEA_LEVEL;

    private ListenerAir() {
    }

    /** What the server told of the air round the player's head. */
    static void told(final ListenerAirPayload payload) {
        air = payload;
    }

    /** Forgets it, as the player leaves a world. */
    static void forget() {
        air = SEA_LEVEL;
    }

    /** The air round the player's head, as last told. */
    static ListenerAirPayload now() {
        return air;
    }

    /** Whether a sealed room holds the player, with its size known. */
    static boolean inRoom() {
        return air.roomVolume() > 0 && air.roomSurface() > 0;
    }

    /** What sound travels through where the player is now. */
    static SoundMedium medium() {
        final Minecraft minecraft = Minecraft.getInstance();
        final boolean underWater = minecraft.gameRenderer != null
                && minecraft.gameRenderer.getMainCamera().getFluidInCamera() == FogType.WATER;
        final ListenerAirPayload told = air;
        return SoundMedium.of(Math.max(0, told.pressure()), Math.max(-273, told.temperature()), underWater);
    }
}
