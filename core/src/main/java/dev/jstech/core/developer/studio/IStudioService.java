/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer.studio;

import java.util.function.Consumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

/**
 * What the server answers one of the studio's tools: the fields of a block, what machines cost, how a loot table
 * spreads. Asked only for a player who may use the developer's tools that read or change the world.
 */
@FunctionalInterface
public interface IStudioService {

    /**
     * Answers {@code player}'s question {@code args} through {@code reply}, once: at once, or later, for a service that
     * measures for a while.
     */
    void answer(ServerPlayer player, CompoundTag args, Consumer<CompoundTag> reply);
}
