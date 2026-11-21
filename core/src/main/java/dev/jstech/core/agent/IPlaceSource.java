/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.agent;

import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** The places a mod tells an agent of, near and far: its landing pads, the low orbits of its worlds. */
@FunctionalInterface
public interface IPlaceSource {

    /**
     * Hands {@code out} each place of the mod in {@code level}, the dimension {@code player} is in, that the player
     * may go to or look for; read on the server's thread.
     */
    void places(ServerLevel level, ServerPlayer player, Consumer<AgentPlace> out);
}
