/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.state;

import dev.jstech.core.JsCore;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** What the states send over the network: the value of a synced state that is a player's to see. */
@EventBusSubscriber(modid = JsCore.MODID)
public final class StateNetwork {

    private static final String VERSION = "1";

    private StateNetwork() {
    }

    @SubscribeEvent
    public static void onRegisterPayloads(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(VERSION);
        registrar.playToClient(StateSyncPayload.TYPE, StateSyncPayload.STREAM_CODEC, StateNetwork::onState);
    }

    /* Runs on a player's game only: the payload is only ever sent to one. */
    private static void onState(final StateSyncPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> ClientStates.receive(payload.state(), payload.value()));
    }
}
