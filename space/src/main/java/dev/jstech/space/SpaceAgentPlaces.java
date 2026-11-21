/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space;

import dev.jstech.core.agent.AgentPlace;
import dev.jstech.core.agent.AgentPlaces;
import dev.jstech.space.landing.ShipLandings;
import dev.jstech.space.shipyard.ShipyardTerminalBlockEntity;
import dev.jstech.space.universe.SolarBody;
import dev.jstech.space.universe.SpaceClimbs;
import dev.jstech.space.universe.StarSystems;
import dev.jstech.space.universe.SystemLayout;
import dev.jstech.space.universe.SystemSpaces;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus;

/**
 * The places J's Space tells an agent of ({@link AgentPlaces}). In the Solar System's space: each world's low orbit,
 * over the world's middle on the space's floor; the floor itself, under the player; and how deep under it a ship goes
 * down into the world below, the descent. In any world: the landing pads the player may set a ship down on, the
 * runways from their beacons, named by the way each runs, and the shipyards loaded round the player.
 */
public final class SpaceAgentPlaces {

    /* How far round the player the shipyards loaded are looked for, in blocks. */
    private static final double SHIPYARDS_WITHIN = 4_096;

    private SpaceAgentPlaces() {
    }

    /** Declares them, once, from the mod's constructor. */
    @ApiStatus.Internal
    public static void declare() {
        AgentPlaces.declare(JsSpace.MODID, SpaceAgentPlaces::places);
    }

    private static void places(final ServerLevel level, final ServerPlayer player, final Consumer<AgentPlace> out) {
        if (SystemSpaces.systemOf(level.dimension()).filter(StarSystems.SOLAR_SYSTEM::equals).isPresent()) {
            for (final SolarBody body : SolarBody.values()) {
                final double[] middle = SystemLayout.centre(body);
                out.accept(new AgentPlace("low_orbit", body.name().toLowerCase(Locale.ROOT),
                        new Vec3(middle[0], SystemLayout.ORBIT_Y, middle[2])));
            }
            out.accept(new AgentPlace("space_floor", "", new Vec3(player.getX(), SystemLayout.ORBIT_Y,
                    player.getZ())));
            out.accept(new AgentPlace("descent", "", new Vec3(player.getX(),
                    SystemLayout.ORBIT_Y - SpaceClimbs.UNDER_THE_FLOOR, player.getZ())));
        }
        for (final ShipLandings.Pad pad : ShipLandings.padsUsableBy(player, level)) {
            out.accept(new AgentPlace("landing_pad", "", ShipLandings.padSpot(level, pad)));
        }
        for (final ShipLandings.Beacon beacon : ShipLandings.beacons(level)) {
            out.accept(new AgentPlace("runway", beacon.way().getSerializedName(), Vec3.atBottomCenterOf(beacon.at())));
        }
        for (final ShipyardTerminalBlockEntity yard : ShipyardTerminalBlockEntity.near(level, player.position(),
                SHIPYARDS_WITHIN)) {
            if (yard.formed()) {
                out.accept(new AgentPlace("shipyard", "", Vec3.atCenterOf(yard.getBlockPos())));
            }
        }
    }
}
