/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * What the server does with a payload a client sent. It runs on the server thread, after the payload's gate
 * has admitted the sender, and is handed the sender and the level the sender is in.
 */
@FunctionalInterface
public interface IServerPayloadHandler<P> {

    void handle(P payload, ServerPlayer player, ServerLevel level);
}
