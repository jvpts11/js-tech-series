/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.flight;

import dev.jstech.space.api.SpaceWorld;
import dev.jstech.space.api.SpaceWorlds;
import dev.jstech.space.network.LandingMapPayload;
import dev.jstech.space.universe.FarMaps;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * The Flight Computer's landing maps: the ground of the world it goes to round its marked spot, or round the world's
 * middle before one is marked, as the world's own generator makes it, worked out away from the server's thread from
 * the generator's noise alone, as the far maps of a climb are, so no land is generated for it.
 */
public final class LandingMaps {

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
        CompletableFuture.supplyAsync(() -> FarMaps.draw(world, ring, centre.getX(), centre.getZ()),
                        Util.backgroundExecutor())
                .thenAcceptAsync(map -> {
                    if (!player.hasDisconnected()) {
                        PacketDistributor.sendToPlayer(player, new LandingMapPayload(computer.target(), width, map));
                    }
                }, player.server);
    }

    /** The level of the world {@code computer} goes to, on {@code player}'s server, if it is there. */
    public static @Nullable ServerLevel levelOf(final ServerPlayer player, final FlightComputerBlockEntity computer) {
        final Optional<SpaceWorld> world = SpaceWorlds.get(player.level(), computer.target());
        return world.map(going -> player.server.getLevel(going.dimension())).orElse(null);
    }
}
