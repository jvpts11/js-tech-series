/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.hazard;

import dev.jstech.core.JsCore;
import dev.jstech.core.client.hud.HudElements;
import dev.jstech.core.gui.HudStack;
import dev.jstech.core.hazard.BodyTemperaturePayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * On a player's game: how warm the player's core is and why, as the server last said it, for the HUD and the body's
 * screen; a normal core until it says anything, and again once the player leaves the world.
 */
public final class BodyTemperatureClient {

    /** The id of its HUD element. */
    public static final ResourceLocation HUD = ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "body_temperature");

    private static volatile BodyTemperaturePayload told = BodyTemperaturePayload.NORMAL;

    private BodyTemperatureClient() {
    }

    /**
     * Hands the game's bus what forgets the reading as the player leaves, and the HUD its element, once, from the
     * Core's client.
     */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class,
                event -> told = BodyTemperaturePayload.NORMAL);
        HudElements.declare(HUD, HudStack.Corner.BOTTOM_LEFT, new BodyTemperatureHud());
    }

    /** The server's word, as it arrives. */
    public static void told(final BodyTemperaturePayload payload) {
        told = payload;
    }

    /** The last word. */
    public static BodyTemperaturePayload now() {
        return told;
    }
}
