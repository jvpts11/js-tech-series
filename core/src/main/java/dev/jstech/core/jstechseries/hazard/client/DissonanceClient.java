/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.hazard.client;

import dev.jstech.core.client.audio.acoustics.SoundShaper;
import dev.jstech.core.client.hazard.HazardsClient;
import dev.jstech.core.client.physics.LightLawsClient;
import dev.jstech.core.jstechseries.hazard.DissonanceSenses;
import dev.jstech.core.jstechseries.hazard.HazardDevices;
import dev.jstech.core.physics.Laws;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

/**
 * Dissonance on the player's game: from its first band the colours of the view swing and the view shimmers, drawn as
 * the laws of light are; from its second every sound of the world plays a little higher or lower, wavering. How far
 * each goes is {@link DissonanceSenses}, read from what the server last told of the player's own dissonance.
 */
public final class DissonanceClient {

    private static final int UNTINTED = 0xFFFFFF;
    private static final double TICKS_A_SECOND = 20.0;

    private DissonanceClient() {
    }

    /** Hands the view and the sounds what dissonance does to them, once, from the Core's client. */
    public static void listen() {
        LightLawsClient.see(seconds -> {
            final double level = HazardsClient.level(HazardDevices.DISSONANCE);
            if (level < DissonanceSenses.SIGHT_WARPS) {
                return null;
            }
            return new Laws.Light(UNTINTED, DissonanceSenses.shift(level, seconds), 0,
                    DissonanceSenses.shimmer(level), 0, 0);
        });
        SoundShaper.shiftPitch(() -> {
            final ClientLevel level = Minecraft.getInstance().level;
            return level == null ? 1 : DissonanceSenses.pitch(HazardsClient.level(HazardDevices.DISSONANCE),
                    level.getGameTime() / TICKS_A_SECOND);
        });
    }
}
