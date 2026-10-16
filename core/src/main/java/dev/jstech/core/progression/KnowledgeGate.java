/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.progression;

import dev.jstech.core.team.CoreTeams;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

/**
 * The knowledge gate, written once for every mod: something that asks for a piece of knowledge runs only for an owner
 * whose team knows it. A recipe is locked for running, never for seeing: the recipe viewers and the manuals always show
 * it, with a lock while the player's team does not know what it needs.
 */
public final class KnowledgeGate {

    private KnowledgeGate() {
    }

    /**
     * Whether a machine owned by {@code owner} may run what asks for {@code knowledge}: anything that asks for none,
     * and what asks for some only when the owner's team knows it whole. A machine of no owner runs nothing locked.
     */
    public static boolean allows(final MinecraftServer server, final @Nullable UUID owner,
                                 final Optional<ResourceLocation> knowledge) {
        if (knowledge.isEmpty()) {
            return true;
        }
        return owner != null && TeamProgress.knows(server, CoreTeams.teamOf(server, owner), knowledge.get());
    }

    /** Whether what asks for {@code knowledge} is open to this client's player's team, as the server last said. */
    public static boolean openHere(final Optional<ResourceLocation> knowledge) {
        return knowledge.isEmpty() || TeamProgress.knowsHere(knowledge.get());
    }
}
