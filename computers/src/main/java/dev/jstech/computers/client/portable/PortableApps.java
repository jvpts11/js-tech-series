/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.portable;

import dev.jstech.computers.operation.payload.CraftPlanPayload;
import dev.jstech.computers.operation.payload.MessengerStatePayload;
import dev.jstech.computers.operation.payload.portable.PortableGalleryPayload;
import dev.jstech.computers.operation.payload.portable.PortableMapPayload;
import dev.jstech.computers.operation.payload.portable.PortableNetworkPayload;
import dev.jstech.computers.operation.payload.portable.PortableProspectPayload;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

/**
 * What the player's game was last told for the apps of the portables its player carries: each device's network view,
 * gallery, map and prospecting, the Messenger the player has open and the plan of a craft asked for. Each app asks
 * again while it is open, so what it shows stays fresh; all of it is let go when the player leaves the world.
 */
public final class PortableApps {

    private static final Map<UUID, PortableNetworkPayload> NETWORK = new HashMap<>();
    private static final Map<UUID, PortableGalleryPayload> GALLERY = new HashMap<>();
    private static final Map<UUID, PortableMapPayload> MAP = new HashMap<>();
    private static final Map<UUID, PortableProspectPayload> PROSPECT = new HashMap<>();
    @Nullable
    private static MessengerStatePayload messenger;
    @Nullable
    private static CraftPlanPayload plan;

    private PortableApps() {
    }

    /** The network app's view of a device came. */
    public static void take(final PortableNetworkPayload view) {
        NETWORK.put(view.device(), view);
    }

    /** A device's gallery came. */
    public static void take(final PortableGalleryPayload view) {
        GALLERY.put(view.device(), view);
        GalleryTextures.keep(view);
    }

    /** What a device's Maps app marks came. */
    public static void take(final PortableMapPayload view) {
        MAP.put(view.device(), view);
    }

    /** What a device's prospecting found came. */
    public static void take(final PortableProspectPayload view) {
        PROSPECT.put(view.device(), view);
    }

    /** The Messenger came, for whichever window or app of it is open. */
    public static void take(final MessengerStatePayload view) {
        messenger = view;
    }

    /** A craft's plan came, for whichever dialog or app asked. */
    public static void take(final CraftPlanPayload view) {
        plan = view;
    }

    @Nullable
    public static PortableNetworkPayload network(final UUID device) {
        return NETWORK.get(device);
    }

    @Nullable
    public static PortableGalleryPayload gallery(final UUID device) {
        return GALLERY.get(device);
    }

    @Nullable
    public static PortableMapPayload map(final UUID device) {
        return MAP.get(device);
    }

    @Nullable
    public static PortableProspectPayload prospect(final UUID device) {
        return PROSPECT.get(device);
    }

    @Nullable
    public static MessengerStatePayload messenger() {
        return messenger;
    }

    @Nullable
    public static CraftPlanPayload plan() {
        return plan;
    }

    /** Forgets the plan shown, so the next one asked for is waited for. */
    public static void forgetPlan() {
        plan = null;
    }

    /** Lets everything go, as the player leaves the world. */
    public static void clear() {
        NETWORK.clear();
        GALLERY.clear();
        MAP.clear();
        PROSPECT.clear();
        messenger = null;
        plan = null;
        GalleryTextures.clear();
        CameraShots.clear();
        MapsAppView.clear();
    }
}
