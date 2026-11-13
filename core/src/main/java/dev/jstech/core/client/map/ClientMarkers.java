/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.map;

import dev.jstech.core.client.gui.component.MapView;
import dev.jstech.core.integration.ftbchunks.FtbChunksIntegration;
import dev.jstech.core.integration.xaero.XaeroIntegration;
import dev.jstech.core.log.CoreLogs;
import dev.jstech.core.map.MapMarkers;
import dev.jstech.core.map.Marker;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * The map markers on the player's game: handed to every map mod installed, through its adapter, whenever they change
 * or the player goes to another dimension, and to the series' own maps ({@link #forMapView}). Each adapter shows the
 * markers of the dimension the player is in.
 */
public final class ClientMarkers {

    private static final Logger LOG = CoreLogs.INTEGRATIONS.logger();
    private static final List<IMapAdapter> ADAPTERS = new CopyOnWriteArrayList<>();
    /* How often the markers are looked at for a change, in ticks. */
    private static final int EVERY = 20;
    private static volatile List<Marker> shown = List.of();
    private static volatile Object shownIn;
    /* Whether a map was not ready for the markers last handed out, which are then handed to the maps again. */
    private static volatile boolean undelivered;
    private static int ticks;

    private ClientMarkers() {
    }

    /** Hands the game's bus what keeps the map mods up to date, and adds the adapters of those installed, once. */
    public static void listen() {
        XaeroIntegration.listen();
        FtbChunksIntegration.listen();
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> {
            if (++ticks % EVERY == 0) {
                refresh();
            }
        });
    }

    /** Adds the adapter of a map mod, which is handed the markers from then on: JourneyMap's adds itself. */
    public static void add(final IMapAdapter adapter) {
        ADAPTERS.add(Objects.requireNonNull(adapter, "adapter"));
        final ClientLevel level = Minecraft.getInstance().level;
        if (level != null && !handed(adapter, level, shown)) {
            undelivered = true;
        }
    }

    /** The markers last handed to the map mods: those of the dimension the player is in. */
    public static List<Marker> shown() {
        return shown;
    }

    /** The markers of the player's dimension, as the series' own maps draw them. */
    public static List<MapView.Marker> forMapView() {
        final List<MapView.Marker> marks = new ArrayList<>();
        for (final Marker marker : shown) {
            marks.add(new MapView.Marker(marker.pos(), marker.label().getString(),
                    0xFF << 24 | marker.colourIn(MapMarkers.layerOf(marker.layer()))));
        }
        return marks;
    }

    /* Hands the map mods the markers of the player's dimension when they, or the dimension, changed. */
    private static void refresh() {
        final ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            // Out of the world, nothing is shown: the next world's markers are handed over even if they are the same.
            shown = List.of();
            shownIn = null;
            return;
        }
        final List<Marker> now = MapMarkers.sentIn(level);
        if (now.equals(shown) && level.dimension().equals(shownIn) && !undelivered) {
            return;
        }
        shown = now;
        shownIn = level.dimension();
        undelivered = false;
        for (final IMapAdapter adapter : ADAPTERS) {
            if (!handed(adapter, level, now)) {
                undelivered = true;
            }
        }
        try {
            FtbChunksIntegration.refresh();
        } catch (final RuntimeException | LinkageError e) {
            LOG.warn("FTB Chunks could not take the markers", e);
        }
    }

    /*
     * Hands {@code adapter} the markers; false when it asks to be handed them again. One that fails is said once and
     * not tried again until the markers change: a map mod's failure is its own, and the others still get theirs.
     */
    private static boolean handed(final IMapAdapter adapter, final ClientLevel level, final List<Marker> markers) {
        try {
            return adapter.show(level, markers);
        } catch (final RuntimeException | LinkageError e) {
            LOG.warn("The map adapter {} could not take the markers", adapter.getClass().getName(), e);
            return true;
        }
    }
}
