/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.progression;

import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;

/**
 * What a mod does when a player makes something: at a table, in a furnace, or in a machine they own, which may make
 * it while they are away. A mod that counts a first making as a discovery listens here
 * ({@link ProgressionTriggers#whenMade(IMadeListener)}).
 */
@FunctionalInterface
public interface IMadeListener {

    /** {@code player} made {@code stack}, on {@code server}. */
    void made(MinecraftServer server, UUID player, ItemStack stack);
}
