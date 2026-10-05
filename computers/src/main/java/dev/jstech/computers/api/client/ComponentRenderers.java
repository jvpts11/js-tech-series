/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.api.client;

import dev.jstech.computers.JsComputers;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * Which renderer draws which kind of generic component, on the side of the game that has screens.
 *
 * <p>The other half of adding a kind: the kind goes in through the register event, on both sides, and its renderer
 * here, from client setup. A kind with no renderer on a player's game shows a placeholder naming it.
 */
@ApiStatus.Experimental
public final class ComponentRenderers {

    /* Concurrent because client setup is handed to every mod at once, on as many threads as the loader likes. */
    private static final Map<String, IComponentRenderer> RENDERERS = new ConcurrentHashMap<>();

    private ComponentRenderers() {
    }

    /**
     * Says what draws the kind registered under {@code kind}. Two mods claiming one kind is a mistake in one of them,
     * so the second is refused with a word in the log.
     */
    public static void register(final ResourceLocation kind, final IComponentRenderer renderer) {
        if (kind == null || renderer == null) {
            return;
        }
        if (RENDERERS.putIfAbsent(kind.toString(), renderer) != null) {
            JsComputers.LOGGER.warn("A renderer is already registered for the component kind {}", kind);
        }
    }

    /** What draws the kind of that name, or null when nothing on this game does. */
    @Nullable
    public static IComponentRenderer get(@Nullable final String kind) {
        return kind == null ? null : RENDERERS.get(kind);
    }
}
