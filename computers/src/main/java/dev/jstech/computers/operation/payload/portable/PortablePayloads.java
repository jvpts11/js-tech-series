/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.portable;

import dev.jstech.computers.client.portable.PortableViews;
import dev.jstech.computers.operation.payload.ClientPayloadHandlers;
import dev.jstech.computers.portable.Portables;
import dev.jstech.core.network.payload.ClientPayloads;
import dev.jstech.core.network.payload.IPayloadGate;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * The payloads of the portable devices: what a device keeps, told to the game of the player carrying it, and what
 * that player did on its system, from a device held in their hand and nowhere else.
 */
public final class PortablePayloads {

    private PortablePayloads() {
    }

    public static void register(final PayloadRegistrar registrar) {
        registrar.playToClient(PortableViewPayload.TYPE, PortableViewPayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread((payload, player) -> PortableViews.take(payload)));
        // Only from a device in one of the sender's hands: nobody works a device they are not holding.
        final IPayloadGate<PortableActionPayload> holding = (player, payload) ->
                !Portables.held(player, payload.device()).isEmpty();
        ClientPayloads.accept(registrar, PortableActionPayload.TYPE, PortableActionPayload.STREAM_CODEC, holding,
                Portables::act);
    }
}
