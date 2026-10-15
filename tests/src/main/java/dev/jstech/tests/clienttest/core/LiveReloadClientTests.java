/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import dev.jstech.core.client.developer.LiveReloadClient;
import dev.jstech.core.developer.LiveReload;
import dev.jstech.core.developer.ModFolders;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * A development run reloading what changed by itself: a file put into the tests mod's assets has the player's game
 * reload its resources, one put into its data has the server reload its data packs, and taking them away again does
 * the same, each once the files stop changing.
 */
public final class LiveReloadClientTests {

    private LiveReloadClientTests() {
    }

    /*
     * The probe is named for this run, so the clients of a battery that share the tests mod's folders do not reload
     * for it in the middle of their own tests.
     */
    @ClientTest(timeoutTicks = 1200)
    public static void changedFiles_areReloadedByThemselves(final ClientTestContext ctx) {
        final Path root = resourceRoot();
        final String probe = LiveReload.ownFile("live_reload_probe", "json");
        final Path asset = root.resolve("assets").resolve(JsTests.MODID).resolve(probe);
        final Path data = root.resolve("data").resolve(JsTests.MODID).resolve(probe);
        final int[] before = new int[2];
        ctx.afterTest(() -> {
            delete(asset);
            delete(data);
        });
        ctx.then(0, () -> {
                    before[0] = LiveReloadClient.reloads();
                    before[1] = LiveReload.dataReloads();
                    write(asset);
                    write(data);
                })
                .thenWaitUntil(() -> LiveReloadClient.reloads() > before[0] && ctx.mc().getOverlay() == null, 400,
                        "the player's game to reload its resources for the new asset")
                .thenWaitUntil(() -> LiveReload.dataReloads() > before[1], 400,
                        "the server to reload its data packs for the new data")
                .then(0, () -> {
                    delete(asset);
                    delete(data);
                })
                .thenWaitUntil(() -> LiveReloadClient.reloads() > before[0] + 1 && ctx.mc().getOverlay() == null, 400,
                        "the resources to be reloaded again once the asset is gone")
                .thenWaitUntil(() -> LiveReload.dataReloads() > before[1] + 1, 400,
                        "the data packs to be reloaded again once the data is gone");
    }

    /** The tests mod's folder of resources in this run: the one of its folders holding its assets. */
    private static Path resourceRoot() {
        final List<Path> folders = ModFolders.ofThisRun().getOrDefault(JsTests.MODID, List.of());
        for (final Path folder : folders) {
            if (Files.isDirectory(folder.resolve("assets"))) {
                return folder;
            }
        }
        throw new IllegalStateException("this run loads the tests mod from no folder of assets: " + folders);
    }

    private static void write(final Path file) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, "{}");
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void delete(final Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
