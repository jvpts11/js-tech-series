/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.integration.ftbchunks;

import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.client.event.MapIconEvent;
import dev.ftb.mods.ftbchunks.api.client.icon.MapIcon;
import dev.ftb.mods.ftbchunks.api.client.icon.MapType;
import dev.ftb.mods.ftblibrary.ui.BaseScreen;
import dev.ftb.mods.ftblibrary.ui.input.Key;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import dev.jstech.core.client.map.ClientMarkers;
import dev.jstech.core.map.MapMarkers;
import dev.jstech.core.map.Marker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.Vec3;

/**
 * The map markers on FTB Chunks' map and minimap: each an icon of the marker's colour at its place, its label in the
 * icon's tooltip; an area at its middle. Loaded only while FTB Chunks is installed.
 */
public final class FtbChunksMarkers {

    private static final int BORDER = 0xFF << 24;

    private FtbChunksMarkers() {
    }

    /** Adds the markers to FTB Chunks' maps as they are drawn, once, as the player's game starts. */
    public static void listen() {
        MapIconEvent.MINIMAP.register(FtbChunksMarkers::icons);
        MapIconEvent.LARGE_MAP.register(FtbChunksMarkers::icons);
    }

    /** Asks FTB Chunks to draw its icons again, the markers having changed. */
    public static void refresh() {
        FTBChunksAPI.clientApi().requestMinimapIconRefresh();
    }

    private static void icons(final MapIconEvent event) {
        for (final Marker marker : ClientMarkers.shown()) {
            if (marker.dimension().equals(event.getDimension())) {
                event.add(new Icon(marker, 0xFF << 24 | marker.colourIn(MapMarkers.layerOf(marker.layer()))));
            }
        }
    }

    /* A marker as an icon: a square of its colour, framed, its label in its tooltip. */
    private record Icon(Marker marker, int argb) implements MapIcon {

        @Override
        public Vec3 getPos(final float partialTick) {
            return Vec3.atCenterOf(this.marker.pos());
        }

        @Override
        public void addTooltip(final TooltipList list) {
            list.add(this.marker.label());
        }

        @Override
        public boolean onMousePressed(final BaseScreen screen, final MouseButton button) {
            return false;
        }

        @Override
        public boolean onKeyPressed(final BaseScreen screen, final Key key) {
            return false;
        }

        @Override
        public void draw(final MapType type, final GuiGraphics graphics, final int x, final int y, final int w,
                         final int h, final boolean outsideVisibleArea, final int iconAlpha) {
            graphics.fill(x, y, x + w, y + h, BORDER);
            graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, this.argb);
        }
    }
}
