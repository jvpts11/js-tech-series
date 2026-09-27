/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.music;

import dev.jstech.computers.audio.SoundfoundryCovers;
import dev.jstech.computers.client.audio.SoundfoundryCoverArt;
import dev.jstech.computers.operation.payload.ClientPayloadHandlers;
import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.computers.operation.payload.SoundfoundryCoverPayload;
import dev.jstech.computers.operation.payload.SoundfoundryCoverRequestPayload;
import java.util.concurrent.CompletableFuture;
import net.minecraft.Util;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * The covers of the Standard Soundfoundry: a window asks for one a page named, and the server answers with it once
 * it is made, off the game's thread, since making one may mean reading a recording for the picture it carries.
 */
public final class SoundfoundryCoverPayloads {

    private SoundfoundryCoverPayloads() {
    }

    /** Registers the payloads this class handles. */
    public static void register(final PayloadRegistrar registrar) {
        ComputerAccess.accept(registrar, SoundfoundryCoverRequestPayload.TYPE,
                SoundfoundryCoverRequestPayload.STREAM_CODEC,
                ComputerAccess.machine(SoundfoundryCoverRequestPayload::hostPos), SoundfoundryCoverPayloads::onRequest);
        registrar.playToClient(SoundfoundryCoverPayload.TYPE, SoundfoundryCoverPayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread((cover, player) -> SoundfoundryCoverArt.accept(cover)));
    }

    private static void onRequest(final SoundfoundryCoverRequestPayload payload, final ServerPlayer player,
                                  final ServerLevel level) {
        final MinecraftServer server = level.getServer();
        final String key = payload.key();
        CompletableFuture.supplyAsync(() -> SoundfoundryCovers.cover(key), Util.backgroundExecutor())
                .thenAcceptAsync(image -> {
                    if (!player.hasDisconnected()) {
                        PacketDistributor.sendToPlayer(player, new SoundfoundryCoverPayload(key, image));
                    }
                }, server);
    }
}
