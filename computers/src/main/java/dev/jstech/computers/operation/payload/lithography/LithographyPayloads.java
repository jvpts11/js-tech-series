/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.lithography;

import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.client.os.LithoKeys;
import dev.jstech.computers.client.os.LithographyApp;
import dev.jstech.computers.lithography.LithographyDesk;
import dev.jstech.computers.operation.payload.ClientPayloadHandlers;
import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * The lithography program's window (computers/docs/design/survival.md, "Masks"): it asks its computer for a look at a
 * station linked to it, or has the station write a mask, and is answered with what it shows.
 */
public final class LithographyPayloads {

    private LithographyPayloads() {
    }

    /** Registers the payloads this class handles. */
    public static void register(final PayloadRegistrar registrar) {
        ComputerAccess.accept(registrar, LithographyRequestPayload.TYPE, LithographyRequestPayload.STREAM_CODEC,
                ComputerAccess.machine(LithographyRequestPayload::hostPos), LithographyPayloads::handle);
        registrar.playToClient(LithographyStatePayload.TYPE, LithographyStatePayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread((payload, player) -> {
                    LithographyApp.accept(payload);
                    LithoKeys.accept(payload);
                }));
    }

    private static void handle(final LithographyRequestPayload payload, final ServerPlayer player,
                               final ServerLevel level) {
        if (!(level.getBlockEntity(payload.hostPos()) instanceof AbstractComputerBlockEntity computer)) {
            return;
        }
        final Optional<TextKey> refused = payload.action() == LithographyRequestPayload.WRITE
                ? LithographyDesk.write(level, computer, payload.station(), payload.mask()) : Optional.empty();
        PacketDistributor.sendToPlayer(player, new LithographyStatePayload(payload.hostPos(),
                LithographyDesk.look(level, computer, payload.station()),
                refused.map(TextKey::text).orElse(Text.EMPTY), refused.isPresent()));
    }
}
