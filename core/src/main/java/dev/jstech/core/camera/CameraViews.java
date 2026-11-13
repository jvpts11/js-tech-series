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
    /* The view each player sees now, and the number of the last view started for each. */
    private static final Map<ServerPlayer, CameraView> SHOWING = new WeakHashMap<>();
    private static final Map<ServerPlayer, Integer> NUMBERS = new WeakHashMap<>();
    /*
     * The fewest and the most chunks the server sends round a player, as its chunk map holds the view distance it is
     * set: a server set to less, or to none, as a game test server is, still sends two.
     */
    private static final int LEAST_VIEW = 2;
    private static final int MOST_VIEW = 32;

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
     * player's world, in a chunk the server sends them and near enough for the server to send them the entity too.
     */
    public static boolean watch(final ServerPlayer player, final Entity entity) {
        if (entity.level() != player.level() || !inView(player, entity.position()) || !tracked(player, entity)) {
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
            PacketDistributor.sendToPlayer(player, new CameraViewPayload(Optional.empty(), 0));
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

    /**
     * What the player's game said of the view the server numbered {@code number}, the views started for a player
     * counted from one: it ended as {@code ending} says. Only the view showing now ends: the word about one the server
     * has since replaced comes too late, and ends nothing.
     */
    public static void ended(final ServerPlayer player, final CameraViewEvent.Ending ending, final int number) {
        if (NUMBERS.getOrDefault(player, 0) == number) {
            ended(player, ending);
        }
    }

    /* Starts {@code view} for {@code player}, ending the one before. */
    private static void show(final ServerPlayer player, final CameraView view) {
        final CameraView was = SHOWING.put(player, view);
        if (was != null) {
            JsCore.events().post(new CameraViewEvent(player, was, CameraViewEvent.Ending.STOPPED));
        }
        final int number = NUMBERS.merge(player, 1, Integer::sum);
        PacketDistributor.sendToPlayer(player, new CameraViewPayload(Optional.of(view), number));
    }

    /* Whether {@code place} is in a chunk the server sends {@code player}'s game, which it has to draw. */
    private static boolean inView(final ServerPlayer player, final Vec3 place) {
        return player.getChunkTrackingView().contains(SectionPos.blockToSectionCoord(place.x),
                SectionPos.blockToSectionCoord(place.z));
    }

    /*
     * Whether the server sends {@code player}'s game the entity itself: it does only within the entity's own tracking
     * range, shorter than the view for most, and the player's game cannot watch what it was never sent.
     */
    private static boolean tracked(final ServerPlayer player, final Entity entity) {
        final double range = Math.min(entity.getType().clientTrackingRange(),
                Math.clamp(player.server.getPlayerList().getViewDistance(), LEAST_VIEW, MOST_VIEW))
                * SectionPos.SECTION_SIZE;
        final double dx = entity.getX() - player.getX();
        final double dz = entity.getZ() - player.getZ();
        return dx * dx + dz * dz <= range * range;
    }

    /* Runs on a client only: the payload is only ever sent to one. */
    private static void onView(final CameraViewPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> CameraClientHooks.get().view(payload.view(), payload.number()));
    }

    /* Runs on the server: a view ended on the player's game. */
    private static void onEnded(final CameraViewEndedPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                ended(player, CameraViewEvent.Ending.byCode(payload.ending()).orElse(CameraViewEvent.Ending.STOPPED),
                        payload.number());
            }
        });
    }
}
