/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.camera;

import dev.jstech.core.JsCore;
import java.util.Map;
import net.minecraft.core.SectionPos;
import java.util.Optional;
import java.util.WeakHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Views from elsewhere, started by the server for one player: a scene along a camera path (a rocket's launch seen from
 * outside), the view of an entity (a robot, a drone, a probe), or the view from a point (a periscope). The player's
 * body stays where it is and still; sneaking skips a scene or leaves a view. When a view ends, however it ends, the
 * Core's events say so ({@link CameraViewEvent}), so the mod that started a scene carries on after it.
 *
 * <p>A view shows only what the player's game has of the world: an entity watched or a point looked from is in a
 * chunk the server sends the player, or the view would be of nothing.
 */
public final class CameraViews {

    private static final String VERSION = "1";
    /* The view each player sees now. */
    private static final Map<ServerPlayer, CameraView> SHOWING = new WeakHashMap<>();

    private CameraViews() {
    }

    /** Hands the mod's bus the payloads and the game's what a player leaving ends, once, from the Core. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterPayloadHandlersEvent.class, event -> event.registrar(VERSION)
                .playToClient(CameraViewPayload.TYPE, CameraViewPayload.STREAM_CODEC, CameraViews::onView)
                .playToServer(CameraViewEndedPayload.TYPE, CameraViewEndedPayload.STREAM_CODEC,
                        CameraViews::onEnded));
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedOutEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                SHOWING.remove(player);
            }
        });
    }

    /** Plays the camera path {@code path} for {@code player}, from {@code origin}, facing {@code yaw}. */
    public static void play(final ServerPlayer player, final ResourceLocation path, final Vec3 origin,
                            final float yaw) {
        show(player, new CameraView.Scene(path, origin, yaw));
    }

    /**
     * Shows {@code player} what {@code entity} sees, from its eyes; whether it could, the entity being in the
     * player's world, in a chunk the server sends them.
     */
    public static boolean watch(final ServerPlayer player, final Entity entity) {
        if (entity.level() != player.level() || !inView(player, entity.position())) {
            return false;
        }
        show(player, new CameraView.Watch(entity.getId()));
        return true;
    }

    /**
     * Shows {@code player} the view from {@code place}, looking {@code yaw} and {@code pitch}, turned by the player
     * when {@code turns}; whether it could, the place being in a chunk the server sends them.
     */
    public static boolean lookFrom(final ServerPlayer player, final Vec3 place, final float yaw, final float pitch,
                                   final boolean turns) {
        if (!inView(player, place)) {
            return false;
        }
        show(player, new CameraView.Point(place, yaw, pitch, turns));
        return true;
    }

    /** Ends the view {@code player} sees, if any. */
    public static void end(final ServerPlayer player) {
        final CameraView was = SHOWING.remove(player);
        if (was != null) {
            PacketDistributor.sendToPlayer(player, new CameraViewPayload(Optional.empty()));
            JsCore.events().post(new CameraViewEvent(player, was, CameraViewEvent.Ending.STOPPED));
        }
    }

    /** The view {@code player} sees now, or none. */
    public static Optional<CameraView> showing(final ServerPlayer player) {
        return Optional.ofNullable(SHOWING.get(player));
    }

    /** What the player's game said: the view {@code player} saw ended as {@code ending} says. */
    public static void ended(final ServerPlayer player, final CameraViewEvent.Ending ending) {
        final CameraView was = SHOWING.remove(player);
        if (was != null) {
            JsCore.events().post(new CameraViewEvent(player, was, ending));
        }
    }

    /* Starts {@code view} for {@code player}, ending the one before. */
    private static void show(final ServerPlayer player, final CameraView view) {
        final CameraView was = SHOWING.put(player, view);
        if (was != null) {
            JsCore.events().post(new CameraViewEvent(player, was, CameraViewEvent.Ending.STOPPED));
        }
        PacketDistributor.sendToPlayer(player, new CameraViewPayload(Optional.of(view)));
    }

    /* Whether {@code place} is in a chunk the server sends {@code player}'s game, which it has to draw. */
    private static boolean inView(final ServerPlayer player, final Vec3 place) {
        return player.getChunkTrackingView().contains(SectionPos.blockToSectionCoord(place.x),
                SectionPos.blockToSectionCoord(place.z));
    }

    /* Runs on a client only: the payload is only ever sent to one. */
    private static void onView(final CameraViewPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> CameraClientHooks.get().view(payload.view()));
    }

    /* Runs on the server: a view ended on the player's game. */
    private static void onEnded(final CameraViewEndedPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                ended(player, CameraViewEvent.Ending.byCode(payload.ending()).orElse(CameraViewEvent.Ending.STOPPED));
            }
        });
    }
}
