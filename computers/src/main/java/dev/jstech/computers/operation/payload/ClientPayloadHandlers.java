/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

/**
 * Hands a payload the server sent to what shows it, on the client's main thread.
 *
 * <p>A handler given here may call client-only classes from its body, but it is never a method reference to a
 * client-only class: a method reference resolves its class as soon as the registration runs, and a dedicated
 * server runs the same registration without any client classes to resolve.
 */
public final class ClientPayloadHandlers {

    private ClientPayloadHandlers() {
    }

    /** Runs {@code handler} on the main thread with this client's player. */
    public static <P extends CustomPacketPayload> IPayloadHandler<P> onMainThread(
            final IClientPayloadHandler<P> handler) {
        return (payload, context) -> context.enqueueWork(() -> handler.handle(payload, context.player()));
    }
}
