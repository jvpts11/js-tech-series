/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import dev.jstech.core.client.developer.LiveReloadClient;
import dev.jstech.core.client.guide.GuideClient;
import dev.jstech.core.client.guide.ManualScreen;
import dev.jstech.core.developer.LiveReload;
import dev.jstech.core.developer.ModFolders;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestGuide;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.client.Minecraft;

/**
 * A development run reloading what changed by itself: a file put into the tests mod's assets has the player's game
 * reload its resources, one put into its data has the server reload its data packs, and taking them away again does
 * the same, each once the files stop changing; a manual's file has the manuals alone read again, and the manual open
 * shows it at once.
 */
public final class LiveReloadClientTests {

    private static final int SETTLE = 4;
    private static final int OPEN = 40;
    /* The entry written into the test manual while it is open, at the end of its chapter of ideas. */
    private static final String PROBE = JsTests.MODID + ":live_reload_probe";
    private static final String PROBE_ENTRY = """
            {"id": "%s", "section": "%s:ideas", "order": 99, "title": "A Page Written While the Game Runs",
             "icon": "minecraft:clock", "blocks": [{"type": "text", "text": "Saved a moment ago, and read at once."}]}
            """.formatted(PROBE, JsTests.MODID);

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

    /*
     * An entry written into the test manual's files while the manual is open: the manuals alone are read again, the
     * open manual holds it at once and its reader stays at the page they were reading, though the pages before it
     * moved; and it leaves the manual once its file is gone.
     */
    @ClientTest(timeoutTicks = 1200)
    public static void changedManualFile_isReadAloneAndTheOpenManualLaysItselfOut(final ClientTestContext ctx) {
        final Path entry = resourceRoot().resolve("assets").resolve(JsTests.MODID).resolve("guide")
                .resolve("entries").resolve(LiveReload.ownFile("live_reload_probe", "json"));
        final int[] before = new int[2];
        ctx.afterTest(() -> delete(entry));
        ctx.then(0, () -> GuideClient.open(TestGuide.MANUAL, TestGuide.PRESS))
                .thenAwaitScreen(ManualScreen.class, OPEN)
                .then(SETTLE, () -> {
                    before[0] = LiveReloadClient.guideReloads();
                    before[1] = LiveReloadClient.reloads();
                    write(entry, PROBE_ENTRY);
                })
                .thenWaitUntil(() -> LiveReloadClient.guideReloads() > before[0], 400,
                        "the manuals to be read again for the new entry")
                .thenAssert(SETTLE, () -> manual().book().pageOf(PROBE).isPresent(), "the open manual holds it")
                .thenAssert(0, () -> manual().showing(manual().book().pageOf(TestGuide.PRESS).orElse(-1)),
                        "and its reader is still at the press's page")
                .thenAssert(0, () -> LiveReloadClient.reloads() == before[1],
                        "without every resource read again for it")
                .thenScreenshot(0, "manual_live_reload")
                .then(0, () -> delete(entry))
                .thenWaitUntil(() -> LiveReloadClient.guideReloads() > before[0] + 1, 400,
                        "the manuals to be read again once the entry is gone")
                .thenAssert(SETTLE, () -> manual().book().pageOf(PROBE).isEmpty(), "and it leaves the manual")
                .then(0, () -> ctx.mc().setScreen(null))
                .thenAwaitNoScreen(OPEN);
    }

    private static ManualScreen manual() {
        return (ManualScreen) Minecraft.getInstance().screen;
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
        write(file, "{}");
    }

    private static void write(final Path file, final String text) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, text);
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
