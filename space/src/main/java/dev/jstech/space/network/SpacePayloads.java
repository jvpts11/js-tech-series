/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.network;

import dev.jstech.space.life.SuitControls;
import dev.jstech.space.ship.ShipBoost;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * The payloads J's Space sends between the server and a player's game, and when: the Overworld's map number as a
 * player joins, a far map on the way up ({@code FarMaps}), and the worlds under a system's space as a player arrives in
 * it ({@code SpaceClimbs}), and a Flight Computer's landing map once drawn ({@code LandingMaps}); and from a player's
 * game, the light of their suit's controls pressed, and a ship's boost held at its helm. What a player's game
 * does with each is given to it from its own side, so nothing of a player's game is loaded on a server.
 */
public final class SpacePayloads {

    private static final String VERSION = "1";

    /* What a player's game does with the Overworld's map number and with a far map; nothing until its side gives it. */
    private static volatile IMapSeedReceiver mapSeedReceiver = seed -> {
    };
    private static volatile IFarMapReceiver farMapReceiver = map -> {
    };
    private static volatile IClimbWorldsReceiver climbWorldsReceiver = worlds -> {
    };
    private static volatile ILandingMapReceiver landingMapReceiver = map -> {
    };

    private SpacePayloads() {
    }

    /** Hands the buses the payloads and when they are sent, once, from the mod's constructor. */
    public static void register(final IEventBus modBus) {
        modBus.addListener(RegisterPayloadHandlersEvent.class, event -> {
            final PayloadRegistrar registrar = event.registrar(VERSION);
            registrar.playToClient(OverworldMapPayload.TYPE, OverworldMapPayload.STREAM_CODEC,
                    (payload, context) -> context.enqueueWork(() -> mapSeedReceiver.receive(payload.mapSeed())));
            registrar.playToClient(FarMapPayload.TYPE, FarMapPayload.STREAM_CODEC,
                    (payload, context) -> context.enqueueWork(() -> farMapReceiver.receive(payload)));
            registrar.playToClient(ClimbWorldsPayload.TYPE, ClimbWorldsPayload.STREAM_CODEC,
                    (payload, context) -> context.enqueueWork(() -> climbWorldsReceiver.receive(payload)));
            registrar.playToClient(LandingMapPayload.TYPE, LandingMapPayload.STREAM_CODEC,
                    (payload, context) -> context.enqueueWork(() -> landingMapReceiver.receive(payload)));
            registrar.playToServer(SuitLightPayload.TYPE, SuitLightPayload.STREAM_CODEC,
                    (payload, context) -> context.enqueueWork(() -> SuitControls.light(context.player(),
                            payload.on())));
            registrar.playToServer(ShipBoostPayload.TYPE, ShipBoostPayload.STREAM_CODEC,
                    (payload, context) -> context.enqueueWork(() -> {
                        if (context.player() instanceof ServerPlayer player) {
                            ShipBoost.told(player, payload.held());
                        }
                    }));
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

    /** Gives what a player's game does with a far map, from its own side's entry point. */
    public static void onFarMap(final IFarMapReceiver receiver) {
        farMapReceiver = receiver;
    }

    /** Gives what a player's game does with a Flight Computer's landing map, from its own side's entry point. */
    public static void onLandingMap(final ILandingMapReceiver receiver) {
        landingMapReceiver = receiver;
    }

    /** Gives what a player's game does with the worlds under a system's space, from its own side's entry point. */
    public static void onClimbWorlds(final IClimbWorldsReceiver receiver) {
        climbWorldsReceiver = receiver;
    }

    /** What a player's game does with the Overworld's map number. */
    @FunctionalInterface
    public interface IMapSeedReceiver {

        void receive(long mapSeed);
    }

    /** What a player's game does with a far map. */
    @FunctionalInterface
    public interface IFarMapReceiver {

        void receive(FarMapPayload map);
    }

    /** What a player's game does with the worlds under a system's space. */
    @FunctionalInterface
    public interface IClimbWorldsReceiver {

        void receive(ClimbWorldsPayload worlds);
    }

    /** What a player's game does with a Flight Computer's landing map. */
    @FunctionalInterface
    public interface ILandingMapReceiver {

        void receive(LandingMapPayload map);
    }
}
