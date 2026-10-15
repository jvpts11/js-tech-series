/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.developer;

import dev.jstech.core.developer.LiveReload;
import dev.jstech.core.log.CoreLogs;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * The player's game in a development run reloading its resources, as F3+T would, once the mods' asset files have
 * changed and stayed as they are for a moment.
 */
public final class LiveReloadClient {

    private static final Logger LOGGER = CoreLogs.DEVELOPER.logger();
    private static final AtomicInteger RELOADS = new AtomicInteger();

    private LiveReloadClient() {
    }

    /** Hands the game's bus the reload, once, from the mod's constructor on a player's game; in development only. */
    public static void listen() {
        if (!FMLEnvironment.production) {
            NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, LiveReloadClient::afterClientTick);
        }
    }

    /** How many times the player's game reloaded its resources for a change; for a test to look at. */
    public static int reloads() {
        return RELOADS.get();
    }

    private static void afterClientTick(final ClientTickEvent.Post event) {
        final int changed = LiveReload.takeAssetChanges();
        if (changed > 0) {
            LOGGER.info("{} asset file(s) changed; reloading the resources", changed);
            Minecraft.getInstance().reloadResourcePacks().thenRun(RELOADS::incrementAndGet);
        }
    }
}
