/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.integration.xaero;

import dev.jstech.core.JsCore;
import dev.jstech.core.client.map.IMapAdapter;
import dev.jstech.core.map.MapMarkers;
import dev.jstech.core.map.Marker;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.module.MinimapSession;
import xaero.hud.minimap.waypoint.thirdparty.ThirdPartyWaypoints;
import xaero.hud.minimap.world.MinimapWorld;

/**
 * The map markers in Xaero's Minimap, and so in its World Map: each a waypoint of the Core's own, which the player
 * cannot edit or lose, in the colour of the sixteen the minimap has nearest the marker's. Loaded only while Xaero's
 * Minimap is installed.
 */
public final class XaeroMarkers implements IMapAdapter {

    private static final ResourceLocation ORIGIN = ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "markers");
    private static final int INITIALS = 2;

    @Override
    public void show(final ClientLevel level, final List<Marker> markers) {
        final MinimapSession session = BuiltInHudModules.MINIMAP.getCurrentSession();
        if (session == null) {
            return;
        }
        final MinimapWorld world = session.getWorldManager().getCurrentWorld();
        if (world == null || world.getContainer() == null) {
            return;
        }
        final ThirdPartyWaypoints ours = world.getContainer().getThirdPartyWaypointManager().get(ORIGIN);
        ours.clear();
        for (final Marker marker : markers) {
            final String name = marker.label().getString();
            final String initials = name.isEmpty() ? "J" : name.substring(0, Math.min(INITIALS, name.length()));
            ours.add(marker.id(), new Waypoint(marker.pos().getX(), marker.pos().getY(), marker.pos().getZ(), name,
                    initials, nearest(marker.colourIn(MapMarkers.layerOf(marker.layer())))));
        }
    }

    /* The index of the game's chat colour nearest {@code rgb}, which the minimap's colours follow. */
    private static int nearest(final int rgb) {
        int best = 0;
        long bestDistance = Long.MAX_VALUE;
        int index = 0;
        for (final ChatFormatting colour : ChatFormatting.values()) {
            if (!colour.isColor() || colour.getColor() == null) {
                continue;
            }
            final int other = colour.getColor();
            final long dr = (rgb >> 16 & 0xFF) - (other >> 16 & 0xFF);
            final long dg = (rgb >> 8 & 0xFF) - (other >> 8 & 0xFF);
            final long db = (rgb & 0xFF) - (other & 0xFF);
            final long distance = dr * dr + dg * dg + db * db;
            if (distance < bestDistance) {
                bestDistance = distance;
                best = index;
            }
            index++;
        }
        return best;
    }
}
