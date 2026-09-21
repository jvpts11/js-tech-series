/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.api.client;

import dev.jstech.computers.JsComputers;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Which screen draws which operating space, on the side of the game that has screens.
 *
 * <p>The other half of registering a space. The name goes in on both sides through the register event; the
 * screen goes in here, from client setup, because a dedicated server has no screen to give and must not be
 * asked to load one. A space whose name is registered and whose screen is not leaves the machine at its
 * prompt, which is what a machine with no space is, so a mistake here is a quiet fallback rather than a
 * crash in front of somebody.
 */
public final class OperatingSpaceScreens {

    /*
     * Concurrent because this is a public way in and client setup is handed to every mod at once, on as
     * many threads as the loader cares to use. Two addons registering a space from there at the same
     * moment must not be able to leave the map in a state where neither of them is in it.
     */
    private static final Map<ResourceLocation, IOperatingSpaceScreen> SCREENS = new ConcurrentHashMap<>();

    private OperatingSpaceScreens() {
    }

    /**
     * Says which screen draws the space registered under {@code spaceId}.
     *
     * <p>Called from client setup. Two mods claiming one space is a mistake in one of them, so the second is
     * refused with a word in the log rather than quietly taking the first one's place.
     */
    public static void register(final ResourceLocation spaceId, final IOperatingSpaceScreen screen) {
        if (spaceId == null || screen == null) {
            return;
        }
        if (SCREENS.putIfAbsent(spaceId, screen) != null) {
            JsComputers.LOGGER.warn("A screen is already registered for the operating space {}", spaceId);
        }
    }

    /** The screen that draws that space, or null when nothing has said. */
    @Nullable
    public static IOperatingSpaceScreen get(@Nullable final ResourceLocation spaceId) {
        return spaceId == null ? null : SCREENS.get(spaceId);
    }
}
