/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio.media;

import com.mojang.logging.LogUtils;
import dev.jstech.core.JsCore;
import java.io.IOException;
import java.nio.file.Path;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

/**
 * Where the recordings live for as long as a server runs: its store opens in the world's own folder when the server
 * starts and closes when it stops, and every tick sends each player their share of what they wait for and keeps the
 * recordings playing in the world heard by whoever is near them.
 */
@EventBusSubscriber(modid = JsCore.MODID)
public final class MediaServerEvents {

    private static final Logger LOGGER = LogUtils.getLogger();

    private MediaServerEvents() {
    }

    @SubscribeEvent
    public static void onServerStarting(final ServerStartingEvent event) {
        final Path root = event.getServer().getWorldPath(LevelResource.ROOT).resolve("jstech").resolve("media");
        try {
            final MediaStore store = new MediaStore(root);
            store.sweepIncoming();
            MediaStore.use(store);
        } catch (final IOException cannotOpen) {
            LOGGER.error("The world's recordings could not be opened at {}; none will play: {}", root,
                    cannotOpen.getMessage());
            MediaStore.use(null);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(final ServerStoppedEvent event) {
        MediaStore.use(null);
        MediaSessions.clear();
    }

    @SubscribeEvent
    public static void onServerTick(final ServerTickEvent.Post event) {
        MediaDownloads.tick(event.getServer());
        MediaSessions.tick(event.getServer());
    }

    @SubscribeEvent
    public static void onLoggedOut(final PlayerEvent.PlayerLoggedOutEvent event) {
        MediaDownloads.forget(event.getEntity().getUUID());
        MediaUploads.forget(event.getEntity().getUUID());
        MediaSessions.forget(event.getEntity().getUUID());
    }
}
