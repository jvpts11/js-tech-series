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
import dev.jstech.space.network.ShipHelmKeysPayload;
import dev.jstech.space.ship.ShipBoost;
import dev.jstech.space.ship.ShipPulse;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * A ship's own keys on a player's game, its boost and its pulse jump: while they sit at a ship's helm, the server is
 * told when they hold either and let it go, and again every so often while one is held, as the helm's own keys are.
 */
public final class ShipHelmKeysClient {

    /* How often keys held are told again, in ticks. */
    private static final int AGAIN = 10;
    private static boolean boost;
    private static boolean pulse;
    private static int since;

    private ShipHelmKeysClient() {
    }

    /** Hands the game's bus the ship's keys, once, from the mod's player-side entry point. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> tick());
    }

    private static void tick() {
        final LocalPlayer player = Minecraft.getInstance().player;
        final boolean helm = player != null && player.getVehicle() instanceof StructureSeatEntity seat && seat.helm();
        final boolean boosting = helm && KeyActionsClient.mapping(ShipBoost.KEY).isDown();
        final boolean pulsing = helm && KeyActionsClient.mapping(ShipPulse.KEY).isDown();
        since++;
        if (boosting != boost || pulsing != pulse || (boosting || pulsing) && since >= AGAIN) {
            boost = boosting;
            pulse = pulsing;
            since = 0;
            PacketDistributor.sendToServer(new ShipHelmKeysPayload(boosting, pulsing));
        }
    }
}
