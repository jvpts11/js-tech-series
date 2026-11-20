/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.ship;

import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.space.client.universe.SystemSpaceRenderer;
import dev.jstech.space.network.ClimbWorldsPayload;
import dev.jstech.space.network.SpacePayloads;
import dev.jstech.space.ship.ShipHomeWay;
import dev.jstech.space.ship.ShipHudTexts;
import dev.jstech.space.universe.SolarBody;
import dev.jstech.space.universe.SystemLayout;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

/**
 * The way home a ship's HUD shows its pilot, back to where they last took off from in the world they fly in or over,
 * as the server last told their game: in the world's sky, how far the place is and its bearing, and its mark over it;
 * in the system's space over the world, the place of its map under the ship, where coming down now comes down, how
 * far the place taken off from is from there and the bearing to the spot of the low orbit a descent comes down onto
 * it from, its mark over that spot ({@link ShipHomeWay}).
 */
final class ShipHudHome {

    /** Nothing to show: no world under the ship, and no place taken off from in it. */
    static final Home NONE = new Home(List.of(), Double.NaN, null, "");
    private static final double THOUSAND = 1000;
    private static final int ROUND = 360;
    /* Where the player last took off from in each world, by the world, as the server last told. */
    private static volatile Map<ResourceLocation, BlockPos> liftOffs = Map.of();

    private ShipHudHome() {
    }

    /** Hands the game what keeps where its player took off from, once, from the HUD's own listening. */
    static void listen() {
        SpacePayloads.onLiftOffs(payload -> liftOffs = payload.places());
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> liftOffs = Map.of());
    }

    /** The way home from {@code at}, in {@code level}. */
    static Home of(final Level level, final Vec3 at) {
        final SolarBody body = SystemLayout.over(at.x, at.z);
        final ClimbWorldsPayload.ClimbWorld below = body == null ? null : SystemSpaceRenderer.worldBelow(body);
        if (below != null) {
            return overTheWorld(body, below, at);
        }
        final BlockPos home = liftOffs.get(level.dimension().location());
        if (home == null) {
            return NONE;
        }
        final Vec3 place = Vec3.atBottomCenterOf(home);
        final double bearing = ShipHomeWay.bearing(place.x - at.x, place.z - at.z);
        final String label = GameText.resolve(ShipHudTexts.LIFT_OFF_M.with(Text.literal(grouped(Math.hypot(
                place.x - at.x, place.z - at.z)))));
        return new Home(List.of(withBearing(label, bearing)), bearing, place, label);
    }

    /*
     * The way home over {@code world}, whose sphere is {@code body}, from {@code at} in the system's space: the place
     * under the ship, and the place taken off from in that world, if any.
     */
    private static Home overTheWorld(final SolarBody body, final ClimbWorldsPayload.ClimbWorld world, final Vec3 at) {
        final double[] under = ShipHomeWay.below(body, at.x, at.z, world.width());
        final String line = GameText.resolve(ShipHudTexts.BELOW.with(Text.literal(grouped(under[0])),
                Text.literal(grouped(under[1]))));
        final BlockPos home = liftOffs.get(world.dimension());
        if (home == null) {
            return new Home(List.of(line), Double.NaN, null, "");
        }
        final Vec3 place = Vec3.atBottomCenterOf(home);
        final double[] spot = ShipHomeWay.over(body, place.x, place.z, world.width());
        final double bearing = ShipHomeWay.bearing(spot[0] - at.x, spot[1] - at.z);
        final String label = GameText.resolve(ShipHudTexts.LIFT_OFF_KM.with(Text.literal(grouped(Math.hypot(
                place.x - under[0], place.z - under[1]) / THOUSAND))));
        return new Home(List.of(line, withBearing(label, bearing)), bearing,
                new Vec3(spot[0], SystemLayout.ORBIT_Y, spot[1]), label);
    }

    /* {@code label} with the bearing beside it, in whole degrees, three figures. */
    private static String withBearing(final String label, final double bearing) {
        return GameText.resolve(ShipHudTexts.BEARING.with(Text.literal(label), Text.literal(String.format(
                Locale.ROOT, "%03d", Math.floorMod(Math.round(bearing), ROUND)))));
    }

    /* A whole number with its thousands set apart, as the HUD writes distances and places. */
    private static String grouped(final double value) {
        return String.format(Locale.ROOT, "%,d", Math.round(value));
    }

    /**
     * What the HUD shows of the way home.
     *
     * @param lines   its lines at the top right, the last the one lit
     * @param bearing the bearing of the place taken off from, in degrees east of north; NaN with none
     * @param mark    where the place's mark stands, in the world or the space; none with no place
     * @param label   the mark's label
     */
    record Home(List<String> lines, double bearing, @Nullable Vec3 mark, String label) {
    }
}
