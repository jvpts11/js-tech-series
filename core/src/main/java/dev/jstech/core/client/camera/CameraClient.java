/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.camera;

import dev.jstech.core.camera.CameraClientHooks;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.CalculateDetachedCameraDistanceEvent;
import net.neoforged.neoforge.client.event.CalculatePlayerTurnEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * The camera on a player's game: zoom through a lens, the view following what is ridden, looking about freely, views
 * from elsewhere the server starts, the views of cameras in the world drawn for the screens that show them, the view
 * shaken and leant, and the cameras mods put in the third person.
 */
public final class CameraClient {

    private CameraClient() {
    }

    /** Hands the mod's bus and the game's what this answers, once, from the Core's constructor on a player's game. */
    public static void listen(final IEventBus modBus) {
        CameraClientHooks.set(CameraViewsClient.hooks());
        modBus.addListener(RegisterClientReloadListenersEvent.class,
                event -> event.registerReloadListener(new CameraPathsClient()));
        modBus.addListener(RegisterGuiLayersEvent.class, CameraZoomClient::onRegisterLayers);
        NeoForge.EVENT_BUS.addListener(ViewportEvent.ComputeFov.class, CameraZoomClient::onFov);
        NeoForge.EVENT_BUS.addListener(ViewportEvent.ComputeFov.class, CameraViewsClient::onFov);
        NeoForge.EVENT_BUS.addListener(CalculatePlayerTurnEvent.class, CameraZoomClient::onTurn);
        NeoForge.EVENT_BUS.addListener(CalculatePlayerTurnEvent.class, FreeLook::onTurn);
        NeoForge.EVENT_BUS.addListener(InputEvent.MouseScrollingEvent.class, CameraZoomClient::onScroll);
        NeoForge.EVENT_BUS.addListener(ViewportEvent.ComputeCameraAngles.class, ChaseCamera::onAngles);
        NeoForge.EVENT_BUS.addListener(ViewportEvent.ComputeCameraAngles.class, FreeLook::onAngles);
        NeoForge.EVENT_BUS.addListener(ViewportEvent.ComputeCameraAngles.class, CameraViewsClient::onAngles);
        // Last, so shakes and leans move the view wherever the rest put it.
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, ViewportEvent.ComputeCameraAngles.class,
                CameraMotion::onAngles);
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> CameraMotion.clear());
        // Late, so a distance another mod set stands as the game's own when no camera of the Core's takes the view.
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, CalculateDetachedCameraDistanceEvent.class,
                ThirdPersonCameras::onDistance);
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class,
                event -> ThirdPersonCameras.clear());
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, ChaseCamera::onTick);
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, FreeLook::onTick);
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, CameraViewsClient::onTick);
        NeoForge.EVENT_BUS.addListener(RenderFrameEvent.Pre.class, FreeLook::onFrame);
        NeoForge.EVENT_BUS.addListener(RenderFrameEvent.Pre.class, CameraViewsClient::onFrame);
        NeoForge.EVENT_BUS.addListener(RenderFrameEvent.Pre.class, CameraFeeds::onFrame);
        NeoForge.EVENT_BUS.addListener(MovementInputUpdateEvent.class, CameraViewsClient::onInput);
        NeoForge.EVENT_BUS.addListener(RenderGuiLayerEvent.Pre.class, CameraViewsClient::onLayer);
        NeoForge.EVENT_BUS.addListener(RenderHandEvent.class, CameraViewsClient::onHand);
        NeoForge.EVENT_BUS.addListener(RenderNameTagEvent.class, CameraViewsClient::onNameTag);
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, CameraFeeds::onLeave);
    }
}
