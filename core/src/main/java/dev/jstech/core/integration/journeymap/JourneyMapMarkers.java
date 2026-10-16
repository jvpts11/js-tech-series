/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.integration.journeymap;

import dev.jstech.core.JsCore;
import dev.jstech.core.client.map.ClientMarkers;
import dev.jstech.core.client.map.IMapAdapter;
import dev.jstech.core.map.MapMarkers;
import dev.jstech.core.map.Marker;
import dev.jstech.core.map.MarkerLayer;
import java.util.List;
import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.IClientPlugin;
import journeymap.api.v2.client.JourneyMapPlugin;
import journeymap.api.v2.client.display.MarkerOverlay;
import journeymap.api.v2.client.display.PolygonOverlay;
import journeymap.api.v2.client.model.MapImage;
import journeymap.api.v2.client.model.MapPolygon;
import journeymap.api.v2.client.model.ShapeProperties;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The map markers on JourneyMap's map and minimap: a point as a marker in its colour, labelled, an area as its chunks
 * shaded in its colour, each layer a group of its own. JourneyMap finds this plugin by its annotation, and loads it
 * only while it is installed itself.
 */
@JourneyMapPlugin(apiVersion = "2.0.0")
public final class JourneyMapMarkers implements IClientPlugin, IMapAdapter {

    private IClientAPI api;

    private static final Logger LOGGER = LoggerFactory.getLogger(JourneyMapMarkers.class);
    /* The game's own map decoration, a small pointed mark, tinted with the marker's colour. */
    private static final ResourceLocation MARK = ResourceLocation.withDefaultNamespace(
            "textures/map/decorations/target_point.png");
    private static final int MARK_SIZE = 8;
    private static final float AREA_OPACITY = 0.3F;
    private static final float EDGE_OPACITY = 0.8F;
    private static final int TOP = 320;

    @Override
    public String getModId() {
        return JsCore.MODID;
    }

    @Override
    public void initialize(final IClientAPI clientApi) {
        this.api = clientApi;
        ClientMarkers.add(this);
    }

    @Override
    public void show(final ClientLevel level, final List<Marker> markers) {
        if (this.api == null) {
            return;
        }
        this.api.removeAll(JsCore.MODID);
        for (final Marker marker : markers) {
            final MarkerLayer layer = MapMarkers.layerOf(marker.layer());
            final int rgb = marker.colourIn(layer);
            try {
                if (marker.isArea()) {
                    for (final ChunkPos chunk : marker.area()) {
                        this.api.show(new PolygonOverlay(JsCore.MODID, marker.dimension(), new ShapeProperties()
                                .setFillColor(rgb).setFillOpacity(AREA_OPACITY).setStrokeColor(rgb)
                                .setStrokeOpacity(EDGE_OPACITY), corners(chunk))
                                .setOverlayGroupName(layer.id().toString()).setTitle(marker.label().getString()));
                    }
                } else {
                    this.api.show(new MarkerOverlay(JsCore.MODID, marker.pos(), new MapImage(MARK, MARK_SIZE,
                            MARK_SIZE).setColor(rgb).centerAnchors()).setDimension(marker.dimension())
                            .setOverlayGroupName(layer.id().toString()).setLabel(marker.label().getString())
                            .setTitle(marker.label().getString()));
                }
            } catch (final Exception refused) {
                LOGGER.warn("JourneyMap refused the map marker {}: {}", marker.id(), refused.toString());
            }
        }
    }

    private static MapPolygon corners(final ChunkPos chunk) {
        return new MapPolygon(new BlockPos(chunk.getMinBlockX(), TOP, chunk.getMinBlockZ()),
                new BlockPos(chunk.getMaxBlockX() + 1, TOP, chunk.getMinBlockZ()),
                new BlockPos(chunk.getMaxBlockX() + 1, TOP, chunk.getMaxBlockZ() + 1),
                new BlockPos(chunk.getMinBlockX(), TOP, chunk.getMaxBlockZ() + 1));
    }
}
