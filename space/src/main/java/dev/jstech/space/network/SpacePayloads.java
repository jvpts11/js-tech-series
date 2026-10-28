/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.network;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * The payloads J's Space sends between the server and a player's game, and when: the Overworld's map number as a
 * player joins. What a player's game does with each is given to it from its own side, so nothing of a player's game is
 * loaded on a server.
 */
public final class SpacePayloads {

    private static final String VERSION = "1";

    /* What a player's game does with the Overworld's map number; nothing until its side gives it. */
    private static volatile IMapSeedReceiver mapSeedReceiver = seed -> {
    };

    private SpacePayloads() {
    }

    /** Hands the buses the payloads and when they are sent, once, from the mod's constructor. */
    public static void register(final IEventBus modBus) {
        modBus.addListener(RegisterPayloadHandlersEvent.class, event -> {
            final PayloadRegistrar registrar = event.registrar(VERSION);
            registrar.playToClient(OverworldMapPayload.TYPE, OverworldMapPayload.STREAM_CODEC,
                    (payload, context) -> context.enqueueWork(() -> mapSeedReceiver.receive(payload.mapSeed())));
        });
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                PacketDistributor.sendToPlayer(player, new OverworldMapPayload(
                        OverworldMapPayload.mapSeedOf(player.server.overworld().getSeed())));
            }
        });
    }

    /** Gives what a player's game does with the Overworld's map number, from its own side's entry point. */
    public static void onMapSeed(final IMapSeedReceiver receiver) {
        mapSeedReceiver = receiver;
    }

    /** What a player's game does with the Overworld's map number. */
    @FunctionalInterface
    public interface IMapSeedReceiver {

        void receive(long mapSeed);
    }
}
