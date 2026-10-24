/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.progression;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

/**
 * What opens a piece of knowledge to every team, whatever they know: a mod whose setting turns its research off, so
 * nothing that asks for one of its discoveries is locked. Handed to {@link KnowledgeGate#openWhen}.
 */
@FunctionalInterface
public interface IKnowledgeOpener {

    /**
     * Whether the knowledge {@code id} is open to everyone now: on the server {@code server}, or, with none, on a
     * player's game as the server last said.
     */
    boolean open(@Nullable MinecraftServer server, ResourceLocation id);
}
