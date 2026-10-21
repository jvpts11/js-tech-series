/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.series;

import dev.jstech.core.client.guide.ManualSiteCommand;
import dev.jstech.core.guide.GuideSite;
import dev.jstech.tests.TestGuide;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

/**
 * The manual written as a website by its command, as a player types it: the front page, the style, the search with
 * the recipes of the world in its words, and the press's page, in the folder the command names.
 */
public final class ManualSiteClientTests {

    private static final int WRITE = 40;

    private ManualSiteClientTests() {
    }

    @ClientTest(timeoutTicks = 200)
    public static void manualSiteCommand_writesTheManualAsAWebsite(final ClientTestContext ctx) {
        final Path folder = ManualSiteCommand.folderOf(TestGuide.MANUAL);
        final long[] started = new long[1];
        ctx.then(0, () -> {
                    started[0] = System.currentTimeMillis();
                    ctx.player().connection.sendCommand("jstech manual site " + TestGuide.MANUAL);
                })
                .thenWaitUntil(() -> writtenSince(folder.resolve(GuideSite.SEARCH), started[0]), WRITE,
                        "the command to write the site")
                .thenAssert(0, () -> Files.isRegularFile(folder.resolve(GuideSite.FRONT))
                        && Files.isRegularFile(folder.resolve(GuideSite.STYLE)), "with its front page and its style")
                .thenAssert(0, () -> Files.isRegularFile(folder.resolve(GuideSite.pathOf(TestGuide.PRESS))),
                        "and the press's page")
                .thenAssert(0, () -> read(folder.resolve(GuideSite.SEARCH)).contains("block of iron"),
                        "and the search finds the press by what its recipes make, read from the world");
    }

    private static boolean writtenSince(final Path file, final long since) {
        try {
            return Files.isRegularFile(file) && Files.getLastModifiedTime(file).compareTo(
                    FileTime.fromMillis(since - 1000)) >= 0;
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String read(final Path file) {
        try {
            return Files.readString(file);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
