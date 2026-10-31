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
import dev.jstech.space.network.ShipBoostPayload;
import dev.jstech.space.ship.ShipBoost;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The boost's key on a player's game: while they sit at a ship's helm, the server is told when they hold it and let it
 * go, and again every so often while held, as the helm's own keys are.
 */
public final class ShipBoostClient {

    /* How often a held boost is told again, in ticks. */
    private static final int AGAIN = 10;
    private static boolean held;
    private static int since;

    private ShipBoostClient() {
    }

    /** Hands the game's bus the boost's key, once, from the mod's player-side entry point. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> tick());
    }

    private static void tick() {
        final LocalPlayer player = Minecraft.getInstance().player;
        final boolean holding = player != null && player.getVehicle() instanceof StructureSeatEntity seat
                && seat.helm() && KeyActionsClient.mapping(ShipBoost.KEY).isDown();
        since++;
        if (holding != held || holding && since >= AGAIN) {
            held = holding;
            since = 0;
            PacketDistributor.sendToServer(new ShipBoostPayload(holding));
        }
    }
}
