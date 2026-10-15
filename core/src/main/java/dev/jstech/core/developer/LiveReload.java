/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer;

import dev.jstech.core.log.CoreLogs;
import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import net.minecraft.server.MinecraftServer;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

/**
 * Data and assets reloaded as they change, in a development run: the {@code assets} and {@code data} folders every mod
 * of the run is loaded from are watched, and once their files stop changing for a moment the server reloads its data
 * packs and the player's game its resources, as {@code /reload} and F3+T would, without anyone asking. A packed game
 * watches nothing.
 */
public final class LiveReload {

    private static final Logger LOGGER = CoreLogs.DEVELOPER.logger();
    /** How long the files have to stay as they are before a reload: an editor's save or a build's copy is over. */
    private static final long QUIET_MILLIS = 750;
    private static final ResourceChanges ASSETS = new ResourceChanges(QUIET_MILLIS);
    private static final ResourceChanges DATA = new ResourceChanges(QUIET_MILLIS);
    private static final AtomicInteger DATA_RELOADS = new AtomicInteger();
    /* What each watched folder holds, assets or data, by the key the watch service hands back for it. */
    private static final Map<WatchKey, Boolean> HOLDS_ASSETS = new ConcurrentHashMap<>();
    /*
     * Several runs can share the mods' folders (test clients side by side). A file one of them writes to see itself
     * reload carries that run's mark in its name, and the other runs pay it no mind.
     */
    private static final String RUN_MARK = ".run-";
    private static final String OWN_MARK = RUN_MARK + ProcessHandle.current().pid() + ".";

    private static volatile boolean watching;

    private LiveReload() {
    }

    /** Starts watching, and hands the game's bus the server's reload, in a development run only. */
    public static void listen() {
        if (FMLEnvironment.production) {
            return;
        }
        final List<Path> roots = ModFolders.ofThisRun().values().stream().flatMap(List::stream).toList();
        try {
            final WatchService service = FileSystems.getDefault().newWatchService();
            for (final Path root : roots) {
                register(service, root.resolve("assets"), true);
                register(service, root.resolve("data"), false);
            }
            if (HOLDS_ASSETS.isEmpty()) {
                service.close();
                return;
            }
            final Thread thread = new Thread(() -> watch(service), "jscore-live-reload");
            thread.setDaemon(true);
            thread.start();
            watching = true;
            NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, LiveReload::afterServerTick);
        } catch (final IOException unwatchable) {
            LOGGER.warn("the mods' folders cannot be watched; reload by hand", unwatchable);
        }
    }

    /** Whether this run watches the mods' folders. */
    public static boolean watching() {
        return watching;
    }

    /** How many asset files changed and have since stayed as they are, for the player's game to reload; once. */
    public static int takeAssetChanges() {
        return ASSETS.take(System.currentTimeMillis());
    }

    /** How many times the server reloaded its data packs for a change; for a test to look at. */
    public static int dataReloads() {
        return DATA_RELOADS.get();
    }

    /**
     * The name of a file only this run reloads for, {@code base} and {@code extension} with the run's mark between
     * ({@code live_screen.run-1234.json}): other runs that share the mods' folders leave it alone.
     */
    public static String ownFile(final String base, final String extension) {
        return base + OWN_MARK + extension;
    }

    private static void afterServerTick(final ServerTickEvent.Post event) {
        final int changed = DATA.take(System.currentTimeMillis());
        if (changed > 0) {
            LOGGER.info("{} data file(s) changed; reloading the data packs", changed);
            final MinecraftServer server = event.getServer();
            server.reloadResources(server.getPackRepository().getSelectedIds())
                    .thenRun(DATA_RELOADS::incrementAndGet);
        }
    }

    /** Watches {@code folder} and every folder under it, if it is there. */
    private static void register(final WatchService service, final Path folder, final boolean assets)
            throws IOException {
        if (!Files.isDirectory(folder)) {
            return;
        }
        try (Stream<Path> tree = Files.walk(folder)) {
            for (final Path each : tree.filter(Files::isDirectory).toList()) {
                HOLDS_ASSETS.put(each.register(service, StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE), assets);
            }
        }
    }

    private static void watch(final WatchService service) {
        while (true) {
            final WatchKey key;
            try {
                key = service.take();
            } catch (final InterruptedException | ClosedWatchServiceException stopped) {
                return;
            }
            final Boolean assets = HOLDS_ASSETS.get(key);
            for (final WatchEvent<?> event : key.pollEvents()) {
                if (assets == null || event.kind() == StandardWatchEventKinds.OVERFLOW) {
                    continue;
                }
                final Path changed = ((Path) key.watchable()).resolve((Path) event.context());
                final String name = changed.getFileName().toString();
                // A folder "modified" is a file in it changing, which that folder's own watch reports.
                if (name.contains(RUN_MARK) && !name.contains(OWN_MARK)
                        || event.kind() == StandardWatchEventKinds.ENTRY_MODIFY && Files.isDirectory(changed)) {
                    continue;
                }
                if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE && Files.isDirectory(changed)) {
                    try {
                        register(service, changed, assets);
                    } catch (final IOException unwatchable) {
                        LOGGER.warn("a new folder cannot be watched: {}", changed, unwatchable);
                    }
                }
                (assets ? ASSETS : DATA).changed(System.currentTimeMillis());
            }
            if (!key.reset()) {
                HOLDS_ASSETS.remove(key);
            }
        }
    }
}
