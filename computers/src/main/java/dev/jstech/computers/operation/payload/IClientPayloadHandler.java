/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import net.minecraft.world.entity.player.Player;

/** What a client does with a payload the server sent, on the client's main thread, with this client's player. */
@FunctionalInterface
public interface IClientPayloadHandler<P> {

    void handle(P payload, Player player);
}
