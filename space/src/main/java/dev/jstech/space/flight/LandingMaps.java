/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.flight;

import dev.jstech.core.team.CoreTeams;
import dev.jstech.space.api.SpaceWorld;
import dev.jstech.space.api.SpaceWorlds;
import dev.jstech.space.exploration.ReliefCoverage;
import dev.jstech.space.exploration.SpaceProperties;
import dev.jstech.space.exploration.WorldCoverage;
import dev.jstech.space.exploration.WorldDiscovery;
import dev.jstech.space.network.FarMapPayload;
import dev.jstech.space.network.LandingMapPayload;
import dev.jstech.space.network.LandingSight;
import dev.jstech.space.universe.FarMaps;
import dev.jstech.space.universe.SphereMap;
import java.util.BitSet;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * The Flight Computer's landing maps: the ground of the world it goes to round its marked spot, or round the world's
 * middle before one is marked, as the world's own generator makes it, worked out away from the server's thread from
 * the generator's noise alone, so no land is generated for it; with what the team asking knows of it: the ground its
 * instruments mapped, its rockets photographed from orbit and its crews stood on, the rest under the grey of the
 * unknown, and where the ground seen is too steep to land on.
 */
public final class LandingMaps {

    /* How steep ground is a hazard to land on: its rise to the next pixel over the pixel's width. */
    private static final double STEEP = 0.5;
    private static final double ROUND = 360;
    private static final double POLE_TO_POLE = 180;

    private LandingMaps() {
    }

    /** Draws {@code computer}'s landing map as it stands now and sends it to {@code player}, when it is drawn. */
    public static void send(final ServerPlayer player, final FlightComputerBlockEntity computer) {
        final ServerLevel world = levelOf(player, computer);
        if (world == null) {
            return;
        }
        final BlockPos marked = computer.mark();
        final BlockPos centre = marked == null ? BlockPos.ZERO : marked;
        final int ring = computer.zoom();
        final double width = world.getWorldBorder().getSize();
        final ResourceLocation target = computer.target();
        final String team = CoreTeams.teamOf(player.server, player.getUUID());
        final ReliefCoverage seen = WorldCoverage.of(player.server, team, target);
        final double relief = WorldDiscovery.known(player.server, team, target, SpaceProperties.RELIEF);
        CompletableFuture.supplyAsync(() -> payload(target, width, FarMaps.draw(world, ring, centre.getX(),
                        centre.getZ()), seen, relief), Util.backgroundExecutor())
                .thenAcceptAsync(map -> {
                    if (!player.hasDisconnected()) {
                        PacketDistributor.sendToPlayer(player, map);
                    }
                }, player.server);
    }

    /** The level of the world {@code computer} goes to, on {@code player}'s server, if it is there. */
    public static @Nullable ServerLevel levelOf(final ServerPlayer player, final FlightComputerBlockEntity computer) {
        final Optional<SpaceWorld> world = SpaceWorlds.get(player.level(), computer.target());
        return world.map(going -> player.server.getLevel(going.dimension())).orElse(null);
    }

    /**
     * The landing map {@code map} of {@code world}, {@code width} blocks wide, as a team sees it that has seen
     * {@code seen} of its ground and knows {@code relief} of its relief from its instruments.
     */
    public static LandingMapPayload payload(final ResourceLocation world, final double width,
                                            final FarMapPayload map, final ReliefCoverage seen, final double relief) {
        final BitSet known = known(map, width, seen, relief);
        return new LandingMapPayload(world, width, map, new LandingSight(known.toByteArray(),
                steep(map, known).toByteArray(), seen.share(relief), cover(width, seen, relief).toByteArray()));
    }

    /** The pixels of {@code map}, of a world {@code width} blocks wide, whose ground is known. */
    public static BitSet known(final FarMapPayload map, final double width, final ReliefCoverage seen,
                               final double relief) {
        final int size = map.size();
        final BitSet out = new BitSet(size * size);
        for (int column = 0; column < size; column++) {
            final double x = map.centreX() + (column - size / 2.0 + 0.5) * map.blocks();
            final double[] tracks = seen.tracks(relief, SphereMap.longitude(x, width));
            for (int row = 0; row < size; row++) {
                final double z = map.centreZ() + (row - size / 2.0 + 0.5) * map.blocks();
                if (relief >= 1 || ReliefCoverage.under(tracks, SphereMap.latitude(z, width))
                        || seen.visited().contains(ReliefCoverage.cell(x, z))) {
                    out.set(row * size + column);
                }
            }
        }
        return out;
    }

    /** The pixels of {@code map} whose ground is known, {@code known}, and too steep to land on. */
    public static BitSet steep(final FarMapPayload map, final BitSet known) {
        final int size = map.size();
        final BitSet out = new BitSet(size * size);
        final double rise = STEEP * map.blocks();
        for (int row = 0; row < size - 1; row++) {
            for (int column = 0; column < size - 1; column++) {
                final int at = row * size + column;
                if (!known.get(at)) {
                    continue;
                }
                final int height = FarMapPayload.blocks(map.heights()[at]);
                if (Math.abs(height - FarMapPayload.blocks(map.heights()[at + 1])) > rise
                        || Math.abs(height - FarMapPayload.blocks(map.heights()[at + size])) > rise) {
                    out.set(at);
                }
            }
        }
        return out;
    }

    /** The whole world's cells, {@link LandingSight#COVER_WIDE} by {@link LandingSight#COVER_TALL}, seen. */
    public static BitSet cover(final double width, final ReliefCoverage seen, final double relief) {
        final BitSet out = new BitSet(LandingSight.COVER_WIDE * LandingSight.COVER_TALL);
        for (int column = 0; column < LandingSight.COVER_WIDE; column++) {
            final double longitude = (column + 0.5) / LandingSight.COVER_WIDE * ROUND - ROUND / 2;
            final double[] tracks = seen.tracks(relief, longitude);
            for (int row = 0; row < LandingSight.COVER_TALL; row++) {
                final double latitude = POLE_TO_POLE / 2 - (row + 0.5) / LandingSight.COVER_TALL * POLE_TO_POLE;
                if (relief >= 1 || ReliefCoverage.under(tracks, latitude) || seen.visited().contains(
                        ReliefCoverage.cell(SphereMap.x(longitude, width), SphereMap.z(latitude, width)))) {
                    out.set(row * LandingSight.COVER_WIDE + column);
                }
            }
        }
        return out;
    }
}
