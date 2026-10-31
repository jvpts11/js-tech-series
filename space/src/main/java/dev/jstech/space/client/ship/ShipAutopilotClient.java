/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.ship;

import dev.jstech.core.client.input.KeyActionsClient;
import dev.jstech.core.moving.StructureSeatEntity;
import dev.jstech.space.network.ShipAutopilotPayload;
import dev.jstech.space.ship.ShipAutopilotControl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The autopilot's key on a player's game: pressed at a ship's helm, the server takes its autopilot on to its next
 * mode; anywhere else it does nothing.
 */
public final class ShipAutopilotClient {

    private ShipAutopilotClient() {
    }

    /** Hands the key its press, once, from the mod's player-side entry point. */
    public static void listen() {
        KeyActionsClient.onPress(ShipAutopilotControl.KEY, () -> {
            final LocalPlayer player = Minecraft.getInstance().player;
            if (player != null && player.getVehicle() instanceof StructureSeatEntity seat && seat.helm()) {
                PacketDistributor.sendToServer(new ShipAutopilotPayload());
            }
        });
    }
}
